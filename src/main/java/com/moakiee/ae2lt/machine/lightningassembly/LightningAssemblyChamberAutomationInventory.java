package com.moakiee.ae2lt.machine.lightningassembly;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class LightningAssemblyChamberAutomationInventory implements IItemHandlerModifiable {
   private final LightningAssemblyChamberInventory inventory;

   public LightningAssemblyChamberAutomationInventory(LightningAssemblyChamberInventory inventory) {
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
      if (slot >= 0 && slot < this.inventory.getSlots()) {
         Objects.requireNonNull(stack, "stack");
         if (stack.m_41619_()) {
            return ItemStack.f_41583_;
         } else if (slot == 10 || slot == 9) {
            return stack;
         } else {
            return this.inventory.isInputSlot(slot) ? this.inventory.insertItem(slot, stack, simulate) : stack;
         }
      } else {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.inventory.getSlots() + ")");
      }
   }

   public ItemStack insertItem(ItemStack stack, boolean simulate) {
      Objects.requireNonNull(stack, "stack");
      if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else {
         ItemStack remainder = stack;

         for (int slot = 0; slot <= 8; slot++) {
            remainder = this.inventory.insertItem(slot, remainder, simulate);
            if (remainder.m_41619_()) {
               return ItemStack.f_41583_;
            }
         }

         return remainder;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (slot != 10) {
         if (slot >= 0 && slot < this.inventory.getSlots()) {
            return ItemStack.f_41583_;
         } else {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.inventory.getSlots() + ")");
         }
      } else {
         return this.inventory.extractItem(slot, amount, simulate);
      }
   }

   public int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      if (slot < 0 || slot >= this.inventory.getSlots()) {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.inventory.getSlots() + ")");
      } else if (stack.m_41619_()) {
         return false;
      } else {
         return slot != 10 && slot != 9 ? this.inventory.isInputSlot(slot) : false;
      }
   }
}
