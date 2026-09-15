package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.TianshuSeedStorageBlockEntity;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class TianshuSeedStorageBlock extends TianshuSupercomputingUnitBlock implements EntityBlock {
   public TianshuSeedStorageBlock(Properties properties) {
      super(properties, TianshuMultiblockComponent.CLOSED_LOOP_SEED_STORAGE);
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TianshuSeedStorageBlockEntity(pos, state);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.m_7702_(pos) instanceof TianshuSeedStorageBlockEntity drive) {
         if (!level.f_46443_) {
            drive.openMenu(player);
         }

         return InteractionResult.m_19078_(level.f_46443_);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.m_60713_(newState.m_60734_()) && !level.f_46443_ && level.m_7702_(pos) instanceof TianshuSeedStorageBlockEntity drive) {
         drive.dropCells();
      }

      super.m_6810_(state, level, pos, newState, moved);
   }
}
