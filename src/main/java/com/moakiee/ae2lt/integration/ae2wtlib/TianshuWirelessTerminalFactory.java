package com.moakiee.ae2lt.integration.ae2wtlib;

import java.lang.reflect.InvocationTargetException;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLLoader;

public final class TianshuWirelessTerminalFactory {
   private static final String INTEGRATION_CLASS = "com.moakiee.ae2lt.integration.ae2wtlib.Ae2wtlibIntegration";

   private TianshuWirelessTerminalFactory() {
   }

   public static boolean isAvailable() {
      return FMLLoader.getLoadingModList().getModFileById("ae2wtlib") != null;
   }

   public static Item create() {
      if (!isAvailable()) {
         throw new IllegalStateException("The wireless Tianshu terminal must not be registered without AE2WTLib");
      } else {
         try {
            Class<?> integration = Class.forName(
               "com.moakiee.ae2lt.integration.ae2wtlib.Ae2wtlibIntegration", true, TianshuWirelessTerminalFactory.class.getClassLoader()
            );
            return Item.class.cast(integration.getMethod("terminal").invoke(null));
         } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException var3) {
            throw new IllegalStateException("AE2WTLib is loaded, but its Tianshu terminal integration is unavailable", var3);
         } catch (InvocationTargetException var4) {
            Throwable cause = var4.getCause();
            if (cause instanceof RuntimeException runtimeException) {
               throw runtimeException;
            } else if (cause instanceof Error error) {
               throw error;
            } else {
               throw new IllegalStateException("Failed to create the AE2WTLib Tianshu terminal", cause);
            }
         }
      }
   }
}
