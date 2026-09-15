package com.moakiee.ae2lt.machine.lightningassembly.recipe;

import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberInventory;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class LightningAssemblyRecipeService {
   public static final int EXTREME_TO_HIGH_RATIO = 4;
   private static final Comparator<LightningAssemblyRecipe> RECIPE_ORDER = Comparator.comparingInt(LightningAssemblyRecipe::priority)
      .reversed()
      .thenComparing(Comparator.<LightningAssemblyRecipe>comparingInt(recipe -> recipe.inputs().size()).reversed())
      .thenComparing(Comparator.comparingInt(LightningAssemblyRecipe::totalInputCount).reversed())
      .thenComparing(recipe -> recipe.m_6423_().toString());
   private static Object cachedRawRecipeList;
   private static RecipeManager cachedRecipeManager;
   private static List<LightningAssemblyRecipe> sortedRecipeCache;
   private static int cachedRecipeOrderFingerprint;

   private LightningAssemblyRecipeService() {
   }

   private static synchronized List<LightningAssemblyRecipe> getSortedRecipes(Level level) {
      RecipeManager recipeManager = level.m_7465_();
      Map<ResourceLocation, LightningAssemblyRecipe> raw = RecipeManagerByTypeAccess.byType(
         recipeManager, (RecipeType)ModRecipeTypes.LIGHTNING_ASSEMBLY_TYPE.get()
      );
      int orderFingerprint = computeRecipeOrderFingerprint(raw);
      if (recipeManager != cachedRecipeManager || raw != cachedRawRecipeList || orderFingerprint != cachedRecipeOrderFingerprint || sortedRecipeCache == null) {
         sortedRecipeCache = new ArrayList<>(raw.values());
         sortedRecipeCache.sort(RECIPE_ORDER);
         cachedRecipeManager = recipeManager;
         cachedRawRecipeList = raw;
         cachedRecipeOrderFingerprint = orderFingerprint;
      }

      return sortedRecipeCache;
   }

   private static int computeRecipeOrderFingerprint(Map<ResourceLocation, LightningAssemblyRecipe> recipes) {
      int hash = 1;

      for (LightningAssemblyRecipe recipe : recipes.values()) {
         hash = 31 * hash + recipe.m_6423_().hashCode();
         hash = 31 * hash + recipe.priority();
         hash = 31 * hash + recipe.inputs().size();
         hash = 31 * hash + recipe.totalInputCount();
      }

      return hash;
   }

   public static synchronized void invalidateSortedRecipeCache() {
      cachedRawRecipeList = null;
      cachedRecipeManager = null;
      sortedRecipeCache = null;
      cachedRecipeOrderFingerprint = 0;
   }

   public static Optional<LightningAssemblyRecipeCandidate> findFirstProcessable(
      Level level, LightningAssemblyChamberInventory inventory, long availableHighVoltage, long availableExtremeHighVoltage
   ) {
      if (level == null) {
         return Optional.empty();
      } else {
         LightningAssemblyRecipeInput input = LightningAssemblyRecipeInput.fromInventory(inventory);
         if (input.m_7983_()) {
            return Optional.empty();
         } else {
            for (LightningAssemblyRecipe recipe : getSortedRecipes(level)) {
               Optional<LightningAssemblyRecipeMatch> match = recipe.planMatch(input);
               if (!match.isEmpty()
                  && !resolveLightningConsumption(inventory, recipe.lightningTier(), recipe.lightningCost(), availableHighVoltage, availableExtremeHighVoltage)
                     .isEmpty()
                  && canAcceptOutput(inventory, recipe.getResultStack())) {
                  return Optional.of(new LightningAssemblyRecipeCandidate(recipe, match.get()));
               }
            }

            return Optional.empty();
         }
      }
   }

   public static Optional<LightningAssemblyRecipe> findRecipeById(Level level, ResourceLocation recipeId) {
      return level != null && recipeId != null
         ? RecipeManagerByTypeAccess.findById(level.m_7465_(), (RecipeType)ModRecipeTypes.LIGHTNING_ASSEMBLY_TYPE.get(), recipeId)
         : Optional.empty();
   }

   public static Optional<LightningAssemblyRecipeCandidate> findLockedRecipeMatch(
      Level level,
      LightningAssemblyChamberInventory inventory,
      LightningAssemblyLockedRecipe lockedRecipe,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      if (level != null && lockedRecipe != null) {
         Optional<LightningAssemblyRecipe> recipe = findRecipeById(level, lockedRecipe.recipeId());
         if (recipe.isEmpty()) {
            return Optional.empty();
         } else {
            LightningAssemblyRecipeInput input = LightningAssemblyRecipeInput.fromInventory(inventory);
            if (input.m_7983_()) {
               return Optional.empty();
            } else {
               Optional<LightningAssemblyRecipeMatch> match = recipe.get().planMatch(input);
               if (match.isEmpty()) {
                  return Optional.empty();
               } else {
                  return resolveLightningConsumption(
                           inventory, lockedRecipe.lightningTier(), lockedRecipe.lightningCost(), availableHighVoltage, availableExtremeHighVoltage
                        )
                        .isEmpty()
                     ? Optional.empty()
                     : Optional.of(new LightningAssemblyRecipeCandidate(recipe.get(), match.get()));
               }
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public static Optional<LightningAssemblyRecipeCandidate> findLockedRecipeMatchIgnoringLightning(
      Level level, LightningAssemblyChamberInventory inventory, LightningAssemblyLockedRecipe lockedRecipe
   ) {
      if (level != null && lockedRecipe != null) {
         Optional<LightningAssemblyRecipe> recipe = findRecipeById(level, lockedRecipe.recipeId());
         if (recipe.isEmpty()) {
            return Optional.empty();
         } else {
            LightningAssemblyRecipeInput input = LightningAssemblyRecipeInput.fromInventory(inventory);
            if (input.m_7983_()) {
               return Optional.empty();
            } else {
               Optional<LightningAssemblyRecipeMatch> match = recipe.get().planMatch(input);
               return match.isEmpty() ? Optional.empty() : Optional.of(new LightningAssemblyRecipeCandidate(recipe.get(), match.get()));
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public static Optional<LightningAssemblyRecipeService.LightningConsumptionPlan> resolveLightningConsumption(
      LightningAssemblyChamberInventory inventory,
      LightningKey.Tier lightningTier,
      int lightningCost,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      if (lightningCost <= 0) {
         return Optional.empty();
      } else if (lightningTier == LightningKey.Tier.HIGH_VOLTAGE) {
         return availableHighVoltage >= (long)lightningCost
            ? Optional.of(new LightningAssemblyRecipeService.LightningConsumptionPlan(LightningKey.HIGH_VOLTAGE, (long)lightningCost, false))
            : Optional.empty();
      } else if (availableExtremeHighVoltage >= (long)lightningCost) {
         return Optional.of(new LightningAssemblyRecipeService.LightningConsumptionPlan(LightningKey.EXTREME_HIGH_VOLTAGE, (long)lightningCost, false));
      } else {
         long highVoltageEquivalent = (long)lightningCost * 4L;
         return inventory.hasLightningCollapseMatrix() && availableHighVoltage >= highVoltageEquivalent
            ? Optional.of(new LightningAssemblyRecipeService.LightningConsumptionPlan(LightningKey.HIGH_VOLTAGE, highVoltageEquivalent, true))
            : Optional.empty();
      }
   }

   public static long getEquivalentHighVoltageCost(LightningKey.Tier lightningTier, int lightningCost) {
      return lightningTier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? (long)lightningCost * 4L : (long)lightningCost;
   }

   public static boolean canAcceptOutput(LightningAssemblyChamberInventory inventory, ItemStack result) {
      return inventory.canAcceptRecipeOutput(result);
   }

   public static record LightningConsumptionPlan(LightningKey key, long amount, boolean matrixSubstitution) {
   }
}
