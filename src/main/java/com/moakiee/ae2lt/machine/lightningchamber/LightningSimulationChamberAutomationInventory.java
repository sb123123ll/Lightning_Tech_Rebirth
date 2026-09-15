package com.moakiee.ae2lt.machine.lightningchamber;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class LightningSimulationChamberAutomationInventory implements IItemHandlerModifiable {
   private final LightningSimulationChamberInventory inventory;

   public LightningSimulationChamberAutomationInventory(LightningSimulationChamberInventory inventory) {
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
      this.inventory.validateSlotIndex(slot);
      Objects.requireNonNull(stack, "stack");
      if (stack.m_41619_()) {
         return ItemStack.f_41583_;
      } else if (slot == 4 || slot == 3) {
         return stack;
      } else {
         return this.inventory.isInputSlot(slot) ? this.inventory.insertItem(slot, stack, simulate) : stack;
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
      if (slot != 4) {
         this.inventory.validateSlotIndex(slot);
         return ItemStack.f_41583_;
      } else {
         return this.inventory.extractItem(slot, amount, simulate);
      }
   }

   public int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      this.inventory.validateSlotIndex(slot);
      if (stack.m_41619_()) {
         return false;
      } else {
         return slot != 4 && slot != 3 ? this.inventory.isInputSlot(slot) : false;
      }
   }
}
