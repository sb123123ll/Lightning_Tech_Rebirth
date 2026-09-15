package com.moakiee.ae2lt.logic.wireless.support;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class WirelessConnectionRange {
   private WirelessConnectionRange() {
   }

   public static boolean isInRange(ResourceKey<Level> hostDimension, BlockPos hostPos, ResourceKey<Level> targetDimension, BlockPos targetPos, int maxDistance) {
      if (!hostDimension.equals(targetDimension)) {
         return false;
      } else if (maxDistance <= 0) {
         return true;
      } else {
         long maxDistanceSquared = (long)maxDistance * (long)maxDistance;
         return hostPos.m_123331_(targetPos) <= (double)maxDistanceSquared;
      }
   }
}
