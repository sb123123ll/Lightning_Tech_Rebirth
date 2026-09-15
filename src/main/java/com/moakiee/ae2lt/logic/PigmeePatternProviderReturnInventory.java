package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.helpers.externalstorage.GenericStackInv;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PigmeePatternProviderReturnInventory extends GenericStackInv {
   public static final int SLOT_COUNT = 9;
   private boolean draining;

   public PigmeePatternProviderReturnInventory(Runnable listener) {
      super(listener, 9);
      this.useRegisteredCapacities();
   }

   public boolean canExtract() {
      return false;
   }

   public boolean canInsert() {
      return !this.draining;
   }

   public boolean drainInto(MEStorage storage, IActionSource source) {
      boolean changed = false;
      this.draining = true;

      try {
         for (int slot = 0; slot < this.size(); slot++) {
            GenericStack stack = this.getStack(slot);
            if (stack != null) {
               long inserted = storage.insert(stack.what(), stack.amount(), Actionable.MODULATE, source);
               if (inserted > 0L) {
                  long remaining = stack.amount() - inserted;
                  this.setStack(slot, remaining > 0L ? new GenericStack(stack.what(), remaining) : null);
                  changed = true;
               }
            }
         }
      } finally {
         this.draining = false;
      }

      return changed;
   }

   public void addDrops(List<ItemStack> drops, Level level, BlockPos pos) {
      for (int slot = 0; slot < this.size(); slot++) {
         GenericStack stack = this.getStack(slot);
         if (stack != null) {
            stack.what().addDrops(stack.amount(), drops, level, pos);
         }
      }
   }
}
