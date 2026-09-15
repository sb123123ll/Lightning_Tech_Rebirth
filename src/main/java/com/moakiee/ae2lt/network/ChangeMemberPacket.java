package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record ChangeMemberPacket(int token, int frequencyId, UUID targetUUID, byte operationType) {
   public static void encode(ChangeMemberPacket pkt, FriendlyByteBuf buf) {
      buf.m_130130_(pkt.token);
      buf.writeInt(pkt.frequencyId);
      buf.m_130077_(pkt.targetUUID);
      buf.writeByte(pkt.operationType);
   }

   public static ChangeMemberPacket decode(FriendlyByteBuf buf) {
      return new ChangeMemberPacket(buf.m_130242_(), buf.readInt(), buf.m_130259_(), buf.readByte());
   }

   public static void handle(ChangeMemberPacket pkt, Supplier<Context> ctxSupplier) {
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
                  } else {
                     int result = freq.changeMembership(player, pkt.targetUUID, pkt.operationType);
                     if (result == 0) {
                        manager.markModified();
                        SyncFrequencyDetailPacket.broadcastMembersTo(player.m_20194_(), pkt.frequencyId);
                     } else {
                        int responseCode = switch (result) {
                           case 1 -> 2;
                           default -> 4;
                        };
                        NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(responseCode));
                     }
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}
