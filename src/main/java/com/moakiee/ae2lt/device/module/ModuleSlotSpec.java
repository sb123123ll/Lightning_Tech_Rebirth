package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;

public record ModuleSlotSpec(DeviceSlotType slot, int index, int capacity, DeviceKind accepts) {
   public static ModuleSlotSpec of(DeviceSlotType slot, DeviceKind accepts) {
      return new ModuleSlotSpec(slot, 0, 1, accepts);
   }

   public static ModuleSlotSpec of(DeviceSlotType slot, int index, DeviceKind accepts) {
      return new ModuleSlotSpec(slot, index, 1, accepts);
   }
}
