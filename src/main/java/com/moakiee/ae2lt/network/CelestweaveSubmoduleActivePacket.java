package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record CelestweaveSubmoduleActivePacket(UUID armorId, String submoduleId, boolean active) {
   public static CelestweaveSubmoduleActivePacket decode(FriendlyByteBuf buf) {
      return new CelestweaveSubmoduleActivePacket(buf.m_130259_(), buf.m_130136_(128), buf.readBoolean());
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130077_(this.armorId);
      buf.m_130072_(this.submoduleId, 128);
      buf.writeBoolean(this.active);
   }

   public static void handle(CelestweaveSubmoduleActivePacket payload, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleCelestweaveSubmoduleActive(payload)));
      ctx.setPacketHandled(true);
   }
}
