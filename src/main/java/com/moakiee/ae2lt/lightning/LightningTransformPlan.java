package com.moakiee.ae2lt.lightning;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class LightningTransformPlan {
   private final List<LightningTransformPlan.Consumption> consumptions;
   private final Vec3 spawnPosition;
   private final int totalConsumedCount;

   private LightningTransformPlan(List<LightningTransformPlan.Consumption> consumptions, Vec3 spawnPosition, int totalConsumedCount) {
      this.consumptions = List.copyOf(consumptions);
      this.spawnPosition = Objects.requireNonNull(spawnPosition, "spawnPosition");
      this.totalConsumedCount = totalConsumedCount;
   }

   public static LightningTransformPlan fromGroupCounts(List<LightningTransformRecipeInput.GroupedStack> groupedStacks, int[] groupConsumptions) {
      List<LightningTransformPlan.Consumption> consumptions = new ArrayList<>();
      double totalX = 0.0;
      double totalY = 0.0;
      double totalZ = 0.0;
      int totalConsumedCount = 0;

      for (int groupIndex = 0; groupIndex < groupedStacks.size(); groupIndex++) {
         int remaining = groupConsumptions[groupIndex];
         if (remaining > 0) {
            for (LightningTransformRecipeInput.ParticipantStack participant : groupedStacks.get(groupIndex).participants()) {
               if (remaining <= 0) {
                  break;
               }

               int take = Math.min(remaining, participant.count());
               if (take > 0) {
                  consumptions.add(new LightningTransformPlan.Consumption(participant.itemEntity(), take, participant.position()));
                  totalX += participant.position().f_82479_ * (double)take;
                  totalY += participant.position().f_82480_ * (double)take;
                  totalZ += participant.position().f_82481_ * (double)take;
                  totalConsumedCount += take;
                  remaining -= take;
               }
            }

            if (remaining > 0) {
               throw new IllegalStateException("Recipe allocation exceeded grouped stack snapshot");
            }
         }
      }

      if (totalConsumedCount <= 0) {
         throw new IllegalStateException("Lightning transform plan must consume at least one item");
      } else {
         Vec3 spawnPosition = new Vec3(totalX / (double)totalConsumedCount, totalY / (double)totalConsumedCount + 0.15, totalZ / (double)totalConsumedCount);
         return new LightningTransformPlan(consumptions, spawnPosition, totalConsumedCount);
      }
   }

   public Vec3 spawnPosition() {
      return this.spawnPosition;
   }

   public int totalConsumedCount() {
      return this.totalConsumedCount;
   }

   public boolean consumeInputs(long gameTime) {
      if (!this.isStillValid(gameTime)) {
         return false;
      } else {
         for (LightningTransformPlan.Consumption consumption : this.consumptions) {
            ItemEntity itemEntity = consumption.itemEntity();
            ItemStack stack = itemEntity.m_32055_();
            stack.m_41774_(consumption.count());
            if (stack.m_41619_()) {
               itemEntity.m_146870_();
            } else {
               itemEntity.m_32045_(stack);
            }
         }

         return true;
      }
   }

   public void applyTransformLocks(long gameTime) {
      for (LightningTransformPlan.Consumption consumption : this.consumptions) {
         ItemEntity itemEntity = consumption.itemEntity();
         if (itemEntity.m_6084_() && !itemEntity.m_32055_().m_41619_()) {
            ProtectedItemEntityHelper.applyTransformLock(itemEntity, gameTime);
         }
      }
   }

   private boolean isStillValid(long gameTime) {
      for (LightningTransformPlan.Consumption consumption : this.consumptions) {
         ItemEntity itemEntity = consumption.itemEntity();
         if (!itemEntity.m_6084_()) {
            return false;
         }

         if (!ProtectedItemEntityHelper.canParticipateInTransform(itemEntity, gameTime)) {
            return false;
         }

         ItemStack stack = itemEntity.m_32055_();
         if (stack.m_41619_() || stack.m_41613_() < consumption.count()) {
            return false;
         }
      }

      return true;
   }

   public static record Consumption(ItemEntity itemEntity, int count, Vec3 originalPosition) {
      public Consumption(ItemEntity itemEntity, int count, Vec3 originalPosition) {
         Objects.requireNonNull(itemEntity, "itemEntity");
         Objects.requireNonNull(originalPosition, "originalPosition");
         if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
         } else {
            this.itemEntity = itemEntity;
            this.count = count;
            this.originalPosition = originalPosition;
         }
      }
   }
}
