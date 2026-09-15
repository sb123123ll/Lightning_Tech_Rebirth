package com.moakiee.ae2lt.mixin.client;

import appeng.client.render.cablebus.CableBusRenderState;
import com.moakiee.ae2lt.client.render.OverloadedCableRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {CableBusRenderState.class},
   remap = false
)
public class CableBusRenderStateMixin implements OverloadedCableRenderStateAccess {
   @Unique
   private boolean ae2lt$overloadedCable;

   @Override
   public boolean ae2lt$isOverloadedCable() {
      return this.ae2lt$overloadedCable;
   }

   @Override
   public void ae2lt$setOverloadedCable(boolean overloadedCable) {
      this.ae2lt$overloadedCable = overloadedCable;
   }

   @Inject(
      method = {"hashCode"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ae2lt$includeOverloadedFlagInHash(CallbackInfoReturnable<Integer> cir) {
      cir.setReturnValue(31 * (Integer)cir.getReturnValue() + Boolean.hashCode(this.ae2lt$overloadedCable));
   }

   @Inject(
      method = {"equals"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ae2lt$includeOverloadedFlagInEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
      if (Boolean.TRUE.equals(cir.getReturnValue())) {
         if (obj instanceof OverloadedCableRenderStateAccess access) {
            cir.setReturnValue(this.ae2lt$overloadedCable == access.ae2lt$isOverloadedCable());
         } else {
            cir.setReturnValue(false);
         }
      }
   }
}
