package com.moakiee.ae2lt.crafting.timewheel;

import com.moakiee.thunderbolt.api.crafting.cpu.ExtendedCraftingCpuClusterProvider;
import org.jetbrains.annotations.Nullable;

public interface TimeWheelCraftingCpuPoolProvider extends ExtendedCraftingCpuClusterProvider {
   @Nullable
   TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool();

   default TimeWheelCraftingCpuPool getExtendedCraftingCpuCluster() {
      return this.getTimeWheelCraftingCpuPool();
   }
}
