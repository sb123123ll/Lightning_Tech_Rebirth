package com.moakiee.ae2lt.logic.craft;

import appeng.api.crafting.IPatternDetails;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public final class MatrixPatternRepository implements MatrixPatternCore {
   private final List<MatrixPatternStorageUnit> units;

   public MatrixPatternRepository(List<MatrixPatternStorageUnit> units) {
      this.units = new ArrayList<>(units == null ? List.of() : units);
   }

   public List<MatrixPatternStorageUnit> units() {
      return List.copyOf(this.units);
   }

   public int capacity() {
      int total = 0;

      for (MatrixPatternStorageUnit unit : this.units) {
         total += unit.capacity();
      }

      return total;
   }

   public int usedSlots() {
      int used = 0;

      for (MatrixPatternStorageUnit unit : this.units) {
         used += unit.usedSlots();
      }

      return used;
   }

   public int freeSlots() {
      return this.capacity() - this.usedSlots();
   }

   public boolean insert(IPatternDetails pattern) {
      if (!isSupportedPattern(pattern)) {
         return false;
      } else {
         for (MatrixPatternStorageUnit unit : this.units) {
            if (unit.insert(pattern)) {
               return true;
            }
         }

         return false;
      }
   }

   public List<IPatternDetails> insertAll(List<? extends IPatternDetails> patterns) {
      if (patterns != null && !patterns.isEmpty()) {
         ArrayList<IPatternDetails> rejected = new ArrayList<>();

         for (IPatternDetails pattern : patterns) {
            if (!this.insert(pattern)) {
               rejected.add(pattern);
            }
         }

         return List.copyOf(rejected);
      } else {
         return List.of();
      }
   }

   public MatrixPatternStorageUnit exposedUnit() {
      for (MatrixPatternStorageUnit unit : this.units) {
         if (!unit.isEmpty()) {
            return unit;
         }
      }

      return null;
   }

   public MatrixPatternRepository.UpgradeResult upgradeT1Units(int maxUpgrades) {
      int totalT1 = 0;

      for (MatrixPatternStorageUnit unit : this.units) {
         if (unit.tier() == MatrixPatternStorageTier.T1) {
            totalT1++;
         }
      }

      int remainingUpgrades = Math.max(0, maxUpgrades);
      int upgraded = 0;

      for (int i = 0; i < this.units.size() && remainingUpgrades > 0; i++) {
         MatrixPatternStorageUnit unitx = this.units.get(i);
         if (unitx.canUpgradeToT2()) {
            this.units.set(i, unitx.upgradeToT2());
            remainingUpgrades--;
            upgraded++;
         }
      }

      return new MatrixPatternRepository.UpgradeResult(upgraded, totalT1, totalT1 - upgraded);
   }

   @Override
   public List<IPatternDetails> getAvailablePatterns() {
      IdentityHashMap<IPatternDetails, Boolean> seen = new IdentityHashMap<>();
      ArrayList<IPatternDetails> result = new ArrayList<>();

      for (MatrixPatternStorageUnit unit : this.units) {
         for (IPatternDetails pattern : unit.getAvailablePatterns()) {
            if (pattern != null && !seen.containsKey(pattern)) {
               seen.put(pattern, Boolean.TRUE);
               result.add(pattern);
            }
         }
      }

      return List.copyOf(result);
   }

   @Override
   public boolean hasPattern(IPatternDetails details) {
      if (!isSupportedPattern(details)) {
         return false;
      } else {
         for (MatrixPatternStorageUnit unit : this.units) {
            if (unit.hasPattern(details)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isSupportedPattern(IPatternDetails pattern) {
      return pattern != null && pattern instanceof IMolecularAssemblerSupportedPattern;
   }

   public static record UpgradeResult(int upgraded, int totalT1BeforeUpgrade, int remainingT1) {
   }
}
