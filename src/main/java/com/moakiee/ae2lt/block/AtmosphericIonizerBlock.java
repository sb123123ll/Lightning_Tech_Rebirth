package com.moakiee.ae2lt.block;

import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.AtmosphericIonizerBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AtmosphericIonizerBlock extends AE2LTBaseEntityBlock<AtmosphericIonizerBlockEntity> {
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.f_61401_;
   private static final VoxelShape SHAPE_LOWER = BlockShapeHelper.or(
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
      Block.m_49796_(3.0, 8.0, 3.0, 13.0, 9.0, 13.0),
      Block.m_49796_(5.0, 9.0, 5.0, 11.0, 16.0, 11.0),
      Block.m_49796_(4.0, 12.0, 4.0, 12.0, 13.0, 12.0),
      Block.m_49796_(4.0, 14.0, 4.0, 12.0, 15.0, 12.0),
      Block.m_49796_(5.0, 8.0, 2.5, 7.0, 13.0, 4.5),
      Block.m_49796_(9.0, 8.0, 2.5, 11.0, 15.0, 4.5),
      Block.m_49796_(9.0, 8.0, 11.5, 11.0, 13.0, 13.5),
      Block.m_49796_(5.0, 8.0, 11.5, 7.0, 15.0, 13.5),
      Block.m_49796_(11.5, 8.0, 5.0, 13.5, 13.0, 7.0),
      Block.m_49796_(11.5, 8.0, 9.0, 13.5, 15.0, 11.0),
      Block.m_49796_(2.5, 8.0, 9.0, 4.5, 13.0, 11.0),
      Block.m_49796_(2.5, 8.0, 5.0, 4.5, 15.0, 7.0)
   );
   private static final VoxelShape SHAPE_UPPER = BlockShapeHelper.or(
      Block.m_49796_(6.0, 0.0, 6.0, 10.0, 4.0, 10.0), Block.m_49796_(6.0, 4.0, 6.0, 10.0, 8.0, 10.0)
   );

   public AtmosphericIonizerBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_(WORKING, false)).m_61124_(HALF, DoubleBlockHalf.LOWER));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WORKING, HALF});
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return state.m_61143_(HALF) == DoubleBlockHalf.UPPER ? null : super.m_142194_(pos, state);
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      BlockState state = super.m_5573_(context);
      if (state == null) {
         return null;
      } else {
         Level level = context.m_43725_();
         BlockPos extensionPos = context.m_8083_().m_7494_();
         return extensionPos.m_123342_() < level.m_151558_() && level.m_8055_(extensionPos).m_60629_(context)
            ? (BlockState)state.m_61124_(HALF, DoubleBlockHalf.LOWER)
            : null;
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
      if (state.m_61143_(HALF) == DoubleBlockHalf.UPPER) {
         BlockState lowerState = level.m_8055_(pos.m_7495_());
         return this.isSameIonizerHalf(lowerState, DoubleBlockHalf.LOWER);
      } else {
         return super.m_7898_(state, level, pos);
      }
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.m_61143_(HALF);
      if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
         if (!this.isSameIonizerHalf(neighborState, DoubleBlockHalf.UPPER)) {
            return Blocks.f_50016_.m_49966_();
         }
      } else if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN && !this.isSameIonizerHalf(neighborState, DoubleBlockHalf.LOWER)) {
         return Blocks.f_50016_.m_49966_();
      }

      return super.m_7417_(state, direction, neighborState, level, pos, neighborPos);
   }

   public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.f_46443_ && state.m_61143_(HALF) == DoubleBlockHalf.UPPER) {
         BlockPos lowerPos = pos.m_7495_();
         BlockState lowerState = level.m_8055_(lowerPos);
         if (this.isSameIonizerHalf(lowerState, DoubleBlockHalf.LOWER)) {
            level.m_46953_(lowerPos, !player.m_7500_(), player);
         }
      }

      super.m_5707_(level, pos, state, player);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.f_46443_ && !state.m_60713_(newState.m_60734_())) {
         boolean lower = state.m_61143_(HALF) == DoubleBlockHalf.LOWER;
         if (lower && level.m_7702_(pos) instanceof AtmosphericIonizerBlockEntity ionizer) {
            ionizer.cancelProcessingForRemoval();
         }

         BlockPos otherPos = lower ? pos.m_7494_() : pos.m_7495_();
         DoubleBlockHalf otherHalf = lower ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
         BlockState otherState = level.m_8055_(otherPos);
         if (this.isSameIonizerHalf(otherState, otherHalf)) {
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
         return this.isSameIonizerHalf(lowerState, DoubleBlockHalf.LOWER)
            ? this.useWithoutItem(lowerState, level, lowerPos, player, hitResult)
            : InteractionResult.PASS;
      } else {
         AtmosphericIonizerBlockEntity blockEntity = (AtmosphericIonizerBlockEntity)this.getBlockEntity(level, pos);
         if (blockEntity == null) {
            return InteractionResult.PASS;
         } else {
            if (!level.m_5776_()) {
               blockEntity.openMenu(player, MenuLocators.forBlockEntity(blockEntity));
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
         return this.isSameIonizerHalf(lowerState, DoubleBlockHalf.LOWER) ? super.useItemOn(stack, lowerState, level, lowerPos, player, hand, hit) : null;
      } else {
         return super.useItemOn(stack, state, level, pos, player, hand, hit);
      }
   }

   private boolean isSameIonizerHalf(BlockState state, DoubleBlockHalf half) {
      return state.m_60713_(this) && state.m_61143_(HALF) == half;
   }
}
