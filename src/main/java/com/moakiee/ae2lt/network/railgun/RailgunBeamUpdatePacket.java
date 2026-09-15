package com.moakiee.ae2lt.network.railgun;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

public record RailgunBeamUpdatePacket(UUID shooterId, Vec3 from, Vec3 to, boolean active) {
   public void write(FriendlyByteBuf buf) {
      buf.m_130077_(this.shooterId);
      buf.writeDouble(this.from.f_82479_);
      buf.writeDouble(this.from.f_82480_);
      buf.writeDouble(this.from.f_82481_);
      buf.writeDouble(this.to.f_82479_);
      buf.writeDouble(this.to.f_82480_);
      buf.writeDouble(this.to.f_82481_);
      buf.writeBoolean(this.active);
   }

   public static RailgunBeamUpdatePacket decode(FriendlyByteBuf buf) {
      return new RailgunBeamUpdatePacket(
         buf.m_130259_(),
         new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
         new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
         buf.readBoolean()
      );
   }

   public static void handle(RailgunBeamUpdatePacket p, Supplier<Context> ctxSup) {
      Context ctx = ctxSup.get();
      ctx.enqueueWork(() -> RailgunClientBridge.beamUpdate(p));
      ctx.setPacketHandled(true);
   }
}
