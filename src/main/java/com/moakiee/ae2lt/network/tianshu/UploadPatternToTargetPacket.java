package com.moakiee.ae2lt.network.tianshu;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record UploadPatternToTargetPacket(int containerId, PatternContainerGroup group) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      this.group.writeToPacket(buf);
   }

   public static UploadPatternToTargetPacket decode(FriendlyByteBuf buf) {
      return new UploadPatternToTargetPacket(buf.m_130242_(), PatternContainerGroup.readFromPacket(buf));
   }

   public static void handle(UploadPatternToTargetPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == packet.containerId()) {
            menu.uploadTianshuPatternToTarget(player, packet.group());
         }
      });
      ctx.setPacketHandled(true);
   }
}
