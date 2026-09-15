package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record SyncFrequencyListPacket(List<SyncFrequencyListPacket.FrequencyEntry> entries) {
   public static void encode(SyncFrequencyListPacket pkt, FriendlyByteBuf buf) {
      buf.writeInt(pkt.entries.size());

      for (SyncFrequencyListPacket.FrequencyEntry e : pkt.entries) {
         buf.writeInt(e.id);
         buf.m_130072_(e.name, 24);
         buf.writeInt(e.color);
         buf.m_130077_(e.ownerUUID);
         buf.writeByte(e.security.getId());
      }
   }

   public static SyncFrequencyListPacket decode(FriendlyByteBuf buf) {
      int size = buf.readInt();
      List<SyncFrequencyListPacket.FrequencyEntry> entries = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         entries.add(
            new SyncFrequencyListPacket.FrequencyEntry(
               buf.readInt(), buf.m_130136_(24), buf.readInt(), buf.m_130259_(), FrequencySecurityLevel.fromId(buf.readByte())
            )
         );
      }

      return new SyncFrequencyListPacket(entries);
   }

   public static SyncFrequencyListPacket fromServer() {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager == null) {
         return new SyncFrequencyListPacket(List.of());
      } else {
         List<SyncFrequencyListPacket.FrequencyEntry> entries = new ArrayList<>();

         for (WirelessFrequency freq : manager.getAllFrequencies()) {
            entries.add(new SyncFrequencyListPacket.FrequencyEntry(freq.getId(), freq.getName(), freq.getColor(), freq.getOwnerUUID(), freq.getSecurity()));
         }

         return new SyncFrequencyListPacket(entries);
      }
   }

   public static void syncOpenMenus(MinecraftServer server) {
      SyncFrequencyListPacket pkt = fromServer();

      for (ServerPlayer player : server.m_6846_().m_11314_()) {
         if (player.f_36096_ instanceof FrequencyMenu) {
            NetworkInit.sendToPlayer(player, pkt);
         }
      }
   }

   public static void handle(SyncFrequencyListPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleFrequencyList(pkt.entries())));
      ctx.setPacketHandled(true);
   }

   public static record FrequencyEntry(int id, String name, int color, UUID ownerUUID, FrequencySecurityLevel security) {
   }
}
