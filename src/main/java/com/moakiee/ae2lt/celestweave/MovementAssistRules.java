package com.moakiee.ae2lt.celestweave;

public final class MovementAssistRules {
   public static final double VANILLA_STEP_HEIGHT = 0.6;

   private MovementAssistRules() {
   }

   public static double movementMultiplier(
      boolean suppressGroundMovement, boolean crouching, boolean sprinting, double walkMultiplier, double sprintMultiplier, double sneakMultiplier
   ) {
      if (suppressGroundMovement) {
         return 1.0;
      } else if (crouching) {
         return positiveOrDefault(sneakMultiplier, 1.0);
      } else {
         return sprinting ? positiveOrDefault(sprintMultiplier, 1.0) : positiveOrDefault(walkMultiplier, 1.0);
      }
   }

   public static double speedModifierAmount(double multiplier) {
      return positiveOrDefault(multiplier, 1.0) - 1.0;
   }

   public static double stepHeightModifierAmount(double configuredHeight) {
      return Math.max(0.0, positiveOrDefault(configuredHeight, 0.6) - 0.6);
   }

   private static double positiveOrDefault(double value, double fallback) {
      return Double.isFinite(value) && value > 0.0 ? value : fallback;
   }
}
