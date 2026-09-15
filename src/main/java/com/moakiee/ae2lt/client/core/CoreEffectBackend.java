package com.moakiee.ae2lt.client.core;

import com.moakiee.ae2lt.client.core.veil.VeilCoreEffectShaders;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;

final class CoreEffectBackend {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DefaultArtifactVersion MINIMUM_VEIL_VERSION = new DefaultArtifactVersion("1.0.0");
   private static final DefaultArtifactVersion MAXIMUM_VEIL_VERSION = new DefaultArtifactVersion("2.0.0");
   private static volatile boolean veilUsable = detectCompatibleVeil();

   private CoreEffectBackend() {
   }

   static boolean useVeil() {
      return veilUsable;
   }

   static void disableVeil(Throwable cause) {
      if (veilUsable) {
         veilUsable = false;
         LOGGER.warn("Veil core-effect backend failed; falling back to native shaders", cause);
      }
   }

   static boolean useShaderPackFallback() {
      return CoreEffectBackend.ShaderPackDetectorHolder.INSTANCE.isShaderPackInUse();
   }

   private static boolean detectCompatibleVeil() {
      if (!ModList.get().isLoaded("veil")) {
         return false;
      } else {
         ArtifactVersion installedVersion = ModList.get().getModContainerById("veil").map(container -> container.getModInfo().getVersion()).orElse(null);
         if (installedVersion != null && installedVersion.compareTo(MINIMUM_VEIL_VERSION) >= 0 && installedVersion.compareTo(MAXIMUM_VEIL_VERSION) < 0) {
            try {
               boolean compatible = VeilCoreEffectShaders.isApiCompatible();
               if (!compatible) {
                  LOGGER.warn("Veil is installed but its shader bridge API is incompatible; using native core-effect shaders");
               }

               return compatible;
            } catch (RuntimeException | LinkageError var2) {
               LOGGER.warn("Veil is installed but its shader bridge API is unavailable; using native core-effect shaders", var2);
               return false;
            }
         } else {
            LOGGER.warn("Unsupported Veil version {} installed; AE2LT supports [1.0.0,2.0.0) and will use native core-effect shaders", installedVersion);
            return false;
         }
      }
   }

   private static CoreEffectBackend.ShaderPackDetector createShaderPackDetector() {
      ModList modList = ModList.get();
      if (!modList.isLoaded("iris") && !modList.isLoaded("oculus")) {
         return () -> false;
      } else {
         for (String apiClassName : new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"}) {
            try {
               Class<?> apiClass = Class.forName(apiClassName, false, CoreEffectBackend.class.getClassLoader());
               Object api = apiClass.getMethod("getInstance").invoke(null);
               Method isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
               return new CoreEffectBackend.ReflectiveShaderPackDetector(api, isShaderPackInUse);
            } catch (LinkageError | SecurityException | ReflectiveOperationException var8) {
            }
         }

         LOGGER.warn("Iris/Oculus is installed but its shader-pack API is unavailable; using the shader-pack-safe core-effect renderer");
         return () -> true;
      }
   }

   private static final class ReflectiveShaderPackDetector implements CoreEffectBackend.ShaderPackDetector {
      private final Object api;
      private final Method isShaderPackInUse;
      private boolean failed;

      private ReflectiveShaderPackDetector(Object api, Method isShaderPackInUse) {
         this.api = api;
         this.isShaderPackInUse = isShaderPackInUse;
      }

      @Override
      public boolean isShaderPackInUse() {
         if (this.failed) {
            return true;
         } else {
            try {
               return Boolean.TRUE.equals(this.isShaderPackInUse.invoke(this.api));
            } catch (LinkageError | RuntimeException | ReflectiveOperationException var2) {
               this.failed = true;
               CoreEffectBackend.LOGGER.warn("Unable to query the active Iris/Oculus shader pack; using the shader-pack-safe core-effect renderer", var2);
               return true;
            }
         }
      }
   }

   private interface ShaderPackDetector {
      boolean isShaderPackInUse();
   }

   private static final class ShaderPackDetectorHolder {
      private static final CoreEffectBackend.ShaderPackDetector INSTANCE = CoreEffectBackend.createShaderPackDetector();
   }
}
