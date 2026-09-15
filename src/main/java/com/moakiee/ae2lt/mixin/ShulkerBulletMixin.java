package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.logic.FloatingMatterCapture;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ShulkerBullet.class})
public abstract class ShulkerBulletMixin {
   @Inject(
      method = {"onHitBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$captureOnPlaneFront(BlockHitResult hit, CallbackInfo ci) {
      ShulkerBullet self = (ShulkerBullet)this;
      if (FloatingMatterCapture.tryCapture(self, hit)) {
         ci.cancel();
      }
   }
}
