package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record RitualItemBurstPacket(int entityId, byte stage) {
   public static final byte PIGMEE_CORE = 0;
   public static final byte UNDYING_MODULE = 1;
   public static final byte PHASE_LOCK_MODULE = 2;

   public void write(FriendlyByteBuf buf) {
      buf.m_130130_(this.entityId);
      buf.writeByte(this.stage);
   }

   public static RitualItemBurstPacket decode(FriendlyByteBuf buf) {
      return new RitualItemBurstPacket(buf.m_130242_(), buf.readByte());
   }

   public static void handle(RitualItemBurstPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleRitualItemBurst(packet)));
      ctx.setPacketHandled(true);
   }
}
