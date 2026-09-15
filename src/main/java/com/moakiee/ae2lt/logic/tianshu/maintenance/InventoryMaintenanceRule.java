package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.stacks.AEKey;
import java.util.Objects;
import java.util.UUID;

public record InventoryMaintenanceRule(
   UUID id, AEKey key, long lowerThreshold, long upperThreshold, long amountPerJob, boolean enabled, boolean replenishing, UUID activeCraftingId
) {
   public InventoryMaintenanceRule(
      UUID id, AEKey key, long lowerThreshold, long upperThreshold, long amountPerJob, boolean enabled, boolean replenishing, UUID activeCraftingId
   ) {
      id = Objects.requireNonNull(id, "id");
      key = Objects.requireNonNull(key, "key");
      if (lowerThreshold < 0L || upperThreshold < lowerThreshold) {
         throw new IllegalArgumentException("maintenance thresholds require 0 <= lower <= upper");
      } else if (amountPerJob <= 0L) {
         throw new IllegalArgumentException("amount per job must be positive");
      } else {
         this.id = id;
         this.key = key;
         this.lowerThreshold = lowerThreshold;
         this.upperThreshold = upperThreshold;
         this.amountPerJob = amountPerJob;
         this.enabled = enabled;
         this.replenishing = replenishing;
         this.activeCraftingId = activeCraftingId;
      }
   }

   public InventoryMaintenanceRule withRuntime(boolean newReplenishing, UUID craftingId) {
      return new InventoryMaintenanceRule(
         this.id, this.key, this.lowerThreshold, this.upperThreshold, this.amountPerJob, this.enabled, newReplenishing, craftingId
      );
   }

   public long nextRequestAmount(long currentStock) {
      return Math.max(0L, Math.min(this.amountPerJob, this.upperThreshold - Math.max(0L, currentStock)));
   }
}
