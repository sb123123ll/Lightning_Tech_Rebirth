package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.config.AE2LTClientConfig;
import net.minecraft.client.gui.screens.Screen;

public final class TianshuUploadTriggerClient {
   private TianshuUploadTriggerClient() {
   }

   public static boolean shouldTrigger() {
      return switch (AE2LTClientConfig.uploadTrigger()) {
         case NO_SHIFT -> !Screen.m_96638_();
         case SHIFT -> Screen.m_96638_();
         case CTRL -> Screen.m_96637_();
         case ALT -> Screen.m_96639_();
         case MANUAL_ONLY -> false;
      };
   }
}
