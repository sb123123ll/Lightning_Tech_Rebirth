package com.moakiee.ae2lt.machine.firmament.recipe;

import com.moakiee.ae2lt.machine.firmament.FirmamentConversionInventory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FirmamentConversionRecipeInput implements Container {
   private final List<FirmamentConversionRecipeInput.SlotStack> slotStacks;
   private final List<ItemStack> displayStacks;

   private FirmamentConversionRecipeInput(List<FirmamentConversionRecipeInput.SlotStack> slotStacks) {
      this.slotStacks = List.copyOf(slotStacks);
      this.displayStacks = this.slotStacks.stream().map(FirmamentConversionRecipeInput.SlotStack::stack).toList();
   }

   public static FirmamentConversionRecipeInput fromInventory(FirmamentConversionInventory inventory) {
      List<FirmamentConversionRecipeInput.SlotStack> slotStacks = new ArrayList<>(3);

      for (int slot = 0; slot <= 2; slot++) {
         ItemStack stack = inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            slotStacks.add(new FirmamentConversionRecipeInput.SlotStack(slot, stack.m_41777_()));
         }
      }

      return new FirmamentConversionRecipeInput(slotStacks);
   }

   public List<FirmamentConversionRecipeInput.SlotStack> slotStacks() {
      return this.slotStacks;
   }

   public boolean m_7983_() {
      return this.slotStacks.isEmpty();
   }

   public ItemStack m_8020_(int index) {
      return this.displayStacks.get(index);
   }

   public int m_6643_() {
      return this.displayStacks.size();
   }

   public ItemStack m_7407_(int index, int count) {
      return ItemStack.f_41583_;
   }

   public ItemStack m_8016_(int index) {
      return ItemStack.f_41583_;
   }

   public void m_6836_(int index, ItemStack stack) {
   }

   public void m_6596_() {
   }

   public boolean m_6542_(Player player) {
      return true;
   }

   public void m_6211_() {
   }

   public static record SlotStack(int slot, ItemStack stack) {
      public SlotStack(int slot, ItemStack stack) {
         if (slot < 0 || slot > 2) {
            throw new IllegalArgumentException("slot must be one of the three input slots");
         } else if (stack.m_41619_()) {
            throw new IllegalArgumentException("stack cannot be empty");
         } else {
            stack = stack.m_41777_();
            this.slot = slot;
            this.stack = stack;
         }
      }
   }
}
