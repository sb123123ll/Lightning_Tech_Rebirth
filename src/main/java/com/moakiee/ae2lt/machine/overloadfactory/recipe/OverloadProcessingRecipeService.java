package com.moakiee.ae2lt.machine.overloadfactory.recipe;

import com.moakiee.ae2lt.logic.FluidStackHelper;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.registry.ModRecipeTypes;
import com.moakiee.ae2lt.util.RecipeManagerByTypeAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

public final class OverloadProcessingRecipeService {
   public static final int EXTREME_TO_HIGH_RATIO = 4;
   private static final Comparator<OverloadProcessingRecipe> RECIPE_ORDER = Comparator.comparingInt(OverloadProcessingRecipe::priority)
      .reversed()
      .thenComparing(Comparator.<OverloadProcessingRecipe>comparingInt(recipe -> recipe.itemInputs().size()).reversed())
      .thenComparing(Comparator.comparingInt(OverloadProcessingRecipe::totalInputCount).reversed())
      .thenComparing(recipe -> recipe.m_6423_().toString());
   private static final Comparator<OverloadProcessingRecipeService.SelectionKey> SELECTION_KEY_ORDER = Comparator.comparingInt(
         OverloadProcessingRecipeService.SelectionKey::parallel
      )
      .reversed()
      .thenComparing(Comparator.comparingInt(OverloadProcessingRecipeService.SelectionKey::priority).reversed())
      .thenComparing(Comparator.comparingInt(OverloadProcessingRecipeService.SelectionKey::itemInputKinds).reversed())
      .thenComparing(Comparator.comparingInt(OverloadProcessingRecipeService.SelectionKey::totalInputCount).reversed())
      .thenComparing(OverloadProcessingRecipeService.SelectionKey::recipeId);
   private static RecipeManager cachedRecipeManager;
   private static List<OverloadProcessingRecipe> sortedRecipeCache;
   private static int cachedRecipeOrderFingerprint;

   private OverloadProcessingRecipeService() {
   }

   private static synchronized List<OverloadProcessingRecipe> getSortedRecipes(Level level) {
      RecipeManager recipeManager = level.m_7465_();
      Map<ResourceLocation, OverloadProcessingRecipe> raw = RecipeManagerByTypeAccess.byType(
         recipeManager, (RecipeType)ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get()
      );
      int orderFingerprint = computeRecipeOrderFingerprint(raw.values());
      if (recipeManager != cachedRecipeManager || orderFingerprint != cachedRecipeOrderFingerprint || sortedRecipeCache == null) {
         sortedRecipeCache = new ArrayList<>(raw.values());
         sortedRecipeCache.sort(RECIPE_ORDER);
         cachedRecipeManager = recipeManager;
         cachedRecipeOrderFingerprint = orderFingerprint;
      }

      return sortedRecipeCache;
   }

   private static int computeRecipeOrderFingerprint(Collection<OverloadProcessingRecipe> recipes) {
      int hash = 1;

      for (OverloadProcessingRecipe recipe : recipes) {
         hash = 31 * hash + recipe.m_6423_().hashCode();
         hash = 31 * hash + System.identityHashCode(recipe);
         hash = 31 * hash + recipe.priority();
         hash = 31 * hash + recipe.itemInputs().size();
         hash = 31 * hash + recipe.totalInputCount();
      }

      return hash;
   }

   public static Optional<OverloadProcessingRecipeCandidate> findFirstProcessable(
      Level level,
      OverloadProcessingFactoryInventory inventory,
      FluidStack inputFluid,
      FluidStack outputFluid,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      if (level == null) {
         return Optional.empty();
      } else {
         OverloadProcessingRecipeInput input = OverloadProcessingRecipeInput.fromInventory(inventory, inputFluid);
         if (input.m_7983_()) {
            return Optional.empty();
         } else {
            List<OverloadProcessingRecipe> recipes = getSortedRecipes(level);
            int parallelCapacity = inventory.getInstalledParallelCapacity();
            OverloadProcessingRecipeCandidate bestCandidate = null;
            OverloadProcessingRecipeService.SelectionKey bestKey = null;

            for (OverloadProcessingRecipe recipe : recipes) {
               Optional<OverloadProcessingRecipeCandidate> candidate = evaluateCandidate(
                  recipe, input, inventory, outputFluid, parallelCapacity, availableHighVoltage, availableExtremeHighVoltage
               );
               if (!candidate.isEmpty()) {
                  OverloadProcessingRecipeService.SelectionKey candidateKey = selectionKey(recipe, candidate.get().parallel());
                  if (bestKey == null || SELECTION_KEY_ORDER.compare(candidateKey, bestKey) < 0) {
                     bestCandidate = candidate.get();
                     bestKey = candidateKey;
                  }
               }
            }

            return Optional.ofNullable(bestCandidate);
         }
      }
   }

   private static Optional<OverloadProcessingRecipeCandidate> evaluateCandidate(
      OverloadProcessingRecipe recipe,
      OverloadProcessingRecipeInput input,
      OverloadProcessingFactoryInventory inventory,
      FluidStack outputFluid,
      int parallelCapacity,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      Optional<OverloadProcessingRecipeService.ParallelMatch> parallelMatch = findMaxParallel(
         recipe, input, inventory, outputFluid, parallelCapacity, availableHighVoltage, availableExtremeHighVoltage
      );
      if (parallelMatch.isEmpty()) {
         return Optional.empty();
      } else {
         OverloadProcessingRecipeService.ParallelMatch match = parallelMatch.get();
         return Optional.of(
            new OverloadProcessingRecipeCandidate(
               recipe,
               match.match(),
               match.parallel(),
               computeTotalEnergy(recipe.totalEnergy(), match.parallel()),
               (long)recipe.lightningCost() * (long)match.parallel()
            )
         );
      }
   }

   public static Optional<OverloadProcessingRecipe> findRecipeById(Level level, ResourceLocation recipeId) {
      return level != null && recipeId != null
         ? RecipeManagerByTypeAccess.findById(level.m_7465_(), (RecipeType)ModRecipeTypes.OVERLOAD_PROCESSING_TYPE.get(), recipeId)
         : Optional.empty();
   }

   public static Optional<OverloadProcessingRecipeCandidate> findLockedRecipeMatch(
      Level level,
      OverloadProcessingFactoryInventory inventory,
      FluidStack inputFluid,
      FluidStack outputFluid,
      OverloadProcessingLockedRecipe lockedRecipe,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      if (level != null && lockedRecipe != null && lockedRecipe.parallel() > 0) {
         Optional<OverloadProcessingRecipe> recipe = findRecipeById(level, lockedRecipe.recipeId());
         if (recipe.isEmpty()) {
            return Optional.empty();
         } else {
            OverloadProcessingRecipeInput input = OverloadProcessingRecipeInput.fromInventory(inventory, inputFluid);
            if (input.m_7983_()) {
               return Optional.empty();
            } else if (computeTotalEnergy(recipe.get().totalEnergy(), lockedRecipe.parallel()) != lockedRecipe.totalEnergy()) {
               return Optional.empty();
            } else if (resolveLightningConsumption(
                  inventory, lockedRecipe.lightningTier(), lockedRecipe.totalLightningCost(), availableHighVoltage, availableExtremeHighVoltage
               )
               .isEmpty()) {
               return Optional.empty();
            } else if (!canAcceptOutputs(inventory, recipe.get(), outputFluid, lockedRecipe.parallel())) {
               return Optional.empty();
            } else {
               Optional<OverloadProcessingRecipeMatch> match = recipe.get().planMatch(input, lockedRecipe.parallel());
               return match.isEmpty()
                  ? Optional.empty()
                  : Optional.of(
                     new OverloadProcessingRecipeCandidate(
                        recipe.get(), match.get(), lockedRecipe.parallel(), lockedRecipe.totalEnergy(), lockedRecipe.totalLightningCost()
                     )
                  );
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public static long computeTotalEnergy(long singleOperationEnergy, int parallel) {
      if (singleOperationEnergy > 0L && parallel > 0) {
         try {
            int maxParallel = OverloadProcessingFactoryInventory.getMaxParallel();
            if (maxParallel <= 1) {
               return Math.multiplyExact(singleOperationEnergy, parallel);
            } else {
               long divisor = (long)(maxParallel * 2 - 2);
               long numeratorFactor = (long)(parallel + maxParallel * 2 - 3);
               long linearEnergy = Math.multiplyExact(singleOperationEnergy, parallel);
               long scaled = Math.multiplyExact(linearEnergy, numeratorFactor);
               return divideCeil(scaled, divisor);
            }
         } catch (ArithmeticException var12) {
            return Long.MAX_VALUE;
         }
      } else {
         return 0L;
      }
   }

   public static Optional<OverloadProcessingRecipeService.LightningConsumptionPlan> resolveLightningConsumption(
      OverloadProcessingFactoryInventory inventory,
      LightningKey.Tier lightningTier,
      long lightningCost,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      return resolveLightningConsumption(
         inventory.hasLightningCollapseMatrix(), lightningTier, lightningCost, availableHighVoltage, availableExtremeHighVoltage
      );
   }

   static Optional<OverloadProcessingRecipeService.LightningConsumptionPlan> resolveLightningConsumption(
      boolean hasLightningCollapseMatrix, LightningKey.Tier lightningTier, long lightningCost, long availableHighVoltage, long availableExtremeHighVoltage
   ) {
      if (lightningCost <= 0L) {
         return Optional.empty();
      } else if (lightningTier == LightningKey.Tier.HIGH_VOLTAGE) {
         return availableHighVoltage >= lightningCost
            ? Optional.of(new OverloadProcessingRecipeService.LightningConsumptionPlan(LightningKey.HIGH_VOLTAGE, lightningCost, false))
            : Optional.empty();
      } else if (availableExtremeHighVoltage >= lightningCost) {
         return Optional.of(new OverloadProcessingRecipeService.LightningConsumptionPlan(LightningKey.EXTREME_HIGH_VOLTAGE, lightningCost, false));
      } else if (!hasLightningCollapseMatrix) {
         return Optional.empty();
      } else {
         long remaining = lightningCost - availableExtremeHighVoltage;
         if (remaining > 2305843009213693951L) {
            return Optional.empty();
         } else {
            long highVoltageNeeded = remaining * 4L;
            if (availableHighVoltage < highVoltageNeeded) {
               return Optional.empty();
            } else {
               return availableExtremeHighVoltage > 0L
                  ? Optional.of(
                     new OverloadProcessingRecipeService.LightningConsumptionPlan(
                        LightningKey.EXTREME_HIGH_VOLTAGE, availableExtremeHighVoltage, LightningKey.HIGH_VOLTAGE, highVoltageNeeded, true
                     )
                  )
                  : Optional.of(new OverloadProcessingRecipeService.LightningConsumptionPlan(LightningKey.HIGH_VOLTAGE, highVoltageNeeded, true));
            }
         }
      }
   }

   public static long getEquivalentHighVoltageCost(LightningKey.Tier lightningTier, long lightningCost) {
      return lightningTier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? lightningCost * 4L : lightningCost;
   }

   private static Optional<OverloadProcessingRecipeService.ParallelMatch> findMaxParallel(
      OverloadProcessingRecipe recipe,
      OverloadProcessingRecipeInput input,
      OverloadProcessingFactoryInventory inventory,
      FluidStack outputFluid,
      int parallelCapacity,
      long availableHighVoltage,
      long availableExtremeHighVoltage
   ) {
      int upper = parallelCapacity;
      if (parallelCapacity <= 0) {
         return Optional.empty();
      } else {
         FluidStack requiredInputFluid = recipe.fluidInput();
         if (!requiredInputFluid.isEmpty()) {
            if (input.inputFluid().isEmpty() || !FluidStackHelper.sameFluidAndTag(requiredInputFluid, input.inputFluid())) {
               return Optional.empty();
            }

            upper = Math.min(parallelCapacity, input.inputFluid().getAmount() / requiredInputFluid.getAmount());
         }

         upper = Math.min(upper, maxLightningParallel(recipe, inventory, availableHighVoltage, availableExtremeHighVoltage));
         if (upper <= 0) {
            return Optional.empty();
         } else {
            Optional<OverloadProcessingRecipe.MatchPlan> plan = recipe.prepareMatch(input);
            if (plan.isEmpty()) {
               return Optional.empty();
            } else {
               upper = (int)Math.min((long)upper, plan.get().maxOperationsByAvailability());
               upper = (int)Math.min((long)upper, maxOutputParallel(inventory, recipe, outputFluid));
               if (upper <= 0) {
                  return Optional.empty();
               } else {
                  boolean checkOutputsPerProbe = recipe.rawItemResults().size() > 1;
                  int low = 1;
                  int high = upper;
                  int best = 0;
                  OverloadProcessingRecipeMatch bestMatch = null;

                  while (low <= high) {
                     int mid = low + high >>> 1;
                     if (checkOutputsPerProbe && !canAcceptOutputs(inventory, recipe, outputFluid, mid)) {
                        high = mid - 1;
                     } else {
                        Optional<OverloadProcessingRecipeMatch> match = plan.get().allocate(mid);
                        if (match.isPresent()) {
                           best = mid;
                           bestMatch = match.get();
                           low = mid + 1;
                        } else {
                           high = mid - 1;
                        }
                     }
                  }

                  return bestMatch == null ? Optional.empty() : Optional.of(new OverloadProcessingRecipeService.ParallelMatch(best, bestMatch));
               }
            }
         }
      }
   }

   private static long maxOutputParallel(OverloadProcessingFactoryInventory inventory, OverloadProcessingRecipe recipe, FluidStack outputFluid) {
      long bound = Long.MAX_VALUE;
      List<ItemStack> itemResults = recipe.rawItemResults();
      if (itemResults.size() == 1) {
         ItemStack result = itemResults.get(0);
         bound = inventory.getOutputCapacityFor(result) / (long)result.m_41613_();
      }

      FluidStack fluidResult = recipe.rawFluidResult();
      if (!fluidResult.isEmpty()) {
         long tankCapacity = 1024000L;
         long space;
         if (outputFluid.isEmpty()) {
            space = tankCapacity;
         } else if (FluidStackHelper.sameFluidAndTag(outputFluid, fluidResult)) {
            space = tankCapacity - (long)outputFluid.getAmount();
         } else {
            space = 0L;
         }

         bound = Math.min(bound, Math.max(0L, space) / (long)fluidResult.getAmount());
      }

      return bound;
   }

   private static int maxLightningParallel(
      OverloadProcessingRecipe recipe, OverloadProcessingFactoryInventory inventory, long availableHighVoltage, long availableExtremeHighVoltage
   ) {
      return maxLightningParallel(
         recipe.lightningTier(), recipe.lightningCost(), inventory.hasLightningCollapseMatrix(), availableHighVoltage, availableExtremeHighVoltage
      );
   }

   static int maxLightningParallel(
      LightningKey.Tier lightningTier, int lightningCost, boolean hasLightningCollapseMatrix, long availableHighVoltage, long availableExtremeHighVoltage
   ) {
      if (lightningCost <= 0 || availableHighVoltage < 0L || availableExtremeHighVoltage < 0L) {
         return 0;
      } else if (lightningTier == LightningKey.Tier.HIGH_VOLTAGE) {
         return (int)Math.min(2147483647L, availableHighVoltage / (long)lightningCost);
      } else {
         long exactParallel = availableExtremeHighVoltage / (long)lightningCost;
         if (!hasLightningCollapseMatrix) {
            return (int)Math.min(2147483647L, exactParallel);
         } else {
            long remainingExtreme = availableExtremeHighVoltage % (long)lightningCost;
            long equivalentCost = (long)lightningCost * 4L;
            long additionalParallel = availableHighVoltage / equivalentCost;
            long remainingHighVoltage = availableHighVoltage % equivalentCost;
            if (remainingExtreme * 4L + remainingHighVoltage >= equivalentCost) {
               additionalParallel++;
            }

            return Long.MAX_VALUE - exactParallel < additionalParallel ? Integer.MAX_VALUE : (int)Math.min(2147483647L, exactParallel + additionalParallel);
         }
      }
   }

   private static boolean canAcceptOutputs(OverloadProcessingFactoryInventory inventory, OverloadProcessingRecipe recipe, FluidStack outputFluid, int parallel) {
      if (!inventory.canAcceptRecipeOutputs(recipe.getScaledItemResults(parallel))) {
         return false;
      } else {
         FluidStack scaledFluid = recipe.getScaledFluidResult(parallel);
         if (scaledFluid.isEmpty()) {
            return true;
         } else {
            return outputFluid.isEmpty()
               ? scaledFluid.getAmount() <= 1024000
               : FluidStackHelper.sameFluidAndTag(outputFluid, scaledFluid) && outputFluid.getAmount() + scaledFluid.getAmount() <= 1024000;
         }
      }
   }

   private static long divideCeil(long dividend, long divisor) {
      if (divisor <= 0L) {
         throw new IllegalArgumentException("divisor must be positive");
      } else {
         return dividend <= 0L ? 0L : dividend / divisor + (dividend % divisor == 0L ? 0L : 1L);
      }
   }

   private static OverloadProcessingRecipeService.SelectionKey selectionKey(OverloadProcessingRecipe recipe, int parallel) {
      return new OverloadProcessingRecipeService.SelectionKey(
         parallel, recipe.priority(), recipe.itemInputs().size(), recipe.totalInputCount(), recipe.m_6423_()
      );
   }

   public static record LightningConsumptionPlan(
      LightningKey primaryKey, long primaryAmount, LightningKey secondaryKey, long secondaryAmount, boolean matrixSubstitution
   ) {
      public LightningConsumptionPlan(LightningKey key, long amount, boolean matrixSubstitution) {
         this(key, amount, null, 0L, matrixSubstitution);
      }

      public boolean hasSecondary() {
         return this.secondaryKey != null && this.secondaryAmount > 0L;
      }
   }

   private static record ParallelMatch(int parallel, OverloadProcessingRecipeMatch match) {
   }

   private static record SelectionKey(int parallel, int priority, int itemInputKinds, int totalInputCount, ResourceLocation recipeId) {
   }
}
