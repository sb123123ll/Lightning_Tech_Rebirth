package com.moakiee.ae2lt.me.cell;

import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.IPartitionList.Builder;
import com.moakiee.ae2lt.item.InfiniteStorageCellItem;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InfiniteCellInventory implements StorageCell {
   private static final String TAG_CELL_ID = "ae2lt:cell_id";
   private final ItemStack stack;
   @Nullable
   private final ISaveProvider saveProvider;
   private final IndexedStorage storage;
   private final ByteTracker byteTracker;
   private final double idleDrain;
   private final IPartitionList partitionList;
   private final IncludeExclude partitionMode;
   private UUID cellId;
   private long lastSyncModCount = -1L;
   private int lastWrittenTypes = -1;
   private long lastWrittenBytes = -1L;

   private InfiniteCellInventory(
      ItemStack stack, @Nullable ISaveProvider saveProvider, int bytesPerType, int maxTypes, long capacityLo, long capacityHi, double idleDrain
   ) {
      this.stack = stack;
      this.saveProvider = saveProvider;
      this.idleDrain = idleDrain;
      this.cellId = this.readCellId();
      InfiniteCellSavedData savedData = InfiniteCellSavedData.getOrNull();
      if (this.cellId != null && savedData != null) {
         this.storage = savedData.getOrCreateStorage(this.cellId);
      } else {
         this.storage = new IndexedStorage();
      }

      this.byteTracker = new ByteTracker(this.storage::getTotalTypes);
      this.byteTracker.configure(bytesPerType, maxTypes, capacityLo, capacityHi);
      this.syncByteTracker();

      if (stack.m_41720_() instanceof InfiniteStorageCellItem cellItem) {
         Builder builder = IPartitionList.builder();
         IUpgradeInventory upgrades = cellItem.getUpgrades(stack);
         if (upgrades.isInstalled(AEItems.FUZZY_CARD)) {
            builder.fuzzyMode(cellItem.getFuzzyMode(stack));
         }
         builder.addAll(cellItem.getConfigInventory(stack).keySet());
         this.partitionMode = upgrades.isInstalled(AEItems.INVERTER_CARD) ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST;
         this.partitionList = builder.build();
      } else {
         this.partitionList = null;
         this.partitionMode = IncludeExclude.WHITELIST;
      }
   }

   private void syncByteTracker() {
      this.byteTracker.rebuild(this.storage.getTypeAmountLo(), this.storage.getTypeAmountHi(), this.storage.getTypeCounts(), this.storage.getTotalTypes());
      this.lastSyncModCount = this.storage.getModCount();
   }

   private void ensureSync() {
      if (this.storage.getModCount() != this.lastSyncModCount) {
         this.syncByteTracker();
      }
   }

   public static InfiniteCellInventory create(
      ItemStack stack, @Nullable ISaveProvider saveProvider, int bytesPerType, int maxTypes, long capacityLo, long capacityHi, double idleDrain
   ) {
      return new InfiniteCellInventory(stack, saveProvider, bytesPerType, maxTypes, capacityLo, capacityHi, idleDrain);
   }

   public static InfiniteCellInventory create(ItemStack stack, int bytesPerType, int maxTypes, long capacity, double idleDrain) {
      return create(stack, null, bytesPerType, maxTypes, capacity, 0L, idleDrain);
   }

   public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (amount <= 0L) {
         return 0L;
      } else if (this.partitionList != null && !this.partitionList.isEmpty() && !this.partitionList.matchesFilter(what, this.partitionMode)) {
         return 0L;
      } else {
         this.ensureSync();
         boolean isNewKey = !this.storage.containsKey(what);
         long maxInsertable = this.byteTracker.computeMaxInsertable(what.getType(), isNewKey);
         if (maxInsertable <= 0L) {
            return 0L;
         } else {
            long toInsert = Math.min(amount, maxInsertable);
            if (mode == Actionable.SIMULATE) {
               return toInsert;
            } else {
               this.storage.insert(what, toInsert, Actionable.MODULATE);
               this.byteTracker.onInsert(what.getType(), toInsert, isNewKey);
               this.lastSyncModCount = this.storage.getModCount();
               this.syncSummary();
               this.markChanged();
               return toInsert;
            }
         }
      }
   }

   public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (amount <= 0L) {
         return 0L;
      } else {
         this.ensureSync();
         if (mode == Actionable.SIMULATE) {
            return this.storage.extract(what, amount, Actionable.SIMULATE);
         } else {
            long taken = this.storage.extract(what, amount, Actionable.MODULATE);
            if (taken > 0L) {
               boolean keyRemoved = !this.storage.containsKey(what);
               this.byteTracker.onExtract(what.getType(), taken, keyRemoved);
               this.lastSyncModCount = this.storage.getModCount();
               this.syncSummary();
               this.markChanged();
            }

            return taken;
         }
      }
   }

   public void getAvailableStacks(KeyCounter out) {
      this.storage.getAvailableStacks(out);
   }

   public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
      if (this.partitionList != null && !this.partitionList.isEmpty()) {
         return this.partitionList.matchesFilter(what, this.partitionMode);
      } else {
         return this.storage.containsKey(what);
      }
   }

   public Component getDescription() {
      return this.stack.m_41786_();
   }

   public CellState getStatus() {
      this.ensureSync();
      if (this.storage.getTotalTypes() == 0) {
         return CellState.EMPTY;
      } else if (this.byteTracker.isFull()) {
         return CellState.FULL;
      } else if (this.byteTracker.isTypeFull()) {
         return CellState.TYPES_FULL;
      } else {
         return CellState.NOT_EMPTY;
      }
   }

   public double getIdleDrain() {
      return this.idleDrain;
   }

   public boolean canFitInsideCell() {
      this.ensureSync();
      return this.storage.getTotalTypes() == 0;
   }

   public void persist() {
      InfiniteCellSavedData savedData = InfiniteCellSavedData.getOrNull();
      if (savedData != null) {
         if (this.storage.getTotalTypes() == 0) {
            if (this.storage.needsPersist()) {
               this.storage.persist(null);
            }

            if (this.cellId != null) {
               savedData.removeCell(this.cellId);
               this.clearCellId();
               this.cellId = null;
            }

            this.syncSummary();
         } else if (this.storage.needsPersist()) {
            if (this.cellId == null) {
               this.cellId = UUID.randomUUID();
               this.writeCellId(this.cellId);
            }

            savedData.persistStorage(this.cellId, this.storage);
            this.ensureSync();
            this.syncSummary();
         }
      }
   }

   public long getUsedBytes() {
      this.ensureSync();
      return this.byteTracker.getUsedBytes();
   }

   public int getTotalTypes() {
      return this.storage.getTotalTypes();
   }

   @Nullable
   private UUID readCellId() {
      CompoundTag tag = ItemStackTagSupport.getTagCopy(this.stack);
      return !tag.m_128403_("ae2lt:cell_id") ? null : tag.m_128342_("ae2lt:cell_id");
   }

   private void writeCellId(UUID id) {
      ItemStackTagSupport.updateTag(this.stack, tag -> tag.m_128362_("ae2lt:cell_id", id));
   }

   private void clearCellId() {
      ItemStackTagSupport.updateTag(this.stack, tag -> tag.m_128473_("ae2lt:cell_id"));
   }

   private void markChanged() {
      if (this.storage.getTotalTypes() == 0) {
         this.persist();
      } else {
         InfiniteCellSavedData savedData = InfiniteCellSavedData.getOrNull();
         if (savedData != null) {
            this.ensureCellId();
            savedData.markStorageDirty(this.cellId, this.storage);
         }
      }

      if (this.saveProvider != null) {
         this.saveProvider.saveChanges();
      }
   }

   private void ensureCellId() {
      if (this.cellId == null) {
         this.cellId = UUID.randomUUID();
         this.writeCellId(this.cellId);
      }
   }

   private void syncSummary() {
      int t = this.storage.getTotalTypes();
      long b = this.byteTracker.getUsedBytes();
      if (t == 0) {
         b = 0L;
      }
      if (t != this.lastWrittenTypes || b != this.lastWrittenBytes) {
         this.lastWrittenTypes = t;
         this.lastWrittenBytes = b;
         long finalB = b;
         ItemStackTagSupport.updateTag(this.stack, tag -> {
            tag.m_128405_("ae2lt:types", t);
            tag.m_128356_("ae2lt:bytes", finalB);
         });
      }
   }
}
