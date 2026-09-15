package com.moakiee.ae2lt.crafting.timewheel;

import com.moakiee.thunderbolt.api.crafting.cpu.ExtendedCraftingCpuClusterHost;

public interface TimeWheelCraftingCpuPoolHost extends TimeWheelCraftingCpuHost, ExtendedCraftingCpuClusterHost, TimeWheelCraftingCpuPoolProvider {
   @Override
   TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool();

   @Override
   default TimeWheelCraftingCpuPool getExtendedCraftingCpuCluster() {
      return this.getTimeWheelCraftingCpuPool();
   }
}
