package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class TeslaCoilCategory implements IRecipeCategory<TeslaCoilCategory.Page> {
   public static final RecipeType<TeslaCoilCategory.Page> TYPE = RecipeType.create("ae2lt", "tesla_coil", TeslaCoilCategory.Page.class);
   private static final int WIDTH = 150;
   private static final int HEIGHT = 90;
   private static final int ROW_Y = 8;
   private static final int INPUT_X = 12;
   private static final int ARROW_X = 54;
   private static final int ARROW_Y = 11;
   private static final int ARROW_W = 35;
   private static final int ARROW_H = 10;
   private static final int OUTPUT_X = 118;
   private static final int OUTPUT_Y = 8;
   private static final int ICON_SIZE = 16;
   private static final int ICON_FRAME_H = 16;
   private static final int ICON_SHEET_H = 96;
   private static final int ICON_FRAMES = 6;
   private static final long ICON_FRAME_MS = 100L;
   private static final long PROCESS_CYCLE_MS = 1500L;
   private static final int TEXT_COLOR = 4210752;
   private static final int[] TEXT_LINES = new int[]{34, 46, 58, 70};
   private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation("ae2lt", "textures/guis/crystal_catalyzer.png");
   private static final int ARROW_U = 176;
   private static final int ARROW_V = 18;
   private static final int ARROW_TEX_W = 256;
   private static final int ARROW_TEX_H = 256;
   private static final ResourceLocation HV_LIGHTNING_TEXTURE = new ResourceLocation("ae2lt", "textures/item/high_voltage_lightning.png");
   private static final ResourceLocation EHV_LIGHTNING_TEXTURE = new ResourceLocation("ae2lt", "textures/item/extreme_high_voltage_lightning.png");
   private final IDrawable icon;

   public TeslaCoilCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.TESLA_COIL.get()));
   }

   public RecipeType<TeslaCoilCategory.Page> getRecipeType() {
      return TYPE;
   }

   public int getWidth() {
      return 150;
   }

   public int getHeight() {
      return 90;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.tesla_coil.title");
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, TeslaCoilCategory.Page page, IFocusGroup focuses) {
      TeslaCoilMode mode = page.mode;
      if (mode == TeslaCoilMode.HIGH_VOLTAGE) {
         int dustCount = Math.max(1, mode.requiredDust());
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, 12, 8)
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStack(new ItemStack((ItemLike)ModItems.OVERLOAD_CRYSTAL_DUST.get(), dustCount)))
            .addRichTooltipCallback((slotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)dustCount));
      }
   }

   public void draw(TeslaCoilCategory.Page page, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      TeslaCoilMode mode = page.mode;
      Font font = Minecraft.m_91087_().f_91062_;
      drawArrow(guiGraphics);
      drawLightningIcon(guiGraphics, mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE ? EHV_LIGHTNING_TEXTURE : HV_LIGHTNING_TEXTURE, 118, 8);
      Component modeText = Component.m_237110_("jei.ae2lt.tesla_coil.mode_label", new Object[]{Component.m_237115_(mode.translationKey())});
      drawCentered(guiGraphics, font, modeText, TEXT_LINES[0]);
      Component energyText = Component.m_237110_("jei.ae2lt.tesla_coil.energy", new Object[]{formatCompactEnergy(mode.totalEnergy())});
      drawCentered(guiGraphics, font, energyText, TEXT_LINES[1]);
      Component inputText;
      if (mode == TeslaCoilMode.HIGH_VOLTAGE) {
         inputText = Component.m_237110_("jei.ae2lt.tesla_coil.consume_dust", new Object[]{mode.requiredDust()});
      } else {
         inputText = Component.m_237110_("jei.ae2lt.tesla_coil.consume_hv", new Object[]{mode.requiredHighVoltage()});
      }

      drawCentered(guiGraphics, font, inputText, TEXT_LINES[2]);
      Component outputText = Component.m_237115_(
         mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE ? "jei.ae2lt.tesla_coil.output_ehv" : "jei.ae2lt.tesla_coil.output_hv"
      );
      drawCentered(guiGraphics, font, outputText, TEXT_LINES[3]);
   }

   private static void drawCentered(GuiGraphics guiGraphics, Font font, Component text, int y) {
      int x = (150 - font.m_92852_(text)) / 2;
      guiGraphics.m_280614_(font, text, x, y, 4210752, false);
   }

   private static void drawArrow(GuiGraphics guiGraphics) {
      long elapsed = Util.m_137550_() % 1500L;
      double progress = (double)elapsed / 1500.0;
      int fillW = Mth.m_14045_((int)Math.ceil(progress * 35.0), 0, 35);
      if (fillW > 0) {
         guiGraphics.m_280163_(ARROW_TEXTURE, 54, 11, 176.0F, 18.0F, fillW, 10, 256, 256);
      }
   }

   private static void drawLightningIcon(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
      int frame = (int)(Util.m_137550_() / 100L % 6L);
      guiGraphics.m_280163_(texture, x, y, 0.0F, (float)(frame * 16), 16, 16, 16, 96);
   }

   private static String formatCompactEnergy(long energy) {
      if (energy >= 1000000L) {
         return formatCompactValue((double)energy / 1000000.0, "m");
      } else {
         return energy >= 1000L ? formatCompactValue((double)energy / 1000.0, "k") : Long.toString(energy);
      }
   }

   private static String formatCompactValue(double value, String suffix) {
      double rounded = (double)Math.round(value * 10.0) / 10.0;
      return Math.abs(rounded - Math.rint(rounded)) < 1.0E-4 ? Long.toString(Math.round(rounded)) + suffix : rounded + suffix;
   }

   public static enum Page {
      HIGH_VOLTAGE(TeslaCoilMode.HIGH_VOLTAGE),
      EXTREME_HIGH_VOLTAGE(TeslaCoilMode.EXTREME_HIGH_VOLTAGE);

      public final TeslaCoilMode mode;

      private Page(TeslaCoilMode mode) {
         this.mode = mode;
      }
   }
}
