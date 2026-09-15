package com.moakiee.ae2lt.network.tianshu;

import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record RequestUploadTargetsPacket(int containerId) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
   }

   public static RequestUploadTargetsPacket decode(FriendlyByteBuf buf) {
      return new RequestUploadTargetsPacket(buf.m_130242_());
   }

   public static void handle(RequestUploadTargetsPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == packet.containerId()) {
            menu.sendUploadTargets(player);
         }
      });
      ctx.setPacketHandled(true);
   }
}
