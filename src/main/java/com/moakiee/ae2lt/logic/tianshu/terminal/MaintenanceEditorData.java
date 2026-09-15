package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.stacks.AEKey;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public record MaintenanceEditorData(
   AEKey target,
   @Nullable UUID ruleId,
   long lowerThreshold,
   long upperThreshold,
   long amountPerJob,
   boolean enabled,
   InventoryMaintenanceStatus status,
   long currentStock,
   boolean craftable,
   boolean recoveryPage,
   List<MaintenanceEditorData.TopologyEntry> topology,
   List<MaintenanceEditorData.VariantEntry> variants
) {
   public MaintenanceEditorData(
      AEKey target,
      @Nullable UUID ruleId,
      long lowerThreshold,
      long upperThreshold,
      long amountPerJob,
      boolean enabled,
      InventoryMaintenanceStatus status,
      long currentStock,
      boolean craftable,
      boolean recoveryPage,
      List<MaintenanceEditorData.TopologyEntry> topology,
      List<MaintenanceEditorData.VariantEntry> variants
   ) {
      topology = List.copyOf(topology);
      variants = List.copyOf(variants);
      this.target = target;
      this.ruleId = ruleId;
      this.lowerThreshold = lowerThreshold;
      this.upperThreshold = upperThreshold;
      this.amountPerJob = amountPerJob;
      this.enabled = enabled;
      this.status = status;
      this.currentStock = currentStock;
      this.craftable = craftable;
      this.recoveryPage = recoveryPage;
      this.topology = topology;
      this.variants = variants;
   }

   public static record TopologyEntry(
      AEKey key,
      int depth,
      boolean craftable,
      long storedAmount,
      long globalReserve,
      ReservedStockMatchMode globalMode,
      long ruleReserve,
      ReservedStockMatchMode ruleMode
   ) {
   }

   public static record VariantEntry(AEKey key, long storedAmount, boolean craftable) {
   }
}
