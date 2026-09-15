package com.moakiee.ae2lt.lightning.strike;

import com.moakiee.ae2lt.recipe.RecipeContainerInput;
import net.minecraft.world.item.ItemStack;

public final class LightningStrikeRecipeInput extends RecipeContainerInput {
   public static final LightningStrikeRecipeInput EMPTY = new LightningStrikeRecipeInput();

   private LightningStrikeRecipeInput() {
   }

   @Override
   public boolean m_7983_() {
      return true;
   }

   @Override
   public ItemStack m_8020_(int slotIndex) {
      return ItemStack.f_41583_;
   }

   @Override
   public int size() {
      return 0;
   }
}
