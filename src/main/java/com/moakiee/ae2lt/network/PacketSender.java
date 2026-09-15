package com.moakiee.ae2lt.network;

import net.minecraft.server.level.ServerPlayer;

public final class PacketSender {
   private PacketSender() {
   }

   public static void sendToServer(Object message) {
      NetworkInit.sendToServer(message);
   }

   public static void sendToPlayer(ServerPlayer player, Object message) {
      NetworkInit.sendToPlayer(player, message);
   }
}
