package com.moakiee.ae2lt.grid.wirelesslink;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class WirelessClusterEntrancePlanner {
   private WirelessClusterEntrancePlanner() {
   }

   @Nullable
   static IGridNode findSupplementalEntrance(Set<IGridNode> cluster, Set<IGridNode> existingEntrances) {
      Set<IGridNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      ArrayDeque<IGridNode> queue = new ArrayDeque<>();
      IGridNode directFallback = null;

      for (IGridNode node : cluster) {
         if (!KnownInternalGridNodes.isSupplementalEntranceExcluded(node) && node.hasFlag(GridFlags.REQUIRE_CHANNEL) && !node.meetsChannelRequirements()) {
            if (visited.add(node)) {
               queue.addLast(node);
            }

            if (directFallback == null && !existingEntrances.contains(node)) {
               directFallback = node;
            }
         }
      }

      while (!queue.isEmpty()) {
         IGridNode nodex = queue.removeFirst();
         if (!existingEntrances.contains(nodex) && !nodex.hasFlag(GridFlags.CANNOT_CARRY) && !nodex.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            return nodex;
         }

         for (IGridConnection connection : nodex.getConnections()) {
            if (!WirelessLinkOps.isWirelessBridge(connection)) {
               IGridNode other;
               try {
                  other = connection.getOtherSide(nodex);
               } catch (IllegalArgumentException var10) {
                  continue;
               }

               if (other != null && !KnownInternalGridNodes.isSupplementalEntranceExcluded(other) && cluster.contains(other) && visited.add(other)) {
                  queue.addLast(other);
               }
            }
         }
      }

      return directFallback;
   }
}
