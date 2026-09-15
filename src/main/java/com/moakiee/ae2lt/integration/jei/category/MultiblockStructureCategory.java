package com.moakiee.ae2lt.integration.jei.category;

import com.moakiee.ae2lt.integration.jei.multiblock.InteractiveMultiblockWidget;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipe;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class MultiblockStructureCategory implements IRecipeCategory<MultiblockStructureRecipe> {
   public static final int WIDTH = 300;
   public static final int HEIGHT = 184;
   private static final int PANEL_WIDTH = 105;
   private static final int PANEL_X = 193;
   private static final int PANEL_CONTENT_TOP = 48;
   private static final int MATERIAL_ROW_STEP = 26;
   private static final int MATERIAL_SLOT_X = 197;
   private static final int MATERIAL_SLOT_Y = 52;
   private static final int SELECTED_SLOT_X = 197;
   private static final int SELECTED_SLOT_Y = 50;
   private static final int ALTERNATIVE_SLOT_X = 197;
   private static final int ALTERNATIVE_SLOT_Y = 111;
   private static final int ALTERNATIVE_COLUMN_STEP = 23;
   private static final int ALTERNATIVE_ROW_STEP = 21;
   private static final int ALTERNATIVE_COLUMNS = 4;
   public static final RecipeType<MultiblockStructureRecipe> TYPE = RecipeType.create("ae2lt", "multiblock_structure", MultiblockStructureRecipe.class);
   private final IDrawable icon;

   public MultiblockStructureCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(new ItemStack((ItemLike)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get()));
   }

   public RecipeType<MultiblockStructureRecipe> getRecipeType() {
      return TYPE;
   }

   public Component getTitle() {
      return Component.m_237115_("jei.ae2lt.multiblock.title");
   }

   public int getWidth() {
      return 300;
   }

   public int getHeight() {
      return 184;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, MultiblockStructureRecipe recipe, IFocusGroup focuses) {
      builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.focusStacks());
      List<MultiblockStructureRecipe.MaterialEntry> materials = recipe.materials();

      for (int i = 0; i < materials.size(); i++) {
         MultiblockStructureRecipe.MaterialEntry material = materials.get(i);
         ItemStack displayStack = new ItemStack(material.block());
         ItemStack transferStack = displayStack.m_41777_();
         transferStack.m_41764_(material.count());
         int y = 52 + i * 26;
         ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.CATALYST, 197, y)
               .setSlotName("multiblock_material_" + i)
               .setStandardSlotBackground()
               .addItemStack(displayStack))
            .addRichTooltipCallback((slot, tooltip) -> {
               tooltip.add(Component.m_237110_("jei.ae2lt.multiblock.count", new Object[]{material.count()}));
               if (!material.note().getString().isEmpty()) {
                  tooltip.add(material.note());
               }
            });
         builder.addSlot(RecipeIngredientRole.INPUT).setSlotName("multiblock_transfer_" + i).addItemStack(transferStack);
      }

      builder.addSlot(RecipeIngredientRole.CATALYST, 197, 50)
         .setSlotName("multiblock_selected_block")
         .setStandardSlotBackground()
         .addItemStacks(recipe.focusStacks());

      for (int i = 0; i < 7; i++) {
         int x = 197 + i % 4 * 23;
         int y = 111 + i / 4 * 21;
         builder.addSlot(RecipeIngredientRole.CATALYST, x, y)
            .setSlotName("multiblock_alternative_" + i)
            .setStandardSlotBackground()
            .addItemStacks(recipe.focusStacks());
      }

      builder.moveRecipeTransferButton(304, 170);
   }

   public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockStructureRecipe recipe, IFocusGroup focuses) {
      IRecipeSlotDrawablesView slots = builder.getRecipeSlots();
      List<IRecipeSlotDrawable> materialSlots = new ArrayList<>(recipe.materials().size());

      for (int i = 0; i < recipe.materials().size(); i++) {
         materialSlots.add(requireSlot(slots, "multiblock_material_" + i));
      }

      List<IRecipeSlotDrawable> transferSlots = new ArrayList<>(recipe.materials().size());

      for (int i = 0; i < recipe.materials().size(); i++) {
         transferSlots.add(requireSlot(slots, "multiblock_transfer_" + i));
      }

      IRecipeSlotDrawable selectedBlockSlot = requireSlot(slots, "multiblock_selected_block");
      List<IRecipeSlotDrawable> alternativeSlots = new ArrayList<>(7);

      for (int i = 0; i < 7; i++) {
         alternativeSlots.add(requireSlot(slots, "multiblock_alternative_" + i));
      }

      InteractiveMultiblockWidget widget = new InteractiveMultiblockWidget(recipe, 300, 184, materialSlots, selectedBlockSlot, alternativeSlots);
      List<IRecipeSlotDrawable> widgetSlots = new ArrayList<>(materialSlots);
      widgetSlots.addAll(transferSlots);
      widgetSlots.add(selectedBlockSlot);
      widgetSlots.addAll(alternativeSlots);
      builder.addSlottedWidget(widget, widgetSlots);
      builder.addInputHandler(widget);
   }

   private static IRecipeSlotDrawable requireSlot(IRecipeSlotDrawablesView slots, String name) {
      return (IRecipeSlotDrawable)slots.findSlotByName(name).orElseThrow(() -> new IllegalStateException("Missing JEI slot: " + name));
   }

   public ResourceLocation getRegistryName(MultiblockStructureRecipe recipe) {
      return recipe.id();
   }
}
