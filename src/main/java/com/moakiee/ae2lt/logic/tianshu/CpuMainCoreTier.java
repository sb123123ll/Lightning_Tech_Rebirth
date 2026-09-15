package com.moakiee.ae2lt.logic.tianshu;

import com.moakiee.ae2lt.logic.compute.ComputeTier;

public enum CpuMainCoreTier {
   BASELINE(ComputeTier.BASELINE, 256.0),
   QUANTUM(ComputeTier.QUANTUM, 8192.0),
   OVERLOAD(ComputeTier.OVERLOAD, 262144.0),
   MULTIDIMENSIONAL(ComputeTier.MULTIDIMENSIONAL, 8.0);

   private final ComputeTier computeTier;
   private final double idlePowerUsage;

   private CpuMainCoreTier(ComputeTier computeTier, double idlePowerUsage) {
      this.computeTier = computeTier;
      this.idlePowerUsage = idlePowerUsage;
   }

   public ComputeTier computeTier() {
      return this.computeTier;
   }

   public double idlePowerUsage() {
      return this.idlePowerUsage;
   }
}
