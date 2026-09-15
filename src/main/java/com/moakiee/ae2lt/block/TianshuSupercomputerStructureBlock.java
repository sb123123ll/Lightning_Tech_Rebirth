package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockUpdateScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class TianshuSupercomputerStructureBlock extends Block implements WrenchDisassemblableBlock {
   public static final BooleanProperty FORMED = MultiblockStateProperties.FORMED;

   public TianshuSupercomputerStructureBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FORMED, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FORMED});
   }

   public void m_6807_(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.m_6807_(state, level, pos, oldState, movedByPiston);
      if (!state.m_60713_(oldState.m_60734_())) {
         TianshuMultiblockUpdateScheduler.scheduleNear(level, pos);
      }
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_())) {
         TianshuMultiblockUpdateScheduler.scheduleNear(level, pos);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
      super.m_6861_(state, level, pos, block, fromPos, movedByPiston);
      TianshuMultiblockUpdateScheduler.scheduleNear(level, fromPos);
   }
}
