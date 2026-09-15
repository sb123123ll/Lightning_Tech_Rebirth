package com.moakiee.ae2lt.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.blockentity.LightningAssemblyChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class LightningAssemblyChamberBlock extends AE2LTBaseEntityBlock<LightningAssemblyChamberBlockEntity> {
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");
   public static final BooleanProperty POWERED = BooleanProperty.m_61465_("powered");

   public LightningAssemblyChamberBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_(
         (BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_(WORKING, false)).m_61124_(POWERED, false))
            .m_61124_(BlockStateProperties.f_61374_, Direction.NORTH)
      );
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WORKING});
      builder.m_61104_(new Property[]{POWERED});
   }

   public IOrientationStrategy getOrientationStrategy() {
      return OrientationStrategies.horizontalFacing();
   }

   protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, LightningAssemblyChamberBlockEntity be) {
      return (BlockState)currentState.m_61124_(POWERED, be.isPowered());
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      LightningAssemblyChamberBlockEntity be = (LightningAssemblyChamberBlockEntity)this.getBlockEntity(level, pos);
      if (be != null) {
         be.onNeighborChanged(fromPos);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else {
         LightningAssemblyChamberBlockEntity be = (LightningAssemblyChamberBlockEntity)this.getBlockEntity(level, pos);
         if (be == null) {
            return InteractionResult.PASS;
         } else {
            if (!level.m_5776_()) {
               be.openMenu(player, MenuLocators.forBlockEntity(be));
            }

            return InteractionResult.m_19078_(level.m_5776_());
         }
      }
   }
}
