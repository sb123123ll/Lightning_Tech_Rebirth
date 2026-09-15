package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingIngredient;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

public class OverloadProcessingCategory implements IRecipeCategory<OverloadProcessingRecipe> {
   public static final RecipeType<OverloadProcessingRecipe> TYPE = RecipeType.create("ae2lt", "overload_processing", OverloadProcessingRecipe.class);
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("ae2lt", "textures/guis/overload_processing_factory.png");
   private static final int BACKGROUND_U = 4;
   private static final int BACKGROUND_V = 14;
   private static final int BACKGROUND_WIDTH = 168;
   private static final int BACKGROUND_HEIGHT = 68;
   private static final int WIDTH = 168;
   private static final int HEIGHT = 90;
   private static final int TEXTURE_WIDTH = 256;
   private static final int TEXTURE_HEIGHT = 256;
   private static final int INPUT_START_X = 25;
   private static final int INPUT_START_Y = 10;
   private static final int SLOT_SPACING = 18;
   private static final int OUTPUT_X = 114;
   private static final int OUTPUT_Y = 28;
   private static final int FLUID_INPUT_X = 5;
   private static final int FLUID_INPUT_Y = 9;
   private static final int FLUID_OUTPUT_X = 147;
   private static final int FLUID_OUTPUT_Y = 9;
   private static final int FLUID_WIDTH = 16;
   private static final int FLUID_HEIGHT = 54;
   private static final int FIRST_TICK_PIXELS = 5;
   private static final int PROCESS_X = 80;
   private static final int PROCESS_Y = 32;
   private static final int PROCESS_OVERLAY_U = 176;
   private static final int PROCESS_OVERLAY_V = 18;
   private static final int PROCESS_OVERLAY_WIDTH = 31;
   private static final int PROCESS_OVERLAY_HEIGHT = 10;
   private static final long PROCESS_CYCLE_MS = 2000L;
   private static final int ENERGY_TEXT_Y = 70;
   private static final int LIGHTNING_TEXT_Y = 80;
   private final IDrawable icon;
   private final IDrawable background;

   public OverloadProcessingCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get()));
      this.background = guiHelper.createDrawable(BACKGROUND_TEXTURE, 4, 14, 168, 68);
   }

   public RecipeType<OverloadProcessingRecipe> getRecipeType() {
      return TYPE;
   }

   public int getWidth() {
      return 168;
   }

   public int getHeight() {
      return 90;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.overload_processing.title");
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, OverloadProcessingRecipe recipe, IFocusGroup focuses) {
      for (int index = 0; index < recipe.itemInputs().size(); index++) {
         OverloadProcessingIngredient input = recipe.itemInputs().get(index);
         int col = index % 3;
         int row = index / 3;
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, 25 + col * 18, 10 + row * 18)
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStacks(expandIngredient(input.ingredient(), input.count())))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)input.count()));
      }

      if (!recipe.fluidInput().isEmpty()) {
         FluidStack fluidInput = recipe.fluidInput();
         builder.addSlot(RecipeIngredientRole.INPUT, 5, 9)
            .setFluidRenderer(displayCapacity(fluidInput.getAmount(), 1024000), false, 16, 54)
            .addIngredient(ForgeTypes.FLUID_STACK, fluidInput);
      }

      if (!recipe.itemResults().isEmpty()) {
         ItemStack result = recipe.itemResults().get(0);
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 28)
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStack(result))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)result.m_41613_()));
      }

      if (!recipe.fluidResult().isEmpty()) {
         FluidStack fluidResult = recipe.fluidResult();
         builder.addSlot(RecipeIngredientRole.OUTPUT, 147, 9)
            .setFluidRenderer(displayCapacity(fluidResult.getAmount(), 1024000), false, 16, 54)
            .addIngredient(ForgeTypes.FLUID_STACK, fluidResult);
      }
   }

   public void draw(OverloadProcessingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      this.background.draw(guiGraphics);
      this.drawProcessOverlay(guiGraphics);
      Font font = Minecraft.m_91087_().f_91062_;
      MutableComponent energyText = Component.m_237110_("jei.ae2lt.overload_processing.energy", new Object[]{formatCompactEnergy(recipe.totalEnergy())});
      int energyX = (168 - font.m_92852_(energyText)) / 2;
      guiGraphics.m_280614_(font, energyText, energyX, 70, 4210752, false);
      MutableComponent lightningText = Component.m_237110_(
         "jei.ae2lt.overload_processing.lightning",
         new Object[]{
            recipe.lightningCost(),
            Component.m_237115_(
               recipe.lightningTier() == LightningKey.Tier.EXTREME_HIGH_VOLTAGE
                  ? "ae2lt.gui.lightning_simulation.tier.extreme_high_voltage"
                  : "ae2lt.gui.lightning_simulation.tier.high_voltage"
            )
         }
      );
      int lightningX = (168 - font.m_92852_(lightningText)) / 2;
      guiGraphics.m_280614_(font, lightningText, lightningX, 80, 4210752, false);
   }

   private void drawProcessOverlay(GuiGraphics guiGraphics) {
      long elapsed = Util.m_137550_() % 2000L;
      double progress = (double)elapsed / 2000.0;
      int width = Mth.m_14045_((int)Math.ceil(progress * 31.0), 0, 31);
      if (width > 0) {
         guiGraphics.m_280163_(BACKGROUND_TEXTURE, 80, 32, 176.0F, 18.0F, width, 10, 256, 256);
      }
   }

   private static List<ItemStack> expandIngredient(Ingredient ingredient, int count) {
      return Arrays.stream(ingredient.m_43908_()).map(stack -> stack.m_255036_(count)).toList();
   }

   private static long displayCapacity(int amount, int tankCapacity) {
      if (amount <= 0) {
         return (long)Math.max(1, tankCapacity);
      } else {
         long tickFloorCapacity = Math.max(1L, (long)amount * 54L / 5L);
         return Math.min((long)tankCapacity, tickFloorCapacity);
      }
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
}
