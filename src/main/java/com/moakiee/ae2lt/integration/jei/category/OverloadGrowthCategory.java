package com.moakiee.ae2lt.integration.jei.category;

import appeng.core.definitions.AEBlocks;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class OverloadGrowthCategory extends AbstractRecipeCategory<OverloadGrowthCategory.Page> {
   public static final RecipeType<OverloadGrowthCategory.Page> TYPE = RecipeType.create("ae2lt", "overload_growth", OverloadGrowthCategory.Page.class);
   private static final int WIDTH = 150;
   private static final int HEIGHT = 60;
   private static final int BODY_COLOR = -12566464;
   private final List<ItemStack> buddingOverloadVariants = List.of(
      new ItemStack((ItemLike)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.FLAWLESS_BUDDING_OVERLOAD_CRYSTAL.get())
   );
   private final List<ItemStack> imperfectBuddingOverloadVariants = List.of(
      new ItemStack((ItemLike)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL.get())
   );
   private final List<ItemStack> buddingOverloadDecayOrder = List.of(
      new ItemStack((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get()),
      new ItemStack((ItemLike)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL.get())
   );
   private final List<ItemStack> imperfectBuddingOverloadDecayOrder = List.of(
      new ItemStack((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get()),
      new ItemStack((ItemLike)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()),
      new ItemStack((ItemLike)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get())
   );
   private final List<ItemStack> budGrowthStages = List.of(
      new ItemStack((ItemLike)ModBlocks.SMALL_OVERLOAD_CRYSTAL_BUD.get()),
      new ItemStack((ItemLike)ModBlocks.MEDIUM_OVERLOAD_CRYSTAL_BUD.get()),
      new ItemStack((ItemLike)ModBlocks.LARGE_OVERLOAD_CRYSTAL_BUD.get()),
      new ItemStack((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get())
   );
   private final int centerX = 75;

   public OverloadGrowthCategory(IGuiHelper guiHelper) {
      super(
         TYPE,
         Component.m_237115_("jei.ae2lt.overload_growth.title"),
         guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get())),
         150,
         60
      );
   }

   public void setRecipe(IRecipeLayoutBuilder builder, OverloadGrowthCategory.Page recipe, IFocusGroup focuses) {
      this.getView(recipe).buildSlots(builder);
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, OverloadGrowthCategory.Page recipe, IFocusGroup focuses) {
      this.getView(recipe).createRecipeExtras(builder, focuses);
   }

   private OverloadGrowthCategory.View getView(OverloadGrowthCategory.Page page) {
      return (OverloadGrowthCategory.View)(switch (page) {
         case BUD_GROWTH -> new OverloadGrowthCategory.BudGrowthView();
         case BUD_LOOT, CLUSTER_LOOT -> new OverloadGrowthCategory.LootView(page);
         case BUDDING_OVERLOAD_DECAY -> new OverloadGrowthCategory.BuddingOverloadDecayView();
         case BUDDING_OVERLOAD_MOVING -> new OverloadGrowthCategory.BuddingOverloadMovingView();
         case BUDDING_OVERLOAD_ACCELERATION -> new OverloadGrowthCategory.BuddingOverloadAccelerationView();
      });
   }

   private class BudGrowthView implements OverloadGrowthCategory.View {
      @Override
      public void createRecipeExtras(IRecipeExtrasBuilder builder, IFocusGroup focuses) {
         builder.addText(Component.m_237115_("jei.ae2lt.overload_growth.bud_growth"), 150, 20).setTextAlignment(HorizontalAlignment.CENTER).setColor(-12566464);
         builder.addRecipeArrow().setPosition(63, 25);
      }

      @Override
      public void buildSlots(IRecipeLayoutBuilder builder) {
         builder.addSlot(RecipeIngredientRole.CATALYST, 35, 25).setStandardSlotBackground().addItemStacks(OverloadGrowthCategory.this.buddingOverloadVariants);
         builder.addOutputSlot(97, 25).setStandardSlotBackground().addItemStacks(OverloadGrowthCategory.this.budGrowthStages);
      }
   }

   private class BuddingOverloadAccelerationView implements OverloadGrowthCategory.View {
      @Override
      public void createRecipeExtras(IRecipeExtrasBuilder builder, IFocusGroup focuses) {
         builder.addText(Component.m_237115_("jei.ae2lt.overload_growth.acceleration"), 150, 38)
            .setTextAlignment(HorizontalAlignment.CENTER)
            .setLineSpacing(0)
            .setColor(-12566464);
         ((ITextWidget)builder.addText(Component.m_237113_("+"), 16, 16).setPosition(67, 40))
            .setTextAlignment(HorizontalAlignment.CENTER)
            .setTextAlignment(VerticalAlignment.CENTER)
            .setColor(-1)
            .setShadow(true);
      }

      @Override
      public void buildSlots(IRecipeLayoutBuilder builder) {
         builder.addInputSlot(51, 40).setStandardSlotBackground().addItemStacks(OverloadGrowthCategory.this.buddingOverloadVariants);
         builder.addSlot(RecipeIngredientRole.CATALYST, 83, 40).setStandardSlotBackground().addItemLike(AEBlocks.GROWTH_ACCELERATOR);
      }
   }

   private class BuddingOverloadDecayView implements OverloadGrowthCategory.View {
      @Override
      public void createRecipeExtras(IRecipeExtrasBuilder builder, IFocusGroup focuses) {
         builder.addText(Component.m_237115_("jei.ae2lt.overload_growth.decay"), 150, 20).setTextAlignment(HorizontalAlignment.CENTER).setColor(-12566464);
         builder.addRecipeArrow().setPosition(63, 30);
         int decayChancePct = 8;
         ((ITextWidget)builder.addText(Component.m_237110_("jei.ae2lt.overload_growth.decay_chance", new Object[]{decayChancePct}), 150, 10).setPosition(0, 50))
            .setTextAlignment(HorizontalAlignment.CENTER)
            .setColor(-12566464);
      }

      @Override
      public void buildSlots(IRecipeLayoutBuilder builder) {
         IRecipeSlotBuilder input = (IRecipeSlotBuilder)builder.addInputSlot(35, 30)
            .setStandardSlotBackground()
            .addItemStacks(OverloadGrowthCategory.this.imperfectBuddingOverloadVariants);
         IRecipeSlotBuilder output = (IRecipeSlotBuilder)builder.addOutputSlot(97, 30)
            .setStandardSlotBackground()
            .addItemStacks(OverloadGrowthCategory.this.imperfectBuddingOverloadDecayOrder);
         builder.createFocusLink(new IIngredientAcceptor[]{input, output});
      }
   }

   private class BuddingOverloadMovingView implements OverloadGrowthCategory.View {
      @Override
      public void createRecipeExtras(IRecipeExtrasBuilder builder, IFocusGroup focuses) {
         builder.addRecipeArrow().setPosition(63, 0);
         builder.addScrollBoxWidget(150, 40, 0, 20)
            .setContents(
               List.of(
                  Component.m_237115_("jei.ae2lt.overload_growth.break_decay").m_130938_(style -> style.m_178520_(-12566464)),
                  Component.m_237115_("jei.ae2lt.overload_growth.silk_touch").m_130938_(style -> style.m_178520_(-12566464)),
                  Component.m_237115_("jei.ae2lt.overload_growth.flawless_note").m_130938_(style -> style.m_178520_(-12566464))
               )
            );
      }

      @Override
      public void buildSlots(IRecipeLayoutBuilder builder) {
         IRecipeSlotBuilder input = (IRecipeSlotBuilder)builder.addInputSlot(35, 0)
            .setStandardSlotBackground()
            .addItemStacks(OverloadGrowthCategory.this.buddingOverloadVariants);
         IRecipeSlotBuilder output = (IRecipeSlotBuilder)builder.addOutputSlot(97, 0)
            .setStandardSlotBackground()
            .addItemStacks(OverloadGrowthCategory.this.buddingOverloadDecayOrder);
         builder.createFocusLink(new IIngredientAcceptor[]{input, output});
      }
   }

   private class LootView implements OverloadGrowthCategory.View {
      private final OverloadGrowthCategory.Page page;

      private LootView(OverloadGrowthCategory.Page page) {
         this.page = page;
      }

      @Override
      public void createRecipeExtras(IRecipeExtrasBuilder builder, IFocusGroup focuses) {
         String key = this.page == OverloadGrowthCategory.Page.BUD_LOOT ? "jei.ae2lt.overload_growth.bud_loot" : "jei.ae2lt.overload_growth.cluster_loot";
         builder.addText(Component.m_237115_(key), 150, 20).setTextAlignment(HorizontalAlignment.CENTER).setColor(-12566464);
         builder.addRecipeArrow().setPosition(63, 25);
         if (this.page == OverloadGrowthCategory.Page.CLUSTER_LOOT) {
            ((ITextWidget)builder.addText(Component.m_237115_("jei.ae2lt.overload_growth.cluster_loot_fortune"), 150, 10).setPosition(0, 50))
               .setTextAlignment(HorizontalAlignment.CENTER)
               .setColor(-12566464);
         }
      }

      @Override
      public void buildSlots(IRecipeLayoutBuilder builder) {
         if (this.page == OverloadGrowthCategory.Page.BUD_LOOT) {
            builder.addInputSlot(35, 25)
               .setStandardSlotBackground()
               .addItemStacks(
                  List.of(
                     new ItemStack((ItemLike)ModBlocks.SMALL_OVERLOAD_CRYSTAL_BUD.get()),
                     new ItemStack((ItemLike)ModBlocks.MEDIUM_OVERLOAD_CRYSTAL_BUD.get()),
                     new ItemStack((ItemLike)ModBlocks.LARGE_OVERLOAD_CRYSTAL_BUD.get())
                  )
               );
            builder.addOutputSlot(97, 25).setStandardSlotBackground().addItemStack(new ItemStack((ItemLike)ModItems.OVERLOAD_CRYSTAL_DUST.get()));
         } else {
            builder.addInputSlot(35, 25).setStandardSlotBackground().addItemStack(new ItemStack((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get()));
            builder.addOutputSlot(97, 25).setStandardSlotBackground().addItemStack(new ItemStack((ItemLike)ModItems.OVERLOAD_CRYSTAL.get(), 4));
         }
      }
   }

   public static enum Page {
      BUD_GROWTH,
      BUD_LOOT,
      CLUSTER_LOOT,
      BUDDING_OVERLOAD_DECAY,
      BUDDING_OVERLOAD_MOVING,
      BUDDING_OVERLOAD_ACCELERATION;
   }

   private interface View {
      default void buildSlots(IRecipeLayoutBuilder builder) {
      }

      default void createRecipeExtras(IRecipeExtrasBuilder builder, IFocusGroup focuses) {
      }
   }
}
