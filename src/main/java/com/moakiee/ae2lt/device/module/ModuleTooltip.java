package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.device.DeviceKind;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ModuleTooltip {
   private ModuleTooltip() {
   }

   public static void appendInstallInfo(OverloadDeviceModuleItem module, List<Component> tooltip) {
      if (module != null) {
         tooltip.add(Component.m_237115_("ae2lt.module.tooltip.installable_on").m_130940_(ChatFormatting.GRAY));
         module.acceptableDevices()
            .stream()
            .sorted(Comparator.comparing(Enum::name))
            .forEach(kind -> tooltip.add(deviceLine(kind).m_130940_(ChatFormatting.GRAY)));
      }
   }

   private static MutableComponent deviceLine(DeviceKind kind) {
      return Component.m_237113_(" - ").m_7220_(Component.m_237115_(deviceKey(kind)));
   }

   private static String deviceKey(DeviceKind kind) {
      return "ae2lt.module.device." + kind.name().toLowerCase(Locale.ROOT);
   }
}
