package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

public interface CelestweaveArmorSubmoduleItem extends OverloadDeviceModuleItem {
   ArmorPart armorPart();

   @Override
   default Set<DeviceKind> acceptableDevices() {
      return Set.of(this.armorPart().deviceKind());
   }

   @Override
   default DeviceSlotType acceptableSlot() {
      return this.armorPart().moduleSlot();
   }

   @Override
   default String moduleTypeId(ItemStack stack) {
      String[] id = new String[]{""};
      this.collectSubmodules(stack, submodule -> {
         if (submodule != null && !submodule.id().isBlank() && id[0].isEmpty()) {
            id[0] = submodule.id();
         }
      });
      return id[0];
   }

   @Override
   default List<DeviceCapability> capabilities(ItemStack stack) {
      return List.of();
   }

   void collectSubmodules(ItemStack var1, Consumer<CelestweaveArmorSubmodule> var2);
}
