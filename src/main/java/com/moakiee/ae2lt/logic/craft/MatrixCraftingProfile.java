package com.moakiee.ae2lt.logic.craft;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record MatrixCraftingProfile(
   MatrixCoreMode mode,
   int coreCount,
   double threadPower,
   double amplifierPower,
   double coolPower,
   int dispatchUnitCount,
   int amplifierUnitCount,
   int coolingUnitCount,
   boolean amplifierLimitExceeded,
   long stableBaseOperations
) {
   public static final int AMPLIFIER_LIMIT = 15;

   public MatrixCraftingProfile(
      MatrixCoreMode mode,
      int coreCount,
      double threadPower,
      double amplifierPower,
      double coolPower,
      int dispatchUnitCount,
      int amplifierUnitCount,
      int coolingUnitCount,
      boolean amplifierLimitExceeded,
      long stableBaseOperations
   ) {
      mode = mode == null ? MatrixCoreMode.NONE : mode;
      coreCount = Math.max(0, coreCount);
      threadPower = Math.max(0.0, threadPower);
      amplifierPower = Math.max(0.0, amplifierPower);
      coolPower = Math.max(0.0, coolPower);
      dispatchUnitCount = Math.max(0, dispatchUnitCount);
      amplifierUnitCount = Math.max(0, amplifierUnitCount);
      coolingUnitCount = Math.max(0, coolingUnitCount);
      stableBaseOperations = Math.max(0L, stableBaseOperations);
      this.mode = mode;
      this.coreCount = coreCount;
      this.threadPower = threadPower;
      this.amplifierPower = amplifierPower;
      this.coolPower = coolPower;
      this.dispatchUnitCount = dispatchUnitCount;
      this.amplifierUnitCount = amplifierUnitCount;
      this.coolingUnitCount = coolingUnitCount;
      this.amplifierLimitExceeded = amplifierLimitExceeded;
      this.stableBaseOperations = stableBaseOperations;
   }

   public static MatrixCraftingProfile empty() {
      return new MatrixCraftingProfile(MatrixCoreMode.NONE, 0, 0.0, 0.0, 0.0, 0, 0, 0, false, 0L);
   }

   public static MatrixCraftingProfile fromUnits(List<MatrixCraftingUnit> units) {
      if (units != null && !units.isEmpty()) {
         MatrixCoreMode mode = MatrixCoreMode.NONE;
         int coreCount = 0;
         double threadPower = 0.0;
         double amplifierPower = 0.0;
         double coolPower = 0.0;
         int dispatchUnitCount = 0;
         int amplifierUnitCount = 0;
         int coolingUnitCount = 0;
         long stableBaseOperations = 0L;

         for (MatrixCraftingUnit unit : units) {
            if (unit != null) {
               switch (unit.kind()) {
                  case CORE:
                     if (++coreCount == 1) {
                        mode = unit.coreMode();
                     } else {
                        mode = MatrixCoreMode.CONFLICT;
                     }
                     break;
                  case THREAD:
                     dispatchUnitCount++;
                     threadPower += (double)unit.power();
                     stableBaseOperations = saturatedAdd(stableBaseOperations, unit.stableOperationContribution());
                     break;
                  case AMPLIFIER:
                     int previousCount = amplifierUnitCount;
                     amplifierUnitCount = saturatedAdd(amplifierUnitCount, unit.power());
                     int acceptedPower = Math.min(unit.power(), Math.max(0, 15 - previousCount));
                     amplifierPower += (double)acceptedPower;
                     break;
                  case COOLER:
                     coolingUnitCount++;
                     coolPower += unit.adjustedCoolPower();
               }
            }
         }

         boolean amplifierLimitExceeded = amplifierUnitCount > 15;
         if (coreCount == 0) {
            mode = MatrixCoreMode.NONE;
         } else if (coreCount > 1) {
            mode = MatrixCoreMode.CONFLICT;
         }

         return coreCount == 1 && mode == MatrixCoreMode.MULTIDIMENSIONAL
            ? new MatrixCraftingProfile(MatrixCoreMode.MULTIDIMENSIONAL, 1, 0.0, 0.0, 0.0, 0, 0, 0, false, 0L)
            : new MatrixCraftingProfile(
               mode,
               coreCount,
               threadPower,
               amplifierPower,
               coolPower,
               dispatchUnitCount,
               amplifierUnitCount,
               coolingUnitCount,
               amplifierLimitExceeded,
               stableBaseOperations
            );
      } else {
         return empty();
      }
   }

   public boolean isValid() {
      return this.issues().isEmpty();
   }

   public boolean hasIssue(MatrixProfileIssue issue) {
      return this.issues().contains(issue);
   }

   public Set<MatrixProfileIssue> issues() {
      EnumSet<MatrixProfileIssue> issues = EnumSet.noneOf(MatrixProfileIssue.class);
      if (this.coreCount == 0) {
         issues.add(MatrixProfileIssue.MISSING_CORE);
      } else if (this.coreCount > 1
         || this.mode == MatrixCoreMode.CONFLICT
         || this.mode != MatrixCoreMode.STABLE
            && this.mode != MatrixCoreMode.QUANTUM
            && this.mode != MatrixCoreMode.OVERLOAD
            && this.mode != MatrixCoreMode.MULTIDIMENSIONAL) {
         issues.add(MatrixProfileIssue.CONFLICTING_CORES);
      }

      if (this.amplifierLimitExceeded) {
         issues.add(MatrixProfileIssue.AMPLIFIER_LIMIT_EXCEEDED);
      }

      if (this.coreCount == 1 && (this.mode == MatrixCoreMode.STABLE || this.mode == MatrixCoreMode.MULTIDIMENSIONAL) && this.amplifierUnitCount > 0) {
         issues.add(MatrixProfileIssue.AMPLIFIER_NOT_SUPPORTED);
      }

      if (this.coreCount == 1 && this.mode != MatrixCoreMode.MULTIDIMENSIONAL && this.threadPower <= 0.0) {
         issues.add(MatrixProfileIssue.MISSING_DISPATCH_UNIT);
      }

      return issues.isEmpty() ? Set.of() : Set.copyOf(issues);
   }

   public MatrixCraftingMath.Snapshot snapshot(double heat) {
      if (!this.isValid()) {
         return MatrixCraftingMath.idleSnapshot(heat, this.coolPower);
      } else {
         return switch (this.mode) {
            case STABLE -> MatrixCraftingMath.stableSnapshot(heat, this.stableBaseOperations, this.coolPower);
            case OVERLOAD -> MatrixCraftingMath.overloadSnapshot(heat, this.threadPower, this.amplifierPower, this.coolPower);
            case MULTIDIMENSIONAL -> MatrixCraftingMath.multidimensionalSnapshot();
            default -> MatrixCraftingMath.quantumSnapshot(heat, this.threadPower, this.amplifierPower, this.coolPower);
         };
      }
   }

   private static int saturatedAdd(int left, int right) {
      long result = (long)left + (long)right;
      return result >= 2147483647L ? Integer.MAX_VALUE : (int)result;
   }

   private static long saturatedAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }
}
