package com.moakiee.ae2lt.client.ctm;

import com.moakiee.ae2lt.block.MatrixFormedBlock;
import com.moakiee.ae2lt.block.MatrixMultiblockComponentBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputerStructureBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputingUnitBlock;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ConnectionPredicates {
   private static final Map<ResourceLocation, ConnectionPredicate> REGISTRY = new HashMap<>();
   public static final ConnectionPredicate SAME_BLOCK = new ConnectionPredicate() {
      @Override
      public boolean isActive(BlockAndTintGetter level, BlockPos pos, BlockState self) {
         return true;
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction dir) {
         return level.m_8055_(pos.m_121945_(dir)).m_60713_(self.m_60734_());
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
         return level.m_8055_(neighbourPos).m_60713_(self.m_60734_());
      }
   };
   public static final ConnectionPredicate MATRIX_FORMED_SAME_BLOCK = new ConnectionPredicate() {
      @Override
      public boolean isActive(BlockAndTintGetter level, BlockPos pos, BlockState self) {
         return ConnectionPredicates.isFormedMatrixComponent(self);
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction dir) {
         BlockState neighbour = level.m_8055_(pos.m_121945_(dir));
         return neighbour.m_60713_(self.m_60734_()) && ConnectionPredicates.isFormedMatrixComponent(neighbour);
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
         BlockState neighbour = level.m_8055_(neighbourPos);
         return neighbour.m_60713_(self.m_60734_()) && ConnectionPredicates.isFormedMatrixComponent(neighbour);
      }
   };
   public static final ConnectionPredicate TIANSHU_FORMED_SAME_BLOCK = new ConnectionPredicate() {
      @Override
      public boolean isActive(BlockAndTintGetter level, BlockPos pos, BlockState self) {
         return ConnectionPredicates.isFormedTianshuComponent(self);
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction dir) {
         BlockState neighbour = level.m_8055_(pos.m_121945_(dir));
         return neighbour.m_60713_(self.m_60734_()) && ConnectionPredicates.isFormedTianshuComponent(neighbour);
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
         BlockState neighbour = level.m_8055_(neighbourPos);
         return neighbour.m_60713_(self.m_60734_()) && ConnectionPredicates.isFormedTianshuComponent(neighbour);
      }
   };
   public static final ConnectionPredicate TIANSHU_FORMED_COOLING_COMPATIBLE = new ConnectionPredicate() {
      @Override
      public boolean isActive(BlockAndTintGetter level, BlockPos pos, BlockState self) {
         return ConnectionPredicates.isFormedTianshuCoolingComponent(self);
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction dir) {
         return ConnectionPredicates.isFormedTianshuCoolingComponent(level.m_8055_(pos.m_121945_(dir)));
      }

      @Override
      public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
         return ConnectionPredicates.isFormedTianshuCoolingComponent(level.m_8055_(neighbourPos));
      }
   };

   private ConnectionPredicates() {
   }

   public static void register(ResourceLocation id, ConnectionPredicate predicate) {
      REGISTRY.put(id, predicate);
   }

   public static ConnectionPredicate get(ResourceLocation id) {
      return REGISTRY.getOrDefault(id, SAME_BLOCK);
   }

   private static ResourceLocation rl(String path) {
      return new ResourceLocation("ae2lt", path);
   }

   private static boolean isFormedMatrixComponent(BlockState state) {
      if (state.m_60734_() instanceof MatrixMultiblockComponentBlock componentBlock
         && componentBlock.matrixComponent(state) != MatrixMultiblockComponent.MATRIX_CONTROLLER
         && state.m_61138_(MatrixFormedBlock.FORMED)) {
         return (Boolean)state.m_61143_(MatrixFormedBlock.FORMED);
      }

      return false;
   }

   private static boolean isFormedTianshuComponent(BlockState state) {
      return state.m_60734_() instanceof TianshuSupercomputerStructureBlock
         && state.m_61138_(TianshuSupercomputerStructureBlock.FORMED)
         && (Boolean)state.m_61143_(TianshuSupercomputerStructureBlock.FORMED);
   }

   private static boolean isFormedTianshuCoolingComponent(BlockState state) {
      if (!state.m_61138_(TianshuSupercomputerStructureBlock.FORMED) || !(Boolean)state.m_61143_(TianshuSupercomputerStructureBlock.FORMED)) {
         return false;
      } else if (state.m_60713_((Block)ModBlocks.PHASE_CHANGE_COOLING_UNIT.get())) {
         return true;
      } else {
         if (state.m_60734_() instanceof TianshuSupercomputingUnitBlock unit && unit.component().isClosedLoopStorage()) {
            return true;
         }

         return false;
      }
   }

   static {
      register(rl("same_block"), SAME_BLOCK);
      register(rl("matrix_formed_same_block"), MATRIX_FORMED_SAME_BLOCK);
      register(rl("tianshu_formed_same_block"), TIANSHU_FORMED_SAME_BLOCK);
      register(rl("tianshu_formed_cooling_compatible"), TIANSHU_FORMED_COOLING_COMPATIBLE);
   }
}
