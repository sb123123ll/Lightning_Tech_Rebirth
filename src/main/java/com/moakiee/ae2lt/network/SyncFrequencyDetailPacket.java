package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record SyncFrequencyDetailPacket(int frequencyId, byte syncType, CompoundTag data) {
   public static final byte TYPE_MEMBERS = 2;
   public static final byte TYPE_CONNECTIONS = 10;

   public static void encode(SyncFrequencyDetailPacket pkt, FriendlyByteBuf buf) {
      buf.writeInt(pkt.frequencyId);
      buf.writeByte(pkt.syncType);
      buf.m_130079_(pkt.data);
   }

   public static SyncFrequencyDetailPacket decode(FriendlyByteBuf buf) {
      int id = buf.readInt();
      byte type = buf.readByte();
      CompoundTag tag = buf.m_130260_();
      return new SyncFrequencyDetailPacket(id, type, tag == null ? new CompoundTag() : tag);
   }

   public static SyncFrequencyDetailPacket forMembers(WirelessFrequency freq) {
      CompoundTag tag = new CompoundTag();
      freq.writeToTag(tag, (byte)2);
      return new SyncFrequencyDetailPacket(freq.getId(), (byte)2, tag);
   }

   public static void broadcastMembersTo(MinecraftServer server, int frequencyId) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null) {
         WirelessFrequency freq = manager.getFrequency(frequencyId);
         if (freq != null) {
            SyncFrequencyDetailPacket pkt = forMembers(freq);

            for (ServerPlayer player : server.m_6846_().m_11314_()) {
               AbstractContainerMenu var8 = player.f_36096_;
               if (var8 instanceof FrequencyMenu) {
                  FrequencyMenu fm = (FrequencyMenu)var8;
                  if (fm.getCurrentFrequencyId() == frequencyId) {
                     NetworkInit.sendToPlayer(player, pkt);
                  }
               }
            }
         }
      }
   }

   public static void sendInitialMembersIfNeeded(ServerPlayer player, int frequencyId) {
      if (frequencyId > 0) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager != null) {
            WirelessFrequency freq = manager.getFrequency(frequencyId);
            if (freq != null) {
               NetworkInit.sendToPlayer(player, forMembers(freq));
            }
         }
      }
   }

   public static SyncFrequencyDetailPacket forConnections(int frequencyId, MinecraftServer server) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      CompoundTag tag = new CompoundTag();
      ListTag list = new ListTag();
      if (manager != null && server != null) {
         for (WirelessFrequencyManager.DeviceEntry d : manager.getDevices(frequencyId)) {
            ServerLevel lvl = server.m_129880_(d.dimension());
            boolean loaded = lvl != null && lvl.m_7726_().m_7131_(d.pos().m_123341_() >> 4, d.pos().m_123343_() >> 4) != null;
            String deviceName = d.deviceName();
            CompoundTag e = new CompoundTag();
            e.m_128359_("dim", d.dimension().m_135782_().toString());
            e.m_128356_("pos", d.pos().m_121878_());
            e.m_128379_("controller", d.isController());
            e.m_128379_("advanced", d.advanced());
            e.m_128379_("loaded", loaded);
            e.m_128359_("name", deviceName);
            list.add(e);
         }
      }

      tag.m_128365_("connections", list);
      return new SyncFrequencyDetailPacket(frequencyId, (byte)10, tag);
   }

   public static void broadcastConnectionsTo(MinecraftServer server, int frequencyId) {
      if (server != null && frequencyId > 0) {
         boolean hasReceiver = false;

         for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player.f_36096_ instanceof FrequencyMenu fm && fm.getCurrentFrequencyId() == frequencyId) {
               hasReceiver = true;
               break;
            }
         }

         if (hasReceiver) {
            SyncFrequencyDetailPacket pkt = forConnections(frequencyId, server);

            for (ServerPlayer playerx : server.m_6846_().m_11314_()) {
               AbstractContainerMenu var7 = playerx.f_36096_;
               if (var7 instanceof FrequencyMenu) {
                  FrequencyMenu fm = (FrequencyMenu)var7;
                  if (fm.getCurrentFrequencyId() == frequencyId) {
                     NetworkInit.sendToPlayer(playerx, pkt);
                  }
               }
            }
         }
      }
   }

   public static void sendInitialConnectionsIfNeeded(ServerPlayer player, int frequencyId) {
      if (frequencyId > 0) {
         NetworkInit.sendToPlayer(player, forConnections(frequencyId, player.m_20194_()));
      }
   }

   public static void handle(SyncFrequencyDetailPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(
         () -> DistExecutor.unsafeRunWhenOn(
               Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleFrequencyDetail(pkt.frequencyId(), pkt.syncType(), pkt.data())
            )
      );
      ctx.setPacketHandled(true);
   }
}
