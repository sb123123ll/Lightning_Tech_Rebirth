package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockUpdateScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class MatrixMultiblockSimpleBlock extends Block implements MatrixMultiblockComponentBlock {
   private final MatrixMultiblockComponent component;

   public MatrixMultiblockSimpleBlock(Properties properties, MatrixMultiblockComponent component) {
      super(properties);
      this.component = component;
   }

   @Override
   public MatrixMultiblockComponent matrixComponent(BlockState state) {
      return this.component;
   }

   public void m_6807_(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.m_6807_(state, level, pos, oldState, movedByPiston);
      if (!state.m_60713_(oldState.m_60734_())) {
         MatrixMultiblockUpdateScheduler.scheduleNear(level, pos);
      }
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_())) {
         MatrixMultiblockUpdateScheduler.scheduleNear(level, pos);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
      super.m_6861_(state, level, pos, block, fromPos, movedByPiston);
      MatrixMultiblockUpdateScheduler.scheduleNear(level, fromPos);
   }
}
