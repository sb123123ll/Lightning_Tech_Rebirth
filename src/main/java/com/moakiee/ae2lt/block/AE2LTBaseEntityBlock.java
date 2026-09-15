package com.moakiee.ae2lt.block;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class AE2LTBaseEntityBlock<T extends AEBaseBlockEntity> extends AEBaseEntityBlock<T> {
   protected AE2LTBaseEntityBlock(Properties properties) {
      super(properties);
   }

   public final InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand, @Nullable ItemStack heldItem, BlockHitResult hit) {
      BlockState state = level.m_8055_(pos);
      ItemStack stack = heldItem == null ? ItemStack.f_41583_ : heldItem;
      if (!stack.m_41619_()) {
         InteractionResult itemResult = this.useItemOn(stack, state, level, pos, player, hand, hit);
         if (itemResult != null) {
            return itemResult;
         }
      }

      return this.useWithoutItem(state, level, pos, player, hit);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      return InteractionResult.PASS;
   }

   @Nullable
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return null;
   }
}
