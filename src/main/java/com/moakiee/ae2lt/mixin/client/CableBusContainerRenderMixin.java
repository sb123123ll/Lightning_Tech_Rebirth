package com.moakiee.ae2lt.mixin.client;

import appeng.api.parts.IPart;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.parts.CableBusContainer;
import com.moakiee.ae2lt.client.render.OverloadedCableRenderStateAccess;
import com.moakiee.ae2lt.part.OverloadedCablePart;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {CableBusContainer.class},
   remap = false
)
public abstract class CableBusContainerRenderMixin {
   @Shadow
   @Nullable
   public abstract IPart getPart(@Nullable Direction var1);

   @Inject(
      method = {"getRenderState"},
      at = {@At("RETURN")}
   )
   private void ae2lt$markOverloadedCable(CallbackInfoReturnable<CableBusRenderState> cir) {
      CableBusRenderState renderState = (CableBusRenderState)cir.getReturnValue();
      if (renderState != null) {
         if (this.getPart(null) instanceof OverloadedCablePart) {
            ((OverloadedCableRenderStateAccess)renderState).ae2lt$setOverloadedCable(true);
         }
      }
   }
}
