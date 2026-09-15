package com.moakiee.ae2lt.logic.tianshu.maintenance;

public enum InventoryMaintenanceBadge {
   GREEN,
   YELLOW,
   RED,
   GRAY;

   public static InventoryMaintenanceBadge from(InventoryMaintenanceStatus status) {
      if (status == null) {
         return YELLOW;
      } else {
         return switch (status) {
            case DISABLED -> GRAY;
            case SATISFIED -> GREEN;
            case MISSING_PATTERN, MISSING_INGREDIENTS, OFFLINE -> RED;
            default -> YELLOW;
         };
      }
   }
}
