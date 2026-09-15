package com.moakiee.ae2lt.machine.crystalcatalyzer.recipe;

import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerInventory;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CrystalCatalyzerRecipeService {
   private CrystalCatalyzerRecipeService() {
   }

   public static Optional<CrystalCatalyzerRecipeCandidate> findRecipe(@Nullable Level level, CrystalCatalyzerInventory inventory) {
      return findRecipe(level, inventory, Mode.CRYSTAL);
   }

   public static Optional<CrystalCatalyzerRecipeCandidate> findRecipe(@Nullable Level level, CrystalCatalyzerInventory inventory, Mode mode) {
      if (level == null) {
         return Optional.empty();
      } else {
         CrystalCatalyzerRecipeInput input = CrystalCatalyzerRecipeInput.fromMachine(inventory);

         for (CrystalCatalyzerRecipe recipe : getRecipes(level)) {
            if (recipe.mode() == mode && !recipe.getOutputTemplate().m_41619_() && recipe.matches(input, level)) {
               return Optional.of(new CrystalCatalyzerRecipeCandidate(recipe));
            }
         }

         return Optional.empty();
      }
   }

   public static Optional<CrystalCatalyzerRecipeCandidate> findRecipeById(@Nullable Level level, ResourceLocation recipeId) {
      if (level == null) {
         return Optional.empty();
      } else {
         CrystalCatalyzerRecipe recipe = (CrystalCatalyzerRecipe)RecipeManagerByTypeAccess.findById(
               level.m_7465_(), (RecipeType)ModRecipeTypes.CRYSTAL_CATALYZER_TYPE.get(), recipeId
            )
            .orElse(null);
         return recipe == null ? Optional.empty() : Optional.of(new CrystalCatalyzerRecipeCandidate(recipe));
      }
   }

   public static boolean isKnownCatalyst(@Nullable Level level, ItemStack stack) {
      return isKnownCatalyst(level, stack, Mode.CRYSTAL);
   }

   public static boolean isKnownCatalyst(@Nullable Level level, ItemStack stack, Mode mode) {
      if (level != null && !stack.m_41619_()) {
         for (CrystalCatalyzerRecipe recipe : getRecipes(level)) {
            if (recipe.mode() == mode && !recipe.getOutputTemplate().m_41619_()) {
               Optional<Ingredient> catalyst = recipe.catalyst();
               if (catalyst.isPresent() && catalyst.get().test(stack)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static List<CrystalCatalyzerRecipe> getRecipes(Level level) {
      return level.m_7465_().m_44013_((RecipeType)ModRecipeTypes.CRYSTAL_CATALYZER_TYPE.get());
   }
}
