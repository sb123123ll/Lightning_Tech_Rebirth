package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MatrixGlassBlock extends MatrixFormedBlock {
   public static final BooleanProperty FORMED = MatrixFormedBlock.FORMED;

   public MatrixGlassBlock(Properties properties, MatrixMultiblockComponent component) {
      super(properties, component);
   }

   @Override
   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   @Override
   public int m_7753_(BlockState state, BlockGetter level, BlockPos pos) {
      return 0;
   }

   @Override
   public float m_7749_(BlockState state, BlockGetter level, BlockPos pos) {
      return 1.0F;
   }

   public boolean m_6104_(BlockState state, BlockState adjacentState, Direction side) {
      boolean facesHiddenCore = (Boolean)state.m_61143_(FORMED)
         && adjacentState.m_61138_(MatrixFormedBlock.FORMED)
         && (Boolean)adjacentState.m_61143_(MatrixFormedBlock.FORMED)
         && adjacentState.m_60734_() instanceof MatrixMultiblockComponentBlock componentBlock
         && (componentBlock.matrixComponent(adjacentState).isMainCore() || componentBlock.matrixComponent(adjacentState).isCraftingUnit());
      return adjacentState.m_60734_() instanceof MatrixGlassBlock || facesHiddenCore;
   }

   public VoxelShape m_5909_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.m_83040_();
   }
}
