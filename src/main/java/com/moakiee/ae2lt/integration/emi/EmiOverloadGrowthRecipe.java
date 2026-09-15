package com.moakiee.ae2lt.integration.emi;

import appeng.core.definitions.AEBlocks;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.api.widget.TextWidget.Alignment;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ItemLike;

final class EmiOverloadGrowthRecipe extends BasicEmiRecipe {
   private static final int WIDTH = 150;
   private static final int CENTER_X = 75;
   private static final int LINKED_SLOT_SEED = 713239;
   private static final List<EmiStack> BUDDING = List.of(
      EmiStack.of((ItemLike)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()),
      EmiStack.of((ItemLike)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get()),
      EmiStack.of((ItemLike)ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL.get()),
      EmiStack.of((ItemLike)ModBlocks.FLAWLESS_BUDDING_OVERLOAD_CRYSTAL.get())
   );
   private static final List<EmiStack> IMPERFECT_BUDDING = BUDDING.subList(0, 3);
   private static final List<EmiStack> DECAY_ORDER = List.of(
      EmiStack.of((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get()),
      EmiStack.of((ItemLike)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()),
      EmiStack.of((ItemLike)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get()),
      EmiStack.of((ItemLike)ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL.get())
   );
   private static final List<EmiStack> IMPERFECT_DECAY_ORDER = DECAY_ORDER.subList(0, 3);
   private static final List<EmiStack> BUD_STAGES = List.of(
      EmiStack.of((ItemLike)ModBlocks.SMALL_OVERLOAD_CRYSTAL_BUD.get()),
      EmiStack.of((ItemLike)ModBlocks.MEDIUM_OVERLOAD_CRYSTAL_BUD.get()),
      EmiStack.of((ItemLike)ModBlocks.LARGE_OVERLOAD_CRYSTAL_BUD.get()),
      EmiStack.of((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get())
   );
   private final EmiOverloadGrowthRecipe.Page page;

   private EmiOverloadGrowthRecipe(EmiOverloadGrowthRecipe.Page page) {
      super(AE2LTEmiCategories.OVERLOAD_GROWTH, EmiRecipeWidgets.syntheticId("overload_growth/" + page.id), 150, 60);
      this.page = page;
      switch (page) {
         case BUD_GROWTH:
            this.catalysts.add(EmiIngredient.of(BUDDING));
            this.outputs.addAll(BUD_STAGES);
            break;
         case BUD_LOOT:
            this.inputs.add(EmiIngredient.of(BUD_STAGES.subList(0, 3)));
            this.outputs.add(EmiStack.of((ItemLike)ModItems.OVERLOAD_CRYSTAL_DUST.get()));
            break;
         case CLUSTER_LOOT:
            this.inputs.add(EmiStack.of((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get()));
            this.outputs.add(EmiStack.of((ItemLike)ModItems.OVERLOAD_CRYSTAL.get(), 4L));
            break;
         case DECAY:
            this.inputs.add(EmiIngredient.of(IMPERFECT_BUDDING));
            this.outputs.addAll(IMPERFECT_DECAY_ORDER);
            break;
         case MOVING:
            this.inputs.add(EmiIngredient.of(BUDDING));
            this.outputs.addAll(DECAY_ORDER);
            break;
         case ACCELERATION:
            this.inputs.add(EmiIngredient.of(BUDDING));
            this.catalysts.add(EmiStack.of(AEBlocks.GROWTH_ACCELERATOR.m_5456_()));
      }
   }

   static void registerAll(EmiRegistry registry) {
      for (EmiOverloadGrowthRecipe.Page page : EmiOverloadGrowthRecipe.Page.values()) {
         registry.addRecipe(new EmiOverloadGrowthRecipe(page));
      }
   }

   public void addWidgets(WidgetHolder widgets) {
      switch (this.page) {
         case BUD_GROWTH:
            title(widgets, "jei.ae2lt.overload_growth.bud_growth", 5);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.catalysts.get(0), 35, 25);
            widgets.addFillingArrow(63, 25, 2000);
            EmiRecipeWidgets.addSlot(widgets, EmiIngredient.of(BUD_STAGES), 97, 25).recipeContext(this);
            break;
         case BUD_LOOT:
            title(widgets, "jei.ae2lt.overload_growth.bud_loot", 5);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.inputs.get(0), 35, 25);
            widgets.addFillingArrow(63, 25, 2000);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.outputs.get(0), 97, 25).recipeContext(this);
            break;
         case CLUSTER_LOOT:
            title(widgets, "jei.ae2lt.overload_growth.cluster_loot", 5);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.inputs.get(0), 35, 25);
            widgets.addFillingArrow(63, 25, 2000);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.outputs.get(0), 97, 25).recipeContext(this);
            title(widgets, "jei.ae2lt.overload_growth.cluster_loot_fortune", 50);
            break;
         case DECAY:
            title(widgets, "jei.ae2lt.overload_growth.decay", 5);
            this.linkedSlots(widgets, IMPERFECT_BUDDING, IMPERFECT_DECAY_ORDER, 30);
            widgets.addFillingArrow(63, 30, 2000);
            EmiRecipeWidgets.centeredText(widgets, Component.m_237110_("jei.ae2lt.overload_growth.decay_chance", new Object[]{8}), 75, 50);
            break;
         case MOVING:
            this.linkedSlots(widgets, BUDDING, DECAY_ORDER, 0);
            widgets.addFillingArrow(63, 0, 2000);
            widgets.addDrawable(
               0,
               20,
               150,
               40,
               (graphics, mouseX, mouseY, delta) -> {
                  int y = 0;
                  Font font = Minecraft.m_91087_().f_91062_;

                  for (String key : List.of(
                     "jei.ae2lt.overload_growth.break_decay", "jei.ae2lt.overload_growth.silk_touch", "jei.ae2lt.overload_growth.flawless_note"
                  )) {
                     for (FormattedCharSequence line : font.m_92923_(Component.m_237115_(key), 146)) {
                        if (y + 9 > 60) {
                           return;
                        }

                        graphics.m_280649_(font, line, 2, y, 4210752, false);
                        y += 9;
                     }
                  }
               }
            );
            break;
         case ACCELERATION:
            title(widgets, "jei.ae2lt.overload_growth.acceleration", 4);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.inputs.get(0), 51, 40);
            widgets.addText(Component.m_237113_("+"), 75, 44, -1, true).horizontalAlign(Alignment.CENTER);
            EmiRecipeWidgets.addSlot(widgets, (EmiIngredient)this.catalysts.get(0), 83, 40).catalyst(true);
      }
   }

   private void linkedSlots(WidgetHolder widgets, List<EmiStack> inputVariants, List<EmiStack> outputVariants, int y) {
      widgets.addGeneratedSlot(random -> (EmiIngredient)inputVariants.get(random.nextInt(inputVariants.size())), 713239, 34, y - 1);
      widgets.addGeneratedSlot(random -> (EmiIngredient)outputVariants.get(random.nextInt(outputVariants.size())), 713239, 96, y - 1).recipeContext(this);
   }

   private static void title(WidgetHolder widgets, String key, int y) {
      EmiRecipeWidgets.centeredText(widgets, Component.m_237115_(key), 75, y);
   }

   public boolean supportsRecipeTree() {
      return false;
   }

   private static enum Page {
      BUD_GROWTH("bud_growth"),
      BUD_LOOT("bud_loot"),
      CLUSTER_LOOT("cluster_loot"),
      DECAY("budding_overload_decay"),
      MOVING("budding_overload_moving"),
      ACCELERATION("budding_overload_acceleration");

      private final String id;

      private Page(String id) {
         this.id = id;
      }
   }
}
