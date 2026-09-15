package com.moakiee.ae2lt.machine.lightningcollector;

import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LightningCollectorInventory extends LargeStackItemHandler {
   public static final int SLOT_CRYSTAL = 0;
   public static final int SLOT_COUNT = 1;

   public LightningCollectorInventory(@Nullable Runnable changeListener) {
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
      return slot == 0 && (stack.m_150930_((Item)ModItems.ELECTRO_CHIME_CRYSTAL.get()) || stack.m_150930_((Item)ModItems.PERFECT_ELECTRO_CHIME_CRYSTAL.get()));
   }

   public void setClientRenderStack(ItemStack stack) {
      this.setStackInSlotUnchecked(0, stack);
   }
}
