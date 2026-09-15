package com.moakiee.ae2lt.item.railgun;

import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import java.util.Set;

final class RailgunModuleRules {
   private static final Set<DeviceKind> RAILGUN_ONLY = Set.of(DeviceKind.RAILGUN);
   private static final Set<DeviceKind> CORE_ACCEPTS = Set.of(DeviceKind.RAILGUN, DeviceKind.CELESTWEAVE_CORE);

   private RailgunModuleRules() {
   }

   static int maxInstallAmount(RailgunModuleType type) {
      return switch (type) {
         case CORE, OVERLOAD_EXECUTION, MULTIDIMENSIONAL_EXECUTION -> 1;
         case COMPUTE, ACCELERATION, RANGE -> 2;
      };
   }

   static Set<DeviceKind> acceptableDevices(RailgunModuleType type) {
      return type == RailgunModuleType.CORE ? CORE_ACCEPTS : RAILGUN_ONLY;
   }

   static DeviceSlotType acceptableSlot(RailgunModuleType type) {
      return switch (type) {
         case CORE -> DeviceSlotType.CORE;
         case OVERLOAD_EXECUTION, MULTIDIMENSIONAL_EXECUTION -> DeviceSlotType.OVERLOAD_EXECUTION;
         case COMPUTE -> DeviceSlotType.COMPUTE;
         case ACCELERATION -> DeviceSlotType.ACCELERATION;
         case RANGE -> DeviceSlotType.RANGE;
      };
   }

   static boolean accepts(RailgunModuleType type, DeviceKind deviceKind, DeviceSlotType slotType) {
      return type == RailgunModuleType.CORE && deviceKind == DeviceKind.CELESTWEAVE_CORE
         ? slotType == DeviceSlotType.CHEST_MODULE
         : deviceKind == DeviceKind.RAILGUN && slotType == acceptableSlot(type);
   }

   static List<DeviceCapability> capabilitiesFor(RailgunModuleType type) {
      return switch (type) {
         case CORE -> List.of(new DeviceCapability.LightningCompensation(16));
         case OVERLOAD_EXECUTION -> List.of(new DeviceCapability.OverloadExecutionTuning(0.02, 200, 8));
         case MULTIDIMENSIONAL_EXECUTION -> List.of();
         case COMPUTE -> List.of(new DeviceCapability.ChainTuning(2, 1, 0), new DeviceCapability.PulseTuning(1.5, 1.0));
         case ACCELERATION -> List.of(new DeviceCapability.AccelerationFactor(0.3));
         case RANGE -> List.of(new DeviceCapability.RangeMultiplier(2.0));
      };
   }
}
