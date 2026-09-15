package com.moakiee.ae2lt.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import com.moakiee.ae2lt.blockentity.PigmeeMentalmathUnitBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class PigmeeMentalmathUnitBlock extends AEBaseEntityBlock<PigmeeMentalmathUnitBlockEntity> {
   public static final DirectionProperty FACING = BlockStateProperties.f_61374_;

   public PigmeeMentalmathUnitBlock() {
      super(metalProps().m_280606_());
      this.m_49959_((BlockState)this.m_49966_().m_61124_(FACING, Direction.NORTH));
   }

   public IOrientationStrategy getOrientationStrategy() {
      return OrientationStrategies.horizontalFacing();
   }
}
