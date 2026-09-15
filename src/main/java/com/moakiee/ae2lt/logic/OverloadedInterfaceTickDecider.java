package com.moakiee.ae2lt.logic;

import appeng.api.networking.ticking.TickRateModulation;

public final class OverloadedInterfaceTickDecider {
   private static final int ALL_NORMAL_DIRECTIONS = 6;

   private OverloadedInterfaceTickDecider() {
   }

   public static boolean hasGridItemIoWork(
      boolean wirelessMode, boolean hasImportBuffer, boolean hasWirelessConnections, boolean importAuto, boolean exportAuto
   ) {
      if (hasImportBuffer) {
         return true;
      } else {
         return wirelessMode ? hasWirelessConnections && (importAuto || exportAuto) : importAuto || exportAuto;
      }
   }

   public static boolean hasServerEnergyWork(
      boolean wirelessMode, boolean hasWirelessConnections, boolean hasEnergyOutput, boolean hasFeKey, boolean hasInductionCard
   ) {
      return (wirelessMode && hasWirelessConnections || hasEnergyOutput) && hasFeKey && hasInductionCard;
   }

   public static TickRateModulation gridTickModulation(boolean hasItemIoWork, boolean craftingCardInstalled, boolean craftingDidWork) {
      if (!hasItemIoWork && !craftingDidWork) {
         return craftingCardInstalled ? TickRateModulation.SLOWER : TickRateModulation.IDLE;
      } else {
         return TickRateModulation.URGENT;
      }
   }

   public static int normalIoDirectionCount(boolean hasConfiguredDirection) {
      return hasConfiguredDirection ? 1 : 6;
   }

   public static boolean shouldRegisterEjectPorts(boolean wirelessMode, boolean ejectMode) {
      return wirelessMode && ejectMode;
   }
}
