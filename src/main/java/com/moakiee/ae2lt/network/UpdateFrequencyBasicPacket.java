package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record UpdateFrequencyBasicPacket(int frequencyId, boolean deleted, String name, int color, UUID ownerUUID, FrequencySecurityLevel security) {
   private static final UUID ZERO_UUID = new UUID(0L, 0L);

   public static void encode(UpdateFrequencyBasicPacket pkt, FriendlyByteBuf buf) {
      buf.writeInt(pkt.frequencyId);
      buf.writeBoolean(pkt.deleted);
      if (!pkt.deleted) {
         buf.m_130072_(pkt.name, 24);
         buf.writeInt(pkt.color);
         buf.m_130077_(pkt.ownerUUID);
         buf.writeByte(pkt.security.getId());
      }
   }

   public static UpdateFrequencyBasicPacket decode(FriendlyByteBuf buf) {
      int id = buf.readInt();
      boolean deleted = buf.readBoolean();
      return deleted
         ? new UpdateFrequencyBasicPacket(id, true, "", 0, ZERO_UUID, FrequencySecurityLevel.PUBLIC)
         : new UpdateFrequencyBasicPacket(id, false, buf.m_130136_(24), buf.readInt(), buf.m_130259_(), FrequencySecurityLevel.fromId(buf.readByte()));
   }

   public static UpdateFrequencyBasicPacket forFrequency(WirelessFrequency freq) {
      return new UpdateFrequencyBasicPacket(freq.getId(), false, freq.getName(), freq.getColor(), freq.getOwnerUUID(), freq.getSecurity());
   }

   public static UpdateFrequencyBasicPacket forDeletion(int frequencyId) {
      return new UpdateFrequencyBasicPacket(frequencyId, true, "", 0, ZERO_UUID, FrequencySecurityLevel.PUBLIC);
   }

   public static void broadcastToPlayers(MinecraftServer server, UpdateFrequencyBasicPacket pkt) {
      if (server != null) {
         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            NetworkInit.sendToPlayer(player, pkt);
         }
      }
   }

   public static void handle(UpdateFrequencyBasicPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleFrequencyBasicUpdate(pkt)));
      ctx.setPacketHandled(true);
   }
}
