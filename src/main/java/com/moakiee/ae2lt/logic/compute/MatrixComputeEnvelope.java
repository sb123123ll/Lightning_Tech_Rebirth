package com.moakiee.ae2lt.logic.compute;

public record MatrixComputeEnvelope(long operationsPerTick, int maxProviderCallsPerTick, double thermalEfficiency, boolean unboundedOperations) {
   public static final int MAX_PROVIDER_CALLS_PER_TICK = 16384;

   public MatrixComputeEnvelope(long operationsPerTick, int maxProviderCallsPerTick, double thermalEfficiency, boolean unboundedOperations) {
      operationsPerTick = Math.max(0L, operationsPerTick);
      maxProviderCallsPerTick = Math.max(0, Math.min(maxProviderCallsPerTick, 16384));
      thermalEfficiency = Double.isFinite(thermalEfficiency) ? Math.max(0.0, thermalEfficiency) : 0.0;
      this.operationsPerTick = operationsPerTick;
      this.maxProviderCallsPerTick = maxProviderCallsPerTick;
      this.thermalEfficiency = thermalEfficiency;
      this.unboundedOperations = unboundedOperations;
   }
}
