package com.moakiee.ae2lt.block;

import appeng.client.render.effects.ParticleTypes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class OverloadCrystalClusterBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.f_61362_;
   public static final DirectionProperty FACING = BlockStateProperties.f_61372_;
   protected final VoxelShape northAabb;
   protected final VoxelShape southAabb;
   protected final VoxelShape eastAabb;
   protected final VoxelShape westAabb;
   protected final VoxelShape upAabb;
   protected final VoxelShape downAabb;

   public OverloadCrystalClusterBlock(int height, int offset, Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)this.m_49966_().m_61124_(WATERLOGGED, false)).m_61124_(FACING, Direction.UP));
      this.upAabb = Block.m_49796_((double)offset, 0.0, (double)offset, (double)(16 - offset), (double)height, (double)(16 - offset));
      this.downAabb = Block.m_49796_((double)offset, (double)(16 - height), (double)offset, (double)(16 - offset), 16.0, (double)(16 - offset));
      this.northAabb = Block.m_49796_((double)offset, (double)offset, (double)(16 - height), (double)(16 - offset), (double)(16 - offset), 16.0);
      this.southAabb = Block.m_49796_((double)offset, (double)offset, 0.0, (double)(16 - offset), (double)(16 - offset), (double)height);
      this.eastAabb = Block.m_49796_(0.0, (double)offset, (double)offset, (double)height, (double)(16 - offset), (double)(16 - offset));
      this.westAabb = Block.m_49796_((double)(16 - height), (double)offset, (double)offset, 16.0, (double)(16 - offset), (double)(16 - offset));
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.m_61143_(FACING)) {
         case NORTH -> this.northAabb;
         case SOUTH -> this.southAabb;
         case EAST -> this.eastAabb;
         case WEST -> this.westAabb;
         case DOWN -> this.downAabb;
         case UP -> this.upAabb;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      return builder.m_287159_(LootContextParams.f_81455_) == null ? List.of() : super.m_49635_(state, builder);
   }

   public boolean m_7898_(BlockState state, LevelReader level, BlockPos pos) {
      Direction direction = (Direction)state.m_61143_(FACING);
      BlockPos supportPos = pos.m_121945_(direction.m_122424_());
      return level.m_8055_(supportPos).m_60783_(level, supportPos, direction);
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
      if ((Boolean)state.m_61143_(WATERLOGGED)) {
         level.m_186469_(currentPos, Fluids.f_76193_, Fluids.f_76193_.m_6718_(level));
      }

      return direction == ((Direction)state.m_61143_(FACING)).m_122424_() && !state.m_60710_(level, currentPos)
         ? Blocks.f_50016_.m_49966_()
         : super.m_7417_(state, direction, neighborState, level, currentPos, neighborPos);
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      FluidState fluidState = context.m_43725_().m_6425_(context.m_8083_());
      return (BlockState)((BlockState)this.m_49966_().m_61124_(WATERLOGGED, fluidState.m_76152_() == Fluids.f_76193_)).m_61124_(FACING, context.m_43719_());
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(FACING, rotation.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_(FACING)));
   }

   public FluidState m_5888_(BlockState state) {
      return state.m_61143_(WATERLOGGED) ? Fluids.f_76193_.m_76068_(false) : super.m_5888_(state);
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{WATERLOGGED, FACING});
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.DESTROY;
   }

   @OnlyIn(Dist.CLIENT)
   public void m_214162_(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (random.m_188503_(12) == 0) {
         Direction facing = (Direction)state.m_61143_(FACING);
         double x = (double)pos.m_123341_() + 0.5 + (double)facing.m_122429_() * 0.3;
         double y = (double)pos.m_123342_() + 0.5 + (double)facing.m_122430_() * 0.3;
         double z = (double)pos.m_123343_() + 0.5 + (double)facing.m_122431_() * 0.3;
         x += ((double)random.m_188501_() - 0.5) * 0.4;
         y += ((double)random.m_188501_() - 0.5) * 0.4;
         z += ((double)random.m_188501_() - 0.5) * 0.4;
         Particle particle = Minecraft.m_91087_().f_91061_.m_107370_(ParticleTypes.LIGHTNING, x, y, z, 0.0, 0.0, 0.0);
         if (particle != null) {
            particle.m_107253_(1.0F, 0.95F, 0.45F);
         }
      }
   }
}
