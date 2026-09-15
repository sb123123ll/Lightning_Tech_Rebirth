package com.moakiee.ae2lt.machine.firmament.recipe;

import net.minecraft.resources.ResourceLocation;

public record FirmamentConversionRecipeCandidate(ResourceLocation id, FirmamentConversionRecipe recipe, FirmamentConversionRecipeMatch match) {
   public ResourceLocation recipeId() {
      return this.id;
   }
}
