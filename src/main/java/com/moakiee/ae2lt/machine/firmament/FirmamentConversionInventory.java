package com.moakiee.ae2lt.machine.firmament;

import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FirmamentConversionInventory extends LargeStackItemHandler {
   public static final int SLOT_INPUT_0 = 0;
   public static final int SLOT_INPUT_1 = 1;
   public static final int SLOT_INPUT_2 = 2;
   public static final int SLOT_OUTPUT_0 = 3;
   public static final int SLOT_OUTPUT_1 = 4;
   public static final int SLOT_OUTPUT_2 = 5;
   public static final int SLOT_OUTPUT_3 = 6;
   public static final int SLOT_OUTPUT = 3;
   public static final int INPUT_SLOT_COUNT = 3;
   public static final int OUTPUT_SLOT_COUNT = 4;
   public static final int SLOT_COUNT = 7;
   public static final int SLOT_LIMIT = 64;

   public FirmamentConversionInventory(@Nullable Runnable changeListener) {
      super(7, changeListener);
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);
      return 64;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      return stack.m_41619_() ? false : this.isInputSlot(slot);
   }

   public boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 2;
   }

   public boolean isOutputSlot(int slot) {
      return slot >= 3 && slot <= 6;
   }

   public ItemStack insertRecipeOutput(ItemStack stack, boolean simulate) {
      return this.insertIntoOutputs(stack, simulate);
   }

   public boolean canAcceptRecipeOutput(ItemStack stack) {
      return this.insertRecipeOutput(stack, true).m_41619_();
   }

   public boolean canAcceptRecipeOutputs(List<ItemStack> outputs) {
      if (outputs.isEmpty()) {
         return false;
      } else {
         ItemStack[] simulated = new ItemStack[4];

         for (int index = 0; index < 4; index++) {
            simulated[index] = this.getStackInSlot(3 + index).m_41777_();
         }

         for (ItemStack stack : outputs) {
            if (stack.m_41619_() || !this.insertIntoOutputArray(simulated, stack).m_41619_()) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean insertRecipeOutputs(List<ItemStack> outputs) {
      if (!this.canAcceptRecipeOutputs(outputs)) {
         return false;
      } else {
         for (ItemStack stack : outputs) {
            if (!this.insertIntoOutputs(stack, false).m_41619_()) {
               return false;
            }
         }

         return true;
      }
   }

   private ItemStack insertIntoOutputs(ItemStack stack, boolean simulate) {
      ItemStack remainder = stack;

      for (int slot = 3; slot <= 6; slot++) {
         if (!this.getStackInSlot(slot).m_41619_()) {
            remainder = this.insertItemUnchecked(slot, remainder, simulate);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      for (int slotx = 3; slotx <= 6; slotx++) {
         if (this.getStackInSlot(slotx).m_41619_()) {
            remainder = this.insertItemUnchecked(slotx, remainder, simulate);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      return remainder;
   }

   private ItemStack insertIntoOutputArray(ItemStack[] simulated, ItemStack stack) {
      ItemStack remainder = stack.m_41777_();

      for (int index = 0; index < simulated.length; index++) {
         ItemStack existing = simulated[index];
         if (!existing.m_41619_() && ItemStack.m_150942_(existing, remainder)) {
            int freeSpace = 64 - existing.m_41613_();
            if (freeSpace > 0) {
               int toInsert = Math.min(freeSpace, remainder.m_41613_());
               simulated[index] = existing.m_255036_(existing.m_41613_() + toInsert);
               if (toInsert == remainder.m_41613_()) {
                  return ItemStack.f_41583_;
               }

               remainder.m_41774_(toInsert);
            }
         }
      }

      for (int indexx = 0; indexx < simulated.length; indexx++) {
         if (simulated[indexx].m_41619_()) {
            int toInsert = Math.min(64, remainder.m_41613_());
            simulated[indexx] = remainder.m_255036_(toInsert);
            if (toInsert == remainder.m_41613_()) {
               return ItemStack.f_41583_;
            }

            remainder.m_41774_(toInsert);
         }
      }

      return remainder;
   }
}
