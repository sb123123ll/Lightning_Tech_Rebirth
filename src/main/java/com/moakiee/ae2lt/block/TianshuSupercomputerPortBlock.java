package com.moakiee.ae2lt.block;

import appeng.block.AEBaseEntityBlock;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockUpdateScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class TianshuSupercomputerPortBlock extends AEBaseEntityBlock<TianshuSupercomputerPortBlockEntity> implements WrenchDisassemblableBlock {
   public static final BooleanProperty FORMED = TianshuSupercomputerStructureBlock.FORMED;

   public TianshuSupercomputerPortBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FORMED, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{FORMED});
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : (tickLevel, pos, tickState, blockEntity) -> {
         if (blockEntity instanceof TianshuSupercomputerPortBlockEntity port) {
            TianshuSupercomputerPortBlockEntity.serverTick(tickLevel, pos, tickState, port);
         }
      };
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
}
