package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.celestweave.module.PhaseFlightMode;
import com.moakiee.ae2lt.client.ClientNetworkPacketHandlers;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.ByteTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record FlightInertiaSyncPacket(
   UUID armorId, boolean inertiaEnabled, boolean flightControlActive, boolean flying, PhaseFlightMode phaseMode, boolean flightLockEnabled
) {
   public static FlightInertiaSyncPacket decode(FriendlyByteBuf buf) {
      return new FlightInertiaSyncPacket(
         buf.m_130259_(),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readBoolean(),
         PhaseFlightMode.fromTag(ByteTag.m_128266_(buf.readByte())),
         buf.readBoolean()
      );
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130077_(this.armorId);
      buf.writeBoolean(this.inertiaEnabled);
      buf.writeBoolean(this.flightControlActive);
      buf.writeBoolean(this.flying);
      buf.writeByte(this.phaseMode.toTag().m_7063_());
      buf.writeBoolean(this.flightLockEnabled);
   }

   public static void handle(FlightInertiaSyncPacket payload, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkPacketHandlers.handleFlightInertia(payload)));
      ctx.setPacketHandled(true);
   }
}
