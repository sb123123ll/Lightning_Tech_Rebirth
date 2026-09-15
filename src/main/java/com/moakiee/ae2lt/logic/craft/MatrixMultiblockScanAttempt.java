package com.moakiee.ae2lt.logic.craft;

import java.util.List;
import net.minecraft.core.Direction;

public record MatrixMultiblockScanAttempt(Direction orientation, List<MatrixMultiblockScanIssue> issues, MatrixMultiblockScanResult result) {
   public MatrixMultiblockScanAttempt(Direction orientation, List<MatrixMultiblockScanIssue> issues, MatrixMultiblockScanResult result) {
      issues = List.copyOf(issues);
      this.orientation = orientation;
      this.issues = issues;
      this.result = result;
   }

   public boolean formed() {
      return this.result != null && this.issues.isEmpty();
   }

   public boolean chunksUnavailable() {
      return this.issues.contains(MatrixMultiblockScanIssue.CHUNKS_UNLOADED);
   }
}
