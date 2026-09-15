package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class ReservedStockRepository {
   public static final long INFINITE = -1L;
   private static final String TAG_ENTRIES = "Entries";
   private final IntSupplier capacity;
   private final LinkedHashMap<AEKey, ReservedStockRepository.Entry> reserves = new LinkedHashMap<>();

   public ReservedStockRepository(IntSupplier capacity) {
      this.capacity = capacity;
   }

   public int capacity() {
      return Math.max(0, this.capacity.getAsInt());
   }

   public int size() {
      return this.reserves.size();
   }

   public long reserve(AEKey key) {
      ReservedStockRepository.Entry entry = this.reservationFor(key);
      return entry != null ? entry.amount() : 0L;
   }

   public ReservedStockMatchMode matchMode(AEKey key) {
      ReservedStockRepository.Entry entry = this.reservationFor(key);
      return entry != null ? entry.mode() : ReservedStockMatchMode.EXACT;
   }

   public Map<AEKey, Long> entries() {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();
      this.reserves.forEach((key, entry) -> result.put(key, Long.valueOf(entry.amount())));
      return Map.copyOf(result);
   }

   public List<ReservedStockRepository.Entry> reservations() {
      return List.copyOf(this.reserves.values());
   }

   public List<ReservedStockRepository.Entry> reservations(int limit) {
      return limit <= 0 ? List.of() : this.reserves.values().stream().limit((long)limit).toList();
   }

   public ReservedStockRepository.PutResult set(AEKey key, long amount) {
      return this.set(key, ReservedStockMatchMode.EXACT, amount);
   }

   public ReservedStockRepository.PutResult set(AEKey key, ReservedStockMatchMode mode, long amount) {
      if (key == null || amount < -1L) {
         return ReservedStockRepository.PutResult.INVALID;
      } else if (mode == null) {
         return ReservedStockRepository.PutResult.INVALID;
      } else if (amount == 0L) {
         boolean removed = this.reserves.remove(key) != null;
         if (mode == ReservedStockMatchMode.IGNORE_SECONDARY) {
            removed |= this.reserves.entrySet().removeIf(entry -> entry.getValue().mode() == mode && sameSecondaryGroup(entry.getKey(), key));
         }

         return removed ? ReservedStockRepository.PutResult.REMOVED : ReservedStockRepository.PutResult.REMOVED;
      } else if (this.reserves.size() > this.capacity()) {
         return ReservedStockRepository.PutResult.FULL;
      } else {
         AEKey storageKey = key;
         if (mode == ReservedStockMatchMode.IGNORE_SECONDARY) {
            for (Map.Entry<AEKey, ReservedStockRepository.Entry> existing : this.reserves.entrySet()) {
               if (existing.getValue().mode() == mode && sameSecondaryGroup(existing.getKey(), key)) {
                  storageKey = existing.getKey();
                  break;
               }
            }
         }

         if (!this.reserves.containsKey(storageKey) && this.reserves.size() >= this.capacity()) {
            return this.capacity() <= 0 ? ReservedStockRepository.PutResult.UNAVAILABLE : ReservedStockRepository.PutResult.FULL;
         } else {
            boolean update = this.reserves.containsKey(storageKey);
            this.reserves.put(storageKey, new ReservedStockRepository.Entry(storageKey, amount, mode));
            return update ? ReservedStockRepository.PutResult.UPDATED : ReservedStockRepository.PutResult.ADDED;
         }
      }
   }

   public long usablePreexistingStock(AEKey key, long snapshotAmount) {
      long stock = Math.max(0L, snapshotAmount);
      ReservedStockRepository.Entry reservation = this.reservationFor(key);
      if (reservation != null && reservation.mode() == ReservedStockMatchMode.IGNORE_SECONDARY) {
         return this.usablePreexistingStock(key, stock, Map.of(key, stock));
      } else {
         long reserve = reservation != null ? reservation.amount() : 0L;
         if (reserve == -1L) {
            return 0L;
         } else {
            return reserve <= 0L ? stock : Math.max(0L, stock - reserve);
         }
      }
   }

   public boolean groupsSecondaryVariants(AEKey key) {
      ReservedStockRepository.Entry reservation = this.reservationFor(key);
      return reservation != null && reservation.mode() == ReservedStockMatchMode.IGNORE_SECONDARY;
   }

   public long usablePreexistingStock(AEKey exactVariant, long exactAmount, Map<AEKey, Long> groupSnapshot) {
      ReservedStockRepository.Entry reservation = this.reservationFor(exactVariant);
      if (reservation != null && reservation.mode() == ReservedStockMatchMode.IGNORE_SECONDARY) {
         long total = 0L;

         for (Map.Entry<AEKey, Long> entry : groupSnapshot.entrySet()) {
            if (this.belongsToGroupReservation(reservation, entry.getKey())) {
               total = saturatingAdd(total, Math.max(0L, entry.getValue()));
            }
         }

         long usableTotal = reservation.amount() == -1L ? 0L : Math.max(0L, total - reservation.amount());
         List<Map.Entry<AEKey, Long>> variants = groupSnapshot.entrySet()
            .stream()
            .filter(entryx -> this.belongsToGroupReservation(reservation, (AEKey)entryx.getKey()))
            .sorted(Comparator.comparing(entryx -> stableKey((AEKey)entryx.getKey())))
            .toList();
         long remaining = usableTotal;

         for (Map.Entry<AEKey, Long> variant : variants) {
            long usable = Math.min(Math.max(0L, variant.getValue()), remaining);
            if (variant.getKey().equals(exactVariant)) {
               return usable;
            }

            remaining -= usable;
         }

         return 0L;
      } else {
         return this.usablePreexistingStock(exactVariant, exactAmount);
      }
   }

   public void writeTo(CompoundTag parent, Provider registries) {
      ListTag list = new ListTag();

      for (ReservedStockRepository.Entry entry : this.reserves.values()) {
         CompoundTag tag = GenericStack.writeTag(new GenericStack(entry.key(), 1L));
         tag.m_128356_("Reserve", entry.amount());
         tag.m_128359_("Mode", entry.mode().name());
         list.add(tag);
      }

      parent.m_128365_("Entries", list);
   }

   public void readFrom(CompoundTag parent, Provider registries) {
      this.reserves.clear();
      ListTag list = parent.m_128437_("Entries", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag tag = list.m_128728_(i);
         GenericStack stack = GenericStack.readTag(tag);
         long amount = tag.m_128454_("Reserve");

         ReservedStockMatchMode mode;
         try {
            mode = ReservedStockMatchMode.valueOf(tag.m_128461_("Mode"));
         } catch (IllegalArgumentException var11) {
            mode = ReservedStockMatchMode.EXACT;
         }

         if (stack != null && (amount == -1L || amount > 0L)) {
            this.reserves.put(stack.what(), new ReservedStockRepository.Entry(stack.what(), amount, mode));
         }
      }
   }

   private ReservedStockRepository.Entry reservationFor(AEKey key) {
      if (key == null) {
         return null;
      } else {
         ReservedStockRepository.Entry exact = this.reserves.get(key);
         if (exact != null) {
            return exact;
         } else {
            for (ReservedStockRepository.Entry entry : this.reserves.values()) {
               if (entry.mode() == ReservedStockMatchMode.IGNORE_SECONDARY && sameSecondaryGroup(entry.key(), key)) {
                  return entry;
               }
            }

            return null;
         }
      }
   }

   private boolean belongsToGroupReservation(ReservedStockRepository.Entry groupReservation, AEKey candidate) {
      if (!sameSecondaryGroup(groupReservation.key(), candidate)) {
         return false;
      } else {
         ReservedStockRepository.Entry exact = this.reserves.get(candidate);
         return exact == null || exact == groupReservation || exact.mode() != ReservedStockMatchMode.EXACT;
      }
   }

   private static boolean sameSecondaryGroup(AEKey left, AEKey right) {
      return left != null && right != null && left.dropSecondary().equals(right.dropSecondary());
   }

   private static String stableKey(AEKey key) {
      return key.getId() + "|" + key.getPrimaryKey() + "|" + key.hashCode();
   }

   private static long saturatingAdd(long left, long right) {
      return left >= Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
   }

   public static record Entry(AEKey key, long amount, ReservedStockMatchMode mode) {
   }

   public static enum PutResult {
      ADDED,
      UPDATED,
      REMOVED,
      FULL,
      UNAVAILABLE,
      INVALID;
   }
}
