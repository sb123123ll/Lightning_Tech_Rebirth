package com.moakiee.ae2lt.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.moakiee.ae2lt.blockentity.LightningSimulationChamberBlockEntity;
import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightningSimulationChamberBlock extends AE2LTBaseEntityBlock<LightningSimulationChamberBlockEntity> {
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");
   public static final DirectionProperty FACING = BlockStateProperties.f_61374_;
   private static final VoxelShape NORTH_SHAPE = BlockShapeHelper.or(
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
      Block.m_49796_(0.0, 12.0, 0.0, 16.0, 16.0, 14.0),
      Block.m_49796_(0.0, 4.0, 3.0, 2.0, 12.0, 13.0),
      Block.m_49796_(14.0, 4.0, 3.0, 16.0, 12.0, 13.0),
      Block.m_49796_(3.0, 4.0, 14.0, 13.0, 16.0, 16.0),
      Block.m_49796_(7.0, 2.0, 13.0, 9.0, 12.0, 14.0)
   );
   private static final EnumMap<Direction, VoxelShape> SHAPES = BlockShapeHelper.createHorizontalFacingShapes(NORTH_SHAPE);

   public LightningSimulationChamberBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_(WORKING, false)).m_61124_(FACING, Direction.NORTH));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WORKING});
   }

   public IOrientationStrategy getOrientationStrategy() {
      return OrientationStrategies.horizontalFacing();
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.m_61143_(FACING));
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.m_61143_(FACING));
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      LightningSimulationChamberBlockEntity be = (LightningSimulationChamberBlockEntity)this.getBlockEntity(level, pos);
      if (be != null) {
         be.onNeighborChanged(fromPos);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else {
         LightningSimulationChamberBlockEntity be = (LightningSimulationChamberBlockEntity)this.getBlockEntity(level, pos);
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
