package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.lightning.strike.LightningStrikeRecipe;
import com.moakiee.ae2lt.lightning.strike.StructureRequirement;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class EmiLightningStrikeRecipe extends EmiBackedRecipe<LightningStrikeRecipe> {
   private static final int WIDTH = 178;
   private static final int MATERIALS_X = 104;
   private static final int MATERIALS_Y = 50;
   private final Map<Block, Integer> blockCounts = new LinkedHashMap<>();
   private final Map<Block, Boolean> blockConsumes = new HashMap<>();

   EmiLightningStrikeRecipe(ResourceLocation id, LightningStrikeRecipe recipe) {
      super(AE2LTEmiCategories.LIGHTNING_STRIKE, id, recipe, 178, 110);
      this.inputs.add(EmiStack.of(recipe.centerInput()));
      this.outputs.add(EmiStack.of(recipe.centerOutput()));

      for (StructureRequirement requirement : recipe.requirements()) {
         this.blockCounts.merge(requirement.block(), 1, Integer::sum);
         this.blockConsumes.merge(requirement.block(), requirement.consume(), (a, b) -> a || b);
      }

      this.blockCounts.forEach((block, count) -> {
         if (this.blockConsumes.getOrDefault(block, false)) {
            this.inputs.add(EmiStack.of(block, (long)count.intValue()));
         } else {
            this.catalysts.add(EmiStack.of(block, (long)count.intValue()));
         }
      });
   }

   public void addWidgets(WidgetHolder widgets) {
      widgets.addText(
         Component.m_237115_(this.recipe.requiresNaturalLightning() ? "jei.ae2lt.lightning_strike.natural_only" : "jei.ae2lt.lightning_strike.any_lightning")
            .m_130940_(this.recipe.requiresNaturalLightning() ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_AQUA),
         4,
         2,
         4210752,
         false
      );
      widgets.addText(Component.m_237115_("jei.ae2lt.lightning_strike.materials"), 104, 38, 4210752, false);
      EmiLightningStrikePreviewWidget.Builder preview = new EmiLightningStrikePreviewWidget.Builder(4, 14, 96, 92);

      for (StructureRequirement requirement : this.recipe.requirements()) {
         preview.addBlock(requirement.block(), requirement.offset());
      }

      preview.addBlock(this.recipe.centerInput(), BlockPos.f_121853_);
      preview.addBlock(Blocks.f_152587_, new BlockPos(0, 1, 0));
      widgets.add(preview.build());
      EmiRecipeWidgets.addSlot(widgets, EmiStack.of(this.recipe.centerInput()), 104, 14);
      widgets.addFillingArrow(124, 15, 2000);
      EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.outputs.get(0), 156, 14).recipeContext(this);
      int index = 0;

      for (Entry<Block, Integer> entry : this.blockCounts.entrySet()) {
         int x = 104 + index % 4 * 18;
         int y = 50 + index / 4 * 18;
         EmiStack stack = EmiStack.of((ItemLike)entry.getKey(), (long)entry.getValue().intValue());
         SlotWidget slot = EmiRecipeWidgets.addSlot(widgets, stack, x, y);
         if (!this.blockConsumes.getOrDefault(entry.getKey(), false)) {
            slot.catalyst(true);
         }

         index++;
      }
   }
}
