package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MatrixFormedBlock extends MatrixMultiblockSimpleBlock {
   public static final BooleanProperty FORMED = MultiblockStateProperties.FORMED;

   public MatrixFormedBlock(Properties properties, MatrixMultiblockComponent component) {
      super(properties, component);
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FORMED, Boolean.FALSE));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{FORMED});
   }

   public VoxelShape m_7952_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesTransparentFormedModel(state) ? Shapes.m_83040_() : super.m_7952_(state, level, pos);
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesTransparentFormedModel(state) || super.m_7420_(state, level, pos);
   }

   public int m_7753_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesTransparentFormedModel(state) ? 0 : super.m_7753_(state, level, pos);
   }

   public float m_7749_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesTransparentFormedModel(state) ? 1.0F : super.m_7749_(state, level, pos);
   }

   private boolean usesTransparentFormedModel(BlockState state) {
      if (!(Boolean)state.m_61143_(FORMED)) {
         return false;
      } else {
         return switch (this.matrixComponent(state)) {
            case STABLE_MAIN_CORE, QUANTUM_MAIN_CORE, OVERLOAD_MAIN_CORE, MULTIDIMENSIONAL_MAIN_CORE, THREAD_UNIT_T1, THREAD_UNIT_T2, AMPLIFIER_UNIT, THERMAL_CONTROL_UNIT_T1, THERMAL_CONTROL_UNIT_T2 -> true;
            default -> false;
         };
      }
   }
}
