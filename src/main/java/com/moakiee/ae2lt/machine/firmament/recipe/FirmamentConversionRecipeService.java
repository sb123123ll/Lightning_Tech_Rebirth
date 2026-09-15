package com.moakiee.ae2lt.machine.firmament.recipe;

import com.moakiee.ae2lt.machine.firmament.FirmamentConversionInventory;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class FirmamentConversionRecipeService {
   private static final Comparator<Entry<ResourceLocation, FirmamentConversionRecipe>> RECIPE_ORDER = Comparator.<Entry<ResourceLocation, FirmamentConversionRecipe>>comparingInt(
         entry -> entry.getValue().priority()
      )
      .reversed()
      .thenComparing(Comparator.<Entry<ResourceLocation, FirmamentConversionRecipe>>comparingInt(entry -> entry.getValue().inputs().size()).reversed())
      .thenComparing(Comparator.<Entry<ResourceLocation, FirmamentConversionRecipe>>comparingInt(entry -> entry.getValue().totalInputCount()).reversed())
      .thenComparing(entry -> entry.getKey().toString());

   private FirmamentConversionRecipeService() {
   }

   public static Optional<FirmamentConversionRecipeCandidate> findFirstProcessable(Level level, FirmamentConversionInventory inventory) {
      if (level == null) {
         return Optional.empty();
      } else {
         FirmamentConversionRecipeInput input = FirmamentConversionRecipeInput.fromInventory(inventory);
         if (input.m_7983_()) {
            return Optional.empty();
         } else {
            List<Entry<ResourceLocation, FirmamentConversionRecipe>> recipes = new ArrayList<>(
               RecipeManagerByTypeAccess.byType(level.m_7465_(), (RecipeType)ModRecipeTypes.FIRMAMENT_CONVERSION_TYPE.get()).entrySet()
            );
            recipes.sort(RECIPE_ORDER);

            for (Entry<ResourceLocation, FirmamentConversionRecipe> entry : recipes) {
               FirmamentConversionRecipe recipe = entry.getValue();
               Optional<FirmamentConversionRecipeMatch> match = recipe.planMatch(input);
               if (!match.isEmpty() && canAcceptOutputs(inventory, recipe.getResultStacks())) {
                  return Optional.of(new FirmamentConversionRecipeCandidate(entry.getKey(), recipe, match.get()));
               }
            }

            return Optional.empty();
         }
      }
   }

   public static Optional<FirmamentConversionRecipeCandidate> findRecipeById(Level level, ResourceLocation recipeId) {
      return level != null && recipeId != null
         ? RecipeManagerByTypeAccess.findById(level.m_7465_(), (RecipeType)ModRecipeTypes.FIRMAMENT_CONVERSION_TYPE.get(), recipeId)
            .map(recipe -> new FirmamentConversionRecipeCandidate(recipeId, recipe, null))
         : Optional.empty();
   }

   public static Optional<FirmamentConversionRecipeCandidate> findLockedRecipeMatch(
      Level level, FirmamentConversionInventory inventory, FirmamentConversionLockedRecipe lockedRecipe
   ) {
      if (level != null && lockedRecipe != null) {
         Optional<FirmamentConversionRecipeCandidate> recipe = findRecipeById(level, lockedRecipe.recipeId());
         if (!recipe.isEmpty() && recipe.get().recipe().processTime() == lockedRecipe.processTime()) {
            FirmamentConversionRecipeInput input = FirmamentConversionRecipeInput.fromInventory(inventory);
            if (input.m_7983_()) {
               return Optional.empty();
            } else {
               Optional<FirmamentConversionRecipeMatch> match = recipe.get().recipe().planMatch(input);
               if (match.isEmpty()) {
                  return Optional.empty();
               } else {
                  return !canAcceptOutputs(inventory, recipe.get().recipe().getResultStacks())
                     ? Optional.empty()
                     : Optional.of(new FirmamentConversionRecipeCandidate(recipe.get().recipeId(), recipe.get().recipe(), match.get()));
               }
            }
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   public static boolean canAcceptOutput(FirmamentConversionInventory inventory, ItemStack result) {
      return inventory.canAcceptRecipeOutput(result);
   }

   public static boolean canAcceptOutputs(FirmamentConversionInventory inventory, List<ItemStack> results) {
      return inventory.canAcceptRecipeOutputs(results);
   }
}
