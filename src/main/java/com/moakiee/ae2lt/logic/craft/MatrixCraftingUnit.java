package com.moakiee.ae2lt.logic.craft;

import java.util.Objects;

public record MatrixCraftingUnit(MatrixCraftingUnit.Kind kind, MatrixCoreMode coreMode, int power, int distance) {
   public MatrixCraftingUnit(MatrixCraftingUnit.Kind kind, MatrixCoreMode coreMode, int power, int distance) {
      kind = Objects.requireNonNull(kind);
      coreMode = coreMode == null ? MatrixCoreMode.NONE : coreMode;
      power = Math.max(0, power);
      distance = Math.max(0, distance);
      this.kind = kind;
      this.coreMode = coreMode;
      this.power = power;
      this.distance = distance;
   }

   public static MatrixCraftingUnit quantumCore() {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.CORE, MatrixCoreMode.QUANTUM, 0, 0);
   }

   public static MatrixCraftingUnit stableCore() {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.CORE, MatrixCoreMode.STABLE, 0, 0);
   }

   public static MatrixCraftingUnit overloadCore() {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.CORE, MatrixCoreMode.OVERLOAD, 0, 0);
   }

   public static MatrixCraftingUnit multidimensionalCore() {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.CORE, MatrixCoreMode.MULTIDIMENSIONAL, 0, 0);
   }

   public static MatrixCraftingUnit t1Threader() {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.THREAD, MatrixCoreMode.NONE, 1, 0);
   }

   public static MatrixCraftingUnit t2Threader() {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.THREAD, MatrixCoreMode.NONE, 2, 0);
   }

   public static MatrixCraftingUnit threadPower(int power) {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.THREAD, MatrixCoreMode.NONE, power, 0);
   }

   public static MatrixCraftingUnit amplifier() {
      return amplifierPower(1);
   }

   public static MatrixCraftingUnit amplifierPower(int power) {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.AMPLIFIER, MatrixCoreMode.NONE, power, 0);
   }

   public static MatrixCraftingUnit t1Cooler(int distance) {
      return coolerPower(1, distance);
   }

   public static MatrixCraftingUnit t2Cooler(int distance) {
      return coolerPower(2, distance);
   }

   public static MatrixCraftingUnit coolerPower(int power, int distance) {
      return new MatrixCraftingUnit(MatrixCraftingUnit.Kind.COOLER, MatrixCoreMode.NONE, power, distance);
   }

   public double adjustedCoolPower() {
      return this.kind == MatrixCraftingUnit.Kind.COOLER ? (double)this.power * coolingDecay(this.distance) : 0.0;
   }

   public long stableOperationContribution() {
      if (this.kind == MatrixCraftingUnit.Kind.THREAD && this.power > 0) {
         return switch (this.power) {
            case 1 -> 1024L;
            case 2 -> 3584L;
            default -> Math.multiplyExact(1024L, this.power);
         };
      } else {
         return 0L;
      }
   }

   public static double coolingDecay(int distance) {
      return switch (distance) {
         case 1 -> 1.0;
         case 2 -> 0.75;
         case 3 -> 0.5;
         case 4 -> 0.25;
         default -> 0.0;
      };
   }

   public static enum Kind {
      CORE,
      THREAD,
      AMPLIFIER,
      COOLER;
   }
}
