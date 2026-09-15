package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationRecipe;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationRecipeService;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class EmiLightningSimulationRecipe extends EmiBackedRecipe<LightningSimulationRecipe> {
   private static final ResourceLocation TEXTURE = EmiRecipeWidgets.texture("guis/lightning_simulation_room.png");
   private static final int WIDTH = 168;

   EmiLightningSimulationRecipe(ResourceLocation id, LightningSimulationRecipe recipe) {
      super(AE2LTEmiCategories.LIGHTNING_SIMULATION, id, recipe, 168, 96);
      recipe.inputs().forEach(input -> this.inputs.add(EmiRecipeWidgets.ingredient(input.ingredient(), (long)input.count())));
      this.outputs.add(EmiStack.of(recipe.getResultStack()));
   }

   public void addWidgets(WidgetHolder widgets) {
      widgets.addTexture(TEXTURE, 0, 0, 168, 64, 5, 14);

      for (int index = 0; index < this.inputs.size(); index++) {
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.inputs.get(index), 34, 8 + index * 18).drawBack(false);
      }

      EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.outputs.get(0), 114, 27).drawBack(false).recipeContext(this);
      EmiRecipeWidgets.centeredText(
         widgets, Component.m_237110_("jei.ae2lt.lightning_simulation.energy", new Object[]{EmiRecipeWidgets.compactEnergy(this.recipe.totalEnergy())}), 84, 66
      );
      EmiRecipeWidgets.centeredText(
         widgets,
         Component.m_237110_(
            "jei.ae2lt.lightning_simulation.lightning", new Object[]{this.recipe.lightningCost(), EmiRecipeWidgets.tierName(this.recipe.lightningTier())}
         ),
         84,
         76
      );
      if (this.recipe.lightningTier() == LightningKey.Tier.EXTREME_HIGH_VOLTAGE) {
         EmiRecipeWidgets.centeredText(
            widgets,
            Component.m_237110_(
               "jei.ae2lt.lightning_simulation.substitution",
               new Object[]{LightningSimulationRecipeService.getEquivalentHighVoltageCost(this.recipe.lightningTier(), this.recipe.lightningCost())}
            ),
            84,
            86
         );
      }
   }
}
