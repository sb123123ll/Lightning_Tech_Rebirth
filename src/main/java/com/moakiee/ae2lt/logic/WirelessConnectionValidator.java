package com.moakiee.ae2lt.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class WirelessConnectionValidator {
   public static final int PERIODIC_PRUNE_INTERVAL_TICKS = 100;
   public static final int PERIODIC_PRUNE_MAX_CHECKS = 64;

   private WirelessConnectionValidator() {
   }

   public static boolean shouldRunPeriodicPrune(ServerLevel level, BlockPos hostPos) {
      return com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionValidator.shouldRunPeriodicPrune(level, hostPos, 100);
   }

   public static WirelessConnectionValidator.Status validate(ServerLevel hostLevel, BlockPos hostPos, WirelessConnectionRef target) {
      return convert(
         com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionValidator.validate(
            hostLevel, hostPos, target, WirelessConnectionRange.maxConnectorDistance()
         )
      );
   }

   public static WirelessConnectionValidator.Status validate(ServerLevel hostLevel, BlockPos hostPos, ResourceKey<Level> targetDimension, BlockPos targetPos) {
      return convert(
         com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionValidator.validate(
            hostLevel, hostPos, targetDimension, targetPos, WirelessConnectionRange.maxConnectorDistance()
         )
      );
   }

   private static WirelessConnectionValidator.Status convert(com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionValidator.Status status) {
      return WirelessConnectionValidator.Status.valueOf(status.name());
   }

   public static enum Status {
      VALID,
      UNLOADED,
      REMOVE;
   }
}
