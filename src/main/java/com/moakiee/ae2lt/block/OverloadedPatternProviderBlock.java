package com.moakiee.ae2lt.block;

import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class OverloadedPatternProviderBlock<T extends OverloadedPatternProviderBlockEntity> extends AE2LTBaseEntityBlock<T> {
   public OverloadedPatternProviderBlock() {
      super(metalProps().m_280606_());
      this.m_49959_((BlockState)this.m_49966_().m_61124_(PatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      super.m_7926_(builder);
      builder.m_61104_(new Property[]{PatternProviderBlock.PUSH_DIRECTION});
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      T be = (T)this.getBlockEntity(level, pos);
      if (be != null) {
         be.getLogic().updateRedstoneState();
         be.onNeighborChanged();
      }
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else if (InteractionUtil.canWrenchRotate(heldItem)) {
         this.setSide(level, pos, hit.m_82434_());
         return InteractionResult.m_19078_(level.m_5776_());
      } else {
         return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (InteractionUtil.isInAlternateUseMode(player)) {
         return InteractionResult.PASS;
      } else {
         T be = (T)this.getBlockEntity(level, pos);
         if (be != null) {
            if (!level.m_5776_()) {
               be.openMenu(player, MenuLocators.forBlockEntity(be));
            }

            return InteractionResult.m_19078_(level.m_5776_());
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public void setSide(Level level, BlockPos pos, Direction facing) {
      BlockState currentState = level.m_8055_(pos);
      Direction pushSide = ((PushDirection)currentState.m_61143_(PatternProviderBlock.PUSH_DIRECTION)).getDirection();
      PushDirection newPushDirection;
      if (pushSide == facing.m_122424_()) {
         newPushDirection = PushDirection.fromDirection(facing);
      } else if (pushSide == facing) {
         newPushDirection = PushDirection.ALL;
      } else if (pushSide == null) {
         newPushDirection = PushDirection.fromDirection(facing.m_122424_());
      } else {
         newPushDirection = PushDirection.fromDirection(Platform.rotateAround(pushSide, facing));
      }

      level.m_46597_(pos, (BlockState)currentState.m_61124_(PatternProviderBlock.PUSH_DIRECTION, newPushDirection));
      T be = (T)this.getBlockEntity(level, pos);
      if (be != null) {
         be.onNeighborChanged();
      }
   }
}
