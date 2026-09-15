package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.grid.FrequencyAccessLevel;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record EditFrequencyPacket(int token, int frequencyId, String name, int color, FrequencySecurityLevel security, String password) {
   public static void encode(EditFrequencyPacket pkt, FriendlyByteBuf buf) {
      buf.m_130130_(pkt.token);
      buf.writeInt(pkt.frequencyId);
      buf.m_130072_(pkt.name, 24);
      buf.writeInt(pkt.color);
      buf.writeByte(pkt.security.getId());
      buf.m_130072_(pkt.password, 16);
   }

   public static EditFrequencyPacket decode(FriendlyByteBuf buf) {
      return new EditFrequencyPacket(
         buf.m_130242_(), buf.readInt(), buf.m_130136_(24), buf.readInt(), FrequencySecurityLevel.fromId(buf.readByte()), buf.m_130136_(16)
      );
   }

   public static void handle(EditFrequencyPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(
         () -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
               FrequencyMenu menu = FrequencyMenu.validateToken(player, pkt.token);
               if (menu != null && menu.getCurrentFrequencyId() == pkt.frequencyId) {
                  WirelessFrequencyManager manager = WirelessFrequencyManager.get();
                  if (manager != null) {
                     WirelessFrequency freq = manager.getFrequency(pkt.frequencyId);
                     if (freq == null) {
                        NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(3));
                     } else {
                        FrequencyAccessLevel access = freq.getPlayerAccess(player);
                        if (!access.isManager()) {
                           NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                        } else if (pkt.name.isBlank()) {
                           NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
                        } else {
                           boolean securityChanged = freq.getSecurity() != pkt.security;
                           boolean passwordChanged = pkt.security == FrequencySecurityLevel.ENCRYPTED
                              && !pkt.password.isEmpty()
                              && !WirelessFrequency.hashPassword(pkt.password, freq.getId()).equals(freq.getPassword());
                           if ((securityChanged || passwordChanged) && !access.isOwner()) {
                              NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                           } else {
                              FrequencySecurityLevel effectiveSecurity = pkt.security;
                              if (effectiveSecurity == FrequencySecurityLevel.ENCRYPTED && pkt.password.isBlank() && freq.getPassword().isBlank()) {
                                 effectiveSecurity = FrequencySecurityLevel.PRIVATE;
                              }

                              freq.setName(pkt.name);
                              freq.setColor(pkt.color);
                              if (freq.getSecurity() != effectiveSecurity) {
                                 freq.setSecurity(effectiveSecurity);
                              }

                              if (effectiveSecurity != FrequencySecurityLevel.ENCRYPTED) {
                                 freq.setPassword("");
                              } else if (!pkt.password.isEmpty()) {
                                 freq.setPassword(pkt.password);
                              }

                              manager.markModified();
                              UpdateFrequencyBasicPacket.broadcastToPlayers(player.m_20194_(), UpdateFrequencyBasicPacket.forFrequency(freq));
                           }
                        }
                     }
                  }
               } else {
                  NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
               }
            }
         }
      );
      ctx.setPacketHandled(true);
   }
}
