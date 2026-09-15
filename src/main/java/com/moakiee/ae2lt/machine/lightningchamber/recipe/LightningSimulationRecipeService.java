package com.moakiee.ae2lt.machine.lightningchamber.recipe;

import com.moakiee.ae2lt.machine.lightningchamber.LightningSimulationChamberInventory;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class LightningSimulationRecipeService {
   public static final int EXTREME_TO_HIGH_RATIO = 4;
   private static final Comparator<LightningSimulationRecipe> RECIPE_ORDER = Comparator.comparingInt(LightningSimulationRecipe::priority)
      .reversed()
      .thenComparing(Comparator.<LightningSimulationRecipe>comparingInt(recipe -> recipe.inputs().size()).reversed())
      .thenComparing(Comparator.comparingInt(LightningSimulationRecipe::totalInputCount).reversed())
      .thenComparing(recipe -> recipe.m_6423_().toString());
   private static RecipeManager cachedRecipeManager;
   private static List<LightningSimulationRecipe> sortedRecipeCache;
   private static int cachedRecipeOrderFingerprint;

   private LightningSimulationRecipeService() {
   }

   private static synchronized List<LightningSimulationRecipe> getSortedRecipes(Level level) {
      RecipeManager recipeManager = level.m_7465_();
      List<LightningSimulationRecipe> raw = recipeManager.m_44013_((RecipeType)ModRecipeTypes.LIGHTNING_SIMULATION_TYPE.get());
      int orderFingerprint = computeRecipeOrderFingerprint(raw);
      if (recipeManager != cachedRecipeManager || orderFingerprint != cachedRecipeOrderFingerprint || sortedRecipeCache == null) {
         sortedRecipeCache = new ArrayList<>(raw);
         sortedRecipeCache.sort(RECIPE_ORDER);
         cachedRecipeManager = recipeManager;
         cachedRecipeOrderFingerprint = orderFingerprint;
      }

      return sortedRecipeCache;
   }

   private static int computeRecipeOrderFingerprint(List<LightningSimulationRecipe> recipes) {
      int hash = 1;

      for (LightningSimulationRecipe recipe : recipes) {
         hash = 31 * hash + recipe.m_6423_().hashCode();
         hash = 31 * hash + System.identityHashCode(recipe);
         hash = 31 * hash + recipe.priority();
         hash = 31 * hash + recipe.inputs().size();
         hash = 31 * hash + recipe.totalInputCount();
      }

      return hash;
   }

   public static Optional<LightningSimulationRecipeCandidate> findFirstProcessable(
      Level level, LightningSimulationChamberInventory inventory, long availableHighVoltage, long availableExtremeHighVoltage
   ) {
      if (level == null) {
         return Optional.empty();
      } else {
         LightningSimulationRecipeInput input = LightningSimulationRecipeInput.fromInventory(inventory);
         if (input.m_7983_()) {
            return Optional.empty();
         } else {
            for (LightningSimulationRecipe recipe : getSortedRecipes(level)) {
               Optional<LightningSimulationRecipeMatch> match = recipe.planMatch(input);
               if (!match.isEmpty()
                  && !resolveLightningConsumption(inventory, recipe.lightningTier(), recipe.lightningCost(), availableHighVoltage, availableExtremeHighVoltage)
                     .isEmpty()
                  && canAcceptOutput(inventory, recipe.getResultStack())) {
                  return Optional.of(new LightningSimulationRecipeCandidate(recipe, match.get()));
               }
            }

            return Optional.empty();
         }
      }
   }

   public static Optional<LightningSimulationRecipe> findRecipeById(Level level, ResourceLocation recipeId) {
      return level != null && recipeId != null
         ? RecipeManagerByTypeAccess.findById(level.m_7465_(), (RecipeType)ModRecipeTypes.LIGHTNING_SIMULATION_TYPE.get(), recipeId)
         : Optional.empty();
   }

   public static Optional<LightningSimulationRecipeCandidate> findLockedRecipeMatch(
      Level level,
      LightningSimulationChamberInventory inventory,
      LightningSimulationLockedRecipe lockedRecipe,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      if (level != null && lockedRecipe != null) {
         Optional<LightningSimulationRecipe> recipe = findRecipeById(level, lockedRecipe.recipeId());
         if (recipe.isEmpty()) {
            return Optional.empty();
         } else {
            LightningSimulationRecipeInput input = LightningSimulationRecipeInput.fromInventory(inventory);
            if (input.m_7983_()) {
               return Optional.empty();
            } else {
               Optional<LightningSimulationRecipeMatch> match = recipe.get().planMatch(input);
               if (match.isEmpty()) {
                  return Optional.empty();
               } else {
                  return resolveLightningConsumption(
                           inventory, lockedRecipe.lightningTier(), lockedRecipe.lightningCost(), availableHighVoltage, availableExtremeHighVoltage
                        )
                        .isEmpty()
                     ? Optional.empty()
                     : Optional.of(new LightningSimulationRecipeCandidate(recipe.get(), match.get()));
               }
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public static Optional<LightningSimulationRecipeCandidate> findLockedRecipeMatchIgnoringLightning(
      Level level, LightningSimulationChamberInventory inventory, LightningSimulationLockedRecipe lockedRecipe
   ) {
      if (level != null && lockedRecipe != null) {
         Optional<LightningSimulationRecipe> recipe = findRecipeById(level, lockedRecipe.recipeId());
         if (recipe.isEmpty()) {
            return Optional.empty();
         } else {
            LightningSimulationRecipeInput input = LightningSimulationRecipeInput.fromInventory(inventory);
            if (input.m_7983_()) {
               return Optional.empty();
            } else {
               Optional<LightningSimulationRecipeMatch> match = recipe.get().planMatch(input);
               return match.isEmpty() ? Optional.empty() : Optional.of(new LightningSimulationRecipeCandidate(recipe.get(), match.get()));
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public static Optional<LightningSimulationRecipeService.LightningConsumptionPlan> resolveLightningConsumption(
      LightningSimulationChamberInventory inventory,
      LightningKey.Tier lightningTier,
      int lightningCost,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      if (lightningCost <= 0) {
         return Optional.empty();
      } else if (lightningTier == LightningKey.Tier.HIGH_VOLTAGE) {
         return availableHighVoltage >= (long)lightningCost
            ? Optional.of(new LightningSimulationRecipeService.LightningConsumptionPlan(LightningKey.HIGH_VOLTAGE, (long)lightningCost, false))
            : Optional.empty();
      } else if (availableExtremeHighVoltage >= (long)lightningCost) {
         return Optional.of(new LightningSimulationRecipeService.LightningConsumptionPlan(LightningKey.EXTREME_HIGH_VOLTAGE, (long)lightningCost, false));
      } else {
         long highVoltageEquivalent = (long)lightningCost * 4L;
         return inventory.hasLightningCollapseMatrix() && availableHighVoltage >= highVoltageEquivalent
            ? Optional.of(new LightningSimulationRecipeService.LightningConsumptionPlan(LightningKey.HIGH_VOLTAGE, highVoltageEquivalent, true))
            : Optional.empty();
      }
   }

   public static long getEquivalentHighVoltageCost(LightningKey.Tier lightningTier, int lightningCost) {
      return lightningTier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? (long)lightningCost * 4L : (long)lightningCost;
   }

   public static boolean canAcceptOutput(LightningSimulationChamberInventory inventory, ItemStack result) {
      return inventory.canAcceptRecipeOutput(result);
   }

   public static record LightningConsumptionPlan(LightningKey key, long amount, boolean matrixSubstitution) {
   }
}
