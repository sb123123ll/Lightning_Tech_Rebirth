package com.moakiee.ae2lt.machine.teslacoil;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class TeslaCoilAutomationInventory implements IItemHandlerModifiable {
   private final TeslaCoilInventory inventory;

   public TeslaCoilAutomationInventory(TeslaCoilInventory inventory) {
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
      Objects.requireNonNull(stack, "stack");
      if (slot < 0 || slot >= this.inventory.getSlots()) {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range");
      } else if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else if (slot == 1) {
         return stack;
      } else {
         return !this.inventory.isItemValid(slot, stack) ? stack : this.inventory.insertItem(slot, stack, simulate);
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (slot >= 0 && slot < this.inventory.getSlots()) {
         return ItemStack.f_41583_;
      } else {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range");
      }
   }

   public int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      if (slot < 0 || slot >= this.inventory.getSlots()) {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range");
      } else if (stack.m_41619_()) {
         return false;
      } else {
         return slot == 1 ? false : this.inventory.isItemValid(slot, stack);
      }
   }
}
