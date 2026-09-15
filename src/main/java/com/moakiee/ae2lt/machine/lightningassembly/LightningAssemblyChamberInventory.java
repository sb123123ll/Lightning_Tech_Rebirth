package com.moakiee.ae2lt.machine.lightningassembly;

import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LightningAssemblyChamberInventory extends LargeStackItemHandler {
   public static final int SLOT_INPUT_0 = 0;
   public static final int SLOT_INPUT_1 = 1;
   public static final int SLOT_INPUT_2 = 2;
   public static final int SLOT_INPUT_3 = 3;
   public static final int SLOT_INPUT_4 = 4;
   public static final int SLOT_INPUT_5 = 5;
   public static final int SLOT_INPUT_6 = 6;
   public static final int SLOT_INPUT_7 = 7;
   public static final int SLOT_INPUT_8 = 8;
   public static final int SLOT_CATALYST = 9;
   public static final int SLOT_OUTPUT = 10;
   public static final int SLOT_COUNT = 11;
   public static final int LARGE_SLOT_LIMIT = 8192;
   public static final int MATRIX_SLOT_LIMIT = 1;

   public LightningAssemblyChamberInventory(@Nullable Runnable changeListener) {
      super(11, changeListener);
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);
      return LightningAssemblySlotLimits.getSlotLimit(slot);
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      if (stack.m_41619_()) {
         return false;
      } else if (slot == 10) {
         return false;
      } else {
         return slot == 9 ? this.isCatalystItem(stack) : this.isInputSlot(slot);
      }
   }

   public boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 8;
   }

   public boolean isLightningCollapseMatrix(ItemStack stack) {
      return stack.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
   }

   public boolean isCatalystItem(ItemStack stack) {
      return this.isLightningCollapseMatrix(stack);
   }

   public boolean hasLightningCollapseMatrix() {
      return this.isLightningCollapseMatrix(this.getStackInSlot(9));
   }

   public ItemStack insertRecipeOutput(ItemStack stack, boolean simulate) {
      return this.insertItemUnchecked(10, stack, simulate);
   }

   public boolean canAcceptRecipeOutput(ItemStack stack) {
      return this.insertRecipeOutput(stack, true).m_41619_();
   }

   public void setClientRenderStack(int slot, ItemStack stack) {
      this.setStackInSlotUnchecked(slot, stack);
   }
}
