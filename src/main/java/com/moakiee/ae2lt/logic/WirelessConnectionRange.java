package com.moakiee.ae2lt.logic;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class WirelessConnectionRange {
   private WirelessConnectionRange() {
   }

   public static int maxConnectorDistance() {
      return AE2LTCommonConfig.wirelessConnectorMaxDistance();
   }

   public static boolean isConnectorLinkInRange(Level level, BlockPos hostPos, BlockPos targetPos) {
      return isConnectorLinkInRange(level.m_46472_(), hostPos, level.m_46472_(), targetPos);
   }

   public static boolean isConnectorLinkInRange(ResourceKey<Level> hostDimension, BlockPos hostPos, ResourceKey<Level> targetDimension, BlockPos targetPos) {
      return com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionRange.isInRange(
         hostDimension, hostPos, targetDimension, targetPos, maxConnectorDistance()
      );
   }
}
