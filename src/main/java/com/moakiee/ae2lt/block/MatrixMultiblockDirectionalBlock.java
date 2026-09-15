package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class MatrixMultiblockDirectionalBlock extends MatrixMultiblockSimpleBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;

   public MatrixMultiblockDirectionalBlock(Properties properties, MatrixMultiblockComponent component) {
      super(properties, component);
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{FACING});
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(FACING, rotation.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_(FACING)));
   }
}
