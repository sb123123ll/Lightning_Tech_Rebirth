package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class EmiOverloadProcessingRecipe extends EmiBackedRecipe<OverloadProcessingRecipe> {
   private static final ResourceLocation TEXTURE = EmiRecipeWidgets.texture("guis/overload_processing_factory.png");
   private static final int WIDTH = 168;
   private static final int FLUID_HEIGHT = 54;
   private static final int FIRST_TICK_PIXELS = 5;
   private final EmiStack fluidInput;
   private final EmiStack fluidOutput;

   EmiOverloadProcessingRecipe(ResourceLocation id, OverloadProcessingRecipe recipe) {
      super(AE2LTEmiCategories.OVERLOAD_PROCESSING, id, recipe, 168, 90);
      recipe.itemInputs().forEach(input -> this.inputs.add(EmiRecipeWidgets.ingredient(input.ingredient(), (long)input.count())));
      this.fluidInput = recipe.fluidInput().isEmpty() ? EmiStack.EMPTY : EmiRecipeWidgets.fluid(recipe.fluidInput());
      if (!this.fluidInput.isEmpty()) {
         this.inputs.add(this.fluidInput);
      }

      recipe.itemResults().forEach(result -> this.outputs.add(EmiStack.of(result)));
      this.fluidOutput = recipe.fluidResult().isEmpty() ? EmiStack.EMPTY : EmiRecipeWidgets.fluid(recipe.fluidResult());
      if (!this.fluidOutput.isEmpty()) {
         this.outputs.add(this.fluidOutput);
      }
   }

   public void addWidgets(WidgetHolder widgets) {
      widgets.addTexture(TEXTURE, 0, 0, 168, 68, 4, 14);

      for (int index = 0; index < this.recipe.itemInputs().size(); index++) {
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.inputs.get(index), 25 + index % 3 * 18, 10 + index / 3 * 18).drawBack(false);
      }

      if (!this.fluidInput.isEmpty()) {
         widgets.addTank(this.fluidInput, 5, 9, 16, 54, displayCapacity(this.recipe.fluidInput().getAmount(), 1024000)).drawBack(false);
      }

      if (!this.recipe.itemResults().isEmpty()) {
         EmiRecipeWidgets.addLargeStackSlot(widgets, EmiStack.of(this.recipe.itemResults().get(0)), 114, 28).drawBack(false).recipeContext(this);
      }

      if (!this.fluidOutput.isEmpty()) {
         widgets.addTank(this.fluidOutput, 147, 9, 16, 54, displayCapacity(this.recipe.fluidResult().getAmount(), 1024000)).drawBack(false).recipeContext(this);
      }

      widgets.addAnimatedTexture(TEXTURE, 80, 32, 31, 10, 176, 18, 2000, true, false, false);
      EmiRecipeWidgets.centeredText(
         widgets, Component.m_237110_("jei.ae2lt.overload_processing.energy", new Object[]{EmiRecipeWidgets.compactEnergy(this.recipe.totalEnergy())}), 84, 70
      );
      EmiRecipeWidgets.centeredText(
         widgets,
         Component.m_237110_(
            "jei.ae2lt.overload_processing.lightning", new Object[]{this.recipe.lightningCost(), EmiRecipeWidgets.tierName(this.recipe.lightningTier())}
         ),
         84,
         80
      );
   }

   private static int displayCapacity(int amount, int tankCapacity) {
      if (amount <= 0) {
         return Math.max(1, tankCapacity);
      } else {
         long tickFloorCapacity = Math.max(1L, (long)amount * 54L / 5L);
         return (int)Math.min((long)tankCapacity, tickFloorCapacity);
      }
   }
}
