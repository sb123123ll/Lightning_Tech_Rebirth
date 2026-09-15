package com.moakiee.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityPhaseMovementMixin {
   @Inject(
      method = {"isCrouching"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$exposePhaseFlightCrouchChord(CallbackInfoReturnable<Boolean> cir) {
      if (this instanceof Player player
         && PhaseFlightControlRules.isCrouchChord(PhaseWingFlight.canUse(player), PhaseFlightPlayerState.isJumpHeld(player), player.m_6144_())) {
         cir.setReturnValue(true);
      }
   }

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void ae2lt$authorizeVanillaTravelMoveVelocity(Entity entity, Vec3 movement, Operation<Void> original) {
      if (this instanceof Player player && PhaseFlightMovementGuard.isVanillaTravelScopeActive(player)) {
         PhaseFlightMovementGuard.runAsVanillaTravelMovement(player, () -> original.call(new Object[]{entity, movement}));
         return;
      }

      original.call(new Object[]{entity, movement});
   }

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"
      )}
   )
   private void ae2lt$authorizeVanillaTravelMoveVelocityComponents(Entity entity, double x, double y, double z, Operation<Void> original) {
      if (this instanceof Player player && PhaseFlightMovementGuard.isVanillaTravelScopeActive(player)) {
         PhaseFlightMovementGuard.runAsVanillaTravelMovement(player, () -> original.call(new Object[]{entity, x, y, z}));
         return;
      }

      original.call(new Object[]{entity, x, y, z});
   }

   @WrapMethod(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"}
   )
   private void ae2lt$guardPhaseFlightMove(MoverType moverType, Vec3 movement, Operation<Void> original) {
      if (this instanceof Player player && PhaseFlightMovementGuard.blocksExternalForces(player) && !PhaseFlightMovementGuard.isSelfMovementAuthorized(player)) {
         return;
      }

      if (this instanceof Player player) {
         PhaseFlightMovementGuard.runAsMovementPositionUpdate(player, () -> original.call(new Object[]{moverType, movement}));
      } else {
         original.call(new Object[]{moverType, movement});
      }
   }

   @Inject(
      method = {"setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$blockExternalPhaseFlightForce(Vec3 movement, CallbackInfo ci) {
      if (this instanceof Player player && PhaseFlightMovementGuard.blocksExternalForces(player) && !PhaseFlightMovementGuard.isSelfMovementAuthorized(player)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"setPosRaw(DDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$blockExternalPhaseFlightTeleport(double x, double y, double z, CallbackInfo ci) {
      if (this instanceof Player player
         && PhaseFlightMovementGuard.blocksExternalTeleports(player)
         && !PhaseFlightMovementGuard.isSelfTeleportAuthorized(player)
         && !PhaseFlightMovementGuard.isMovementPositionUpdate(player)) {
         Vec3 current = player.m_20182_();
         if (Double.compare(current.f_82479_, x) != 0 || Double.compare(current.f_82480_, y) != 0 || Double.compare(current.f_82481_, z) != 0) {
            if (player instanceof ServerPlayer serverPlayer) {
               PhaseFlightMovementGuard.notifyBlockedTeleport(serverPlayer, new Vec3(x, y, z));
            }

            ci.cancel();
         }

         return;
      }
   }
}
