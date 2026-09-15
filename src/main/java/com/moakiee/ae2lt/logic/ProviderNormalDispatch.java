package com.moakiee.ae2lt.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

final class ProviderNormalDispatch {
   private final Map<Direction, ProviderTarget> targets = new EnumMap<>(Direction.class);
   private int cursor;

   ProviderTarget target(ServerLevel level, BlockPos providerPos, Direction pushDirection) {
      BlockPos targetPos = providerPos.m_121945_(pushDirection);
      Direction targetFace = pushDirection.m_122424_();
      return this.targets
         .compute(
            pushDirection,
            (direction, current) -> (ProviderTarget)(current != null
                     && current.dimension().equals(level.m_46472_())
                     && current.pos().equals(targetPos)
                     && current.boundFace() == targetFace
                  ? current
                  : new ProviderTarget(level.m_46472_(), targetPos, targetFace))
         );
   }

   void restore(Direction pushDirection, ProviderTarget target) {
      this.targets.put(pushDirection, target);
   }

   Map<Direction, ProviderTarget> targets() {
      return Map.copyOf(this.targets);
   }

   List<Direction> dispatchOrder(List<Direction> directions) {
      if (directions.isEmpty()) {
         return List.of();
      } else {
         int start = Math.floorMod(this.cursor, directions.size());
         ArrayList<Direction> ordered = new ArrayList<>(directions.size());

         for (int i = 0; i < directions.size(); i++) {
            ordered.add(directions.get((start + i) % directions.size()));
         }

         this.cursor = (this.cursor + 1) % directions.size();
         return ordered;
      }
   }

   long dispatchBatch(Collection<ProviderTarget> currentTargets, long maxCopies, ProviderNormalDispatch.BatchAttempt attempt) {
      List<ProviderTarget> orderedTargets = List.copyOf(currentTargets);
      long remaining = maxCopies;
      int targetsRemaining = orderedTargets.size();

      for (ProviderTarget target : orderedTargets) {
         if (remaining <= 0L) {
            break;
         }

         long share = ceilingDivide(remaining, targetsRemaining--);
         ProviderNormalDispatch.BatchAttemptResult result = attempt.push(target, share);
         if (result.ownedCopies > 0L) {
            remaining -= Math.min(remaining, result.ownedCopies);
         }

         if (result.stop || result.globalAbort) {
            break;
         }
      }

      return remaining;
   }

   void patternsChanged() {
      for (ProviderTarget target : this.targets.values()) {
         target.clearBatchHistory();
      }
   }

   void clearRuntimeState() {
      for (ProviderTarget target : this.targets.values()) {
         target.clearRuntimeState();
      }

      this.patternsChanged();
   }

   void clear() {
      this.clearRuntimeState();
      this.targets.clear();
      this.cursor = 0;
   }

   private static long ceilingDivide(long amount, int divisor) {
      return amount > 0L && divisor > 0 ? 1L + (amount - 1L) / (long)divisor : 0L;
   }

   @FunctionalInterface
   interface BatchAttempt {
      ProviderNormalDispatch.BatchAttemptResult push(ProviderTarget var1, long var2);
   }

   static record BatchAttemptResult(long ownedCopies, boolean globalAbort, boolean stop) {
   }
}
