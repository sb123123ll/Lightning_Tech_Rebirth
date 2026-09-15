package com.moakiee.ae2lt.machine.crystalcatalyzer;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class CrystalCatalyzerAutomationInventory implements IItemHandlerModifiable {
   private final CrystalCatalyzerInventory inventory;

   public CrystalCatalyzerAutomationInventory(CrystalCatalyzerInventory inventory) {
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
      } else if (slot == 1 || slot == 2) {
         return stack;
      } else {
         return slot == 0 && this.inventory.isItemValid(0, stack) ? this.inventory.insertItem(slot, stack, simulate) : stack;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return slot != 2 ? ItemStack.f_41583_ : this.inventory.extractItem(slot, amount, simulate);
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
         return slot != 1 && slot != 2 ? slot == 0 && this.inventory.isItemValid(0, stack) : false;
      }
   }
}
