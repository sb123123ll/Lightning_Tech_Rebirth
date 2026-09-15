package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.lightning.LightningTransformRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class EmiLightningTransformRecipe extends EmiBackedRecipe<LightningTransformRecipe> {
   private static final int WIDTH = 134;
   private static final EmiLightningIcon LIGHTNING = new EmiLightningIcon(false);

   EmiLightningTransformRecipe(ResourceLocation id, LightningTransformRecipe recipe) {
      super(AE2LTEmiCategories.LIGHTNING_TRANSFORM, id, recipe, 134, 66);
      recipe.inputs().forEach(input -> this.inputs.add(EmiRecipeWidgets.ingredient(input.ingredient(), (long)input.count())));
      this.outputs.add(EmiStack.of(recipe.m_8043_(Minecraft.m_91087_().f_91073_.m_9598_())));
   }

   public void addWidgets(WidgetHolder widgets) {
      int x = 5;
      int y = 5;
      if (this.inputs.size() < 3) {
         y += (3 - this.inputs.size()) * 10;
      }

      for (EmiIngredient input : this.inputs) {
         EmiRecipeWidgets.addLargeStackSlot(widgets, input, x + 1, y + 1);
         y += 20;
         if (y >= 65) {
            y -= 60;
            x += 18;
         }
      }

      widgets.addFillingArrow(28, 24, 2000);
      widgets.addFillingArrow(81, 24, 2000);
      widgets.addDrawable(57, 26, 16, 16, (graphics, mouseX, mouseY, delta) -> LIGHTNING.render(graphics, 0, 0, delta));
      EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.outputs.get(0), 111, 26).recipeContext(this);
      EmiRecipeWidgets.centeredText(widgets, Component.m_237115_("jei.ae2lt.lightning_transform.label"), 67, 4);
   }
}
