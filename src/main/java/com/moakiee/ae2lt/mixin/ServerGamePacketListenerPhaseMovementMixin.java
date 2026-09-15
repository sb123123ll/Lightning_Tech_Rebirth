package com.moakiee.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import java.util.Set;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerGamePacketListenerImpl.class})
public abstract class ServerGamePacketListenerPhaseMovementMixin {
   @Inject(
      method = {"handlePlayerAbilities"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$rejectExternalLockedFlightUpdate(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
      ServerPlayer player = ((ServerGamePacketListenerImpl)this).f_9743_;
      if (PhaseFlightPlayerState.isFlightLocked(player)) {
         player.m_150110_().f_35935_ = PhaseFlightPlayerState.isFlying(player);
         player.m_6885_();
         ci.cancel();
      }
   }

   @Inject(
      method = {"teleport(DDDFFLjava/util/Set;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$blockExternalPhaseTeleportPacket(
      double x, double y, double z, float yRot, float xRot, Set<RelativeMovement> relativeMovements, CallbackInfo ci
   ) {
      ServerPlayer player = ((ServerGamePacketListenerImpl)this).f_9743_;
      Vec3 target = new Vec3(
         relativeMovements.contains(RelativeMovement.X) ? player.m_20185_() + x : x,
         relativeMovements.contains(RelativeMovement.Y) ? player.m_20186_() + y : y,
         relativeMovements.contains(RelativeMovement.Z) ? player.m_20189_() + z : z
      );
      if (PhaseFlightMovementGuard.blocksExternalTeleports(player)
         && !PhaseFlightMovementGuard.isSelfTeleportAuthorized(player)
         && !player.m_20182_().equals(target)) {
         PhaseFlightMovementGuard.notifyBlockedTeleport(player, target);
         ci.cancel();
      }
   }

   @WrapMethod(
      method = {"handleMovePlayer"}
   )
   private void ae2lt$runPlayerAuthorizedMove(ServerboundMovePlayerPacket packet, Operation<Void> original) {
      ServerPlayer player = ((ServerGamePacketListenerImpl)this).f_9743_;
      PhaseFlightMovementGuard.beginMovementPacket(player);

      try {
         original.call(new Object[]{packet});
      } finally {
         PhaseFlightMovementGuard.endMovementPacket(player);
      }
   }

   @WrapMethod(
      method = {"handleCustomPayload"}
   )
   private void ae2lt$runPlayerPayload(ServerboundCustomPayloadPacket packet, Operation<Void> original) {
      ServerPlayer player = ((ServerGamePacketListenerImpl)this).f_9743_;
      PhaseFlightMovementGuard.beginCustomPayload(player);

      try {
         original.call(new Object[]{packet});
      } finally {
         PhaseFlightMovementGuard.endCustomPayload(player);
      }
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V"
      )}
   )
   private void ae2lt$authorizeVanillaTickPositionRestore(ServerPlayer player, double x, double y, double z, float yRot, float xRot, Operation<Void> original) {
      PhaseFlightMovementGuard.runAsSelfMovement(player, () -> original.call(new Object[]{player, x, y, z, yRot, xRot}));
   }
}
