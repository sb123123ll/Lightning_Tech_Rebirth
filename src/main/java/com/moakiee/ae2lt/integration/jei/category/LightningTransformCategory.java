package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.integration.jei.LightningJeiIngredients;
import com.moakiee.ae2lt.lightning.CountedIngredient;
import com.moakiee.ae2lt.lightning.LightningTransformRecipe;
import com.moakiee.ae2lt.me.key.LightningKey;
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

public class LightningTransformCategory implements IRecipeCategory<LightningTransformRecipe> {
   public static final RecipeType<LightningTransformRecipe> TYPE = RecipeType.create("ae2lt", "lightning_transform", LightningTransformRecipe.class);
   private static final int WIDTH = 134;
   private static final int HEIGHT = 66;
   private static final int INPUT_START_X = 5;
   private static final int INPUT_START_Y = 5;
   private static final int INPUT_SLOT_PITCH = 20;
   private static final int CATALYST_X = 56;
   private static final int CATALYST_Y = 25;
   private static final int OUTPUT_X = 110;
   private static final int OUTPUT_Y = 25;
   private static final int ARROW_LEFT_X = 28;
   private static final int ARROW_RIGHT_X = 81;
   private static final int ARROW_Y = 24;
   private static final int LABEL_Y = 4;
   private static final int TEXT_COLOR = 4210752;
   private final IDrawable icon;
   private final IDrawable lightningDisplay;

   public LightningTransformCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableIngredient(LightningJeiIngredients.TYPE, LightningKey.HIGH_VOLTAGE);
      this.lightningDisplay = guiHelper.createDrawableIngredient(LightningJeiIngredients.TYPE, LightningKey.HIGH_VOLTAGE);
   }

   public RecipeType<LightningTransformRecipe> getRecipeType() {
      return TYPE;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.lightning_transform.title");
   }

   public int getWidth() {
      return 134;
   }

   public int getHeight() {
      return 66;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, LightningTransformRecipe recipe, IFocusGroup focuses) {
      int inputCount = recipe.inputs().size();
      int x = 5;
      int y = 5;
      if (inputCount < 3) {
         y += (3 - inputCount) * 20 / 2;
      }

      for (int index = 0; index < inputCount; index++) {
         CountedIngredient input = recipe.inputs().get(index);
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, x + 1, y + 1)
               .setStandardSlotBackground()
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStacks(expandIngredient(input.ingredient(), input.count())))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)input.count()));
         y += 20;
         if (y >= 65) {
            y -= 60;
            x += 18;
         }
      }

      ItemStack resultStack = recipe.m_8043_(Minecraft.m_91087_().f_91073_.m_9598_());
      ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 111, 26)
            .setOutputSlotBackground()
            .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
            .addItemStack(resultStack))
         .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)resultStack.m_41613_()));
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, LightningTransformRecipe recipe, IFocusGroup focuses) {
      builder.addRecipeArrow().setPosition(28, 24);
      builder.addRecipeArrow().setPosition(81, 24);
   }

   public void draw(LightningTransformRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      Font font = Minecraft.m_91087_().f_91062_;
      MutableComponent label = Component.m_237115_("jei.ae2lt.lightning_transform.label");
      int labelX = (134 - font.m_92852_(label)) / 2;
      guiGraphics.m_280614_(font, label, labelX, 4, 4210752, false);
      this.lightningDisplay.draw(guiGraphics, 57, 26);
   }

   private static List<ItemStack> expandIngredient(Ingredient ingredient, int count) {
      return Arrays.stream(ingredient.m_43908_()).map(stack -> stack.m_255036_(count)).toList();
   }
}
