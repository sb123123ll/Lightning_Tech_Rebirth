package com.moakiee.ae2lt.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record ShieldHitFeedbackSuppressionPacket(int entityId) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.entityId);
   }

   public static ShieldHitFeedbackSuppressionPacket decode(FriendlyByteBuf buf) {
      return new ShieldHitFeedbackSuppressionPacket(buf.m_130242_());
   }

   public static void handle(ShieldHitFeedbackSuppressionPacket payload, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> ShieldHitFeedbackClientBridge.suppress(payload));
      ctx.setPacketHandled(true);
   }
}
