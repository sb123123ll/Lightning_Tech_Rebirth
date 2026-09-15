package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;

final class AE2LTEmiCategories {
   static final EmiRecipeCategory OVERLOAD_GROWTH = category("overload_growth", EmiStack.of((ItemLike)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get()));
   static final EmiRecipeCategory LIGHTNING_ASSEMBLY = category("lightning_assembly", EmiStack.of((ItemLike)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get()));
   static final EmiRecipeCategory LIGHTNING_SIMULATION = category("lightning_simulation", EmiStack.of((ItemLike)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get()));
   static final EmiRecipeCategory LIGHTNING_TRANSFORM = category("lightning_transform", new EmiLightningIcon(false));
   static final EmiRecipeCategory LIGHTNING_STRIKE = category("lightning_strike", new EmiLightningIcon(false));
   static final EmiRecipeCategory OVERLOAD_PROCESSING = category("overload_processing", EmiStack.of((ItemLike)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get()));
   static final EmiRecipeCategory TESLA_COIL = category("tesla_coil", EmiStack.of((ItemLike)ModBlocks.TESLA_COIL.get()));
   static final EmiRecipeCategory CRYSTAL_CATALYZER = category("crystal_catalyzer", EmiStack.of((ItemLike)ModBlocks.CRYSTAL_CATALYZER.get()));
   static final EmiRecipeCategory FIRMAMENT_CONVERSION = category("firmament_conversion", EmiStack.of((ItemLike)ModBlocks.FIRMAMENT_CONVERSION_CORE.get()));

   private AE2LTEmiCategories() {
   }

   static void register(EmiRegistry registry) {
      addCategory(registry, OVERLOAD_GROWTH);
      addCategory(registry, LIGHTNING_TRANSFORM);
      addCategory(registry, LIGHTNING_STRIKE);
      addCategory(registry, LIGHTNING_ASSEMBLY, new ItemStack((ItemLike)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get()));
      addCategory(registry, LIGHTNING_SIMULATION, new ItemStack((ItemLike)ModBlocks.LIGHTNING_SIMULATION_CHAMBER.get()));
      addCategory(registry, OVERLOAD_PROCESSING, new ItemStack((ItemLike)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get()));
      addCategory(registry, TESLA_COIL, new ItemStack((ItemLike)ModBlocks.TESLA_COIL.get()));
      addCategory(registry, CRYSTAL_CATALYZER, new ItemStack((ItemLike)ModBlocks.CRYSTAL_CATALYZER.get()));
      addCategory(registry, FIRMAMENT_CONVERSION, new ItemStack((ItemLike)ModBlocks.FIRMAMENT_CONVERSION_CORE.get()));
      EmiOverloadGrowthRecipe.registerAll(registry);
      EmiTeslaCoilRecipe.registerAll(registry);
      RecipeManager recipeManager = registry.getRecipeManager();
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_ASSEMBLY_TYPE.get())
         .forEach((id, recipe) -> registry.addRecipe(new EmiLightningAssemblyRecipe(id, recipe)));
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_SIMULATION_TYPE.get())
         .forEach((id, recipe) -> registry.addRecipe(new EmiLightningSimulationRecipe(id, recipe)));
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_TRANSFORM_TYPE.get())
         .forEach((id, recipe) -> registry.addRecipe(new EmiLightningTransformRecipe(id, recipe)));
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_STRIKE_TYPE.get())
         .forEach((id, recipe) -> registry.addRecipe(new EmiLightningStrikeRecipe(id, recipe)));
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get())
         .forEach((id, recipe) -> registry.addRecipe(new EmiOverloadProcessingRecipe(id, recipe)));
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.CRYSTAL_CATALYZER_TYPE.get()).forEach((id, recipe) -> {
         if (!recipe.getOutputTemplate().m_41619_()) {
            registry.addRecipe(new EmiCrystalCatalyzerRecipe(id, recipe));
         }
      });
      RecipeManagerByTypeAccess.byType(recipeManager, (RecipeType)ModRecipeTypes.FIRMAMENT_CONVERSION_TYPE.get())
         .forEach((id, recipe) -> registry.addRecipe(new EmiFirmamentConversionRecipe(id, recipe)));
   }

   private static EmiRecipeCategory category(final String path, EmiRenderable icon) {
      return new EmiRecipeCategory(new ResourceLocation("ae2lt", path), icon) {
         public Component getName() {
            return Component.m_237115_("jei.ae2lt." + path + ".title");
         }
      };
   }

   private static void addCategory(EmiRegistry registry, EmiRecipeCategory category, ItemStack... workstations) {
      registry.addCategory(category);

      for (ItemStack workstation : workstations) {
         registry.addWorkstation(category, EmiStack.of(workstation));
      }
   }
}
