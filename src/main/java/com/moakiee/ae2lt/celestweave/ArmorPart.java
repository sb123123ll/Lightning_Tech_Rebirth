package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;

public enum ArmorPart {
   HEAD(DeviceKind.CELESTWEAVE_OCULUS, DeviceSlotType.HEAD_MODULE, "head", 48),
   CHEST(DeviceKind.CELESTWEAVE_CORE, DeviceSlotType.CHEST_MODULE, "chest", 128),
   LEGS(DeviceKind.CELESTWEAVE_CONDUIT, DeviceSlotType.LEGS_MODULE, "legs", 96),
   FEET(DeviceKind.CELESTWEAVE_STRIDE, DeviceSlotType.FEET_MODULE, "feet", 64);

   private final DeviceKind deviceKind;
   private final DeviceSlotType moduleSlot;
   private final String equipmentSlotName;
   private final int dynamicCap;

   private ArmorPart(DeviceKind deviceKind, DeviceSlotType moduleSlot, String equipmentSlotName, int dynamicCap) {
      this.deviceKind = deviceKind;
      this.moduleSlot = moduleSlot;
      this.equipmentSlotName = equipmentSlotName;
      this.dynamicCap = dynamicCap;
   }

   public DeviceKind deviceKind() {
      return this.deviceKind;
   }

   public DeviceSlotType moduleSlot() {
      return this.moduleSlot;
   }

   public String equipmentSlotName() {
      return this.equipmentSlotName;
   }

   public int dynamicCap() {
      return this.dynamicCap;
   }
}
