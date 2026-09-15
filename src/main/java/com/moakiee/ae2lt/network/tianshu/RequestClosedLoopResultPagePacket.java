package com.moakiee.ae2lt.network.tianshu;

import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopResultPage;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import io.netty.handler.codec.DecoderException;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record RequestClosedLoopResultPagePacket(int containerId, ClosedLoopResultPage.Kind kind, int offset) {
   public RequestClosedLoopResultPagePacket(int containerId, ClosedLoopResultPage.Kind kind, int offset) {
      if (kind == null) {
         throw new IllegalArgumentException("missing closed-loop result kind");
      } else if (offset >= 0 && offset < 243) {
         this.containerId = containerId;
         this.kind = kind;
         this.offset = offset;
      } else {
         throw new IllegalArgumentException("invalid closed-loop result offset: " + offset);
      }
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.containerId);
      buf.m_130068_(this.kind);
      buf.m_130130_(this.offset);
   }

   public static RequestClosedLoopResultPagePacket decode(FriendlyByteBuf buf) {
      int containerId = buf.m_130242_();
      ClosedLoopResultPage.Kind kind = (ClosedLoopResultPage.Kind)buf.m_130066_(ClosedLoopResultPage.Kind.class);
      int offset = buf.m_130242_();
      if (offset >= 0 && offset < 243) {
         return new RequestClosedLoopResultPagePacket(containerId, kind, offset);
      } else {
         throw new DecoderException("invalid closed-loop result offset: " + offset);
      }
   }

   public static void handle(RequestClosedLoopResultPagePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && player.f_36096_ instanceof TianshuPatternEncodingTermMenu menu && menu.f_38840_ == packet.containerId()) {
            menu.sendClosedLoopResultPage(player, packet.kind(), packet.offset());
         }
      });
      ctx.setPacketHandled(true);
   }
}
