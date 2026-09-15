package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import org.jetbrains.annotations.Nullable;

public class UnlimitedReturnInventory extends PatternProviderReturnInventory {
   @Nullable
   private ReturnSlotFilter slotFilter;

   private UnlimitedReturnInventory(Runnable listener) {
      super(listener);
   }

   public static UnlimitedReturnInventory create(Runnable listener, @Nullable ReturnSlotFilter filter) {
      UnlimitedReturnInventory inv = new UnlimitedReturnInventory(listener);
      inv.slotFilter = filter;
      return inv;
   }

   public static UnlimitedReturnInventory create(Runnable listener, @Nullable ReturnSlotFilter filter, int slots) {
      int saved = PatternProviderReturnInventory.NUMBER_OF_SLOTS;

      UnlimitedReturnInventory inv;
      try {
         PatternProviderReturnInventory.NUMBER_OF_SLOTS = slots;
         inv = new UnlimitedReturnInventory(listener);
      } finally {
         PatternProviderReturnInventory.NUMBER_OF_SLOTS = saved;
      }

      inv.slotFilter = filter;
      return inv;
   }

   public long insert(int slot, AEKey what, long amount, Actionable mode) {
      if (what != null && amount > 0L) {
         if (!this.isAllowed(what)) {
            return 0L;
         } else if (this.slotFilter != null && !this.slotFilter.isAllowed(slot, what)) {
            return 0L;
         } else {
            for (int i = 0; i < this.size(); i++) {
               if (what.equals(this.getKey(i))) {
                  if (mode == Actionable.MODULATE) {
                     this.setStack(i, new GenericStack(what, this.getAmount(i) + amount));
                  }

                  return amount;
               }
            }

            for (int ix = 0; ix < this.size(); ix++) {
               if (this.getKey(ix) == null) {
                  if (mode == Actionable.MODULATE) {
                     this.setStack(ix, new GenericStack(what, amount));
                  }

                  return amount;
               }
            }

            return 0L;
         }
      } else {
         return 0L;
      }
   }

   public long extract(int slot, AEKey what, long amount, Actionable mode) {
      return 0L;
   }

   public boolean canExtract() {
      return false;
   }

   public long getMaxAmount(AEKey key) {
      return Long.MAX_VALUE;
   }

   public long getCapacity(AEKeyType space) {
      return Long.MAX_VALUE;
   }
}
