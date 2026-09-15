package com.moakiee.ae2lt.mixin.client;

import com.moakiee.ae2lt.client.CelestweaveShieldFireVisuals;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityShieldFireVisualMixin {
   @Inject(
      method = {"displayFireAnimation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$hideShieldedPlayerFire(CallbackInfoReturnable<Boolean> cir) {
      if (CelestweaveShieldFireVisuals.shouldHideFire((Entity)this)) {
         cir.setReturnValue(false);
      }
   }
}
