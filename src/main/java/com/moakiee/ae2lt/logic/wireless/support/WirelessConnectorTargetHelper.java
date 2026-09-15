package com.moakiee.ae2lt.logic.wireless.support;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WirelessConnectorTargetHelper {
   private WirelessConnectorTargetHelper() {
   }

   public static Set<BlockPos> collectTargets(Level level, BlockPos origin, boolean contiguous) {
      return collectTargets(level, origin, contiguous, Integer.MAX_VALUE);
   }

   public static Set<BlockPos> collectTargets(Level level, BlockPos origin, boolean contiguous, int maxTargets) {
      if (maxTargets <= 0) {
         return Set.of();
      } else if (!contiguous) {
         return level.m_7702_(origin) != null ? Set.of(origin.m_7949_()) : Set.of();
      } else if (!level.m_46749_(origin)) {
         return Set.of();
      } else {
         BlockState originState = level.m_8055_(origin);
         BlockEntity originBlockEntity = level.m_7702_(origin);
         if (originBlockEntity == null) {
            return Set.of();
         } else {
            LinkedHashSet<BlockPos> visited = new LinkedHashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(origin.m_7949_());

            while (!queue.isEmpty() && visited.size() < maxTargets) {
               BlockPos current = queue.removeFirst();
               if (visited.add(current) && visited.size() < maxTargets) {
                  for (Direction direction : Direction.values()) {
                     BlockPos next = current.m_121945_(direction);
                     if (!visited.contains(next) && level.m_46749_(next)) {
                        BlockEntity nextBlockEntity = level.m_7702_(next);
                        if (nextBlockEntity != null
                           && nextBlockEntity.getClass() == originBlockEntity.getClass()
                           && level.m_8055_(next).m_60713_(originState.m_60734_())) {
                           queue.addLast(next.m_7949_());
                        }
                     }
                  }
               }
            }

            return visited;
         }
      }
   }
}
