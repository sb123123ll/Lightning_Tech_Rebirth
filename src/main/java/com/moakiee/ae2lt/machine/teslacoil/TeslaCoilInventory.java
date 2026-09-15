package com.moakiee.ae2lt.machine.teslacoil;

import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class TeslaCoilInventory extends LargeStackItemHandler {
   public static final int SLOT_DUST = 0;
   public static final int SLOT_MATRIX = 1;
   public static final int SLOT_COUNT = 2;
   public static final int DUST_SLOT_LIMIT = 1024;

   public TeslaCoilInventory(@Nullable Runnable changeListener) {
      super(2, changeListener);
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);
      return slot == 1 ? 1 : 1024;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      if (stack.m_41619_()) {
         return false;
      } else {
         return switch (slot) {
            case 0 -> this.isOverloadCrystalDust(stack);
            case 1 -> this.isLightningCollapseMatrix(stack);
            default -> false;
         };
      }
   }

   public boolean isOverloadCrystalDust(ItemStack stack) {
      return stack.m_150930_((Item)ModItems.OVERLOAD_CRYSTAL_DUST.get());
   }

   public boolean isLightningCollapseMatrix(ItemStack stack) {
      return stack.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
   }

   public boolean hasRequiredDust(int amount) {
      return this.getStackInSlot(0).m_41613_() >= amount;
   }

   public boolean hasMatrix() {
      return !this.getStackInSlot(1).m_41619_();
   }
}
