package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.FumoBlockEntity;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FumoBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.f_61362_;
   private static final VoxelShape SHAPE_NORTH = Block.m_49796_(3.9, 0.0, 4.0, 12.2, 13.6, 14.7);
   private static final VoxelShape SHAPE_SOUTH = Block.m_49796_(3.8, 0.0, 1.3, 12.1, 13.6, 12.0);
   private static final VoxelShape SHAPE_WEST = Block.m_49796_(4.0, 0.0, 3.8, 14.7, 13.6, 12.2);
   private static final VoxelShape SHAPE_EAST = Block.m_49796_(1.3, 0.0, 3.8, 12.0, 13.6, 12.2);

   public FumoBlock() {
      super(Properties.m_284310_().m_284180_(MapColor.f_283808_).m_60918_(SoundType.f_56745_).m_60978_(0.5F).m_60955_());
      this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH)).m_61124_(WATERLOGGED, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, WATERLOGGED});
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      FluidState fluidState = context.m_43725_().m_6425_(context.m_8083_());
      return (BlockState)((BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_()))
         .m_61124_(WATERLOGGED, fluidState.m_76152_() == Fluids.f_76193_);
   }

   public boolean m_180643_(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
      return false;
   }

   public int m_7753_(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
      return 2;
   }

   public float m_7749_(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
      return 1.0F;
   }

   public boolean m_7420_(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
      return true;
   }

   @NotNull
   public FluidState m_5888_(BlockState state) {
      return state.m_61143_(WATERLOGGED) ? Fluids.f_76193_.m_76068_(false) : super.m_5888_(state);
   }

   @NotNull
   public BlockState m_7417_(
      BlockState state,
      @NotNull Direction facing,
      @NotNull BlockState facingState,
      @NotNull LevelAccessor level,
      @NotNull BlockPos currentPos,
      @NotNull BlockPos facingPos
   ) {
      if ((Boolean)state.m_61143_(WATERLOGGED)) {
         level.m_186469_(currentPos, Fluids.f_76193_, Fluids.f_76193_.m_6718_(level));
      }

      return super.m_7417_(state, facing, facingState, level, currentPos, facingPos);
   }

   @NotNull
   public VoxelShape m_5940_(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
      return this.getShapeForFacing((Direction)state.m_61143_(FACING));
   }

   @NotNull
   public VoxelShape m_5939_(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
      return this.getShapeForFacing((Direction)state.m_61143_(FACING));
   }

   private VoxelShape getShapeForFacing(Direction facing) {
      return switch (facing) {
         case SOUTH -> SHAPE_SOUTH;
         case WEST -> SHAPE_WEST;
         case EAST -> SHAPE_EAST;
         default -> SHAPE_NORTH;
      };
   }

   @NotNull
   public RenderShape m_7514_(@NotNull BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @NotNull
   public InteractionResult m_6227_(
      @NotNull BlockState state,
      @NotNull Level level,
      @NotNull BlockPos pos,
      @NotNull Player player,
      @NotNull InteractionHand hand,
      @NotNull BlockHitResult hitResult
   ) {
      if (!level.m_5776_() && level.m_7702_(pos) instanceof FumoBlockEntity be) {
         be.toggleSpinning();
      }

      return InteractionResult.m_19078_(level.m_5776_());
   }

   @Nullable
   public BlockEntity m_142194_(@NotNull BlockPos pos, @NotNull BlockState state) {
      return new FumoBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
      return level.m_5776_() && blockEntityType == ModBlockEntities.FUMO.get()
         ? (l, p, s, be) -> FumoBlockEntity.clientTick(l, p, s, (FumoBlockEntity)be)
         : null;
   }
}
