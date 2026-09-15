package com.moakiee.ae2lt.celestweave;

import net.minecraft.world.entity.player.Player;

public final class FlightSneakMovement {
   private static final double GROUND_DRAG = 0.546;

   private FlightSneakMovement() {
   }

   public static boolean isActive(Player player) {
      return isActive(player, PhaseFlightPlayerState.isJumpHeld(player), player.m_6144_());
   }

   public static boolean isActive(Player player, boolean jumpHeld, boolean shiftHeld) {
      return isActive(
         PhaseFlightPlayerState.isControlled(player),
         PhaseFlightPlayerState.isFlying(player),
         player.m_21255_(),
         player.m_20159_(),
         jumpHeld,
         shiftHeld,
         PhaseFlightControlRules.exposeGroundCrouch(
            PhaseFlightPlayerState.isFlightLocked(player),
            PhaseFlightMovementGuard.isPhaseModeEnabled(player),
            PhaseFlightPlayerState.isFlying(player),
            player.m_20096_(),
            shiftHeld
         )
      );
   }

   static boolean isActive(boolean controlled, boolean flying, boolean gliding, boolean passenger, boolean jumpHeld, boolean shiftHeld, boolean groundCrouch) {
      return controlled && flying && !gliding && !passenger && (jumpHeld && shiftHeld || groundCrouch);
   }

   public static float movementSpeed(float walkingSpeed) {
      return (float)((double)walkingSpeed / 0.45399999999999996);
   }
}
