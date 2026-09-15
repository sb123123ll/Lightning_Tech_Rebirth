package com.moakiee.ae2lt.logic.tianshu;

import com.moakiee.ae2lt.blockentity.TianshuSupercomputerControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class TianshuMultiblockUpdateScheduler {
   public static void scheduleNear(Level level, BlockPos changedPos) {
      if (level != null && !level.f_46443_ && changedPos != null) {
         for (BlockPos pos : BlockPos.m_121940_(changedPos.m_7918_(-6, -6, -6), changedPos.m_7918_(6, 0, 6))) {
            if (level.m_46749_(pos) && level.m_7702_(pos) instanceof TianshuSupercomputerControllerBlockEntity controller) {
               controller.scheduleStructureCheck();
            }
         }
      }
   }

   private TianshuMultiblockUpdateScheduler() {
   }
}
