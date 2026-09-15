package com.moakiee.ae2lt.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.FlightSneakMovement;
import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.PhaseFlightInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerPhaseMovementMixin {
   @ModifyExpressionValue(
      method = {"aiStep"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isMovingSlowly()Z"
      )}
   )
   private boolean ae2lt$applySneakInputWithoutKeyDelay(boolean movingSlowly) {
      LocalPlayer player = (LocalPlayer)this;
      if (PhaseFlightPlayerState.isControlled(player) && PhaseFlightPlayerState.isFlying(player)) {
         Options options = Minecraft.m_91087_().f_91066_;
         return FlightSneakMovement.isActive(player, options.f_92089_.m_90857_(), options.f_92090_.m_90857_());
      } else {
         return movingSlowly;
      }
   }

   @ModifyExpressionValue(
      method = {"aiStep"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
         opcode = 180
      )}
   )
   private boolean ae2lt$readEffectiveFlightState(boolean vanillaFlying) {
      return PhaseFlightPlayerState.readEffectiveFlying((LocalPlayer)this, vanillaFlying);
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
         opcode = 181,
         ordinal = 0
      )
   )
   private void ae2lt$rejectAlwaysFlyingOverride(Abilities abilities, boolean requestedFlying) {
      LocalPlayer player = (LocalPlayer)this;
      abilities.f_35935_ = PhaseFlightPlayerState.isFlightLocked(player) ? PhaseFlightPlayerState.isFlying(player) : requestedFlying;
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;onUpdateAbilities()V",
         ordinal = 0
      )
   )
   private void ae2lt$syncAlwaysFlyingUnlessLocked(LocalPlayer player) {
      if (!PhaseFlightPlayerState.isFlightLocked(player)) {
         player.m_6885_();
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
         opcode = 181,
         ordinal = 1
      )
   )
   private void ae2lt$applyPhaseFlightInput(Abilities abilities, boolean requestedFlying) {
      LocalPlayer player = (LocalPlayer)this;
      if (!CelestweaveArmorState.isAnyClientFlightControlActive() && !PhaseFlightPlayerState.isFlightLocked(player)) {
         abilities.f_35935_ = requestedFlying;
      } else {
         PhaseFlightPlayerState.activate(player);
         if (PhaseFlightControlRules.rejectFlightToggle(
            PhaseFlightMovementGuard.isPhaseModeEnabled(player), PhaseFlightControlRules.intersectsWorldCollision(player), requestedFlying
         )) {
            requestedFlying = true;
         }

         if (requestedFlying && player.m_21255_()) {
            player.m_36321_();
         }

         PhaseFlightPlayerState.applyFlightInput(player, requestedFlying);
         NetworkInit.sendToServer(PhaseFlightInputPacket.flight(PhaseFlightPlayerState.isJumpHeld(player), requestedFlying));
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;onUpdateAbilities()V",
         ordinal = 1
      )
   )
   private void ae2lt$useSinglePhaseFlightInputPath(LocalPlayer player) {
      if (!CelestweaveArmorState.isAnyClientFlightControlActive() && !PhaseFlightPlayerState.isFlightLocked(player)) {
         player.m_6885_();
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
         opcode = 181,
         ordinal = 2
      )
   )
   private void ae2lt$preserveLockedFlightOnLanding(Abilities abilities, boolean requestedFlying) {
      LocalPlayer player = (LocalPlayer)this;
      if (PhaseFlightControlRules.preserveFlightOnLanding(
         PhaseFlightPlayerState.isFlightLocked(player), PhaseFlightMovementGuard.isPhaseModeEnabled(player), PhaseFlightPlayerState.isFlying(player)
      )) {
         abilities.f_35935_ = PhaseFlightPlayerState.isFlying(player);
      } else {
         abilities.f_35935_ = requestedFlying;
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;onUpdateAbilities()V",
         ordinal = 2
      )
   )
   private void ae2lt$syncLandingExitUnlessLocked(LocalPlayer player) {
      if (!PhaseFlightControlRules.preserveFlightOnLanding(
         PhaseFlightPlayerState.isFlightLocked(player), PhaseFlightMovementGuard.isPhaseModeEnabled(player), PhaseFlightPlayerState.isFlying(player)
      )) {
         player.m_6885_();
      }
   }

   @Inject(
      method = {"isCrouching"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$exposePhaseFlightCrouchChord(CallbackInfoReturnable<Boolean> cir) {
      LocalPlayer player = (LocalPlayer)this;
      boolean crouchChord = PhaseFlightControlRules.isCrouchChord(
         CelestweaveArmorState.isAnyClientFlightControlActive(), PhaseFlightPlayerState.isJumpHeld(player), player.m_6144_()
      );
      boolean groundCrouch = PhaseFlightControlRules.exposeGroundCrouch(
         PhaseFlightPlayerState.isFlightLocked(player),
         PhaseFlightMovementGuard.isPhaseModeEnabled(player),
         PhaseFlightPlayerState.isFlying(player),
         player.m_20096_(),
         player.m_6144_()
      );
      if (crouchChord || groundCrouch) {
         cir.setReturnValue(true);
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )
   )
   private void ae2lt$authorizeVerticalFlightInput(LocalPlayer player, Vec3 movement) {
      PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_20256_(movement));
   }
}
