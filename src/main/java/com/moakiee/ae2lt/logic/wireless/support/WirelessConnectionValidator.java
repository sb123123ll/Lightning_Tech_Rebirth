package com.moakiee.ae2lt.logic.wireless.support;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WirelessConnectionValidator {
   private WirelessConnectionValidator() {
   }

   public static boolean shouldRunPeriodicPrune(ServerLevel level, BlockPos hostPos, int intervalTicks) {
      if (intervalTicks <= 0) {
         return false;
      } else {
         int offset = Math.floorMod(hostPos.m_121878_(), intervalTicks);
         return (level.m_46467_() + (long)offset) % (long)intervalTicks == 0L;
      }
   }

   public static WirelessConnectionValidator.Status validate(ServerLevel hostLevel, BlockPos hostPos, WirelessConnectionRef target, int maxDistance) {
      return validate(hostLevel, hostPos, target.dimension(), target.pos(), maxDistance);
   }

   public static WirelessConnectionValidator.Status validate(
      ServerLevel hostLevel, BlockPos hostPos, ResourceKey<Level> targetDimension, BlockPos targetPos, int maxDistance
   ) {
      if (!WirelessConnectionRange.isInRange(hostLevel.m_46472_(), hostPos, targetDimension, targetPos, maxDistance)) {
         return WirelessConnectionValidator.Status.REMOVE;
      } else {
         ServerLevel targetLevel = hostLevel.m_7654_().m_129880_(targetDimension);
         if (targetLevel == null) {
            return WirelessConnectionValidator.Status.REMOVE;
         } else if (!targetLevel.m_46749_(targetPos)) {
            return WirelessConnectionValidator.Status.UNLOADED;
         } else {
            BlockState state = targetLevel.m_8055_(targetPos);
            return !state.m_60795_() && targetLevel.m_7702_(targetPos) != null
               ? WirelessConnectionValidator.Status.VALID
               : WirelessConnectionValidator.Status.REMOVE;
         }
      }
   }

   public static enum Status {
      VALID,
      UNLOADED,
      REMOVE;
   }
}
