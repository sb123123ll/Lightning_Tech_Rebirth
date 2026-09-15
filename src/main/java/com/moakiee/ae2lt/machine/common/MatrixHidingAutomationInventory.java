package com.moakiee.ae2lt.machine.common;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public abstract class MatrixHidingAutomationInventory<T extends IItemHandlerModifiable> implements IItemHandlerModifiable {
   protected final T inventory;
   private final int matrixSlot;

   protected MatrixHidingAutomationInventory(T inventory, int matrixSlot) {
      this.inventory = Objects.requireNonNull(inventory, "inventory");
      this.matrixSlot = matrixSlot;
   }

   public final int getSlots() {
      return this.inventory.getSlots();
   }

   public final ItemStack getStackInSlot(int slot) {
      return isSlotVisibleToAutomation(slot, this.matrixSlot) ? this.inventory.getStackInSlot(slot) : ItemStack.f_41583_;
   }

   public final void setStackInSlot(int slot, ItemStack stack) {
      if (isSlotVisibleToAutomation(slot, this.matrixSlot)) {
         this.inventory.setStackInSlot(slot, stack);
      }
   }

   public final int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   static boolean isSlotVisibleToAutomation(int slot, int matrixSlot) {
      return slot != matrixSlot;
   }
}
