package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;

public final class ArmorEnergyModuleStorage {
   private ArmorEnergyModuleStorage() {
   }

   public static long capacityFe(ItemStack armor, Provider registries) {
      if (CelestweaveArmorState.hasCachedEnergyModuleCapacityFe(armor)) {
         return CelestweaveArmorState.getCachedEnergyModuleCapacityFe(armor);
      } else if (registries == null) {
         return CelestweaveArmorState.getCachedEnergyModuleCapacityFe(armor);
      } else {
         long capacity = 0L;

         for (ItemStack module : CelestweaveArmorState.loadModuleStacks(armor, registries)) {
            if (module.m_41720_() instanceof ArmorEnergyModuleItem energyModule) {
               capacity = Math.max(capacity, energyModule.armorCapacityFe());
            } else if (module.m_41720_() instanceof OverloadDeviceModuleItem provider) {
               for (DeviceCapability capability : provider.capabilities(module.m_255036_(1))) {
                  if (capability instanceof DeviceCapability.EnergyCapacity energyCapacity) {
                     capacity = Math.max(capacity, energyCapacity.fe());
                  }
               }
            }
         }

         capacity = Math.max(0L, capacity);
         CelestweaveArmorState.setCachedEnergyModuleCapacityFe(armor, capacity);
         return capacity;
      }
   }
}
