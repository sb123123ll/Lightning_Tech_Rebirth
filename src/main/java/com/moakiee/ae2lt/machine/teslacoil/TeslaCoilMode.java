package com.moakiee.ae2lt.machine.teslacoil;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.me.key.LightningKey;
import net.minecraft.util.StringRepresentable;

public enum TeslaCoilMode implements StringRepresentable {
   HIGH_VOLTAGE("high_voltage"),
   EXTREME_HIGH_VOLTAGE("extreme_high_voltage");

   public static final int PROCESS_TICKS = 5;
   private final String serializedName;

   private TeslaCoilMode(String serializedName) {
      this.serializedName = serializedName;
   }

   public static TeslaCoilMode fromOrdinal(int ordinal) {
      return ordinal == EXTREME_HIGH_VOLTAGE.ordinal() ? EXTREME_HIGH_VOLTAGE : HIGH_VOLTAGE;
   }

   public static TeslaCoilMode fromName(String serializedName) {
      for (TeslaCoilMode value : values()) {
         if (value.serializedName.equals(serializedName)) {
            return value;
         }
      }

      return HIGH_VOLTAGE;
   }

   public TeslaCoilMode next() {
      return this == HIGH_VOLTAGE ? EXTREME_HIGH_VOLTAGE : HIGH_VOLTAGE;
   }

   public long totalEnergy() {
      return this == EXTREME_HIGH_VOLTAGE ? (long)AE2LTCommonConfig.teslaCoilExtremeHighVoltageFe() : (long)AE2LTCommonConfig.teslaCoilHighVoltageFe();
   }

   public long requiredEnergyForTick(int completedTicks, long consumedEnergy, long totalEnergy) {
      if (completedTicks >= 5) {
         return 0L;
      } else {
         int remainingTicks = Math.max(1, 5 - completedTicks);
         long remainingEnergy = Math.max(0L, totalEnergy - consumedEnergy);
         return remainingEnergy <= 0L ? 0L : divideCeil(remainingEnergy, (long)remainingTicks);
      }
   }

   public int requiredDust() {
      return this == HIGH_VOLTAGE ? AE2LTCommonConfig.teslaCoilHighVoltageDustCost() : 0;
   }

   public long requiredHighVoltage() {
      return this == EXTREME_HIGH_VOLTAGE ? (long)AE2LTCommonConfig.teslaCoilExtremeHighVoltageInput() : 0L;
   }

   public LightningKey outputKey() {
      return this == EXTREME_HIGH_VOLTAGE ? LightningKey.EXTREME_HIGH_VOLTAGE : LightningKey.HIGH_VOLTAGE;
   }

   public String translationKey() {
      return "ae2lt.gui.tesla_coil.mode." + this.serializedName;
   }

   public String m_7912_() {
      return this.serializedName;
   }

   private static long divideCeil(long dividend, long divisor) {
      if (divisor <= 0L) {
         throw new IllegalArgumentException("divisor must be positive");
      } else {
         return (dividend + divisor - 1L) / divisor;
      }
   }
}
