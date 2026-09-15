package com.moakiee.ae2lt.logic;

public final class PassiveAeCharger {
   private PassiveAeCharger() {
   }

   public static boolean charge(PassiveAeCharger.Storage storage, double passiveAePerTick) {
      if (!(passiveAePerTick > 0.0)) {
         return false;
      } else {
         double current = storage.getAECurrentPower();
         double max = storage.getAEMaxPower();
         double remaining = max - current;
         if (!(remaining > 0.0)) {
            return false;
         } else {
            storage.setInternalCurrentPower(current + Math.min(passiveAePerTick, remaining));
            return true;
         }
      }
   }

   public interface Storage {
      double getAECurrentPower();

      double getAEMaxPower();

      void setInternalCurrentPower(double var1);
   }
}
