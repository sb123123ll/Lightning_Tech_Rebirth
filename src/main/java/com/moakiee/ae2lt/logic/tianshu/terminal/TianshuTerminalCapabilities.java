package com.moakiee.ae2lt.logic.tianshu.terminal;

import com.moakiee.ae2lt.logic.tianshu.TianshuFunctionProfile;

public record TianshuTerminalCapabilities(
   boolean hasTianshu, boolean closedLoopUploadAvailable, boolean inventoryMaintenanceAvailable, boolean reservedStockAvailable
) {
   public static TianshuTerminalCapabilities withoutTianshu() {
      return new TianshuTerminalCapabilities(false, false, false, false);
   }

   public static TianshuTerminalCapabilities forTianshu(boolean formed, TianshuFunctionProfile profile) {
      if (formed && profile != null) {
         boolean closedLoopReady = profile.supportsClosedLoopPatterns() && profile.closedLoopPatternCapacity() > 0 && profile.supportsClosedLoopSeeds();
         boolean maintenanceReady = profile.supportsInventoryMaintenance();
         return new TianshuTerminalCapabilities(true, closedLoopReady, maintenanceReady, maintenanceReady);
      } else {
         return withoutTianshu();
      }
   }

   public boolean allows(TianshuTerminalAction action) {
      if (action == null) {
         return false;
      } else {
         return switch (action) {
            case ENCODE_NORMAL_PATTERN, UPLOAD_NORMAL_PATTERN, ENCODE_CLOSED_LOOP_PATTERN -> true;
            case UPLOAD_CLOSED_LOOP_PATTERN -> this.closedLoopUploadAvailable;
            case CONFIGURE_INVENTORY_MAINTENANCE -> this.inventoryMaintenanceAvailable;
            case CONFIGURE_RESERVED_STOCK -> this.reservedStockAvailable;
         };
      }
   }
}
