package com.moakiee.ae2lt.logic.compute;

public enum ComputeTier {
   BASELINE(0, 512, 1048576L, 1024L, false),
   QUANTUM(15, 6144, 268435456L, 20480L, false),
   OVERLOAD(15, 16384, 68719476736L, 4194304L, false),
   MULTIDIMENSIONAL(0, 16384, Long.MAX_VALUE, Long.MAX_VALUE, true);

   private final int maxAmplifierUnits;
   private final int dispatchCap;
   private final long internalStorage;
   private final long copyCap;
   private final boolean multidimensional;

   private ComputeTier(int maxAmplifierUnits, int dispatchCap, long internalStorage, long copyCap, boolean multidimensional) {
      this.maxAmplifierUnits = maxAmplifierUnits;
      this.dispatchCap = dispatchCap;
      this.internalStorage = internalStorage;
      this.copyCap = copyCap;
      this.multidimensional = multidimensional;
   }

   public int maxAmplifierUnits() {
      return this.maxAmplifierUnits;
   }

   public int dispatchCap() {
      return this.dispatchCap;
   }

   public long internalStorage() {
      return this.internalStorage;
   }

   public long copyCap() {
      return this.copyCap;
   }

   public boolean multidimensional() {
      return this.multidimensional;
   }
}
