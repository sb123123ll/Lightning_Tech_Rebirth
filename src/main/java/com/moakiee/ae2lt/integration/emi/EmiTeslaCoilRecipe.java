package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import com.moakiee.ae2lt.registry.ModItems;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

final class EmiTeslaCoilRecipe extends BasicEmiRecipe {
   private static final ResourceLocation ARROW_TEXTURE = EmiRecipeWidgets.texture("guis/crystal_catalyzer.png");
   private static final int WIDTH = 150;
   private final TeslaCoilMode mode;
   private final EmiLightningIcon lightningIcon;

   private EmiTeslaCoilRecipe(TeslaCoilMode mode) {
      super(AE2LTEmiCategories.TESLA_COIL, EmiRecipeWidgets.syntheticId("tesla_coil/" + mode.m_7912_()), 150, 90);
      this.mode = mode;
      this.lightningIcon = new EmiLightningIcon(mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE);
      if (mode == TeslaCoilMode.HIGH_VOLTAGE) {
         this.inputs.add(EmiStack.of((ItemLike)ModItems.OVERLOAD_CRYSTAL_DUST.get(), (long)Math.max(1, mode.requiredDust())));
      }
   }

   static void registerAll(EmiRegistry registry) {
      for (TeslaCoilMode mode : TeslaCoilMode.values()) {
         registry.addRecipe(new EmiTeslaCoilRecipe(mode));
      }
   }

   public void addWidgets(WidgetHolder widgets) {
      if (!this.inputs.isEmpty()) {
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.inputs.get(0), 12, 8);
      }

      widgets.addAnimatedTexture(ARROW_TEXTURE, 54, 11, 35, 10, 176, 18, 1500, true, false, false);
      widgets.addDrawable(118, 8, 16, 16, (graphics, mouseX, mouseY, delta) -> this.lightningIcon.render(graphics, 0, 0, delta));
      EmiRecipeWidgets.centeredText(
         widgets, Component.m_237110_("jei.ae2lt.tesla_coil.mode_label", new Object[]{Component.m_237115_(this.mode.translationKey())}), 75, 34
      );
      EmiRecipeWidgets.centeredText(
         widgets, Component.m_237110_("jei.ae2lt.tesla_coil.energy", new Object[]{EmiRecipeWidgets.compactEnergy(this.mode.totalEnergy())}), 75, 46
      );
      EmiRecipeWidgets.centeredText(
         widgets,
         Component.m_237110_(
            this.mode == TeslaCoilMode.HIGH_VOLTAGE ? "jei.ae2lt.tesla_coil.consume_dust" : "jei.ae2lt.tesla_coil.consume_hv",
            new Object[]{this.mode == TeslaCoilMode.HIGH_VOLTAGE ? (long)this.mode.requiredDust() : this.mode.requiredHighVoltage()}
         ),
         75,
         58
      );
      EmiRecipeWidgets.centeredText(
         widgets,
         Component.m_237115_(this.mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE ? "jei.ae2lt.tesla_coil.output_ehv" : "jei.ae2lt.tesla_coil.output_hv"),
         75,
         70
      );
   }

   public boolean supportsRecipeTree() {
      return false;
   }
}
