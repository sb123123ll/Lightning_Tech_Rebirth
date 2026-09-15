package com.moakiee.ae2lt.machine.atmosphericionizer;

import com.moakiee.ae2lt.item.WeatherCondensateItem;
import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AtmosphericIonizerInventory extends LargeStackItemHandler {
   public static final int SLOT_CONDENSATE = 0;
   public static final int SLOT_COUNT = 1;

   public AtmosphericIonizerInventory(@Nullable Runnable changeListener) {
      super(1, changeListener);
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);
      return 1;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      return !stack.m_41619_() && stack.m_41720_() instanceof WeatherCondensateItem;
   }
}
