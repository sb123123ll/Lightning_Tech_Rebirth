package com.moakiee.ae2lt.worldgen;

public final class FirmamentStarshipPlacement {
   private FirmamentStarshipPlacement() {
   }

   public static FirmamentStarshipPlacement.Position offsetFromAnchor(int anchorX, int anchorY, int anchorZ, int horizontalOffset, int verticalOffset) {
      double length = Math.sqrt((double)anchorX * (double)anchorX + (double)anchorZ * (double)anchorZ);
      if (length < 1.0) {
         return new FirmamentStarshipPlacement.Position(anchorX + horizontalOffset, anchorY + verticalOffset, anchorZ);
      } else {
         int offsetX = (int)Math.round((double)anchorX / length * (double)horizontalOffset);
         int offsetZ = (int)Math.round((double)anchorZ / length * (double)horizontalOffset);
         return new FirmamentStarshipPlacement.Position(anchorX + offsetX, anchorY + verticalOffset, anchorZ + offsetZ);
      }
   }

   public static FirmamentStarshipPlacement.Position offsetFromStartChunk(
      int startChunkMiddleX, int anchorY, int startChunkMiddleZ, int horizontalOffset, int verticalOffset
   ) {
      return offsetFromAnchor(startChunkMiddleX, anchorY, startChunkMiddleZ, horizontalOffset, verticalOffset);
   }

   public static FirmamentStarshipPlacement.Position originFromCenter(FirmamentStarshipPlacement.Position center, int sizeX, int sizeZ) {
      return new FirmamentStarshipPlacement.Position(center.x() - sizeX / 2, center.y(), center.z() - sizeZ / 2);
   }

   public static int clampStartY(int desiredY, int minBuildHeight, int maxBuildHeight, int templateHeight) {
      int maxStartY = Math.max(minBuildHeight, maxBuildHeight - templateHeight);
      return Math.max(minBuildHeight, Math.min(desiredY, maxStartY));
   }

   public static record Position(int x, int y, int z) {
   }
}
