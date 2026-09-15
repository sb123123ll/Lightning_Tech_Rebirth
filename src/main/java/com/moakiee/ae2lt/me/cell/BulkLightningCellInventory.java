package com.moakiee.ae2lt.me.cell;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.moakiee.ae2lt.item.BulkLightningStorageCellItem;
import com.moakiee.ae2lt.me.key.LightningKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class BulkLightningCellInventory implements StorageCell {
   private final ItemStack stack;
   @Nullable
   private final ISaveProvider saveProvider;
   private final double idleDrain;
   private long highVoltage;
   private long extremeHighVoltage;
   private boolean dirty;

   public BulkLightningCellInventory(ItemStack stack, @Nullable ISaveProvider saveProvider) {
      if (stack.m_41720_() instanceof BulkLightningStorageCellItem cellItem) {
         this.stack = stack;
         this.saveProvider = saveProvider;
         this.idleDrain = cellItem.getIdleDrain();
         BulkLightningStorageCellItem.StoredAmounts var5 = BulkLightningStorageCellItem.readStoredAmounts(stack);
         this.highVoltage = var5.highVoltage();
         this.extremeHighVoltage = var5.extremeHighVoltage();
      } else {
         throw new IllegalArgumentException("Cell is not a bulk lightning storage cell");
      }
   }

   public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (amount > 0L && isSupported(what)) {
         long stored = this.getStored(what);
         long inserted = Math.min(amount, Long.MAX_VALUE - stored);
         if (inserted > 0L && mode == Actionable.MODULATE) {
            this.setStored(what, stored + inserted);
            this.markChanged();
         }

         return inserted;
      } else {
         return 0L;
      }
   }

   public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (amount > 0L && isSupported(what)) {
         long stored = this.getStored(what);
         long extracted = Math.min(amount, stored);
         if (extracted > 0L && mode == Actionable.MODULATE) {
            this.setStored(what, stored - extracted);
            this.markChanged();
         }

         return extracted;
      } else {
         return 0L;
      }
   }

   public void getAvailableStacks(KeyCounter out) {
      if (this.highVoltage > 0L) {
         out.add(LightningKey.HIGH_VOLTAGE, this.highVoltage);
      }

      if (this.extremeHighVoltage > 0L) {
         out.add(LightningKey.EXTREME_HIGH_VOLTAGE, this.extremeHighVoltage);
      }
   }

   public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
      return isSupported(what) && this.getStored(what) > 0L;
   }

   public CellState getStatus() {
      return this.highVoltage == 0L && this.extremeHighVoltage == 0L ? CellState.EMPTY : CellState.NOT_EMPTY;
   }

   public double getIdleDrain() {
      return this.idleDrain;
   }

   public boolean canFitInsideCell() {
      return this.highVoltage == 0L && this.extremeHighVoltage == 0L;
   }

   public void persist() {
      if (this.dirty) {
         BulkLightningStorageCellItem.writeStoredAmounts(this.stack, this.highVoltage, this.extremeHighVoltage);
         this.dirty = false;
      }
   }

   public Component getDescription() {
      return this.stack.m_41786_();
   }

   private static boolean isSupported(AEKey key) {
      return LightningKey.HIGH_VOLTAGE.equals(key) || LightningKey.EXTREME_HIGH_VOLTAGE.equals(key);
   }

   private long getStored(AEKey key) {
      return LightningKey.EXTREME_HIGH_VOLTAGE.equals(key) ? this.extremeHighVoltage : this.highVoltage;
   }

   private void setStored(AEKey key, long amount) {
      if (LightningKey.EXTREME_HIGH_VOLTAGE.equals(key)) {
         this.extremeHighVoltage = amount;
      } else {
         this.highVoltage = amount;
      }
   }

   private void markChanged() {
      this.dirty = true;
      if (this.saveProvider != null) {
         this.saveProvider.saveChanges();
      } else {
         this.persist();
      }
   }
}
