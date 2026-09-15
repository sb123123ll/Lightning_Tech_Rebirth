package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipe;
import com.moakiee.ae2lt.machine.lightningchamber.recipe.LightningSimulationIngredient;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.Arrays;
import java.util.List;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class LightningAssemblyCategory implements IRecipeCategory<LightningAssemblyRecipe> {
   public static final RecipeType<LightningAssemblyRecipe> TYPE = RecipeType.create("ae2lt", "lightning_assembly", LightningAssemblyRecipe.class);
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("ae2lt", "textures/guis/lightning_assembly_chamber.png");
   private static final int BACKGROUND_U = 19;
   private static final int BACKGROUND_V = 10;
   private static final int BACKGROUND_WIDTH = 156;
   private static final int BACKGROUND_HEIGHT = 78;
   private static final int WIDTH = 156;
   private static final int INPUT_START_X = 10;
   private static final int INPUT_START_Y = 21;
   private static final int INPUT_SPACING = 18;
   private static final int OUTPUT_X = 107;
   private static final int OUTPUT_Y = 39;
   private static final int ENERGY_TEXT_Y = 80;
   private static final int LIGHTNING_TEXT_Y = 90;
   private static final int HEIGHT = 100;
   private final IDrawable icon;
   private final IDrawable background;

   public LightningAssemblyCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get()));
      this.background = guiHelper.createDrawable(BACKGROUND_TEXTURE, 19, 10, 156, 78);
   }

   public RecipeType<LightningAssemblyRecipe> getRecipeType() {
      return TYPE;
   }

   public int getWidth() {
      return 156;
   }

   public int getHeight() {
      return 100;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.lightning_assembly.title");
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, LightningAssemblyRecipe recipe, IFocusGroup focuses) {
      for (int index = 0; index < recipe.inputs().size() && index < 9; index++) {
         LightningSimulationIngredient input = recipe.inputs().get(index);
         int col = index % 3;
         int row = index / 3;
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, 10 + col * 18, 21 + row * 18)
               .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
               .addItemStacks(expandIngredient(input.ingredient(), input.count())))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)input.count()));
      }

      ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 39)
            .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
            .addItemStack(recipe.getResultStack()))
         .addRichTooltipCallback((recipeSlotView, tooltip) -> LargeStackCountRenderer.appendCountTooltip(tooltip, (long)recipe.getResultStack().m_41613_()));
   }

   public void draw(LightningAssemblyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      this.background.draw(guiGraphics);
      Font font = Minecraft.m_91087_().f_91062_;
      MutableComponent energyText = Component.m_237110_("jei.ae2lt.lightning_assembly.energy", new Object[]{formatCompactEnergy(recipe.totalEnergy())});
      int energyX = (156 - font.m_92852_(energyText)) / 2;
      guiGraphics.m_280614_(font, energyText, energyX, 80, 4210752, false);
      MutableComponent lightningText = Component.m_237110_(
         "jei.ae2lt.lightning_assembly.lightning",
         new Object[]{
            recipe.lightningCost(),
            Component.m_237115_(
               recipe.lightningTier() == LightningKey.Tier.EXTREME_HIGH_VOLTAGE
                  ? "ae2lt.gui.lightning_simulation.tier.extreme_high_voltage"
                  : "ae2lt.gui.lightning_simulation.tier.high_voltage"
            )
         }
      );
      int lightningX = (156 - font.m_92852_(lightningText)) / 2;
      guiGraphics.m_280614_(font, lightningText, lightningX, 90, 4210752, false);
   }

   private static List<ItemStack> expandIngredient(Ingredient ingredient, int count) {
      return Arrays.stream(ingredient.m_43908_()).map(stack -> stack.m_255036_(count)).toList();
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
