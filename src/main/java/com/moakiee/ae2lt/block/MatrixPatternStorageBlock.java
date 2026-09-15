package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.MatrixPatternStorageBlockEntity;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.Nullable;

public class MatrixPatternStorageBlock extends MatrixFormedBlock implements EntityBlock {
   public MatrixPatternStorageBlock(Properties properties, MatrixMultiblockComponent component) {
      super(properties, component);
      if (!component.isPatternStorage()) {
         throw new IllegalArgumentException("Matrix pattern storage block requires a pattern-storage component");
      }
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new MatrixPatternStorageBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return null;
   }

   @Override
   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_())
         && !(newState.m_60734_() instanceof MatrixPatternStorageBlock)
         && !level.f_46443_
         && level.m_7702_(pos) instanceof MatrixPatternStorageBlockEntity storage) {
         storage.dropStoredPatterns(level, pos);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }
}
