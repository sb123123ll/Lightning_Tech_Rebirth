package com.moakiee.ae2lt.mixin.client;

import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({LevelRenderer.class})
public abstract class LevelRendererPhaseFlightMixin {
   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"
      ),
      index = 3
   )
   private boolean ae2lt$useSpectatorCullingWhilePhaseFlying(boolean isSpectator) {
      return isSpectator || PhaseFlightMovementGuard.isPhaseFlightActive(Minecraft.m_91087_().f_91074_);
   }
}
