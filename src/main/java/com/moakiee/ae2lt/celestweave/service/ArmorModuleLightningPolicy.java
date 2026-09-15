package com.moakiee.ae2lt.celestweave.service;

import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;

public final class ArmorModuleLightningPolicy {
   private ArmorModuleLightningPolicy() {
   }

   public static ArmorLightningService.LightningCost passiveCost(
      List<DeviceCapability> capabilities,
      boolean movingFlight,
      boolean phaseTraversalActive,
      long reachHvPerTick,
      long flightHvPerTick,
      long phaseFlightHvPerTick
   ) {
      boolean creativeFlight = false;
      boolean phaseTraversal = false;
      boolean reachExtension = false;

      for (DeviceCapability capability : capabilities) {
         if (capability instanceof DeviceCapability.FlightMode mode) {
            creativeFlight = true;
         } else if (capability instanceof DeviceCapability.PhaseTraversal) {
            phaseTraversal = true;
         } else if (capability instanceof DeviceCapability.InteractionRange) {
            reachExtension = true;
         }
      }

      if (phaseTraversal && phaseTraversalActive) {
         return ArmorLightningService.LightningCost.hv(phaseFlightHvPerTick);
      } else if (creativeFlight) {
         long amount = flightHvPerTick;
         if (movingFlight) {
            amount = flightHvPerTick + flightHvPerTick;
         }

         return ArmorLightningService.LightningCost.hv(amount);
      } else {
         return reachExtension ? ArmorLightningService.LightningCost.hv(reachHvPerTick) : ArmorLightningService.LightningCost.NONE;
      }
   }

   public static ArmorLightningService.LightningCost triggeredCost(ArmorModuleLightningPolicy.Trigger trigger) {
      return switch (trigger) {
         case MATRIX_SHIELD -> ArmorLightningService.LightningCost.hv(1L);
         case PHASE_SHIELD -> ArmorLightningService.LightningCost.ehv(1L);
         case UNDYING -> ArmorLightningService.LightningCost.ehv(512L);
         default -> ArmorLightningService.LightningCost.NONE;
      };
   }

   public static enum Trigger {
      DASH,
      MATRIX_SHIELD,
      PHASE_SHIELD,
      REFLECT,
      UNDYING,
      PURIFICATION,
      SATURATION,
      DIG_AFFINITY;
   }
}
