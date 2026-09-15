package com.moakiee.ae2lt.mixin.client;

import com.moakiee.ae2lt.client.ShieldHitFeedbackClientState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerShieldHitFeedbackMixin {
   @Inject(
      method = {"hurtTo"},
      at = {@At("RETURN")}
   )
   private void ae2lt$clearShieldHealthFeedback(float health, CallbackInfo ci) {
      ShieldHitFeedbackClientState.clearAfterHealthSync((LocalPlayer)this);
   }
}
