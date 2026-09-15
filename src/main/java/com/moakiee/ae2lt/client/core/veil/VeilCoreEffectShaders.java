package com.moakiee.ae2lt.client.core.veil;

import foundry.veil.api.client.render.VeilRenderBridge;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.resources.ResourceLocation;

public final class VeilCoreEffectShaders {
   private static final ResourceLocation TIANSHU_SHADER = new ResourceLocation("ae2lt", "multiblock/tianshu_core");
   private static final ResourceLocation MATRIX_SHADER = new ResourceLocation("ae2lt", "multiblock/matrix_core");

   private VeilCoreEffectShaders() {
   }

   public static boolean isApiCompatible() {
      try {
         Method shaderState = VeilRenderBridge.class.getMethod("shaderState", ResourceLocation.class);
         return Modifier.isStatic(shaderState.getModifiers()) && ShaderStateShard.class.isAssignableFrom(shaderState.getReturnType());
      } catch (SecurityException | ReflectiveOperationException var1) {
         return false;
      }
   }

   public static ShaderStateShard tianshu() {
      return VeilRenderBridge.shaderState(TIANSHU_SHADER);
   }

   public static ShaderStateShard matrix() {
      return VeilRenderBridge.shaderState(MATRIX_SHADER);
   }
}
