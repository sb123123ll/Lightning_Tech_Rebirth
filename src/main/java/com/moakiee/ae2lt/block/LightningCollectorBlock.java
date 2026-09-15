package com.moakiee.ae2lt.block;

import appeng.menu.locator.MenuLocators;
import com.moakiee.ae2lt.blockentity.LightningCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightningCollectorBlock extends AE2LTBaseEntityBlock<LightningCollectorBlockEntity> {
   public static final BooleanProperty WORKING = BooleanProperty.m_61465_("working");
   private static final VoxelShape SHAPE = BlockShapeHelper.or(
      Block.m_49796_(1.0, 0.0, 1.0, 15.0, 12.0, 15.0),
      Block.m_49796_(4.0, 12.0, 4.0, 12.0, 16.0, 12.0),
      Block.m_49796_(6.0, 12.0, 0.0, 10.0, 16.0, 4.0),
      Block.m_49796_(0.0, 12.0, 6.0, 4.0, 16.0, 10.0),
      Block.m_49796_(6.0, 12.0, 12.0, 10.0, 16.0, 16.0),
      Block.m_49796_(12.0, 12.0, 6.0, 16.0, 16.0, 10.0)
   );

   public LightningCollectorBlock() {
      super(metalProps().m_60955_().m_280606_());
      this.m_49959_((BlockState)this.m_49966_().m_61124_(WORKING, false));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{WORKING});
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      LightningCollectorBlockEntity blockEntity = (LightningCollectorBlockEntity)this.getBlockEntity(level, pos);
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
