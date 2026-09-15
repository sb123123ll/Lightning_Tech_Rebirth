package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPool;
import com.moakiee.ae2lt.logic.tianshu.TianshuFunctionProfile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface TianshuInventoryMaintenanceHost {
   boolean isFormed();

   boolean isCpuActive();

   TianshuFunctionProfile getFunctionProfile();

   TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool();

   @Nullable
   Level m_58904_();

   @Nullable
   IGrid getGrid();

   IActionSource getActionSource();

   @Nullable
   IGridNode getActionableNode();

   void maintenanceStateChanged();
}
