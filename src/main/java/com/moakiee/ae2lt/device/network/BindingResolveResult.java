package com.moakiee.ae2lt.device.network;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import org.jetbrains.annotations.Nullable;

public record BindingResolveResult(@Nullable IGrid grid, @Nullable IWirelessAccessPoint accessPoint, @Nullable BindingResolveResult.FailureReason failure) {
   public static BindingResolveResult ok(IGrid grid, IWirelessAccessPoint accessPoint) {
      return new BindingResolveResult(grid, accessPoint, null);
   }

   public static BindingResolveResult fail(BindingResolveResult.FailureReason failure) {
      return new BindingResolveResult(null, null, failure);
   }

   public boolean success() {
      return this.failure == null && this.grid != null;
   }

   public static enum FailureReason {
      NOT_BOUND,
      DIM_NOT_LOADED,
      NO_AP,
      INACTIVE_AP,
      OUT_OF_RANGE,
      WRONG_DIMENSION;
   }
}
