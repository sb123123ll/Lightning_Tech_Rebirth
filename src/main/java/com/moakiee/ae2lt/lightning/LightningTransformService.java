package com.moakiee.ae2lt.lightning;

import com.moakiee.ae2lt.registry.ModRecipeTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LightningTransformService {
   private static final int MAX_ROUNDS_PER_STRIKE = 32;
   private static final Comparator<LightningTransformRecipe> RECIPE_ORDER = Comparator.comparingInt(LightningTransformRecipe::priority)
      .reversed()
      .thenComparing(Comparator.comparingInt(LightningTransformRecipe::ingredientCount).reversed())
      .thenComparing(Comparator.comparingInt(LightningTransformRecipe::totalInputCount).reversed())
      .thenComparing(recipe -> recipe.m_6423_().toString());

   private LightningTransformService() {
   }

   public static void handleLightning(ServerLevel level, LightningBolt lightningBolt) {
      long gameTime = level.m_46467_();
      List<ItemEntity> candidatePool = collectCandidates(level, lightningBolt.m_20182_(), gameTime);
      if (!candidatePool.isEmpty()) {
         List<LightningTransformRecipe> sortedRecipes = getSortedRecipes(level);
         if (!sortedRecipes.isEmpty()) {
            List<LightningTransformPlan> executedPlans = new ArrayList<>();
            List<LightningTransformService.PendingOutput> pendingOutputs = new ArrayList<>();

            for (int round = 0; round < 32; round++) {
               List<ItemEntity> candidates = filterStillEligible(candidatePool, gameTime);
               if (candidates.isEmpty()) {
                  break;
               }

               LightningTransformRecipeInput input = LightningTransformRecipeInput.fromEntities(candidates);
               if (input.size() == 0) {
                  break;
               }

               Optional<LightningTransformService.MatchedRecipe> matchedRecipe = selectRecipe(sortedRecipes, input);
               if (matchedRecipe.isEmpty()) {
                  break;
               }

               LightningTransformPlan plan = matchedRecipe.get().plan();
               if (!plan.consumeInputs(gameTime)) {
                  break;
               }

               pendingOutputs.add(new LightningTransformService.PendingOutput(matchedRecipe.get().recipe().m_8043_(level.m_9598_()), plan.spawnPosition()));
               executedPlans.add(plan);
            }

            spawnAccumulatedResults(level, pendingOutputs, gameTime);

            for (LightningTransformPlan plan : executedPlans) {
               plan.applyTransformLocks(gameTime);
            }
         }
      }
   }

   private static List<ItemEntity> collectCandidates(ServerLevel level, Vec3 lightningPosition, long gameTime) {
      AABB searchBox = new AABB(lightningPosition, lightningPosition).m_82377_(3.0, 2.0, 3.0);
      return level.m_6443_(ItemEntity.class, searchBox, itemEntity -> ProtectedItemEntityHelper.canParticipateInTransform(itemEntity, gameTime));
   }

   private static List<ItemEntity> filterStillEligible(List<ItemEntity> pool, long gameTime) {
      List<ItemEntity> result = new ArrayList<>(pool.size());

      for (ItemEntity itemEntity : pool) {
         if (ProtectedItemEntityHelper.canParticipateInTransform(itemEntity, gameTime)) {
            result.add(itemEntity);
         }
      }

      return result;
   }

   private static List<LightningTransformRecipe> getSortedRecipes(ServerLevel level) {
      List<LightningTransformRecipe> recipes = new ArrayList<>(level.m_7465_().m_44013_((RecipeType)ModRecipeTypes.LIGHTNING_TRANSFORM_TYPE.get()));
      recipes.sort(RECIPE_ORDER);
      return recipes;
   }

   private static Optional<LightningTransformService.MatchedRecipe> selectRecipe(
      List<LightningTransformRecipe> sortedRecipes, LightningTransformRecipeInput input
   ) {
      for (LightningTransformRecipe recipe : sortedRecipes) {
         Optional<LightningTransformPlan> plan = recipe.planMatch(input);
         if (plan.isPresent()) {
            return Optional.of(new LightningTransformService.MatchedRecipe(recipe, plan.get()));
         }
      }

      return Optional.empty();
   }

   private static void spawnAccumulatedResults(ServerLevel level, List<LightningTransformService.PendingOutput> pendingOutputs, long gameTime) {
      if (!pendingOutputs.isEmpty()) {
         Map<LightningTransformService.ItemStackKey, LightningTransformService.AccumulatedOutput> accumulated = new LinkedHashMap<>();

         for (LightningTransformService.PendingOutput pending : pendingOutputs) {
            if (!pending.result.m_41619_()) {
               LightningTransformService.ItemStackKey key = new LightningTransformService.ItemStackKey(pending.result);
               accumulated.computeIfAbsent(key, ignored -> new LightningTransformService.AccumulatedOutput(pending.result))
                  .add(pending.result.m_41613_(), pending.position);
            }
         }

         for (LightningTransformService.AccumulatedOutput output : accumulated.values()) {
            Vec3 position = output.averagePosition();
            ItemStack remaining = output.stack.m_255036_(output.totalCount);

            while (!remaining.m_41619_()) {
               int spawnCount = Math.min(remaining.m_41613_(), remaining.m_41741_());
               ItemStack spawnedStack = remaining.m_255036_(spawnCount);
               remaining.m_41774_(spawnCount);
               ItemEntity itemEntity = new ItemEntity(level, position.f_82479_, position.f_82480_, position.f_82481_, spawnedStack);
               itemEntity.m_20256_(Vec3.f_82478_);
               ProtectedItemEntityHelper.applyOutputProtection(itemEntity, gameTime);
               level.m_7967_(itemEntity);
            }
         }
      }
   }

   private static final class AccumulatedOutput {
      private final ItemStack stack;
      private int totalCount;
      private double totalX;
      private double totalY;
      private double totalZ;
      private int positionWeight;

      private AccumulatedOutput(ItemStack stack) {
         this.stack = stack.m_255036_(1);
      }

      private void add(int count, Vec3 position) {
         this.totalCount += count;
         this.totalX = this.totalX + position.f_82479_ * (double)count;
         this.totalY = this.totalY + position.f_82480_ * (double)count;
         this.totalZ = this.totalZ + position.f_82481_ * (double)count;
         this.positionWeight += count;
      }

      private Vec3 averagePosition() {
         return new Vec3(this.totalX / (double)this.positionWeight, this.totalY / (double)this.positionWeight, this.totalZ / (double)this.positionWeight);
      }
   }

   private static final class ItemStackKey {
      private final ItemStack stack;
      private final int hash;

      private ItemStackKey(ItemStack stack) {
         this.stack = stack.m_255036_(1);
         this.hash = Objects.hash(this.stack.m_41720_(), this.stack.m_41783_());
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return other instanceof LightningTransformService.ItemStackKey itemStackKey ? ItemStack.m_150942_(this.stack, itemStackKey.stack) : false;
         }
      }

      @Override
      public int hashCode() {
         return this.hash;
      }
   }

   private static record MatchedRecipe(LightningTransformRecipe recipe, LightningTransformPlan plan) {
   }

   private static record PendingOutput(ItemStack result, Vec3 position) {
   }
}
