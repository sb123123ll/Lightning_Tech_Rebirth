package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeleteFrequencyPacket(int token, int frequencyId) {
   public static void encode(DeleteFrequencyPacket pkt, FriendlyByteBuf buf) {
      buf.m_130130_(pkt.token);
      buf.writeInt(pkt.frequencyId);
   }

   public static DeleteFrequencyPacket decode(FriendlyByteBuf buf) {
      return new DeleteFrequencyPacket(buf.m_130242_(), buf.readInt());
   }

   public static void handle(DeleteFrequencyPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (FrequencyMenu.validateToken(player, pkt.token) == null) {
               NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
            } else {
               WirelessFrequencyManager manager = WirelessFrequencyManager.get();
               if (manager != null) {
                  WirelessFrequency freq = manager.getFrequency(pkt.frequencyId);
                  if (freq == null) {
                     NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(3));
                  } else if (!freq.getPlayerAccess(player).isOwner()) {
                     NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                  } else {
                     manager.deleteFrequency(pkt.frequencyId, player.m_20194_());
                     UpdateFrequencyBasicPacket.broadcastToPlayers(player.m_20194_(), UpdateFrequencyBasicPacket.forDeletion(pkt.frequencyId));
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}
