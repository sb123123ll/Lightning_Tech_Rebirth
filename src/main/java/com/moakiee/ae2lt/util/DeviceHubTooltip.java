package com.moakiee.ae2lt.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class DeviceHubTooltip {
   private static final String OPEN_DEVICE_HUB_TOOLTIP = "ae2lt.tooltip.open_device_hub";

   private DeviceHubTooltip() {
   }

   public static Component openConfigHint() {
      return Component.m_237110_("ae2lt.tooltip.open_device_hub", new Object[]{Component.m_237117_("key.ae2lt.open_config").m_130940_(ChatFormatting.YELLOW)})
         .m_130940_(ChatFormatting.GRAY);
   }
}
