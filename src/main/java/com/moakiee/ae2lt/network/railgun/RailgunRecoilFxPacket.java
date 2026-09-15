package com.moakiee.ae2lt.network.railgun;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record RailgunRecoilFxPacket(float pitchUp, int tierOrdinal) {
   public void write(FriendlyByteBuf buf) {
      buf.writeFloat(this.pitchUp);
      buf.m_130130_(this.tierOrdinal);
   }

   public static RailgunRecoilFxPacket decode(FriendlyByteBuf buf) {
      return new RailgunRecoilFxPacket(buf.readFloat(), buf.m_130242_());
   }

   public static void handle(RailgunRecoilFxPacket p, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> RailgunClientBridge.recoil(p));
      ctx.setPacketHandled(true);
   }
}
