package com.moakiee.ae2lt.me.cell;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class FixedInfiniteCellInventory implements StorageCell {
   private final ItemStack stack;
   private final AEKey storedKey;
   private final double idleDrain;
   private final ISaveProvider host;
   private final boolean finiteOuter;

   public FixedInfiniteCellInventory(ItemStack stack, double idleDrain, ISaveProvider host) {
      if (!(stack.m_41720_() instanceof FixedInfiniteCellItem)) {
         throw new IllegalArgumentException("Cell isn't a fixed infinite cell");
      } else {
         this.stack = stack;
         this.storedKey = FixedInfiniteCellItem.getEffectiveKey(stack);
         this.idleDrain = idleDrain;
         this.host = host;
         this.finiteOuter = FixedInfiniteCellItem.isOuterCell(stack);
      }
   }

   public CellState getStatus() {
      return this.isConsumed() ? CellState.EMPTY : CellState.NOT_EMPTY;
   }

   public double getIdleDrain() {
      return this.idleDrain;
   }

   public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (this.finiteOuter) {
         return 0L;
      } else {
         return this.storedKey.equals(what) ? amount : 0L;
      }
   }

   public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (this.finiteOuter) {
         if (!this.isConsumed() && this.storedKey.equals(what) && amount > 0L) {
            long extracted = Math.min(amount, (long)what.getAmountPerUnit());
            if (mode == Actionable.MODULATE && extracted > 0L) {
               FixedInfiniteCellItem.setResultConsumed(this.stack, true);
               if (this.host != null) {
                  this.host.saveChanges();
               }
            }

            return extracted;
         } else {
            return 0L;
         }
      } else {
         return this.storedKey.equals(what) ? amount : 0L;
      }
   }

   public void persist() {
   }

   public Component getDescription() {
      return this.stack.m_41786_();
   }

   public void getAvailableStacks(KeyCounter out) {
      if (!this.isConsumed()) {
         out.add(this.storedKey, this.finiteOuter ? (long)this.storedKey.getAmountPerUnit() : getInfiniteAmount(this.storedKey));
      }
   }

   public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
      return !this.isConsumed() && this.storedKey.equals(what);
   }

   private static long getInfiniteAmount(AEKey key) {
      return 2147483647L * (long)key.getAmountPerUnit();
   }

   private boolean isConsumed() {
      return this.finiteOuter && FixedInfiniteCellItem.isResultConsumed(this.stack);
   }
}
