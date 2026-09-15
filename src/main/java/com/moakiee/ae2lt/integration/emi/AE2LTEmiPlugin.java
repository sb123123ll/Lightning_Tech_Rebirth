package com.moakiee.ae2lt.integration.emi;

import appeng.integration.modules.emi.EmiEncodePatternHandler;
import com.moakiee.ae2lt.integration.ae2wtlib.TianshuWirelessTerminalFactory;
import com.moakiee.ae2lt.integration.recipeviewer.multiblock.MultiblockStructureRecipes;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import com.moakiee.ae2lt.registry.ModBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

@EmiEntrypoint
public final class AE2LTEmiPlugin implements EmiPlugin {
   public static final EmiRecipeCategory MULTIBLOCK_STRUCTURE = new EmiRecipeCategory(
      new ResourceLocation("ae2lt", "multiblock_structure"), EmiStack.of((ItemLike)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get())
   ) {
      public Component getName() {
         return Component.m_237115_("jei.ae2lt.multiblock.title");
      }
   };

   public void register(EmiRegistry registry) {
      EmiMultiblockInputEvents.register();
      registry.addRecipeHandler(TianshuPatternEncodingTermMenu.TYPE, new EmiEncodePatternHandler(TianshuPatternEncodingTermMenu.class));
      if (TianshuWirelessTerminalFactory.isAvailable()) {
         registry.addRecipeHandler(TianshuWirelessPatternEncodingTermMenu.TYPE, new EmiEncodePatternHandler(TianshuWirelessPatternEncodingTermMenu.class));
      }

      AE2LTEmiCategories.register(registry);
      registry.addCategory(MULTIBLOCK_STRUCTURE);
      registry.addWorkstation(MULTIBLOCK_STRUCTURE, EmiStack.of((ItemLike)ModBlocks.MATTER_WARPING_MATRIX_CONTROLLER.get()));
      registry.addWorkstation(MULTIBLOCK_STRUCTURE, EmiStack.of((ItemLike)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get()));
      MultiblockStructureRecipes.all().stream().map(EmiMultiblockStructureRecipe::new).forEach(registry::addRecipe);
   }
}
