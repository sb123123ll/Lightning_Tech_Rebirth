package com.moakiee.ae2lt.machine.overloadfactory.recipe;

import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import com.moakiee.ae2lt.recipe.RecipeContainerInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public final class OverloadProcessingRecipeInput extends RecipeContainerInput {
   private final List<OverloadProcessingRecipeInput.SlotStack> slotStacks;
   private final FluidStack inputFluid;

   public OverloadProcessingRecipeInput(List<OverloadProcessingRecipeInput.SlotStack> slotStacks, FluidStack inputFluid) {
      this.slotStacks = List.copyOf(slotStacks);
      this.inputFluid = Objects.requireNonNull(inputFluid, "inputFluid");
   }

   public List<OverloadProcessingRecipeInput.SlotStack> slotStacks() {
      return this.slotStacks;
   }

   public FluidStack inputFluid() {
      return this.inputFluid;
   }

   public static OverloadProcessingRecipeInput fromInventory(OverloadProcessingFactoryInventory inventory, FluidStack inputFluid) {
      List<OverloadProcessingRecipeInput.SlotStack> slotStacks = new ArrayList<>(9);

      for (int slot = 0; slot <= 8; slot++) {
         ItemStack stack = inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            slotStacks.add(new OverloadProcessingRecipeInput.SlotStack(slot, stack));
         }
      }

      return new OverloadProcessingRecipeInput(List.copyOf(slotStacks), inputFluid.copy());
   }

   @Override
   public boolean m_7983_() {
      return this.slotStacks.isEmpty() && this.inputFluid.isEmpty();
   }

   @Override
   public ItemStack m_8020_(int index) {
      return this.slotStacks.get(index).stack();
   }

   @Override
   public int size() {
      return this.slotStacks.size();
   }

   public static record SlotStack(int slot, ItemStack stack) {
      public SlotStack(int slot, ItemStack stack) {
         if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("slot must be an input slot");
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
