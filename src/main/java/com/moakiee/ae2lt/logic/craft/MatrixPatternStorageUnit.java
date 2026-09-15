package com.moakiee.ae2lt.logic.craft;

import appeng.api.crafting.IPatternDetails;
import java.util.ArrayList;
import java.util.List;

public final class MatrixPatternStorageUnit implements MatrixPatternCore {
   public static final int T1_CAPACITY = 36;
   public static final int T2_CAPACITY = 72;
   private final MatrixPatternStorageTier tier;
   private final IPatternDetails[] slots;

   public MatrixPatternStorageUnit(int capacity) {
      this(MatrixPatternStorageTier.CUSTOM, capacity);
   }

   private MatrixPatternStorageUnit(MatrixPatternStorageTier tier, int capacity) {
      if (capacity <= 0) {
         throw new IllegalArgumentException("capacity must be positive");
      } else {
         this.tier = tier;
         this.slots = new IPatternDetails[capacity];
      }
   }

   public static MatrixPatternStorageUnit t1() {
      return new MatrixPatternStorageUnit(MatrixPatternStorageTier.T1, 36);
   }

   public static MatrixPatternStorageUnit t2() {
      return new MatrixPatternStorageUnit(MatrixPatternStorageTier.T2, 72);
   }

   public MatrixPatternStorageTier tier() {
      return this.tier;
   }

   public int capacity() {
      return this.slots.length;
   }

   public int usedSlots() {
      int used = 0;

      for (IPatternDetails pattern : this.slots) {
         if (pattern != null) {
            used++;
         }
      }

      return used;
   }

   public int freeSlots() {
      return this.capacity() - this.usedSlots();
   }

   public boolean isEmpty() {
      return this.usedSlots() == 0;
   }

   public boolean isFull() {
      return this.freeSlots() == 0;
   }

   public boolean insert(IPatternDetails pattern) {
      if (pattern == null) {
         return false;
      } else {
         for (int i = 0; i < this.slots.length; i++) {
            if (this.slots[i] == null) {
               this.slots[i] = pattern;
               return true;
            }
         }

         return false;
      }
   }

   public IPatternDetails get(int slot) {
      return slot >= 0 && slot < this.slots.length ? this.slots[slot] : null;
   }

   public IPatternDetails extract(int slot) {
      if (slot >= 0 && slot < this.slots.length) {
         IPatternDetails pattern = this.slots[slot];
         this.slots[slot] = null;
         return pattern;
      } else {
         return null;
      }
   }

   @Override
   public boolean hasPattern(IPatternDetails details) {
      if (details == null) {
         return false;
      } else {
         for (IPatternDetails pattern : this.slots) {
            if (MatrixPatternCore.samePattern(pattern, details)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean canUpgradeToT2() {
      return this.tier == MatrixPatternStorageTier.T1;
   }

   public MatrixPatternStorageUnit upgradeToT2() {
      if (!this.canUpgradeToT2()) {
         return this;
      } else {
         MatrixPatternStorageUnit upgraded = t2();

         for (int i = 0; i < this.slots.length; i++) {
            upgraded.slots[i] = this.slots[i];
         }

         return upgraded;
      }
   }

   @Override
   public List<IPatternDetails> getAvailablePatterns() {
      ArrayList<IPatternDetails> result = new ArrayList<>();

      for (IPatternDetails pattern : this.slots) {
         if (pattern != null) {
            result.add(pattern);
         }
      }

      return List.copyOf(result);
   }
}
