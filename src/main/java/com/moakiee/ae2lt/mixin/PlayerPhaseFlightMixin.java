package com.moakiee.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moakiee.ae2lt.celestweave.FlightSneakMovement;
import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerPhaseFlightMixin implements PhaseFlightPlayerState.Access {
   @Shadow
   @Final
   private Abilities f_36077_;
   @Unique
   private boolean ae2lt$phaseFlightControlled;
   @Unique
   private boolean ae2lt$phaseFlying;
   @Unique
   private boolean ae2lt$phaseJumpHeld;
   @Unique
   private boolean ae2lt$phaseFlightLocked = true;
   @Unique
   private boolean ae2lt$sneakHoverTravel;

   @Override
   public boolean ae2lt$isPhaseFlightControlled() {
      return this.ae2lt$phaseFlightControlled;
   }

   @Override
   public void ae2lt$setPhaseFlightControlled(boolean controlled) {
      this.ae2lt$phaseFlightControlled = controlled;
   }

   @Override
   public boolean ae2lt$isPhaseFlying() {
      return this.ae2lt$phaseFlying;
   }

   @Override
   public void ae2lt$setPhaseFlying(boolean flying) {
      this.ae2lt$phaseFlying = flying;
   }

   @Override
   public boolean ae2lt$isPhaseJumpHeld() {
      return this.ae2lt$phaseJumpHeld;
   }

   @Override
   public void ae2lt$setPhaseJumpHeld(boolean jumpHeld) {
      this.ae2lt$phaseJumpHeld = jumpHeld;
   }

   @Override
   public boolean ae2lt$isPhaseFlightLocked() {
      return this.ae2lt$phaseFlightLocked;
   }

   @Override
   public void ae2lt$setPhaseFlightLocked(boolean locked) {
      this.ae2lt$phaseFlightLocked = locked;
   }

   @Override
   public boolean ae2lt$getVanillaFlying() {
      return this.f_36077_.f_35935_;
   }

   @Override
   public void ae2lt$setVanillaFlying(boolean flying) {
      this.f_36077_.f_35935_ = flying;
   }

   @Inject(
      method = {"getAbilities"},
      at = {@At("HEAD")}
   )
   private void ae2lt$projectLockedPhaseFlying(CallbackInfoReturnable<Abilities> cir) {
      if (this.ae2lt$phaseFlightControlled && this.ae2lt$phaseFlightLocked) {
         this.f_36077_.f_35935_ = this.ae2lt$phaseFlying;
      }
   }

   @ModifyExpressionValue(
      method = {"tick", "isAffectedByFluids", "maybeBackOffFromEdge", "travel", "updateSwimming", "makeStuckInBlock", "getMovementEmission", "isSwimming", "isPushedByFluid", "getBlockSpeedFactor", "getFlyingSpeed"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
         opcode = 180
      )}
   )
   private boolean ae2lt$readLockedPhaseFlying(boolean vanillaFlying) {
      return PhaseFlightPlayerState.readEffectiveFlying((Player)this, vanillaFlying);
   }

   @Inject(
      method = {"updatePlayerPose"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$keepPhaseFlightOutOfSwimmingPose(CallbackInfo ci) {
      Player player = (Player)this;
      boolean phaseTraversal = PhaseFlightSubmodule.hasTransientPhaseState(player);
      boolean crouchChord = PhaseFlightControlRules.isCrouchChord(
         PhaseFlightPlayerState.isControlled(player), PhaseFlightPlayerState.isJumpHeld(player), player.m_6144_()
      );
      boolean groundCrouch = PhaseFlightControlRules.exposeGroundCrouch(
         PhaseFlightPlayerState.isFlightLocked(player),
         PhaseFlightMovementGuard.isPhaseModeEnabled(player),
         PhaseFlightPlayerState.isFlying(player),
         player.m_20096_(),
         player.m_6144_()
      );
      if (phaseTraversal || crouchChord || groundCrouch) {
         Pose pose;
         if (player.m_21255_()) {
            pose = Pose.FALL_FLYING;
         } else if (!crouchChord && !groundCrouch) {
            pose = Pose.STANDING;
         } else {
            pose = Pose.CROUCHING;
         }

         player.m_20124_(pose);
         ci.cancel();
      }
   }

   @WrapOperation(
      method = {"travel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void ae2lt$authorizePlayerTravelVelocity(Player player, Vec3 movement, Operation<Void> original) {
      PhaseFlightMovementGuard.runAsVanillaTravelMovement(player, () -> original.call(new Object[]{player, movement}));
   }

   @WrapOperation(
      method = {"travel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(DDD)V"
      )}
   )
   private void ae2lt$authorizePlayerTravelVelocityComponents(Player player, double x, double y, double z, Operation<Void> original) {
      PhaseFlightMovementGuard.runAsVanillaTravelMovement(player, () -> original.call(new Object[]{player, x, y, z}));
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")}
   )
   private void ae2lt$beginPlayerAuthorizedTravel(Vec3 travelVector, CallbackInfo ci) {
      Player player = (Player)this;
      this.ae2lt$sneakHoverTravel = player.m_6109_() && FlightSneakMovement.isActive(player);
      if (this.ae2lt$sneakHoverTravel) {
         player.m_6858_(false);
         PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_20256_(Vec3.f_82478_));
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")}
   )
   private void ae2lt$endPlayerAuthorizedTravel(Vec3 travelVector, CallbackInfo ci) {
      Player player = (Player)this;
      if (this.ae2lt$sneakHoverTravel) {
         PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_20256_(Vec3.f_82478_));
         this.ae2lt$sneakHoverTravel = false;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;updateIsUnderwater()Z",
         shift = Shift.BEFORE
      )}
   )
   private void ae2lt$applyPhaseFlightPseudoSpectator(CallbackInfo ci) {
      Player player = (Player)this;
      if (PhaseFlightSubmodule.hasTransientPhaseState(player)) {
         PhaseFlightSubmodule.applyTransientPhaseState(player);
      }
   }
}
