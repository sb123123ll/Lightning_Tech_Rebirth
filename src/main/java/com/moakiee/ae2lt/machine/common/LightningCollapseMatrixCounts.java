package com.moakiee.ae2lt.machine.common;

public final class LightningCollapseMatrixCounts {
   private LightningCollapseMatrixCounts() {
   }

   public static int clampToSlotLimit(int requestedCount, int slotLimit) {
      return Math.max(0, Math.min(requestedCount, Math.max(0, slotLimit)));
   }
}
