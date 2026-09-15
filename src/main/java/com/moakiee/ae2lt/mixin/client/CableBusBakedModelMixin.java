package com.moakiee.ae2lt.mixin.client;

import appeng.client.render.cablebus.CableBusBakedModel;
import appeng.client.render.cablebus.CableBusRenderState;
import com.moakiee.ae2lt.client.render.OverloadedCableRenderHelper;
import com.moakiee.ae2lt.client.render.OverloadedCableRenderStateAccess;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {CableBusBakedModel.class},
   remap = false
)
public class CableBusBakedModelMixin {
   @Inject(
      method = {"addCableQuads"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$renderOverloadedCable(CableBusRenderState renderState, List<BakedQuad> quadsOut, CallbackInfo ci) {
      if (renderState != null) {
         if (((OverloadedCableRenderStateAccess)renderState).ae2lt$isOverloadedCable()) {
            OverloadedCableRenderHelper.addCableQuads(renderState, quadsOut);
            ci.cancel();
         }
      }
   }
}
