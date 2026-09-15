package com.moakiee.ae2lt.item.railgun;

import com.moakiee.ae2lt.celestweave.ArmorEnergyModuleItem;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import net.minecraft.world.item.ItemStack;

public final class RailgunEnergyModuleStorage {
   private RailgunEnergyModuleStorage() {
   }

   public static long capacityFe(ItemStack railgun) {
      long capacity = 0L;

      for (ItemStack module : RailgunModuleStorage.INSTANCE.listEntries(railgun)) {
         if (module.m_41720_() instanceof ArmorEnergyModuleItem energyModule) {
            capacity = Math.max(capacity, energyModule.capacityFe());
         } else if (module.m_41720_() instanceof OverloadDeviceModuleItem provider) {
            for (DeviceCapability capability : provider.capabilities(module.m_255036_(1))) {
               if (capability instanceof DeviceCapability.EnergyCapacity energyCapacity) {
                  capacity = Math.max(capacity, energyCapacity.fe());
               }
            }
         }
      }

      return Math.max(0L, capacity);
   }
}
