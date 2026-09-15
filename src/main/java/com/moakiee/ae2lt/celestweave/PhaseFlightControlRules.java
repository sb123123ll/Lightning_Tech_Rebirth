package com.moakiee.ae2lt.celestweave;

import net.minecraft.world.entity.player.Player;

public final class PhaseFlightControlRules {
   private PhaseFlightControlRules() {
   }

   public static boolean rejectFlightToggle(boolean phaseModeEnabled, boolean insideWall, boolean requestedFlying) {
      return phaseModeEnabled && insideWall && !requestedFlying;
   }

   public static boolean isCrouchChord(boolean flightControlActive, boolean jumpHeld, boolean shiftHeld) {
      return flightControlActive && jumpHeld && shiftHeld;
   }

   public static boolean shouldSyncJumpInput(boolean jumpHeld, boolean lastJumpHeld, long controlGeneration, long lastControlGeneration) {
      return jumpHeld != lastJumpHeld || controlGeneration != lastControlGeneration;
   }

   public static boolean preserveFlightOnLanding(boolean flightLocked, boolean phaseModeEnabled, boolean phaseFlying) {
      return flightLocked && !phaseModeEnabled && phaseFlying;
   }

   public static boolean effectiveFlying(boolean controlled, boolean flightLocked, boolean phaseFlying, boolean vanillaFlying) {
      return controlled && flightLocked ? phaseFlying : vanillaFlying;
   }

   public static boolean handoffFlying(boolean effectiveFlying, boolean flightSourceAvailable) {
      return effectiveFlying && flightSourceAvailable;
   }

   public static boolean exposeGroundCrouch(boolean flightLocked, boolean phaseModeEnabled, boolean phaseFlying, boolean onGround, boolean shiftHeld) {
      return flightLocked && !phaseModeEnabled && phaseFlying && onGround && shiftHeld;
   }

   public static boolean intersectsWorldCollision(Player player) {
      return player != null && !player.m_9236_().m_45756_(player, player.m_20191_());
   }
}
