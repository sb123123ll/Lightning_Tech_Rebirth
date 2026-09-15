package com.moakiee.ae2lt.logic.craft;

public enum MatrixPatternStorageTier {
   CUSTOM(0),
   T1(36),
   T2(72);

   private final int capacity;

   private MatrixPatternStorageTier(int capacity) {
      this.capacity = capacity;
   }

   public int capacity() {
      return this.capacity;
   }
}
