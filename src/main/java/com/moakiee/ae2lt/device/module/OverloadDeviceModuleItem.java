package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface OverloadDeviceModuleItem {
   Set<DeviceKind> acceptableDevices();

   DeviceSlotType acceptableSlot();

   default boolean accepts(DeviceKind deviceKind, DeviceSlotType slotType) {
      return this.acceptableDevices().contains(deviceKind) && this.acceptableSlot() == slotType;
   }

   default String moduleTypeId(ItemStack stack) {
      return "";
   }

   default int getMaxInstallAmount() {
      return 0;
   }

   List<DeviceCapability> capabilities(ItemStack var1);

   @Nullable
   default OverloadDeviceSubmodule asSubmodule() {
      return null;
   }
}
