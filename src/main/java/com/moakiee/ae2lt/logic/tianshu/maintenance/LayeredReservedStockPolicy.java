package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.stacks.AEKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LayeredReservedStockPolicy {
   private final ReservedStockRepository global;
   private final ReservedStockRepository additional;

   public LayeredReservedStockPolicy(ReservedStockRepository global, ReservedStockRepository additional) {
      this.global = global;
      this.additional = additional;
   }

   public boolean isEmpty() {
      return size(this.global) == 0 && size(this.additional) == 0;
   }

   public boolean groupsSecondaryVariants(AEKey key) {
      return groups(this.global, key) || groups(this.additional, key);
   }

   public long usablePreexistingStock(AEKey key, long snapshotAmount) {
      long usable = Math.max(0L, snapshotAmount);
      if (this.global != null) {
         usable = Math.min(usable, this.global.usablePreexistingStock(key, snapshotAmount));
      }

      if (this.additional != null) {
         usable = Math.min(usable, this.additional.usablePreexistingStock(key, snapshotAmount));
      }

      return usable;
   }

   public long usablePreexistingStock(AEKey key, long snapshotAmount, Map<AEKey, Long> groupSnapshot) {
      if (!this.groupsSecondaryVariants(key)) {
         return this.usablePreexistingStock(key, snapshotAmount);
      } else {
         Map<AEKey, Long> stock = normalizedGroup(key, snapshotAmount, groupSnapshot);
         List<AEKey> variants = stock.keySet().stream().sorted(Comparator.comparing(LayeredReservedStockPolicy::stableKey)).toList();
         LinkedHashMap<AEKey, Long> exactCaps = new LinkedHashMap<>();

         for (AEKey variant : variants) {
            long amount = stock.get(variant);
            long usable = Math.min(amount, exactCap(this.global, variant, amount));
            usable = Math.min(usable, exactCap(this.additional, variant, amount));
            exactCaps.put(variant, Long.valueOf(usable));
         }

         ArrayList<LayeredReservedStockPolicy.GroupConstraint> constraints = new ArrayList<>();
         addGroupConstraint(constraints, this.global, variants, stock);
         addGroupConstraint(constraints, this.additional, variants, stock);
         LinkedHashMap<AEKey, Long> allocated = new LinkedHashMap<>();

         for (AEKey variant : variants) {
            long usable = exactCaps.get(variant);

            for (LayeredReservedStockPolicy.GroupConstraint constraint : constraints) {
               if (constraint.includes(variant)) {
                  usable = Math.min(usable, constraint.remaining());
               }
            }

            allocated.put(variant, Long.valueOf(usable));

            for (LayeredReservedStockPolicy.GroupConstraint constraintx : constraints) {
               if (constraintx.includes(variant)) {
                  constraintx.consume(usable);
               }
            }
         }

         return allocated.getOrDefault(key, 0L);
      }
   }

   private static void addGroupConstraint(
      List<LayeredReservedStockPolicy.GroupConstraint> constraints, ReservedStockRepository repository, List<AEKey> variants, Map<AEKey, Long> stock
   ) {
      if (repository != null) {
         ReservedStockRepository.Entry group = null;

         for (ReservedStockRepository.Entry reservation : repository.reservations()) {
            if (reservation.mode() == ReservedStockMatchMode.IGNORE_SECONDARY && variants.stream().anyMatch(variantx -> sameGroup(reservation.key(), variantx))
               )
             {
               group = reservation;
               break;
            }
         }

         if (group != null) {
            ArrayList<AEKey> included = new ArrayList<>();
            long total = 0L;

            for (AEKey variant : variants) {
               if (sameGroup(group.key(), variant) && !hasExact(repository, variant)) {
                  included.add(variant);
                  total = saturatingAdd(total, stock.getOrDefault(variant, 0L));
               }
            }

            long capacity = group.amount() == -1L ? 0L : Math.max(0L, total - group.amount());
            constraints.add(new LayeredReservedStockPolicy.GroupConstraint(List.copyOf(included), capacity));
         }
      }
   }

   private static Map<AEKey, Long> normalizedGroup(AEKey key, long snapshotAmount, Map<AEKey, Long> groupSnapshot) {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();
      if (groupSnapshot != null) {
         groupSnapshot.forEach((variant, amount) -> {
            if (variant != null && sameGroup(key, variant)) {
               result.put(variant, Long.valueOf(Math.max(0L, amount)));
            }
         });
      }

      result.putIfAbsent(key, Long.valueOf(Math.max(0L, snapshotAmount)));
      return result;
   }

   private static long exactCap(ReservedStockRepository repository, AEKey key, long stock) {
      if (repository == null) {
         return stock;
      } else {
         for (ReservedStockRepository.Entry reservation : repository.reservations()) {
            if (reservation.mode() == ReservedStockMatchMode.EXACT && reservation.key().equals(key)) {
               return reservation.amount() == -1L ? 0L : Math.max(0L, stock - reservation.amount());
            }
         }

         return stock;
      }
   }

   private static boolean hasExact(ReservedStockRepository repository, AEKey key) {
      return repository == null
         ? false
         : repository.reservations().stream().anyMatch(reservation -> reservation.mode() == ReservedStockMatchMode.EXACT && reservation.key().equals(key));
   }

   private static boolean groups(ReservedStockRepository repository, AEKey key) {
      return repository != null && repository.groupsSecondaryVariants(key);
   }

   private static int size(ReservedStockRepository repository) {
      return repository != null ? repository.size() : 0;
   }

   private static boolean sameGroup(AEKey left, AEKey right) {
      return left != null && right != null && left.dropSecondary().equals(right.dropSecondary());
   }

   private static String stableKey(AEKey key) {
      return key.getId() + "|" + key.getPrimaryKey() + "|" + key.hashCode();
   }

   private static long saturatingAdd(long left, long right) {
      return left >= Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
   }

   private static final class GroupConstraint {
      private final List<AEKey> included;
      private long remaining;

      private GroupConstraint(List<AEKey> included, long remaining) {
         this.included = included;
         this.remaining = remaining;
      }

      private boolean includes(AEKey key) {
         return this.included.contains(key);
      }

      private long remaining() {
         return this.remaining;
      }

      private void consume(long amount) {
         this.remaining = Math.max(0L, this.remaining - amount);
      }
   }
}
