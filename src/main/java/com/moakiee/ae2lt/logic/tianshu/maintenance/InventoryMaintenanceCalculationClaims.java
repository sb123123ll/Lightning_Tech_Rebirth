package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

final class InventoryMaintenanceCalculationClaims {
   private static final Map<IGrid, Map<AEKey, UUID>> CLAIMS = new WeakHashMap<>();

   static synchronized boolean tryClaim(IGrid grid, AEKey key, UUID owner) {
      if (grid != null && key != null && owner != null) {
         Map<AEKey, UUID> byKey = CLAIMS.computeIfAbsent(grid, ignored -> new HashMap<>());
         UUID existing = byKey.get(key);
         if (existing != null && !existing.equals(owner)) {
            return false;
         } else {
            byKey.put(key, owner);
            return true;
         }
      } else {
         return false;
      }
   }

   static synchronized boolean claimedByOther(IGrid grid, AEKey key, UUID owner) {
      Map<AEKey, UUID> byKey = CLAIMS.get(grid);
      if (byKey == null) {
         return false;
      } else {
         UUID claim = byKey.get(key);
         return claim == null ? false : !claim.equals(owner);
      }
   }

   static synchronized void release(IGrid grid, AEKey key, UUID owner) {
      Map<AEKey, UUID> byKey = CLAIMS.get(grid);
      if (byKey != null) {
         UUID claim = byKey.get(key);
         if (claim != null && claim.equals(owner)) {
            byKey.remove(key);
         }

         if (byKey.isEmpty()) {
            CLAIMS.remove(grid);
         }
      }
   }

   private InventoryMaintenanceCalculationClaims() {
   }
}
