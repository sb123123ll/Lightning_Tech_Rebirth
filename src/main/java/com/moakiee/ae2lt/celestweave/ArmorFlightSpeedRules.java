package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.FlightSubmodule;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import net.minecraft.world.item.ItemStack;

public final class ArmorFlightSpeedRules {
   private ArmorFlightSpeedRules() {
   }

   public static float activeFlightSpeed(ItemStack armor) {
      if (CelestweaveArmorState.isSubmoduleRuntimeActive(armor, PhaseFlightSubmodule.INSTANCE.id())) {
         return (float)Math.max(0.05F, PhaseFlightSubmodule.phaseSpeed(armor));
      } else {
         return CelestweaveArmorState.isSubmoduleRuntimeActive(armor, FlightSubmodule.INSTANCE.id()) ? FlightSubmodule.flightSpeed(armor) : 0.05F;
      }
   }
}
