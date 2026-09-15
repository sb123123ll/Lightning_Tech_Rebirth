package com.moakiee.ae2lt.network.hub;

import com.moakiee.ae2lt.menu.hub.DeviceHubHost;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record OpenDeviceHubPacket(int defaultTab) {
   public static OpenDeviceHubPacket decode(FriendlyByteBuf buf) {
      return new OpenDeviceHubPacket(buf.m_130242_());
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.defaultTab);
   }

   public static void handle(OpenDeviceHubPacket pkt, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            DeviceHubHost.open(player, pkt.defaultTab());
         }
      });
      ctx.setPacketHandled(true);
   }
}
