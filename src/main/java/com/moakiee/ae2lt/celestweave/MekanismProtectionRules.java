package com.moakiee.ae2lt.celestweave;

public final class MekanismProtectionRules {
   public static final int RADIATION_REGEN_INTERVAL_TICKS = 20;
   public static final float MIN_RADIATION_HEALING = 2.0F;
   public static final float MAX_RADIATION_HEALING = 10.0F;

   private MekanismProtectionRules() {
   }

   public static boolean shouldRegenerate(long gameTime, double radiationLevel, double minimumRadiation, float health, float maximumHealth) {
      return gameTime % 20L == 0L
         && Double.isFinite(radiationLevel)
         && Double.isFinite(minimumRadiation)
         && radiationLevel >= Math.max(0.0, minimumRadiation)
         && health > 0.0F
         && health < maximumHealth;
   }

   public static float radiationHealing(double scaledSeverity) {
      double severity = Double.isFinite(scaledSeverity) ? Math.max(0.0, Math.min(1.0, scaledSeverity)) : 0.0;
      return (float)(2.0 + 8.0 * severity);
   }

   public static long absorbedJoules(long availableJoules, double dissipationPercent) {
      if (availableJoules <= 0L || !Double.isFinite(dissipationPercent) || dissipationPercent <= 0.0) {
         return 0L;
      } else {
         return dissipationPercent >= 1.0 ? availableJoules : Math.max(0L, (long)Math.floor((double)availableJoules * dissipationPercent));
      }
   }

   public static long joulesToForgeEnergy(long joules, double joulesPerForgeEnergy) {
      if (joules > 0L && Double.isFinite(joulesPerForgeEnergy) && !(joulesPerForgeEnergy <= 0.0)) {
         double converted = (double)joules / joulesPerForgeEnergy;
         return converted >= 9.223372E18F ? Long.MAX_VALUE : Math.max(0L, (long)Math.floor(converted));
      } else {
         return 0L;
      }
   }
}
