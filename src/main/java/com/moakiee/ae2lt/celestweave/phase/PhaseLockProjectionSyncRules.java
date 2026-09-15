package com.moakiee.ae2lt.celestweave.phase;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class PhaseLockProjectionSyncRules {
   private PhaseLockProjectionSyncRules() {
   }

   public static PhaseLockProjectionSyncRules.Direction direction(
      UUID armorId, long armorUpdate, @Nullable PhaseLockProjectionLink projectionLink, boolean mirroredFieldsEqual
   ) {
      if (projectionLink == null || !armorId.equals(projectionLink.armorId()) || projectionLink.update() < armorUpdate) {
         return PhaseLockProjectionSyncRules.Direction.ARMOR_TO_PROJECTION;
      } else {
         return mirroredFieldsEqual ? PhaseLockProjectionSyncRules.Direction.NONE : PhaseLockProjectionSyncRules.Direction.PROJECTION_TO_ARMOR;
      }
   }

   public static long nextUpdate(long armorUpdate, @Nullable PhaseLockProjectionLink projectionLink) {
      long projectionUpdate = projectionLink == null ? 0L : projectionLink.update();
      long current = Math.max(0L, Math.max(armorUpdate, projectionUpdate));
      return current == Long.MAX_VALUE ? Long.MAX_VALUE : current + 1L;
   }

   public static enum Direction {
      NONE,
      ARMOR_TO_PROJECTION,
      PROJECTION_TO_ARMOR;
   }
}
