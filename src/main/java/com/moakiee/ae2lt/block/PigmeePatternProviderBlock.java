package com.moakiee.ae2lt.block;

import appeng.block.crafting.PushDirection;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.moakiee.ae2lt.blockentity.PigmeePatternProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public final class PigmeePatternProviderBlock extends AE2LTBaseEntityBlock<PigmeePatternProviderBlockEntity> {
   public static final EnumProperty<PushDirection> PUSH_DIRECTION = EnumProperty.m_61587_("push_direction", PushDirection.class);

   public PigmeePatternProviderBlock() {
      super(metalProps().m_280606_());
      this.m_49959_((BlockState)this.m_49966_().m_61124_(PUSH_DIRECTION, PushDirection.ALL));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{PUSH_DIRECTION});
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      if (InteractionUtil.canWrenchRotate(heldItem)) {
         this.setOutputSide(level, pos, hit.m_82434_());
         return InteractionResult.m_19078_(level.m_5776_());
      } else {
         return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      PigmeePatternProviderBlockEntity blockEntity = (PigmeePatternProviderBlockEntity)this.getBlockEntity(level, pos);
      if (blockEntity == null) {
         return InteractionResult.PASS;
      } else {
         if (!level.m_5776_()) {
            blockEntity.openMenu(player, MenuLocators.forBlockEntity(blockEntity));
         }

         return InteractionResult.m_19078_(level.m_5776_());
      }
   }

   private void setOutputSide(Level level, BlockPos pos, Direction clickedSide) {
      BlockState state = level.m_8055_(pos);
      Direction currentSide = ((PushDirection)state.m_61143_(PUSH_DIRECTION)).getDirection();
      PushDirection next;
      if (currentSide == clickedSide.m_122424_()) {
         next = PushDirection.fromDirection(clickedSide);
      } else if (currentSide == clickedSide) {
         next = PushDirection.ALL;
      } else if (currentSide == null) {
         next = PushDirection.fromDirection(clickedSide.m_122424_());
      } else {
         next = PushDirection.fromDirection(Platform.rotateAround(currentSide, clickedSide));
      }

      level.m_46597_(pos, (BlockState)state.m_61124_(PUSH_DIRECTION, next));
   }
}
