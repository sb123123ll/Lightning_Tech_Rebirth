package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.me.key.LightningKey;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.api.widget.TextWidget.Alignment;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

final class EmiRecipeWidgets {
   static final int TEXT_COLOR = 4210752;

   private EmiRecipeWidgets() {
   }

   static ResourceLocation texture(String path) {
      return new ResourceLocation("ae2lt", "textures/" + path);
   }

   static ResourceLocation syntheticId(String path) {
      return new ResourceLocation("ae2lt", "/emi/" + path);
   }

   static EmiIngredient ingredient(Ingredient ingredient, long count) {
      return EmiIngredient.of(ingredient, count);
   }

   static EmiStack fluid(FluidStack stack) {
      return EmiStack.of(stack.getFluid(), stack.getTag(), (long)stack.getAmount());
   }

   static void centeredText(WidgetHolder widgets, Component text, int centerX, int y) {
      widgets.addText(text, centerX, y, 4210752, false).horizontalAlign(Alignment.CENTER);
   }

   static SlotWidget addLargeStackSlot(WidgetHolder widgets, EmiIngredient ingredient, int x, int y) {
      return (SlotWidget)widgets.add(new EmiLargeStackSlotWidget(ingredient, x - 1, y - 1));
   }

   static SlotWidget addSlot(WidgetHolder widgets, EmiIngredient ingredient, int x, int y) {
      return widgets.addSlot(ingredient, x - 1, y - 1);
   }

   static Component tierName(LightningKey.Tier tier) {
      return Component.m_237115_(
         tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE
            ? "ae2lt.gui.lightning_simulation.tier.extreme_high_voltage"
            : "ae2lt.gui.lightning_simulation.tier.high_voltage"
      );
   }

   static String compactEnergy(long energy) {
      if (energy >= 1000000L) {
         return compactValue((double)energy / 1000000.0, "m");
      } else {
         return energy >= 1000L ? compactValue((double)energy / 1000.0, "k") : Long.toString(energy);
      }
   }

   static String processTime(int ticks) {
      double seconds = (double)ticks / 20.0;
      return seconds == Math.floor(seconds) ? (int)seconds + "s" : String.format(Locale.ROOT, "%.1fs", seconds);
   }

   private static String compactValue(double value, String suffix) {
      double rounded = (double)Math.round(value * 10.0) / 10.0;
      return Math.abs(rounded - Math.rint(rounded)) < 1.0E-4 ? Long.toString(Math.round(rounded)) + suffix : rounded + suffix;
   }
}
