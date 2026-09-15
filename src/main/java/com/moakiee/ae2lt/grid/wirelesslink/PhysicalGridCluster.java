package com.moakiee.ae2lt.grid.wirelesslink;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class PhysicalGridCluster {
   private PhysicalGridCluster() {
   }

   static Set<IGridNode> collect(IGridNode start) {
      Set<IGridNode> visited = newIdentityNodeSet();
      ArrayDeque<IGridNode> queue = new ArrayDeque<>();
      visited.add(start);
      queue.add(start);

      while (!queue.isEmpty()) {
         IGridNode node = queue.removeFirst();

         for (IGridConnection connection : node.getConnections()) {
            if (!WirelessLinkOps.isWirelessBridge(connection)) {
               IGridNode other;
               try {
                  other = connection.getOtherSide(node);
               } catch (IllegalArgumentException var8) {
                  continue;
               }

               if (other != null && !KnownInternalGridNodes.isSupplementalEntranceExcluded(other) && visited.add(other)) {
                  queue.addLast(other);
               }
            }
         }
      }

      return visited;
   }

   static Set<IGridNode> directNeighbours(Iterable<IGridNode> changedNodes) {
      Set<IGridNode> changed = newIdentityNodeSet();

      for (IGridNode node : changedNodes) {
         changed.add(node);
      }

      Set<IGridNode> neighbours = newIdentityNodeSet();

      for (IGridNode node : changed) {
         for (IGridConnection connection : node.getConnections()) {
            if (!WirelessLinkOps.isWirelessBridge(connection)) {
               IGridNode other;
               try {
                  other = connection.getOtherSide(node);
               } catch (IllegalArgumentException var9) {
                  continue;
               }

               if (other != null && !KnownInternalGridNodes.isSupplementalEntranceExcluded(other) && !changed.contains(other)) {
                  neighbours.add(other);
               }
            }
         }
      }

      return neighbours;
   }

   static Set<IGridNode> newIdentityNodeSet() {
      return Collections.newSetFromMap(new IdentityHashMap<>());
   }
}
