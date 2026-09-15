package com.moakiee.ae2lt.celestweave;

public final class ArmorMitigationRules {
   private static final float MID_ORDINARY_PASS_RATE = 0.2F;
   private static final float MID_HARD_PASS_RATE = 0.5F;

   private ArmorMitigationRules() {
   }

   static ArmorMitigationRules.DamageClass classify(boolean environmentDamage, boolean hardDamage) {
      if (environmentDamage) {
         return ArmorMitigationRules.DamageClass.ENVIRONMENT;
      } else {
         return hardDamage ? ArmorMitigationRules.DamageClass.HARD : ArmorMitigationRules.DamageClass.ORDINARY;
      }
   }

   public static float apply(String stage, ArmorMitigationRules.DamageClass damageClass, float incomingDamage) {
      float incoming = Math.max(0.0F, incomingDamage);
      if (incoming <= 0.0F) {
         return 0.0F;
      } else {
         return switch (stage) {
            case "matrix_shield" -> applyMidStage(damageClass, incoming);
            case "phase_shield", "multidimensional_protection" -> 0.0F;
            default -> incoming;
         };
      }
   }

   private static float applyMidStage(ArmorMitigationRules.DamageClass damageClass, float incoming) {
      return switch (damageClass) {
         case ENVIRONMENT -> 0.0F;
         case HARD -> incoming * 0.5F;
         case ORDINARY -> incoming * 0.2F;
      };
   }

   public static enum DamageClass {
      ENVIRONMENT,
      ORDINARY,
      HARD;
   }
}
