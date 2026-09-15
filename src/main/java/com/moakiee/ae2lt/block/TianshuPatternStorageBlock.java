package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.TianshuPatternStorageBlockEntity;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.Nullable;

public final class TianshuPatternStorageBlock extends TianshuSupercomputingUnitBlock implements EntityBlock {
   public TianshuPatternStorageBlock(Properties properties) {
      super(properties, TianshuMultiblockComponent.CLOSED_LOOP_PATTERN_STORAGE);
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TianshuPatternStorageBlockEntity(pos, state);
   }

   @Override
   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.m_60713_(newState.m_60734_()) && !level.f_46443_ && level.m_7702_(pos) instanceof TianshuPatternStorageBlockEntity storage) {
         storage.dropStoredPatterns(level, pos);
      }

      super.m_6810_(state, level, pos, newState, moved);
   }
}
