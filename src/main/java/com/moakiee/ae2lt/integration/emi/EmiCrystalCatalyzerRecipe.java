package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

final class EmiCrystalCatalyzerRecipe extends EmiBackedRecipe<CrystalCatalyzerRecipe> {
   private static final ResourceLocation TEXTURE = EmiRecipeWidgets.texture("guis/crystal_catalyzer.png");
   private static final int WIDTH = 128;
   private final EmiStack fluid = EmiRecipeWidgets.fluid(CrystalCatalyzerBlockEntity.getFixedFluidPerCycle());

   EmiCrystalCatalyzerRecipe(ResourceLocation id, CrystalCatalyzerRecipe recipe) {
      super(AE2LTEmiCategories.CRYSTAL_CATALYZER, id, recipe, 128, 114);
      this.inputs.add(this.fluid);
      recipe.catalyst().ifPresent(catalyst -> this.inputs.add(EmiRecipeWidgets.ingredient(catalyst, (long)recipe.catalystCount())));
      this.outputs.add(EmiStack.of(recipe.getOutputTemplate()));
   }

   public void addWidgets(WidgetHolder widgets) {
      widgets.addTexture(TEXTURE, 0, 0, 128, 62, 22, 14);
      FluidStack fixedFluid = CrystalCatalyzerBlockEntity.getFixedFluidPerCycle();
      widgets.addTank(this.fluid, 4, 4, 16, 53, Math.max(1, fixedFluid.getAmount()))
         .drawBack(false)
         .appendTooltip(Component.m_237110_("jei.ae2lt.crystal_catalyzer.fluid_fixed", new Object[]{fixedFluid.getAmount()}));
      if (this.inputs.size() > 1) {
         int perInstance = Math.max(1, this.recipe.catalystCount());
         EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.inputs.get(1), 34, 16)
            .drawBack(false)
            .appendTooltip(Component.m_237110_("jei.ae2lt.crystal_catalyzer.catalyst_parallel", new Object[]{perInstance, 256}));
      }

      int baseCount = this.recipe.getOutputTemplate().m_41613_();
      int matrixMultiplier = 4;
      int catalystPerInstance = Math.max(1, this.recipe.catalystCount());
      EmiRecipeWidgets.addLargeStackSlot(widgets, (EmiIngredient)this.outputs.get(0), 95, 16)
         .drawBack(false)
         .recipeContext(this)
         .appendTooltip(Component.m_237110_("jei.ae2lt.crystal_catalyzer.output_base", new Object[]{baseCount, matrixMultiplier}))
         .appendTooltip(Component.m_237110_("jei.ae2lt.crystal_catalyzer.output_parallel", new Object[]{catalystPerInstance}));
      widgets.addAnimatedTexture(TEXTURE, 52, 19, 35, 10, 176, 18, this.recipe.mode() == Mode.CRYSTAL ? 1000 : 2000, true, false, false);
      boolean compactText = widgets.getHeight() < 114;
      int firstLineY = compactText ? 60 : 64;
      int lineSpacing = compactText ? 8 : 10;
      statusText(
         widgets,
         Component.m_237110_("jei.ae2lt.crystal_catalyzer.energy", new Object[]{EmiRecipeWidgets.compactEnergy((long)this.recipe.energyPerCycle())}),
         firstLineY,
         compactText
      );
      statusText(
         widgets,
         Component.m_237110_("jei.ae2lt.crystal_catalyzer.time", new Object[]{this.recipe.mode() == Mode.CRYSTAL ? "1s" : "2s"}),
         firstLineY + lineSpacing,
         compactText
      );
      statusText(
         widgets,
         Component.m_237110_(
            "jei.ae2lt.crystal_catalyzer.lightning", new Object[]{this.recipe.lightningCost(), EmiRecipeWidgets.tierName(this.recipe.lightningTier())}
         ),
         firstLineY + lineSpacing * 2,
         compactText
      );
      statusText(widgets, Component.m_237115_("jei.ae2lt.crystal_catalyzer.matrix_note_line1"), firstLineY + lineSpacing * 3, compactText);
      statusText(
         widgets,
         Component.m_237110_("jei.ae2lt.crystal_catalyzer.matrix_note_line2", new Object[]{matrixMultiplier}),
         firstLineY + lineSpacing * 4,
         compactText
      );
   }

   private static void statusText(WidgetHolder widgets, Component text, int y, boolean compact) {
      if (!compact) {
         EmiRecipeWidgets.centeredText(widgets, text, 64, y);
      } else {
         widgets.addDrawable(0, y, 128, 8, (graphics, mouseX, mouseY, delta) -> {
            Font font = Minecraft.m_91087_().f_91062_;
            float scale = 0.8F;
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_85841_(scale, scale, 1.0F);
            int x = Math.round(64.0F / scale - (float)font.m_92852_(text) / 2.0F);
            graphics.m_280614_(font, text, x, 0, 4210752, false);
            graphics.m_280168_().m_85849_();
         });
      }
   }
}
