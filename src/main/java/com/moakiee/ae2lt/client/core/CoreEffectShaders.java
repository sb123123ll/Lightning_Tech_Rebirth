package com.moakiee.ae2lt.client.core;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class CoreEffectShaders {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation TIANSHU_SHADER = new ResourceLocation("ae2lt", "multiblock/tianshu_core");
   private static final ResourceLocation MATRIX_SHADER = new ResourceLocation("ae2lt", "multiblock/matrix_core");
   private static final CoreEffectShaders.ShaderTracker TIANSHU = new CoreEffectShaders.ShaderTracker();
   private static final CoreEffectShaders.ShaderTracker MATRIX = new CoreEffectShaders.ShaderTracker();

   private CoreEffectShaders() {
   }

   @SubscribeEvent
   public static void registerShaders(RegisterShadersEvent event) throws IOException {
      if (CoreEffectBackend.useVeil()) {
         LOGGER.info("Compatible Veil detected; preparing native core-effect shaders as fallback");
      } else {
         LOGGER.info("Veil not detected or incompatible; using the native core-effect shader backend");
      }

      registerShader(event, TIANSHU_SHADER, TIANSHU);
      registerShader(event, MATRIX_SHADER, MATRIX);
   }

   static ShaderStateShard tianshu() {
      return TIANSHU.shard;
   }

   static ShaderStateShard matrix() {
      return MATRIX.shard;
   }

   private static void registerShader(RegisterShadersEvent event, ResourceLocation location, CoreEffectShaders.ShaderTracker tracker) throws IOException {
      event.registerShader(
         new CoreEffectShaders.TimedShaderInstance(event.getResourceProvider(), location, DefaultVertexFormat.f_166851_), tracker::setInstance
      );
   }

   private static final class ShaderTracker implements Supplier<ShaderInstance> {
      private final ShaderStateShard shard = new ShaderStateShard(this);
      private ShaderInstance instance;

      private void setInstance(ShaderInstance instance) {
         this.instance = instance;
      }

      public ShaderInstance get() {
         return this.instance;
      }
   }

   private static final class TimedShaderInstance extends ShaderInstance {
      private static final long TIME_WRAP_MILLIS = 3600000L;
      private final Uniform effectTime = this.m_173348_("EffectTime");

      private TimedShaderInstance(ResourceProvider resources, ResourceLocation location, VertexFormat vertexFormat) throws IOException {
         super(resources, location, vertexFormat);
      }

      public void m_173363_() {
         if (this.effectTime != null) {
            this.effectTime.m_5985_((float)(System.currentTimeMillis() % 3600000L) / 1000.0F);
         }

         super.m_173363_();
      }
   }
}
