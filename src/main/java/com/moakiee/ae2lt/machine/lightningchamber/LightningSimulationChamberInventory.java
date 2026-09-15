package com.moakiee.ae2lt.machine.lightningchamber;

import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LightningSimulationChamberInventory extends LargeStackItemHandler {
   public static final int SLOT_INPUT_0 = 0;
   public static final int SLOT_INPUT_1 = 1;
   public static final int SLOT_INPUT_2 = 2;
   public static final int SLOT_CATALYST = 3;
   public static final int SLOT_OUTPUT = 4;
   public static final int SLOT_COUNT = 5;
   public static final int LARGE_SLOT_LIMIT = 8192;
   public static final int MATRIX_SLOT_LIMIT = 1;

   public LightningSimulationChamberInventory(@Nullable Runnable changeListener) {
      super(5, changeListener);
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);
      return slot == 3 ? 1 : 8192;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      if (stack.m_41619_()) {
         return false;
      } else if (slot == 4) {
         return false;
      } else {
         return slot == 3 ? this.isCatalystItem(stack) : this.isInputSlot(slot);
      }
   }

   public boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 2;
   }

   public boolean isLightningCollapseMatrix(ItemStack stack) {
      return stack.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
   }

   public boolean isCatalystItem(ItemStack stack) {
      return this.isLightningCollapseMatrix(stack);
   }

   public boolean hasLightningCollapseMatrix() {
      return this.isLightningCollapseMatrix(this.getStackInSlot(3));
   }

   public ItemStack insertRecipeOutput(ItemStack stack, boolean simulate) {
      return this.insertItemUnchecked(4, stack, simulate);
   }

   public boolean canAcceptRecipeOutput(ItemStack stack) {
      return this.insertRecipeOutput(stack, true).m_41619_();
   }

   public void setClientRenderStack(int slot, ItemStack stack) {
      this.setStackInSlotUnchecked(slot, stack);
   }
}
