package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.integration.jei.LargeStackJeiItemRenderer;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
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

public class CrystalCatalyzerCategory implements IRecipeCategory<CrystalCatalyzerRecipe> {
   public static final RecipeType<CrystalCatalyzerRecipe> TYPE = RecipeType.create("ae2lt", "crystal_catalyzer", CrystalCatalyzerRecipe.class);
   private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("ae2lt", "textures/guis/crystal_catalyzer.png");
   private static final int BACKGROUND_U = 22;
   private static final int BACKGROUND_V = 14;
   private static final int BACKGROUND_WIDTH = 128;
   private static final int BACKGROUND_HEIGHT = 62;
   private static final int WIDTH = 128;
   private static final int TEXTURE_WIDTH = 256;
   private static final int TEXTURE_HEIGHT = 256;
   private static final int FLUID_X = 4;
   private static final int FLUID_Y = 4;
   private static final int FLUID_WIDTH = 16;
   private static final int FLUID_HEIGHT = 53;
   private static final int CATALYST_X = 34;
   private static final int CATALYST_Y = 16;
   private static final int OUTPUT_X = 95;
   private static final int OUTPUT_Y = 16;
   private static final int PROCESS_X = 52;
   private static final int PROCESS_Y = 19;
   private static final int PROCESS_OVERLAY_U = 176;
   private static final int PROCESS_OVERLAY_V = 18;
   private static final int PROCESS_OVERLAY_WIDTH = 35;
   private static final int PROCESS_OVERLAY_HEIGHT = 10;
   private static final long PROCESS_CYCLE_MS_CRYSTAL = 1000L;
   private static final long PROCESS_CYCLE_MS_DUST = 2000L;
   private static final int ENERGY_TEXT_Y = 64;
   private static final int TIME_TEXT_Y = 74;
   private static final int LIGHTNING_TEXT_Y = 84;
   private static final int MATRIX_LINE1_Y = 94;
   private static final int MATRIX_LINE2_Y = 104;
   private static final int HEIGHT = 114;
   private final IDrawable icon;
   private final IDrawable background;

   public CrystalCatalyzerCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.CRYSTAL_CATALYZER.get()));
      this.background = guiHelper.createDrawable(BACKGROUND_TEXTURE, 22, 14, 128, 62);
   }

   public RecipeType<CrystalCatalyzerRecipe> getRecipeType() {
      return TYPE;
   }

   public int getWidth() {
      return 128;
   }

   public int getHeight() {
      return 114;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.crystal_catalyzer.title");
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, CrystalCatalyzerRecipe recipe, IFocusGroup focuses) {
      FluidStack fluid = CrystalCatalyzerBlockEntity.getFixedFluidPerCycle();
      int fluidDisplayCapacity = Math.max(1, fluid.getAmount());
      ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, 4, 4)
            .setFluidRenderer((long)fluidDisplayCapacity, false, 16, 53)
            .addIngredient(ForgeTypes.FLUID_STACK, fluid))
         .addRichTooltipCallback(
            (slotView, tooltip) -> tooltip.add(Component.m_237110_("jei.ae2lt.crystal_catalyzer.fluid_fixed", new Object[]{fluid.getAmount()}))
         );
      recipe.catalyst()
         .ifPresent(
            catalyst -> {
               int perInstance = Math.max(1, recipe.catalystCount());
               ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.INPUT, 34, 16)
                     .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
                     .addItemStacks(expandIngredient(catalyst, perInstance)))
                  .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                     LargeStackCountRenderer.appendCountTooltip(tooltip, (long)perInstance);
                     tooltip.add(Component.m_237110_("jei.ae2lt.crystal_catalyzer.catalyst_parallel", new Object[]{perInstance, 256}));
                  });
            }
         );
      ItemStack baseOutput = recipe.getOutputTemplate();
      int matrixMultiplier = 4;
      int baseCount = baseOutput.m_41613_();
      int catalystPerInstance = Math.max(1, recipe.catalystCount());
      ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 16)
            .setCustomRenderer(VanillaTypes.ITEM_STACK, LargeStackJeiItemRenderer.INSTANCE)
            .addItemStack(baseOutput))
         .addRichTooltipCallback((recipeSlotView, tooltip) -> {
            LargeStackCountRenderer.appendCountTooltip(tooltip, (long)baseCount);
            tooltip.add(Component.m_237110_("jei.ae2lt.crystal_catalyzer.output_base", new Object[]{baseCount, matrixMultiplier}));
            tooltip.add(Component.m_237110_("jei.ae2lt.crystal_catalyzer.output_parallel", new Object[]{catalystPerInstance}));
         });
   }

   public void draw(CrystalCatalyzerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
      this.background.draw(guiGraphics);
      this.drawProcessOverlay(guiGraphics, recipe.mode());
      Font font = Minecraft.m_91087_().f_91062_;
      MutableComponent energyText = Component.m_237110_("jei.ae2lt.crystal_catalyzer.energy", new Object[]{formatCompactEnergy((long)recipe.energyPerCycle())});
      int energyX = (128 - font.m_92852_(energyText)) / 2;
      guiGraphics.m_280614_(font, energyText, energyX, 64, 4210752, false);
      String timeStr = recipe.mode() == Mode.CRYSTAL ? "1s" : "2s";
      MutableComponent timeText = Component.m_237110_("jei.ae2lt.crystal_catalyzer.time", new Object[]{timeStr});
      int timeX = (128 - font.m_92852_(timeText)) / 2;
      guiGraphics.m_280614_(font, timeText, timeX, 74, 4210752, false);
      MutableComponent lightningText = Component.m_237110_(
         "jei.ae2lt.crystal_catalyzer.lightning",
         new Object[]{
            recipe.lightningCost(),
            Component.m_237115_(
               recipe.lightningTier() == LightningKey.Tier.EXTREME_HIGH_VOLTAGE
                  ? "ae2lt.gui.lightning_simulation.tier.extreme_high_voltage"
                  : "ae2lt.gui.lightning_simulation.tier.high_voltage"
            )
         }
      );
      int lightningX = (128 - font.m_92852_(lightningText)) / 2;
      guiGraphics.m_280614_(font, lightningText, lightningX, 84, 4210752, false);
      MutableComponent matrixLine1 = Component.m_237115_("jei.ae2lt.crystal_catalyzer.matrix_note_line1");
      int matrixLine1X = (128 - font.m_92852_(matrixLine1)) / 2;
      guiGraphics.m_280614_(font, matrixLine1, matrixLine1X, 94, 4210752, false);
      MutableComponent matrixLine2 = Component.m_237110_("jei.ae2lt.crystal_catalyzer.matrix_note_line2", new Object[]{4});
      int matrixLine2X = (128 - font.m_92852_(matrixLine2)) / 2;
      guiGraphics.m_280614_(font, matrixLine2, matrixLine2X, 104, 4210752, false);
   }

   private void drawProcessOverlay(GuiGraphics guiGraphics, Mode mode) {
      long cycleMs = mode == Mode.CRYSTAL ? 1000L : 2000L;
      long elapsed = Util.m_137550_() % cycleMs;
      double progress = (double)elapsed / (double)cycleMs;
      int width = Mth.m_14045_((int)Math.ceil(progress * 35.0), 0, 35);
      if (width > 0) {
         guiGraphics.m_280163_(BACKGROUND_TEXTURE, 52, 19, 176.0F, 18.0F, width, 10, 256, 256);
      }
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
