package com.moakiee.ae2lt.machine.firmament;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class FirmamentConversionAutomationInventory implements IItemHandlerModifiable {
   private final FirmamentConversionInventory inventory;

   public FirmamentConversionAutomationInventory(FirmamentConversionInventory inventory) {
      this.inventory = Objects.requireNonNull(inventory, "inventory");
   }

   public int getSlots() {
      return this.inventory.getSlots();
   }

   public ItemStack getStackInSlot(int slot) {
      return this.inventory.getStackInSlot(slot);
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      this.inventory.setStackInSlot(slot, stack);
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      this.validateSlotIndex(slot);
      Objects.requireNonNull(stack, "stack");
      if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else {
         return !this.inventory.isInputSlot(slot) ? stack : this.inventory.insertItem(slot, stack, simulate);
      }
   }

   public ItemStack insertItem(ItemStack stack, boolean simulate) {
      Objects.requireNonNull(stack, "stack");
      if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else {
         ItemStack remainder = stack;

         for (int slot = 0; slot <= 2; slot++) {
            remainder = this.inventory.insertItem(slot, remainder, simulate);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }

         return remainder;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      this.validateSlotIndex(slot);
      return !this.inventory.isOutputSlot(slot) ? ItemStack.f_41583_ : this.inventory.extractItem(slot, amount, simulate);
   }

   public int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      return stack.m_41619_() ? false : this.inventory.isInputSlot(slot);
   }

   private void validateSlotIndex(int slot) {
      if (slot < 0 || slot >= this.inventory.getSlots()) {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.inventory.getSlots() + ")");
      }
   }
}
