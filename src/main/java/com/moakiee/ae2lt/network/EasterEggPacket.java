package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record EasterEggPacket() {
   public static EasterEggPacket decode(FriendlyByteBuf buf) {
      return new EasterEggPacket();
   }

   public static void encode(EasterEggPacket payload, FriendlyByteBuf buf) {
   }

   public static void handle(EasterEggPacket payload, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientNetworkPacketHandlers::handleEasterEgg));
      ctx.setPacketHandled(true);
   }
}
