package com.moakiee.ae2lt.item;

import appeng.blockentity.AEBaseBlockEntity;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

abstract class AbstractPatternProviderUpgradeItem extends Item {
   protected AbstractPatternProviderUpgradeItem(Properties properties) {
      super(properties);
   }

   public final InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return this.tryUpgrade(context, stack);
   }

   public final InteractionResult m_6225_(UseOnContext context) {
      return this.tryUpgrade(context, context.m_43722_());
   }

   private InteractionResult tryUpgrade(UseOnContext context, ItemStack stack) {
      if (context.m_43723_() == null) {
         return InteractionResult.PASS;
      } else {
         Level level = context.m_43725_();
         BlockPos pos = context.m_8083_();
         if (!this.canUpgrade(level, pos)) {
            return InteractionResult.PASS;
         } else {
            if (!level.m_5776_()) {
               this.upgrade(level, pos, stack);
            }

            return InteractionResult.m_19078_(level.m_5776_());
         }
      }
   }

   public final boolean canUpgrade(Level level, BlockPos pos) {
      BlockState originalState = level.m_8055_(pos);
      BlockEntity originalEntity = level.m_7702_(pos);
      return originalEntity != null && this.isSupportedSource(originalState, originalEntity);
   }

   public final void upgrade(Level level, BlockPos pos, ItemStack stack) {
      if (!level.m_5776_() && this.canUpgrade(level, pos)) {
         BlockEntity originalEntity = level.m_7702_(pos);
         BlockState originalState = level.m_8055_(pos);
         BlockState replacementState = copySharedProperties(originalState, this.replacementBlock().m_49966_());
         BlockEntity replacementEntity = this.createReplacement(pos, replacementState);
         replaceBlockEntity(level, pos, originalEntity, replacementEntity, replacementState);
         stack.m_41774_(1);
      }
   }

   protected abstract boolean isSupportedSource(BlockState var1, BlockEntity var2);

   protected abstract Block replacementBlock();

   protected abstract BlockEntity createReplacement(BlockPos var1, BlockState var2);

   private static void replaceBlockEntity(Level level, BlockPos pos, BlockEntity oldEntity, BlockEntity replacementEntity, BlockState replacementState) {
      CompoundTag savedTag = oldEntity.m_187480_();
      level.m_46747_(pos);
      level.m_7471_(pos, false);
      level.m_7731_(pos, replacementState, 3);
      level.m_151523_(replacementEntity);
      replacementEntity.m_142466_(savedTag);
      if (replacementEntity instanceof AEBaseBlockEntity aeBlockEntity) {
         aeBlockEntity.markForUpdate();
      } else {
         replacementEntity.m_6596_();
      }
   }

   private static BlockState copySharedProperties(BlockState originalState, BlockState replacementState) {
      BlockState state = replacementState;
      UnmodifiableIterator var3 = originalState.m_61148_().entrySet().iterator();

      while (var3.hasNext()) {
         Entry<Property<?>, Comparable<?>> entry = (Entry<Property<?>, Comparable<?>>)var3.next();
         Property property = entry.getKey();
         if (state.m_61138_(property)) {
            state = (BlockState)state.m_61124_(property, entry.getValue());
         }
      }

      return state;
   }
}
