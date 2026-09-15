package com.moakiee.ae2lt.device.capability;

import com.moakiee.ae2lt.device.module.DeviceModuleHost;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import java.util.ArrayList;
import java.util.List;

public final class CapabilityResolver {
   private CapabilityResolver() {
   }

   public static List<DeviceCapability> collect(DeviceModuleHost host) {
      List<DeviceCapability> out = new ArrayList<>();
      host.installedModuleStacks().forEach(stack -> {
         if (stack.m_41720_() instanceof OverloadDeviceModuleItem module) {
            out.addAll(module.capabilities(stack));
         }
      });
      return out;
   }
}
