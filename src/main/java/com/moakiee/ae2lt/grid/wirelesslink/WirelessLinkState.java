package com.moakiee.ae2lt.grid.wirelesslink;

public enum WirelessLinkState {
   CONNECTED(false, false),
   PENDING_TARGET_CHUNK(false, false),
   PENDING_TRANSMITTER(false, false),
   TARGET_NOT_READY(false, false),
   CLUSTER_FREQUENCY_CONFLICT(false, false),
   FREQUENCY_INVALID(false, false),
   PERMISSION_DENIED(false, false),
   TARGET_MISSING(true, true),
   TARGET_TYPE_CHANGED(true, true),
   TARGET_NOT_NETWORK_DEVICE(true, false),
   PART_MISSING(true, true),
   PART_TYPE_CHANGED(true, true),
   PART_NOT_NETWORK_DEVICE(true, false),
   REDUNDANT_LINK(true, false),
   DISCONNECTED(true, false),
   REMOVED(true, false);

   private final boolean cleanupCandidate;
   private final boolean deterministicFailure;

   private WirelessLinkState(boolean cleanupCandidate, boolean deterministicFailure) {
      this.cleanupCandidate = cleanupCandidate;
      this.deterministicFailure = deterministicFailure;
   }

   public boolean isCleanupCandidate() {
      return this.cleanupCandidate;
   }

   public boolean isDeterministicFailure() {
      return this.deterministicFailure;
   }
}
