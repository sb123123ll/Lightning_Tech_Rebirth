package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;

public final class MaintenanceRequestability {
   public static boolean isRequestable(ICraftingService crafting, AEKey key) {
      return crafting != null && key != null && (crafting.isCraftable(key) || crafting.canEmitFor(key));
   }

   private MaintenanceRequestability() {
   }
}
