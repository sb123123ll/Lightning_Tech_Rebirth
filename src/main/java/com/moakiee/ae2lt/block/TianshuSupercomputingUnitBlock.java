package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockUpdateScheduler;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockComponent;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockUpdateScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TianshuSupercomputingUnitBlock extends Block implements MatrixMultiblockComponentBlock {
   public static final BooleanProperty FORMED = TianshuSupercomputerStructureBlock.FORMED;
   private final TianshuMultiblockComponent component;
   private final MatrixMultiblockComponent matrixComponent;

   public TianshuSupercomputingUnitBlock(Properties properties, TianshuMultiblockComponent component) {
      this(properties, component, MatrixMultiblockComponent.OTHER);
   }

   public TianshuSupercomputingUnitBlock(Properties properties, TianshuMultiblockComponent component, MatrixMultiblockComponent matrixComponent) {
      super(properties);
      this.component = component;
      this.matrixComponent = matrixComponent;
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FORMED, false));
   }

   public TianshuMultiblockComponent component() {
      return this.component;
   }

   @Override
   public MatrixMultiblockComponent matrixComponent(BlockState state) {
      return this.matrixComponent;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FORMED});
   }

   public VoxelShape m_7952_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesHiddenFormedModel(state) ? Shapes.m_83040_() : super.m_7952_(state, level, pos);
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesHiddenFormedModel(state) || super.m_7420_(state, level, pos);
   }

   public int m_7753_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesHiddenFormedModel(state) ? 0 : super.m_7753_(state, level, pos);
   }

   public float m_7749_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.usesHiddenFormedModel(state) ? 1.0F : super.m_7749_(state, level, pos);
   }

   public void m_6807_(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.m_6807_(state, level, pos, oldState, movedByPiston);
      if (!state.m_60713_(oldState.m_60734_())) {
         TianshuMultiblockUpdateScheduler.scheduleNear(level, pos);
         this.scheduleMatrixUpdate(level, pos);
      }
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_())) {
         TianshuMultiblockUpdateScheduler.scheduleNear(level, pos);
         this.scheduleMatrixUpdate(level, pos);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
      super.m_6861_(state, level, pos, block, fromPos, movedByPiston);
      if (this.matrixComponent != MatrixMultiblockComponent.OTHER) {
         MatrixMultiblockUpdateScheduler.scheduleNear(level, fromPos);
      }
   }

   private void scheduleMatrixUpdate(Level level, BlockPos pos) {
      if (this.matrixComponent != MatrixMultiblockComponent.OTHER) {
         MatrixMultiblockUpdateScheduler.scheduleNear(level, pos);
      }
   }

   private boolean usesHiddenFormedModel(BlockState state) {
      if (!(Boolean)state.m_61143_(FORMED)) {
         return false;
      } else {
         return switch (this.component) {
            case MAIN_BASELINE, MAIN_QUANTUM, MAIN_OVERLOAD, MAIN_MULTIDIMENSIONAL, BLANK_UNIT, STORAGE_UNIT, PARALLEL_UNIT, AMPLIFIER_UNIT -> true;
            default -> false;
         };
      }
   }
}
