package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;

public class BuddingOverloadCrystalBlock extends Block {
   public static final int GROWTH_CHANCE = 5;
   public static final int DECAY_CHANCE = 12;
   private static final Direction[] DIRECTIONS = Direction.values();

   public BuddingOverloadCrystalBlock(Properties properties) {
      super(properties);
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.DESTROY;
   }

   public void m_213898_(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
      if (randomSource.m_188503_(5) == 0) {
         Direction direction = (Direction)Util.m_214670_(DIRECTIONS, randomSource);
         BlockPos targetPos = pos.m_121945_(direction);
         BlockState targetState = level.m_8055_(targetPos);
         Block newCluster = null;
         if (canClusterGrowAtState(targetState)) {
            newCluster = (Block)ModBlocks.SMALL_OVERLOAD_CRYSTAL_BUD.get();
         } else if (targetState.m_60713_((Block)ModBlocks.SMALL_OVERLOAD_CRYSTAL_BUD.get())
            && targetState.m_61143_(AmethystClusterBlock.f_152006_) == direction) {
            newCluster = (Block)ModBlocks.MEDIUM_OVERLOAD_CRYSTAL_BUD.get();
         } else if (targetState.m_60713_((Block)ModBlocks.MEDIUM_OVERLOAD_CRYSTAL_BUD.get())
            && targetState.m_61143_(AmethystClusterBlock.f_152006_) == direction) {
            newCluster = (Block)ModBlocks.LARGE_OVERLOAD_CRYSTAL_BUD.get();
         } else if (targetState.m_60713_((Block)ModBlocks.LARGE_OVERLOAD_CRYSTAL_BUD.get())
            && targetState.m_61143_(AmethystClusterBlock.f_152006_) == direction) {
            newCluster = (Block)ModBlocks.OVERLOAD_CRYSTAL_CLUSTER.get();
         }

         if (newCluster != null) {
            BlockState newClusterState = (BlockState)((BlockState)newCluster.m_49966_().m_61124_(AmethystClusterBlock.f_152006_, direction))
               .m_61124_(AmethystClusterBlock.f_152005_, targetState.m_60819_().m_76152_() == Fluids.f_76193_);
            level.m_46597_(targetPos, newClusterState);
            if (this != ModBlocks.FLAWLESS_BUDDING_OVERLOAD_CRYSTAL.get() && randomSource.m_188503_(12) == 0) {
               Block newBlock;
               if (this == ModBlocks.FLAWED_BUDDING_OVERLOAD_CRYSTAL.get()) {
                  newBlock = (Block)ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get();
               } else if (this == ModBlocks.CRACKED_BUDDING_OVERLOAD_CRYSTAL.get()) {
                  newBlock = (Block)ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get();
               } else {
                  if (this != ModBlocks.DAMAGED_BUDDING_OVERLOAD_CRYSTAL.get()) {
                     throw new IllegalStateException("Unexpected block: " + this);
                  }

                  newBlock = (Block)ModBlocks.OVERLOAD_CRYSTAL_BLOCK.get();
               }

               level.m_46597_(pos, newBlock.m_49966_());
            }
         }
      }
   }

   public static boolean canClusterGrowAtState(BlockState state) {
      return state.m_60795_() || state.m_60713_(Blocks.f_49990_) && state.m_60819_().m_76186_() == 8;
   }
}
