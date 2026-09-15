package com.moakiee.ae2lt.logic.craft;

import com.moakiee.ae2lt.logic.compute.ComputeTier;
import com.moakiee.ae2lt.logic.compute.ComputingUnitTotals;
import com.moakiee.ae2lt.logic.compute.MatrixComputeEnvelope;
import com.moakiee.ae2lt.logic.compute.UnifiedCraftingComputeCalculator;

public final class MatrixCraftingMath {
   public static final long STABLE_T1_OPERATIONS = 1024L;
   public static final long STABLE_T2_OPERATIONS = 3584L;
   public static final long STABLE_OPERATION_CAP = 4096L;
   public static final long QUANTUM_OPERATION_CAP = 122880L;
   private static final double HEAT_CAPACITY_BASE = 2048.0;
   private static final double HEAT_CAPACITY_PER_COOL_UNIT = 150.0;
   private static final double COOLING_BASE = 8.0E-5;
   private static final double COOLING_PER_COOL_UNIT = 2.5E-5;
   private static final double NORMAL_HEAT_GAIN_PER_DISPATCH_UNIT = 0.256;
   private static final double OVERLOAD_HEAT_GAIN_PER_DISPATCH_UNIT = 1.2032;
   private static final double COLD_EFFICIENCY_FLOOR = 0.45;
   private static final double OVERLOAD_EFFICIENCY_FLOOR = 0.05;

   private MatrixCraftingMath() {
   }

   public static double coolUnits(double coolPower) {
      return nonNegative(coolPower);
   }

   public static double heatCapacity(double coolUnits) {
      return 2048.0 + nonNegative(coolUnits) * 150.0;
   }

   public static double coolingRate(double coolUnits) {
      return 8.0E-5 + nonNegative(coolUnits) * 2.5E-5;
   }

   public static double overloadHeatCurve(double normalizedHeat) {
      double h = clamp01(normalizedHeat);
      return Math.pow(clamp01(4.0 * h * (1.0 - h)), 5.0);
   }

   public static MatrixCraftingMath.Snapshot idleSnapshot(double heat, double coolPower) {
      double currentHeat = nonNegative(heat);
      return new MatrixCraftingMath.Snapshot(currentHeat, normalizedHeat(currentHeat, coolPower), 0L, 0.0);
   }

   public static MatrixCraftingMath.Snapshot multidimensionalSnapshot() {
      return new MatrixCraftingMath.Snapshot(0.0, 0.0, Long.MAX_VALUE, 1.0);
   }

   public static MatrixCraftingMath.Snapshot stableSnapshot(double heat, double dispatchUnits, double amplifierUnits, double coolPower) {
      return coldSnapshot(ComputeTier.BASELINE, heat, dispatchUnits, amplifierUnits, coolPower);
   }

   public static MatrixCraftingMath.Snapshot stableSnapshot(double heat, long stableBaseOperations, double coolPower) {
      double currentHeat = nonNegative(heat);
      double normalizedHeat = normalizedHeat(currentHeat, coolPower);
      double efficiency = clamp(1.0 - normalizedHeat, 0.45, 1.0);
      long baseOperations = Math.min(4096L, Math.max(0L, stableBaseOperations));
      return fixedBaseSnapshot(currentHeat, normalizedHeat, baseOperations, efficiency);
   }

   public static MatrixCraftingMath.Snapshot quantumSnapshot(double heat, double dispatchUnits, double amplifierUnits, double coolPower) {
      return coldSnapshot(ComputeTier.QUANTUM, heat, dispatchUnits, amplifierUnits, coolPower, 122880L);
   }

   public static MatrixCraftingMath.Snapshot overloadSnapshot(double heat, double dispatchUnits, double amplifierUnits, double coolPower) {
      double currentHeat = nonNegative(heat);
      double normalizedHeat = normalizedHeat(currentHeat, coolPower);
      double efficiency = 0.05 + 0.95 * overloadHeatCurve(normalizedHeat);
      return snapshot(ComputeTier.OVERLOAD, currentHeat, normalizedHeat, dispatchUnits, amplifierUnits, coolPower, efficiency);
   }

   private static MatrixCraftingMath.Snapshot coldSnapshot(ComputeTier tier, double heat, double dispatchUnits, double amplifierUnits, double coolPower) {
      return coldSnapshot(tier, heat, dispatchUnits, amplifierUnits, coolPower, tier.copyCap());
   }

   private static MatrixCraftingMath.Snapshot coldSnapshot(
      ComputeTier tier, double heat, double dispatchUnits, double amplifierUnits, double coolPower, long operationCap
   ) {
      double currentHeat = nonNegative(heat);
      double normalizedHeat = normalizedHeat(currentHeat, coolPower);
      double efficiency = clamp(1.0 - normalizedHeat, 0.45, 1.0);
      return snapshot(tier, currentHeat, normalizedHeat, dispatchUnits, amplifierUnits, coolPower, efficiency, operationCap);
   }

   private static MatrixCraftingMath.Snapshot snapshot(
      ComputeTier tier, double heat, double normalizedHeat, double dispatchUnits, double amplifierUnits, double coolPower, double efficiency
   ) {
      return snapshot(tier, heat, normalizedHeat, dispatchUnits, amplifierUnits, coolPower, efficiency, tier.copyCap());
   }

   private static MatrixCraftingMath.Snapshot snapshot(
      ComputeTier tier, double heat, double normalizedHeat, double dispatchUnits, double amplifierUnits, double coolPower, double efficiency, long operationCap
   ) {
      int dispatchCount = floorToInt(dispatchUnits);
      int amplifierCount = floorToInt(amplifierUnits);
      int coolingCount = floorToInt(coolPower);
      ComputingUnitTotals units = new ComputingUnitTotals(dispatchCount, amplifierCount, 0, coolingCount);
      MatrixComputeEnvelope envelope = UnifiedCraftingComputeCalculator.matrixEnvelope(tier, units, efficiency, operationCap);
      return new MatrixCraftingMath.Snapshot(heat, normalizedHeat, envelope.operationsPerTick(), envelope.thermalEfficiency());
   }

   private static MatrixCraftingMath.Snapshot fixedBaseSnapshot(double heat, double normalizedHeat, long baseOperations, double efficiency) {
      double sanitizedEfficiency = Math.max(0.0, Math.min(1.0, efficiency));
      double scaledOperations = (double)baseOperations * sanitizedEfficiency;
      long operations = Double.isFinite(scaledOperations) && !(scaledOperations >= 9.223372E18F)
         ? Math.max(0L, (long)Math.floor(scaledOperations))
         : Long.MAX_VALUE;
      return new MatrixCraftingMath.Snapshot(heat, normalizedHeat, operations, sanitizedEfficiency);
   }

   public static double advanceHeatForCompletedTick(
      double heat, MatrixCoreMode mode, long acceptedOperations, long availableOperations, double dispatchUnits, double coolPower
   ) {
      double load = availableOperations <= 0L ? 0.0 : clamp((double)Math.max(0L, acceptedOperations) / (double)availableOperations, 0.0, 1.0);
      double gainPerUnit = mode == MatrixCoreMode.OVERLOAD ? 1.2032 : 0.256;
      double nextHeat = nonNegative(heat) + nonNegative(dispatchUnits) * gainPerUnit * load;
      nextHeat -= coolingRate(coolUnits(coolPower)) * nextHeat;
      return nonNegative(nextHeat);
   }

   private static double normalizedHeat(double heat, double coolPower) {
      return clamp01(nonNegative(heat) / heatCapacity(coolUnits(coolPower)));
   }

   private static int floorToInt(double value) {
      if (!Double.isFinite(value) || value <= 0.0) {
         return 0;
      } else {
         return value >= 2.147483647E9 ? Integer.MAX_VALUE : (int)Math.floor(value);
      }
   }

   private static double nonNegative(double value) {
      return Double.isFinite(value) && !(value <= 0.0) ? value : 0.0;
   }

   private static double clamp01(double value) {
      return clamp(value, 0.0, 1.0);
   }

   private static double clamp(double value, double min, double max) {
      return !Double.isFinite(value) ? min : Math.max(min, Math.min(max, value));
   }

   public static record Snapshot(double heat, double normalizedHeat, long operationsPerTick, double efficiencyFactor) {
   }
}
