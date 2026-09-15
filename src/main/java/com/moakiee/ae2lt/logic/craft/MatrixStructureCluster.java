package com.moakiee.ae2lt.logic.craft;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;

public final class MatrixStructureCluster {
   private final MatrixMultiblockScanResult scanResult;
   private final Set<BlockPos> memberPositions;
   private final MatrixCraftingProfile craftingProfile;
   private final MatrixPatternRepository patternRepository;
   private boolean destroyed;

   public MatrixStructureCluster(MatrixMultiblockScanResult scanResult) {
      this.scanResult = scanResult;
      this.memberPositions = new HashSet<>();

      for (MatrixMultiblockMember member : scanResult.members()) {
         this.memberPositions.add(member.worldPos());
      }

      this.craftingProfile = scanResult.craftingProfile();
      this.patternRepository = scanResult.createEmptyPatternRepository();
   }

   public MatrixMultiblockScanResult scanResult() {
      return this.scanResult;
   }

   public boolean isFormed() {
      return !this.destroyed;
   }

   public boolean isDestroyed() {
      return this.destroyed;
   }

   public void destroy() {
      this.destroyed = true;
   }

   public boolean contains(BlockPos pos) {
      return this.memberPositions.contains(pos);
   }

   public Set<BlockPos> memberPositions() {
      return Set.copyOf(this.memberPositions);
   }

   public MatrixCraftingProfile craftingProfile() {
      return this.destroyed ? MatrixCraftingProfile.empty() : this.craftingProfile;
   }

   public MatrixPatternRepository patternRepository() {
      return this.patternRepository;
   }
}
