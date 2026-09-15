package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionIngredient;
import com.moakiee.ae2lt.machine.firmament.recipe.FirmamentConversionRecipe;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class FirmamentConversionCategory implements IRecipeCategory<FirmamentConversionRecipe> {
   public static final RecipeType<FirmamentConversionRecipe> TYPE = RecipeType.create("ae2lt", "firmament_conversion", FirmamentConversionRecipe.class);
   private static final int WIDTH = 134;
   private static final int SLOT = 18;
   private static final int INPUT_X = 18;
   private static final int ARROW_X = 46;
   private static final int ARROW_WIDTH = 24;
   private static final int OUTPUT_X = 80;
   private static final int SLOT_AREA_Y = 4;
   private static final int SLOT_AREA_HEIGHT = 54;
   private static final int ARROW_Y = 22;
   private static final int TIME_TEXT_Y = 64;
   private static final int HEIGHT = 76;
   private static final int TEXT_COLOR = 4210752;
   private final IDrawable icon;

   public FirmamentConversionCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.FIRMAMENT_CONVERSION_CORE.get()));
   }

   public RecipeType<FirmamentConversionRecipe> getRecipeType() {
      return TYPE;
   }

   public int getWidth() {
      return 134;
   }

   public int getHeight() {
      return 76;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.firmament_conversion.title");
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, FirmamentConversionRecipe recipe, IFocusGroup focuses) {
      int inputCount = recipe.inputs().size();
      int inputOffsetY = (54 - inputCount * 18) / 2;

      for (int index = 0; index < inputCount; index++) {
         FirmamentConversionIngredient input = recipe.inputs().get(index);
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, 19, 4 + inputOffsetY + index * 18 + 1)
               .setStandardSlotBackground()
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStacks(expandIngredient(input.ingredient(), input.count())))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)input.count()));
      }

      List<ItemStack> results = recipe.getResultStacks();
      int outputCount = results.size();
      int outputCols = Math.min(outputCount, 2);
      int outputRows = (outputCount + 1) / 2;
      int outputOffsetY = (54 - outputRows * 18) / 2;
      int outputOffsetX = (36 - outputCols * 18) / 2;

      for (int index = 0; index < outputCount; index++) {
         ItemStack result = results.get(index);
         int col = index % 2;
         int row = index / 2;
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 80 + outputOffsetX + col * 18 + 1, 4 + outputOffsetY + row * 18 + 1)
               .setStandardSlotBackground()
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStack(result))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)result.m_41613_()));
      }
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, FirmamentConversionRecipe recipe, IFocusGroup focuses) {
      builder.addRecipeArrow().setPosition(46, 22);
   }

   public void draw(FirmamentConversionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      Font font = Minecraft.m_91087_().f_91062_;
      String timeStr = formatProcessTime(recipe.processTime());
      MutableComponent timeText = Component.m_237110_("jei.ae2lt.firmament_conversion.time", new Object[]{timeStr});
      int timeX = (134 - font.m_92852_(timeText)) / 2;
      guiGraphics.m_280614_(font, timeText, timeX, 64, 4210752, false);
   }

   private static List<ItemStack> expandIngredient(Ingredient ingredient, int count) {
      return Arrays.stream(ingredient.m_43908_()).map(stack -> stack.m_255036_(count)).toList();
   }

   private static String formatProcessTime(int ticks) {
      double seconds = (double)ticks / 20.0;
      return seconds == Math.floor(seconds) ? (int)seconds + "s" : String.format("%.1fs", seconds);
   }
}
