package com.moakiee.ae2lt.grid.wirelesslink;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.me.GridConnection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class WirelessLinkOps {
   private static final Set<IGridConnection> WIRELESS_BRIDGES = Collections.newSetFromMap(new IdentityHashMap<>());

   private WirelessLinkOps() {
   }

   public static boolean hasLiveConnection(@Nullable IGridConnection connection, @Nullable IGridNode node) {
      if (connection != null && node != null) {
         for (IGridConnection candidate : node.getConnections()) {
            if (candidate == connection) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean isConnectedTo(@Nullable IGridConnection connection, @Nullable IGridNode node, @Nullable IGridNode other) {
      return hasLiveConnection(connection, node) && !connection.isInWorld() && other != null && connection.getOtherSide(node) == other;
   }

   @Nullable
   private static IGridConnection findConnection(IGridNode node, IGridNode other) {
      for (IGridConnection connection : node.getConnections()) {
         if (connection.getOtherSide(node) == other && hasLiveConnection(connection, other)) {
            return connection;
         }
      }

      return null;
   }

   public static synchronized void destroy(@Nullable IGridConnection connection, @Nullable IGridNode node) {
      if (connection != null && WIRELESS_BRIDGES.remove(connection)) {
         if (hasLiveConnection(connection, node) && !connection.isInWorld()) {
            connection.destroy();
         }
      }
   }

   public static synchronized boolean isWirelessBridge(@Nullable IGridConnection connection) {
      return connection != null && WIRELESS_BRIDGES.contains(connection);
   }

   static synchronized void trackWirelessBridge(IGridConnection connection) {
      WIRELESS_BRIDGES.add(connection);
   }

   public static synchronized void clearWirelessBridgeTracking() {
      WIRELESS_BRIDGES.clear();
   }

   public static synchronized IGridConnection createVirtualConnection(IGridNode targetNode, IGridNode transmitterNode) {
      IGridConnection existing = findConnection(targetNode, transmitterNode);
      if (existing != null) {
         return reuseVirtualConnection(existing);
      } else {
         try {
            GridConnection connection = GridConnection.create(targetNode, transmitterNode, null);
            trackWirelessBridge(connection);
            return connection;
         } catch (IllegalStateException var4) {
            existing = findConnection(targetNode, transmitterNode);
            if (isDuplicateConnectionFailure(var4) && existing != null && !existing.isInWorld()) {
               return reuseVirtualConnection(existing);
            } else {
               throw var4;
            }
         }
      }
   }

   private static IGridConnection reuseVirtualConnection(IGridConnection connection) {
      if (connection.isInWorld()) {
         throw new IllegalStateException("A physical connection already exists between the target and transmitter.");
      } else {
         trackWirelessBridge(connection);
         return connection;
      }
   }

   private static boolean isDuplicateConnectionFailure(IllegalStateException exception) {
      String message = exception.getMessage();
      return message != null && message.contains("already exists");
   }
}
