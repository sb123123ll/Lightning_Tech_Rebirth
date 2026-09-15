package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record CreateFrequencyPacket(int token, String name, int color, FrequencySecurityLevel security, String password) {
   public static void encode(CreateFrequencyPacket pkt, FriendlyByteBuf buf) {
      buf.m_130130_(pkt.token);
      buf.m_130072_(pkt.name, 24);
      buf.writeInt(pkt.color);
      buf.writeByte(pkt.security.getId());
      buf.m_130072_(pkt.password, 16);
   }

   public static CreateFrequencyPacket decode(FriendlyByteBuf buf) {
      return new CreateFrequencyPacket(buf.m_130242_(), buf.m_130136_(24), buf.readInt(), FrequencySecurityLevel.fromId(buf.readByte()), buf.m_130136_(16));
   }

   public static void handle(CreateFrequencyPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (FrequencyMenu.validateToken(player, pkt.token) == null) {
               NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
            } else {
               WirelessFrequencyManager manager = WirelessFrequencyManager.get();
               if (manager != null) {
                  if (pkt.name.isBlank()) {
                     NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
                  } else {
                     FrequencySecurityLevel effectiveSecurity = pkt.security;
                     if (effectiveSecurity == FrequencySecurityLevel.ENCRYPTED && pkt.password.isBlank()) {
                        effectiveSecurity = FrequencySecurityLevel.PRIVATE;
                     }

                     WirelessFrequency freq = manager.createFrequency(player, pkt.name, pkt.color, effectiveSecurity, pkt.password);
                     if (freq != null) {
                        UpdateFrequencyBasicPacket.broadcastToPlayers(player.m_20194_(), UpdateFrequencyBasicPacket.forFrequency(freq));
                        SyncFrequencyDetailPacket.sendInitialMembersIfNeeded(player, freq.getId());
                     }
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}
