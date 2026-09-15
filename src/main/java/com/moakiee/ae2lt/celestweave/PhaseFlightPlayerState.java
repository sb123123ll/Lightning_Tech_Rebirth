package com.moakiee.ae2lt.celestweave;

import net.minecraft.world.entity.player.Player;

public final class PhaseFlightPlayerState {
   private PhaseFlightPlayerState() {
   }

   public static void activate(Player player) {
      if (player instanceof PhaseFlightPlayerState.Access access && !access.ae2lt$isPhaseFlightControlled()) {
         access.ae2lt$setPhaseFlying(access.ae2lt$getVanillaFlying());
         access.ae2lt$setPhaseFlightLocked(true);
         access.ae2lt$setPhaseFlightControlled(true);
      }
   }

   public static boolean isControlled(Player player) {
      if (player instanceof PhaseFlightPlayerState.Access access && access.ae2lt$isPhaseFlightControlled()) {
         return true;
      }

      return false;
   }

   public static boolean isFlying(Player player) {
      return readEffectiveFlying(player, getVanillaFlying(player));
   }

   public static boolean readEffectiveFlying(Player player, boolean vanillaFlying) {
      return player instanceof PhaseFlightPlayerState.Access access
         ? PhaseFlightControlRules.effectiveFlying(
            access.ae2lt$isPhaseFlightControlled(), access.ae2lt$isPhaseFlightLocked(), access.ae2lt$isPhaseFlying(), vanillaFlying
         )
         : vanillaFlying;
   }

   public static boolean isJumpHeld(Player player) {
      if (player instanceof PhaseFlightPlayerState.Access access && access.ae2lt$isPhaseFlightControlled() && access.ae2lt$isPhaseJumpHeld()) {
         return true;
      }

      return false;
   }

   public static void applyFlightInput(Player player, boolean flying) {
      writeFlying(player, flying);
   }

   public static void synchronizeFlying(Player player, boolean flying) {
      writeFlying(player, flying);
   }

   private static void writeFlying(Player player, boolean flying) {
      if (player instanceof PhaseFlightPlayerState.Access access && access.ae2lt$isPhaseFlightControlled()) {
         access.ae2lt$setPhaseFlying(flying);
         access.ae2lt$setVanillaFlying(flying);
      }
   }

   public static void setJumpHeld(Player player, boolean jumpHeld) {
      if (player instanceof PhaseFlightPlayerState.Access access && access.ae2lt$isPhaseFlightControlled()) {
         access.ae2lt$setPhaseJumpHeld(jumpHeld);
      }
   }

   public static boolean isFlightLocked(Player player) {
      if (player instanceof PhaseFlightPlayerState.Access access && access.ae2lt$isPhaseFlightControlled() && access.ae2lt$isPhaseFlightLocked()) {
         return true;
      }

      return false;
   }

   public static void setFlightLocked(Player player, boolean locked) {
      if (player instanceof PhaseFlightPlayerState.Access access && access.ae2lt$isPhaseFlightControlled() && access.ae2lt$isPhaseFlightLocked() != locked) {
         if (locked) {
            access.ae2lt$setPhaseFlying(access.ae2lt$getVanillaFlying());
         } else {
            access.ae2lt$setVanillaFlying(access.ae2lt$isPhaseFlying());
         }

         access.ae2lt$setPhaseFlightLocked(locked);
         return;
      }
   }

   public static boolean getVanillaFlying(Player player) {
      return player instanceof PhaseFlightPlayerState.Access access ? access.ae2lt$getVanillaFlying() : player != null && player.m_150110_().f_35935_;
   }

   public static void endControl(Player player) {
      endControl(player, isFlying(player));
   }

   public static void endControl(Player player, boolean flying) {
      if (player instanceof PhaseFlightPlayerState.Access access) {
         access.ae2lt$setVanillaFlying(flying);
         access.ae2lt$setPhaseJumpHeld(false);
         access.ae2lt$setPhaseFlying(false);
         access.ae2lt$setPhaseFlightLocked(true);
         access.ae2lt$setPhaseFlightControlled(false);
      } else if (player != null) {
         player.m_150110_().f_35935_ = flying;
      }
   }

   public interface Access {
      boolean ae2lt$isPhaseFlightControlled();

      void ae2lt$setPhaseFlightControlled(boolean var1);

      boolean ae2lt$isPhaseFlying();

      void ae2lt$setPhaseFlying(boolean var1);

      boolean ae2lt$isPhaseJumpHeld();

      void ae2lt$setPhaseJumpHeld(boolean var1);

      boolean ae2lt$isPhaseFlightLocked();

      void ae2lt$setPhaseFlightLocked(boolean var1);

      boolean ae2lt$getVanillaFlying();

      void ae2lt$setVanillaFlying(boolean var1);
   }
}
