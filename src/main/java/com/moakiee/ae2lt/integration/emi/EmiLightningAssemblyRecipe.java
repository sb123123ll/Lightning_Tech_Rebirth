package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class EmiLightningAssemblyRecipe extends EmiBackedRecipe<LightningAssemblyRecipe> {
   private static final ResourceLocation TEXTURE = EmiRecipeWidgets.texture("guis/lightning_assembly_chamber.png");
   private static final int WIDTH = 156;
   private static final int BACKGROUND_HEIGHT = 78;

   EmiLightningAssemblyRecipe(ResourceLocation id, LightningAssemblyRecipe recipe) {
      super(AE2LTEmiCategories.LIGHTNING_ASSEMBLY, id, recipe, 156, 100);
      recipe.inputs().forEach(input -> this.inputs.add(EmiRecipeWidgets.ingredient(input.ingredient(), (long)input.count())));
      this.outputs.add(EmiStack.of(recipe.getResultStack()));
   }

   public void addWidgets(WidgetHolder widgets) {
      widgets.addTexture(TEXTURE, 0, 0, 156, 78, 19, 10);

      for (int index = 0; index < this.inputs.size() && index < 9; index++) {
         int x = 10 + index % 3 * 18;
         int y = 21 + index / 3 * 18;
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.inputs.get(index), x, y).drawBack(false);
      }

      EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.outputs.get(0), 107, 39).drawBack(false).recipeContext(this);
      EmiRecipeWidgets.centeredText(
         widgets, Component.m_237110_("jei.ae2lt.lightning_assembly.energy", new Object[]{EmiRecipeWidgets.compactEnergy(this.recipe.totalEnergy())}), 78, 80
      );
      EmiRecipeWidgets.centeredText(
         widgets,
         Component.m_237110_(
            "jei.ae2lt.lightning_assembly.lightning", new Object[]{this.recipe.lightningCost(), EmiRecipeWidgets.tierName(this.recipe.lightningTier())}
         ),
         78,
         90
      );
   }
}
