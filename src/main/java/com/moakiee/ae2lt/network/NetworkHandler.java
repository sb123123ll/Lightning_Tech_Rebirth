package com.moakiee.ae2lt.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class NetworkHandler {
   private NetworkHandler() {
   }

   public static void sendToTrackingChunk(ServerLevel level, ChunkPos chunkPos, Object payload) {
      for (ServerPlayer p : level.m_7726_().f_8325_.m_183262_(chunkPos, false)) {
         NetworkInit.sendToPlayer(p, payload);
      }
   }
}
