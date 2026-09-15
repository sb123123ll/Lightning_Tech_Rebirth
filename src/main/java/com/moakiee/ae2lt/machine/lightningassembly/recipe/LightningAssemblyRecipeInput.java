package com.moakiee.ae2lt.machine.lightningassembly.recipe;

import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberInventory;
import com.moakiee.ae2lt.recipe.RecipeContainerInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class LightningAssemblyRecipeInput extends RecipeContainerInput {
   private final List<LightningAssemblyRecipeInput.SlotStack> slotStacks;
   private final List<ItemStack> displayStacks;

   private LightningAssemblyRecipeInput(List<LightningAssemblyRecipeInput.SlotStack> slotStacks) {
      this.slotStacks = List.copyOf(slotStacks);
      this.displayStacks = this.slotStacks.stream().map(LightningAssemblyRecipeInput.SlotStack::stack).toList();
   }

   public static LightningAssemblyRecipeInput fromInventory(LightningAssemblyChamberInventory inventory) {
      List<LightningAssemblyRecipeInput.SlotStack> slotStacks = new ArrayList<>(9);

      for (int slot = 0; slot <= 8; slot++) {
         ItemStack stack = inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            slotStacks.add(new LightningAssemblyRecipeInput.SlotStack(slot, stack.m_41777_()));
         }
      }

      return new LightningAssemblyRecipeInput(slotStacks);
   }

   public List<LightningAssemblyRecipeInput.SlotStack> slotStacks() {
      return this.slotStacks;
   }

   @Override
   public boolean m_7983_() {
      return this.slotStacks.isEmpty();
   }

   @Override
   public ItemStack m_8020_(int index) {
      return this.displayStacks.get(index);
   }

   @Override
   public int size() {
      return this.displayStacks.size();
   }

   public static record SlotStack(int slot, ItemStack stack) {
      public SlotStack(int slot, ItemStack stack) {
         if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("slot must be one of the nine input slots");
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
