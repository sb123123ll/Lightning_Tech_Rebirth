package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class EmiFirmamentConversionRecipe extends EmiBackedRecipe<FirmamentConversionRecipe> {
   private static final int WIDTH = 134;
   private static final int SLOT_AREA_Y = 4;
   private static final int SLOT_AREA_HEIGHT = 54;

   EmiFirmamentConversionRecipe(ResourceLocation id, FirmamentConversionRecipe recipe) {
      super(AE2LTEmiCategories.FIRMAMENT_CONVERSION, id, recipe, 134, 76);
      recipe.inputs().forEach(input -> this.inputs.add(EmiRecipeWidgets.ingredient(input.ingredient(), (long)input.count())));
      recipe.getResultStacks().forEach(result -> this.outputs.add(EmiStack.of(result)));
   }

   public void addWidgets(WidgetHolder widgets) {
      int inputOffsetY = (54 - this.inputs.size() * 18) / 2;

      for (int index = 0; index < this.inputs.size(); index++) {
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.inputs.get(index), 19, 4 + inputOffsetY + index * 18 + 1);
      }

      int outputCols = Math.min(this.outputs.size(), 2);
      int outputRows = (this.outputs.size() + 1) / 2;
      int outputOffsetY = (54 - outputRows * 18) / 2;
      int outputOffsetX = (36 - outputCols * 18) / 2;

      for (int index = 0; index < this.outputs.size(); index++) {
         int col = index % 2;
         int row = index / 2;
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.outputs.get(index), 81 + outputOffsetX + col * 18, 4 + outputOffsetY + row * 18 + 1)
            .recipeContext(this);
      }

      widgets.addFillingArrow(46, 22, 2000);
      EmiRecipeWidgets.centeredText(
         widgets, Component.m_237110_("jei.ae2lt.firmament_conversion.time", new Object[]{EmiRecipeWidgets.processTime(this.recipe.processTime())}), 67, 64
      );
   }
}
