package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;

final class FlightAbilityRestoreRules {
   private FlightAbilityRestoreRules() {
   }

   static FlightAbilityRestoreRules.Target targetAfterReleaseProbe(boolean currentGameModeFlight, boolean mayflyReasserted, boolean currentFlying) {
      boolean targetMayfly = currentGameModeFlight || mayflyReasserted;
      boolean targetFlying = PhaseFlightControlRules.handoffFlying(currentFlying, targetMayfly);
      return new FlightAbilityRestoreRules.Target(targetMayfly, targetFlying);
   }

   static record Target(boolean mayfly, boolean flying) {
   }
}
