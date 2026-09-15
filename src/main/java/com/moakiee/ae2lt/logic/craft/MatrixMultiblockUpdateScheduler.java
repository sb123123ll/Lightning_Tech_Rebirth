package com.moakiee.ae2lt.logic.craft;

import com.moakiee.ae2lt.blockentity.MatrixControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class MatrixMultiblockUpdateScheduler {
   private MatrixMultiblockUpdateScheduler() {
   }

   public static void scheduleNear(Level level, BlockPos changedPos) {
      if (level != null && !level.f_46443_ && changedPos != null) {
         for (BlockPos controllerPos : MatrixMultiblockScanner.candidateControllerPositions(changedPos)) {
            if (level.m_46749_(controllerPos) && level.m_7702_(controllerPos) instanceof MatrixControllerBlockEntity controller) {
               controller.scheduleStructureCheck();
            }
         }
      }
   }
}
