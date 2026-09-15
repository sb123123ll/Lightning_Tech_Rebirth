package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

final class EmiMultiblockStructureRecipe implements EmiRecipe {
   private final MultiblockStructureRecipe structure;
   private final List<EmiIngredient> inputs;
   private final List<EmiStack> outputs;

   EmiMultiblockStructureRecipe(MultiblockStructureRecipe structure) {
      this.structure = structure;
      this.inputs = structure.materials().stream().map(material -> EmiStack.of(material.block(), (long)material.count())).toList();
      this.outputs = structure.focusStacks().stream().<EmiStack>map(EmiStack::of).toList();
   }

   public EmiRecipeCategory getCategory() {
      return AE2LTEmiPlugin.MULTIBLOCK_STRUCTURE;
   }

   public ResourceLocation getId() {
      return new ResourceLocation(this.structure.id().m_135827_(), "/" + this.structure.id().m_135815_());
   }

   public List<EmiIngredient> getInputs() {
      return this.inputs;
   }

   public List<EmiStack> getOutputs() {
      return this.outputs;
   }

   public int getDisplayWidth() {
      return 300;
   }

   public int getDisplayHeight() {
      return 184;
   }

   public void addWidgets(WidgetHolder widgets) {
      widgets.add(new EmiInteractiveMultiblockWidget(this.structure, widgets.getWidth(), widgets.getHeight()));
   }

   public boolean supportsRecipeTree() {
      return false;
   }

   public boolean hideCraftable() {
      return true;
   }

   @Nullable
   public Recipe<?> getBackingRecipe() {
      return null;
   }
}
