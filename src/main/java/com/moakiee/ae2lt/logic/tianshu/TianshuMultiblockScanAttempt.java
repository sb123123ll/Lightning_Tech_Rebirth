package com.moakiee.ae2lt.logic.tianshu;

import java.util.List;

public record TianshuMultiblockScanAttempt(TianshuMultiblockScanResult result, List<TianshuMultiblockScanIssue> issues) {
   public boolean formed() {
      return this.result != null && this.issues.isEmpty();
   }

   public boolean chunksUnavailable() {
      return this.issues.contains(TianshuMultiblockScanIssue.CHUNKS_UNLOADED);
   }
}
