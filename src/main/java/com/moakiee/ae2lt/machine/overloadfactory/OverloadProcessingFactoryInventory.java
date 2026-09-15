package com.moakiee.ae2lt.machine.overloadfactory;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.machine.lightningchamber.LargeStackItemHandler;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OverloadProcessingFactoryInventory extends LargeStackItemHandler {
   public static final int SLOT_INPUT_0 = 0;
   public static final int SLOT_INPUT_8 = 8;
   public static final int SLOT_MATRIX = 9;
   public static final int SLOT_OUTPUT_0 = 10;
   public static final int SLOT_COUNT = 11;
   public static final int INPUT_SLOT_COUNT = 9;
   public static final int OUTPUT_SLOT_COUNT = 1;
   public static final int LARGE_SLOT_LIMIT = 16384;
   public static final int MATRIX_SLOT_LIMIT = 32;

   public OverloadProcessingFactoryInventory(@Nullable Runnable changeListener) {
      super(11, changeListener);
   }

   @Override
   public int getSlotLimit(int slot) {
      this.validateSlotIndex(slot);
      return slot == 9 ? 32 : 16384;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      if (stack.m_41619_()) {
         return false;
      } else if (this.isOutputSlot(slot)) {
         return false;
      } else {
         return slot == 9 ? this.isLightningCollapseMatrix(stack) : this.isInputSlot(slot);
      }
   }

   public boolean isInputSlot(int slot) {
      return slot >= 0 && slot <= 8;
   }

   public boolean isOutputSlot(int slot) {
      return slot == 10;
   }

   public boolean isLightningCollapseMatrix(ItemStack stack) {
      return stack.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get());
   }

   public int getInstalledMatrixCount() {
      ItemStack stack = this.getStackInSlot(9);
      return this.isLightningCollapseMatrix(stack) ? Math.min(32, stack.m_41613_()) : 0;
   }

   public boolean hasLightningCollapseMatrix() {
      return this.getInstalledMatrixCount() > 0;
   }

   public int getInstalledParallelCapacity() {
      int matrixCount = this.getInstalledMatrixCount();
      return matrixCount <= 0 ? 1 : getMaxParallelForMatrixCount(matrixCount);
   }

   public static int getMaxParallelForMatrixCount(int matrixCount) {
      if (matrixCount <= 0) {
         return 0;
      } else {
         int parallelPerMatrix = AE2LTCommonConfig.overloadFactoryParallelPerMatrix();
         int maxParallel = getMaxParallel();
         long scaledParallel = (long)matrixCount * (long)parallelPerMatrix;
         return (int)Math.min((long)maxParallel, Math.min(2147483647L, scaledParallel));
      }
   }

   public static int getMaxParallel() {
      long maxParallel = 32L * (long)AE2LTCommonConfig.overloadFactoryParallelPerMatrix();
      return (int)Math.min(2147483647L, maxParallel);
   }

   public boolean canAcceptRecipeOutputs(List<ItemStack> outputs) {
      ItemStack[] simulated = new ItemStack[1];

      for (int index = 0; index < 1; index++) {
         simulated[index] = this.getStackInSlot(10 + index).m_41777_();
      }

      for (ItemStack stack : outputs) {
         if (stack.m_41619_() || !this.insertIntoOutputArray(simulated, stack).m_41619_()) {
            return false;
         }
      }

      return true;
   }

   public long getOutputCapacityFor(ItemStack stack) {
      long capacity = 0L;

      for (int slot = 10; slot < 11; slot++) {
         ItemStack existing = this.getStackInSlot(slot);
         if (existing.m_41619_()) {
            capacity += 16384L;
         } else if (ItemStack.m_150942_(existing, stack)) {
            capacity += (long)Math.max(0, 16384 - existing.m_41613_());
         }
      }

      return capacity;
   }

   public boolean insertRecipeOutputs(List<ItemStack> outputs) {
      for (ItemStack stack : outputs) {
         if (stack.m_41619_() || !this.insertIntoOutputs(stack, false).m_41619_()) {
            return false;
         }
      }

      return true;
   }

   public void setClientRenderStack(int slot, ItemStack stack) {
      this.setStackInSlotUnchecked(slot, stack);
   }

   private ItemStack insertIntoOutputs(ItemStack stack, boolean simulate) {
      ItemStack remainder = stack;

      for (int slot = 10; slot < 11; slot++) {
         if (!this.getStackInSlot(slot).m_41619_()) {
            remainder = this.insertItemUnchecked(slot, remainder, simulate);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }
      }

      for (int slotx = 10; slotx < 11; slotx++) {
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
            int freeSpace = 16384 - existing.m_41613_();
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
            int toInsert = Math.min(16384, remainder.m_41613_());
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
