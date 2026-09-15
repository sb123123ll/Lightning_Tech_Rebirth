package com.moakiee.ae2lt.machine.common;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class InsertOnlyAutomationInventory implements IItemHandlerModifiable {
   private final IItemHandlerModifiable inventory;

   public InsertOnlyAutomationInventory(IItemHandlerModifiable inventory) {
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
      return this.inventory.insertItem(slot, stack, simulate);
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      this.validateSlotIndex(slot);
      return ItemStack.f_41583_;
   }

   public int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return this.inventory.isItemValid(slot, stack);
   }

   private void validateSlotIndex(int slot) {
      if (slot < 0 || slot >= this.inventory.getSlots()) {
         throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.inventory.getSlots() + ")");
      }
   }
}
