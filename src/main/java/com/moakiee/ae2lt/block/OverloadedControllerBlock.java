package com.moakiee.ae2lt.block;

import appeng.block.networking.ControllerBlock;
import appeng.block.networking.ControllerBlock.ControllerBlockState;
import appeng.block.networking.ControllerBlock.ControllerRenderType;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.networktool.NetworkStatusMenu;
import com.moakiee.ae2lt.blockentity.OverloadedControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class OverloadedControllerBlock extends AE2LTBaseEntityBlock<OverloadedControllerBlockEntity> {
   public OverloadedControllerBlock() {
      super(metalProps().m_280606_().m_60978_(6.0F));
      this.m_49959_(
         (BlockState)((BlockState)this.m_49966_().m_61124_(ControllerBlock.CONTROLLER_STATE, ControllerBlockState.offline))
            .m_61124_(ControllerBlock.CONTROLLER_TYPE, ControllerRenderType.block)
      );
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{ControllerBlock.CONTROLLER_STATE, ControllerBlock.CONTROLLER_TYPE});
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return this.updateControllerType(this.m_49966_(), context.m_43725_(), context.m_8083_());
   }

   public BlockState m_7417_(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
      return this.updateControllerType(state, level, pos);
   }

   private BlockState updateControllerType(BlockState baseState, LevelAccessor level, BlockPos pos) {
      ControllerRenderType type = ControllerRenderType.block;
      int x = pos.m_123341_();
      int y = pos.m_123342_();
      int z = pos.m_123343_();
      boolean xx = this.isOverloadedController(level, x - 1, y, z) && this.isOverloadedController(level, x + 1, y, z);
      boolean yy = this.isOverloadedController(level, x, y - 1, z) && this.isOverloadedController(level, x, y + 1, z);
      boolean zz = this.isOverloadedController(level, x, y, z - 1) && this.isOverloadedController(level, x, y, z + 1);
      if (xx && !yy && !zz) {
         type = ControllerRenderType.column_x;
      } else if (!xx && yy && !zz) {
         type = ControllerRenderType.column_y;
      } else if (!xx && !yy && zz) {
         type = ControllerRenderType.column_z;
      } else if ((xx ? 1 : 0) + (yy ? 1 : 0) + (zz ? 1 : 0) >= 2) {
         type = (Math.abs(x) + Math.abs(y) + Math.abs(z)) % 2 == 0 ? ControllerRenderType.inside_a : ControllerRenderType.inside_b;
      }

      return (BlockState)baseState.m_61124_(ControllerBlock.CONTROLLER_TYPE, type);
   }

   private boolean isOverloadedController(LevelAccessor level, int x, int y, int z) {
      return level.m_8055_(new BlockPos(x, y, z)).m_60734_() instanceof OverloadedControllerBlock;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.m_7702_(pos) instanceof OverloadedControllerBlockEntity be) {
         if (!level.f_46443_) {
            MenuOpener.open(NetworkStatusMenu.CONTROLLER_TYPE, player, MenuLocators.forBlockEntity(be));
         }

         return InteractionResult.m_19078_(level.f_46443_);
      } else {
         return super.useWithoutItem(state, level, pos, player, hitResult);
      }
   }
}
