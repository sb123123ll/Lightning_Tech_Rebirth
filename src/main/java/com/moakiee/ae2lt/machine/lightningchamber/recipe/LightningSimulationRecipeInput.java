package com.moakiee.ae2lt.machine.lightningchamber.recipe;

import com.moakiee.ae2lt.machine.lightningchamber.LightningSimulationChamberInventory;
import com.moakiee.ae2lt.recipe.RecipeContainerInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class LightningSimulationRecipeInput extends RecipeContainerInput {
   private final List<LightningSimulationRecipeInput.SlotStack> slotStacks;
   private final List<ItemStack> displayStacks;

   private LightningSimulationRecipeInput(List<LightningSimulationRecipeInput.SlotStack> slotStacks) {
      this.slotStacks = List.copyOf(slotStacks);
      this.displayStacks = this.slotStacks.stream().map(LightningSimulationRecipeInput.SlotStack::stack).toList();
   }

   public static LightningSimulationRecipeInput fromInventory(LightningSimulationChamberInventory inventory) {
      List<LightningSimulationRecipeInput.SlotStack> slotStacks = new ArrayList<>(3);

      for (int slot = 0; slot <= 2; slot++) {
         ItemStack stack = inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            slotStacks.add(new LightningSimulationRecipeInput.SlotStack(slot, stack.m_41777_()));
         }
      }

      return new LightningSimulationRecipeInput(slotStacks);
   }

   public List<LightningSimulationRecipeInput.SlotStack> slotStacks() {
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
