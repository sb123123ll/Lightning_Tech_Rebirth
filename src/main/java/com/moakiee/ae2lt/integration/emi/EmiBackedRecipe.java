package com.moakiee.ae2lt.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

abstract class EmiBackedRecipe<T extends Recipe<?>> extends BasicEmiRecipe {
   protected final T recipe;

   EmiBackedRecipe(EmiRecipeCategory category, ResourceLocation id, T recipe, int width, int height) {
      super(category, id, width, height);
      this.recipe = recipe;
   }

   @Nullable
   public Recipe<?> getBackingRecipe() {
      return this.recipe;
   }
}
