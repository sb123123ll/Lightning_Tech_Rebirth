package com.moakiee.ae2lt.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.TeslaCoilBlockEntity;
import com.moakiee.ae2lt.blockentity.TeslaCoilUpperBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TeslaCoilBlock extends AE2LTBaseEntityBlock<TeslaCoilBlockEntity> {
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");
   public static final DirectionProperty FACING = BlockStateProperties.f_61374_;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.f_61401_;
   private static final VoxelShape SHAPE_LOWER = BlockShapeHelper.or(
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 9.0, 16.0),
      Block.m_49796_(7.0, 8.0, 1.0, 9.0, 16.0, 6.0),
      Block.m_49796_(10.0, 8.0, 7.0, 15.0, 16.0, 9.0),
      Block.m_49796_(7.0, 8.0, 10.0, 9.0, 16.0, 15.0),
      Block.m_49796_(1.0, 8.0, 7.0, 6.0, 16.0, 9.0),
      Block.m_49796_(5.0, 9.0, 5.0, 11.0, 11.0, 11.0),
      Block.m_49796_(5.5, 11.0, 5.5, 10.5, 16.0, 10.5)
   );
   private static final VoxelShape SHAPE_UPPER = BlockShapeHelper.or(
      Block.m_49796_(7.0, 0.0, 1.0, 9.0, 2.0, 6.0),
      Block.m_49796_(10.0, 0.0, 7.0, 15.0, 2.0, 9.0),
      Block.m_49796_(7.0, 0.0, 10.0, 9.0, 2.0, 15.0),
      Block.m_49796_(1.0, 0.0, 7.0, 6.0, 2.0, 9.0),
      Block.m_49796_(5.5, 0.0, 5.5, 10.5, 9.0, 10.5),
      Block.m_49796_(5.0, 2.0, 5.0, 11.0, 7.0, 11.0),
      Block.m_49796_(5.0, 7.0, 5.0, 11.0, 8.0, 11.0),
      Block.m_49796_(5.0, 9.0, 5.0, 11.0, 10.0, 11.0),
      Block.m_49796_(6.0, 10.0, 6.0, 10.0, 14.0, 10.0),
      Block.m_49796_(1.0, 12.0, 1.0, 12.0, 15.0, 4.0),
      Block.m_49796_(1.0, 12.0, 4.0, 4.0, 15.0, 15.0),
      Block.m_49796_(4.0, 12.0, 12.0, 15.0, 15.0, 15.0),
      Block.m_49796_(12.0, 12.0, 1.0, 15.0, 15.0, 12.0),
      Block.m_49796_(7.5, 10.0, 1.0, 8.5, 12.0, 6.0),
      Block.m_49796_(10.0, 10.0, 7.5, 15.0, 12.0, 8.5),
      Block.m_49796_(7.5, 10.0, 10.0, 8.5, 12.0, 15.0),
      Block.m_49796_(1.0, 10.0, 7.5, 6.0, 12.0, 8.5)
   );

   public TeslaCoilBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_(
         (BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_(WORKING, false)).m_61124_(FACING, Direction.NORTH))
            .m_61124_(HALF, DoubleBlockHalf.LOWER)
      );
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WORKING, HALF});
   }

   public IOrientationStrategy getOrientationStrategy() {
      return OrientationStrategies.horizontalFacing();
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return (BlockEntity)(state.m_61143_(HALF) == DoubleBlockHalf.UPPER ? new TeslaCoilUpperBlockEntity(pos, state) : super.m_142194_(pos, state));
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState blockState, BlockEntityType<T> type) {
      return blockState.m_61143_(HALF) == DoubleBlockHalf.UPPER ? null : super.m_142354_(level, blockState, type);
   }

   public static BlockPos getCapabilityHostPos(DoubleBlockHalf half, BlockPos pos) {
      return TeslaCoilHalfHelper.getCapabilityHostPos(half, pos);
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      BlockPos pos = context.m_8083_();
      BlockPos above = pos.m_7494_();
      Level level = context.m_43725_();
      if (above.m_123342_() < level.m_151558_() && level.m_8055_(above).m_60629_(context)) {
         BlockState state = super.m_5573_(context);
         return state == null ? null : (BlockState)state.m_61124_(HALF, DoubleBlockHalf.LOWER);
      } else {
         return null;
      }
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.m_6402_(level, pos, state, placer, stack);
      if (!level.f_46443_ && state.m_61143_(HALF) == DoubleBlockHalf.LOWER) {
         BlockState upperState = (BlockState)state.m_61124_(HALF, DoubleBlockHalf.UPPER);
         level.m_7731_(pos.m_7494_(), upperState, 3);
      }
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.m_61143_(HALF) == DoubleBlockHalf.UPPER ? SHAPE_UPPER : SHAPE_LOWER;
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.m_5940_(state, level, pos, context);
   }

   public VoxelShape m_7952_(BlockState state, BlockGetter level, BlockPos pos) {
      return Shapes.m_83040_();
   }

   public RenderShape m_7514_(BlockState state) {
      return state.m_61143_(HALF) == DoubleBlockHalf.UPPER ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   public boolean m_7898_(BlockState state, LevelReader level, BlockPos pos) {
      if (state.m_61143_(HALF) != DoubleBlockHalf.UPPER) {
         return super.m_7898_(state, level, pos);
      } else {
         BlockState below = level.m_8055_(pos.m_7495_());
         return below.m_60713_(this) && below.m_61143_(HALF) == DoubleBlockHalf.LOWER;
      }
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.m_61143_(HALF);
      if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
         if (!neighborState.m_60713_(this) || neighborState.m_61143_(HALF) != DoubleBlockHalf.UPPER) {
            return Blocks.f_50016_.m_49966_();
         }
      } else if (half == DoubleBlockHalf.UPPER
         && direction == Direction.DOWN
         && (!neighborState.m_60713_(this) || neighborState.m_61143_(HALF) != DoubleBlockHalf.LOWER)) {
         return Blocks.f_50016_.m_49966_();
      }

      return super.m_7417_(state, direction, neighborState, level, pos, neighborPos);
   }

   public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.f_46443_ && state.m_61143_(HALF) == DoubleBlockHalf.UPPER) {
         BlockPos lowerPos = pos.m_7495_();
         BlockState lowerState = level.m_8055_(lowerPos);
         if (lowerState.m_60713_(this) && lowerState.m_61143_(HALF) == DoubleBlockHalf.LOWER) {
            level.m_46953_(lowerPos, !player.m_7500_(), player);
         }
      }

      super.m_5707_(level, pos, state, player);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.f_46443_ && !state.m_60713_(newState.m_60734_())) {
         BlockPos otherPos = state.m_61143_(HALF) == DoubleBlockHalf.LOWER ? pos.m_7494_() : pos.m_7495_();
         DoubleBlockHalf otherHalf = state.m_61143_(HALF) == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
         BlockState otherState = level.m_8055_(otherPos);
         if (otherState.m_60713_(this) && otherState.m_61143_(HALF) == otherHalf) {
            level.m_7731_(otherPos, Blocks.f_50016_.m_49966_(), 2);
         }
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      return state.m_61143_(HALF) == DoubleBlockHalf.UPPER ? List.of() : super.m_49635_(state, builder);
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
      return new ItemStack(this.m_5456_());
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (state.m_61143_(HALF) == DoubleBlockHalf.UPPER) {
         BlockPos lowerPos = pos.m_7495_();
         BlockState lowerState = level.m_8055_(lowerPos);
         return lowerState.m_60713_(this) && lowerState.m_61143_(HALF) == DoubleBlockHalf.LOWER
            ? this.useWithoutItem(lowerState, level, lowerPos, player, hitResult)
            : InteractionResult.PASS;
      } else {
         TeslaCoilBlockEntity be = (TeslaCoilBlockEntity)this.getBlockEntity(level, pos);
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

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (state.m_61143_(HALF) == DoubleBlockHalf.UPPER) {
         BlockPos lowerPos = pos.m_7495_();
         BlockState lowerState = level.m_8055_(lowerPos);
         return lowerState.m_60713_(this) && lowerState.m_61143_(HALF) == DoubleBlockHalf.LOWER
            ? super.useItemOn(stack, lowerState, level, lowerPos, player, hand, hit)
            : null;
      } else {
         return super.useItemOn(stack, state, level, pos, player, hand, hit);
      }
   }
}
