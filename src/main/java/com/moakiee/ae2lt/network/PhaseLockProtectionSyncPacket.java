package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record PhaseLockProtectionSyncPacket(UUID armorId, boolean blockExternalForces) {
   public static PhaseLockProtectionSyncPacket decode(FriendlyByteBuf buf) {
      return new PhaseLockProtectionSyncPacket(buf.m_130259_(), buf.readBoolean());
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130077_(this.armorId);
      buf.writeBoolean(this.blockExternalForces);
   }

   public static void handle(PhaseLockProtectionSyncPacket payload, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handlePhaseLockProtection(payload)));
      ctx.setPacketHandled(true);
   }
}
