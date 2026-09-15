package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import com.moakiee.ae2lt.celestweave.module.PhaseLockSubmodule;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record PhaseFlightInputPacket(boolean jumpHeld, boolean hasFlightInput, boolean flying) {
   public static PhaseFlightInputPacket jump(boolean jumpHeld) {
      return new PhaseFlightInputPacket(jumpHeld, false, false);
   }

   public static PhaseFlightInputPacket flight(boolean jumpHeld, boolean flying) {
      return new PhaseFlightInputPacket(jumpHeld, true, flying);
   }

   public static PhaseFlightInputPacket decode(FriendlyByteBuf buf) {
      return new PhaseFlightInputPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeBoolean(this.jumpHeld);
      buf.writeBoolean(this.hasFlightInput);
      buf.writeBoolean(this.flying);
   }

   public static void handle(PhaseFlightInputPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(
         () -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
               boolean flightModuleActive = PhaseWingFlight.canUse(player);
               boolean flightLockActive = PhaseLockSubmodule.isFlightLockEnabled(player);
               PhaseFlightPlayerState.setJumpHeld(player, packet.jumpHeld() && flightModuleActive);
               if (packet.hasFlightInput() && (flightModuleActive || flightLockActive)) {
                  PhaseFlightPlayerState.activate(player);
                  boolean requestedFlying = packet.flying();
                  if (PhaseFlightControlRules.rejectFlightToggle(
                     PhaseFlightMovementGuard.isPhaseModeEnabled(player), PhaseFlightControlRules.intersectsWorldCollision(player), requestedFlying
                  )) {
                     requestedFlying = true;
                  }

                  if (requestedFlying && player.m_21255_()) {
                     player.m_36321_();
                  }

                  PhaseFlightPlayerState.applyFlightInput(player, requestedFlying);
                  player.m_6885_();
               }
            }
         }
      );
      ctx.setPacketHandled(true);
   }
}
