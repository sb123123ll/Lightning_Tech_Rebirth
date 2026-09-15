package com.moakiee.ae2lt.client.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface ConnectionPredicate {
   boolean isActive(BlockAndTintGetter var1, BlockPos var2, BlockState var3);

   boolean connects(BlockAndTintGetter var1, BlockPos var2, BlockState var3, Direction var4);

   default boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
      for (Direction dir : Direction.values()) {
         if (pos.m_121945_(dir).equals(neighbourPos)) {
            return this.connects(level, pos, self, dir);
         }
      }

      return false;
   }
}
