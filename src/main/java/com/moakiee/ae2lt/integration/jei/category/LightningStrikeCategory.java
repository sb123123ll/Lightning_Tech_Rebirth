package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.integration.jei.LightningJeiIngredients;
import com.moakiee.ae2lt.integration.jei.MultiblockPreviewWidget;
import com.moakiee.ae2lt.lightning.strike.LightningStrikeRecipe;
import com.moakiee.ae2lt.lightning.strike.StructureRequirement;
import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class LightningStrikeCategory implements IRecipeCategory<LightningStrikeRecipe> {
   public static final RecipeType<LightningStrikeRecipe> TYPE = RecipeType.create("ae2lt", "lightning_strike", LightningStrikeRecipe.class);
   private static final int WIDTH = 178;
   private static final int HEIGHT = 110;
   private static final int PREVIEW_X = 4;
   private static final int PREVIEW_Y = 14;
   private static final int PREVIEW_W = 96;
   private static final int PREVIEW_H = 92;
   private static final int CENTER_INPUT_X = 104;
   private static final int CENTER_INPUT_Y = 14;
   private static final int ARROW_X = 124;
   private static final int ARROW_Y = 15;
   private static final int CENTER_OUTPUT_X = 156;
   private static final int CENTER_OUTPUT_Y = 14;
   private static final int MATERIALS_LABEL_Y = 38;
   private static final int MATERIALS_X = 104;
   private static final int MATERIALS_Y = 50;
   private static final int MATERIAL_CELL = 18;
   private static final int MATERIALS_PER_ROW = 4;
   private static final int TEXT_COLOR = 4210752;
   private final IDrawable icon;

   public LightningStrikeCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableIngredient(LightningJeiIngredients.TYPE, LightningKey.HIGH_VOLTAGE);
   }

   public RecipeType<LightningStrikeRecipe> getRecipeType() {
      return TYPE;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.lightning_strike.title");
   }

   public int getWidth() {
      return 178;
   }

   public int getHeight() {
      return 110;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, LightningStrikeRecipe recipe, IFocusGroup focuses) {
      builder.addSlot(RecipeIngredientRole.INPUT, 104, 14).setStandardSlotBackground().addItemStack(new ItemStack(recipe.centerInput()));
      builder.addSlot(RecipeIngredientRole.OUTPUT, 156, 14).setOutputSlotBackground().addItemStack(new ItemStack(recipe.centerOutput()));
      Map<Block, Integer> blockCounts = new LinkedHashMap<>();
      Map<Block, Boolean> blockConsumes = new HashMap<>();

      for (StructureRequirement req : recipe.requirements()) {
         blockCounts.merge(req.block(), 1, Integer::sum);
         blockConsumes.merge(req.block(), req.consume(), (a, b) -> a || b);
      }

      int index = 0;

      for (Entry<Block, Integer> entry : blockCounts.entrySet()) {
         Block block = entry.getKey();
         int count = entry.getValue();
         int col = index % 4;
         int row = index / 4;
         int slotX = 104 + col * 18;
         int slotY = 50 + row * 18;
         builder.addSlot(blockConsumes.getOrDefault(block, false) ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CATALYST, slotX, slotY)
            .setStandardSlotBackground()
            .addItemStack(new ItemStack(block, count));
         index++;
      }
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, LightningStrikeRecipe recipe, IFocusGroup focuses) {
      builder.addRecipeArrow().setPosition(124, 15);
      MultiblockPreviewWidget.Builder widgetBuilder = MultiblockPreviewWidget.builder(4, 14, 96, 92);

      for (StructureRequirement req : recipe.requirements()) {
         widgetBuilder.addBlock(req.block(), req.offset());
      }

      widgetBuilder.addBlock(recipe.centerInput(), BlockPos.f_121853_);
      widgetBuilder.addBlock(Blocks.f_152587_, new BlockPos(0, 1, 0));
      builder.addWidget(widgetBuilder.build());
   }

   public void draw(LightningStrikeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      Font font = Minecraft.m_91087_().f_91062_;
      Component lightningLabel = recipe.requiresNaturalLightning()
         ? Component.m_237115_("jei.ae2lt.lightning_strike.natural_only").m_130940_(ChatFormatting.DARK_PURPLE)
         : Component.m_237115_("jei.ae2lt.lightning_strike.any_lightning").m_130940_(ChatFormatting.DARK_AQUA);
      guiGraphics.m_280614_(font, lightningLabel, 4, 2, 4210752, false);
      guiGraphics.m_280614_(font, Component.m_237115_("jei.ae2lt.lightning_strike.materials"), 104, 38, 4210752, false);
   }
}
