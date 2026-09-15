package com.moakiee.ae2lt.block;

import appeng.block.AEBaseEntityBlock;
import com.moakiee.ae2lt.blockentity.MatrixPortBlockEntity;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockUpdateScheduler;
import com.moakiee.ae2lt.menu.MatrixPortMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MatrixPortBlock extends AEBaseEntityBlock<MatrixPortBlockEntity> implements MatrixMultiblockComponentBlock {
   public static final BooleanProperty FORMED = MatrixFormedBlock.FORMED;

   public MatrixPortBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FORMED, Boolean.FALSE));
   }

   @Override
   public MatrixMultiblockComponent matrixComponent(BlockState state) {
      return MatrixMultiblockComponent.MATRIX_PORT;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{FORMED});
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : (tickLevel, pos, tickState, blockEntity) -> {
         if (blockEntity instanceof MatrixPortBlockEntity port) {
            MatrixPortBlockEntity.serverTick(tickLevel, pos, tickState, port);
         }
      };
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

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (level.m_7702_(pos) instanceof MatrixPortBlockEntity port) {
         if (!level.f_46443_ && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
               serverPlayer,
               new SimpleMenuProvider((id, inventory, ignored) -> new MatrixPortMenu(id, inventory, port), state.m_60734_().m_49954_()),
               buffer -> MatrixPortMenu.writeExtraData(buffer, port)
            );
         }

         return InteractionResult.m_19078_(level.f_46443_);
      } else {
         return InteractionResult.PASS;
      }
   }
}
