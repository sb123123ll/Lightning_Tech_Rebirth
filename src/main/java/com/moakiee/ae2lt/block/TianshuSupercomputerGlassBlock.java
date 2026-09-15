package com.moakiee.ae2lt.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class TianshuSupercomputerGlassBlock extends TianshuSupercomputerStructureBlock {
   public TianshuSupercomputerGlassBlock(Properties properties) {
      super(properties);
   }

   public boolean m_6104_(BlockState state, BlockState adjacentState, Direction side) {
      boolean facesHiddenCore = (Boolean)state.m_61143_(FORMED)
         && adjacentState.m_60734_() instanceof TianshuSupercomputingUnitBlock
         && adjacentState.m_61138_(TianshuSupercomputingUnitBlock.FORMED)
         && (Boolean)adjacentState.m_61143_(TianshuSupercomputingUnitBlock.FORMED);
      return adjacentState.m_60713_(this) || facesHiddenCore || super.m_6104_(state, adjacentState, side);
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   public int m_7753_(BlockState state, BlockGetter level, BlockPos pos) {
      return 0;
   }
}
