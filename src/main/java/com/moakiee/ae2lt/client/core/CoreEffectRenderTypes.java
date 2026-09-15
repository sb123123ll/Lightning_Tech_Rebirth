package com.moakiee.ae2lt.client.core;

import com.moakiee.ae2lt.client.core.veil.VeilCoreEffectShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;

final class CoreEffectRenderTypes extends RenderType {
   private static final CoreEffectRenderTypes.EffectShaders SHADERS = selectShaders();
   private static final RenderType TIANSHU = createEffectType("tianshu", SHADERS.tianshu());
   private static final RenderType MATRIX_CORE = createEffectType("matrix_core", SHADERS.matrix());
   private static final RenderType MATRIX_GLOW = createGlowEffectType("matrix_glow", SHADERS.matrix());
   private static final RenderType TIANSHU_SHADER_PACK_FALLBACK = createShaderPackFallbackType("tianshu", false, true, true);
   private static final RenderType MATRIX_CORE_SHADER_PACK_FALLBACK = createShaderPackFallbackType("matrix_core", false, true, true);
   private static final RenderType MATRIX_GLOW_SHADER_PACK_FALLBACK = createShaderPackFallbackType("matrix_glow", true, false, false);

   private CoreEffectRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
   }

   static RenderType tianshu(boolean shaderPackActive) {
      return shaderPackActive ? TIANSHU_SHADER_PACK_FALLBACK : TIANSHU;
   }

   static RenderType matrixCore(boolean shaderPackActive) {
      return shaderPackActive ? MATRIX_CORE_SHADER_PACK_FALLBACK : MATRIX_CORE;
   }

   static RenderType matrixGlow(boolean shaderPackActive) {
      return shaderPackActive ? MATRIX_GLOW_SHADER_PACK_FALLBACK : MATRIX_GLOW;
   }

   private static CoreEffectRenderTypes.EffectShaders selectShaders() {
      if (CoreEffectBackend.useVeil()) {
         try {
            return new CoreEffectRenderTypes.EffectShaders(VeilCoreEffectShaders.tianshu(), VeilCoreEffectShaders.matrix());
         } catch (LinkageError | RuntimeException var1) {
            CoreEffectBackend.disableVeil(var1);
         }
      }

      return new CoreEffectRenderTypes.EffectShaders(CoreEffectShaders.tianshu(), CoreEffectShaders.matrix());
   }

   private static RenderType createEffectType(String name, ShaderStateShard shader) {
      CompositeState state = CompositeState.m_110628_()
         .m_173292_(shader)
         .m_173290_(f_110147_)
         .m_110685_(f_110139_)
         .m_110663_(f_110113_)
         .m_110687_(f_110114_)
         .m_110661_(f_110110_)
         .m_110671_(f_110153_)
         .m_110691_(false);
      return m_173215_("ae2lt_core_effect_" + name, DefaultVertexFormat.f_166851_, Mode.TRIANGLES, 262144, false, true, state);
   }

   private static RenderType createGlowEffectType(String name, ShaderStateShard shader) {
      CompositeState state = CompositeState.m_110628_()
         .m_173292_(shader)
         .m_173290_(f_110147_)
         .m_110685_(f_110135_)
         .m_110663_(f_110113_)
         .m_110687_(f_110115_)
         .m_110661_(f_110110_)
         .m_110671_(f_110153_)
         .m_110691_(false);
      return m_173215_("ae2lt_core_effect_" + name, DefaultVertexFormat.f_166851_, Mode.TRIANGLES, 262144, false, false, state);
   }

   private static RenderType createShaderPackFallbackType(String name, boolean additive, boolean writesDepth, boolean sortOnUpload) {
      CompositeState state = CompositeState.m_110628_()
         .m_173292_(f_173104_)
         .m_173290_(f_110147_)
         .m_110685_(additive ? f_110135_ : f_110139_)
         .m_110663_(f_110113_)
         .m_110687_(writesDepth ? f_110114_ : f_110115_)
         .m_110661_(f_110110_)
         .m_110671_(f_110153_)
         .m_110691_(false);
      return m_173215_("ae2lt_core_effect_" + name + "_shader_pack_fallback", DefaultVertexFormat.f_166851_, Mode.TRIANGLES, 262144, false, sortOnUpload, state);
   }

   private static record EffectShaders(ShaderStateShard tianshu, ShaderStateShard matrix) {
   }
}
