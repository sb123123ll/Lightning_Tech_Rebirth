package com.moakiee.ae2lt.blockentity;

import appeng.api.stacks.AEItemKey;
import com.moakiee.ae2lt.api.pattern.PatternProviderUiProfile;
import com.moakiee.ae2lt.block.OverloadedPatternProviderBlock;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ExtendedOverloadedPatternProviderBlockEntity extends OverloadedPatternProviderBlockEntity implements PatternProviderUiProfile {
   public static final String TITLE_TRANSLATION_KEY = "ae2lt.gui.title.extended_overloaded_pattern_provider";

   public ExtendedOverloadedPatternProviderBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType<?>)ModBlockEntities.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get(), pos, blockState);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ExtendedOverloadedPatternProviderBlockEntity be) {
      OverloadedPatternProviderBlockEntity.serverTick(level, pos, state, be);
   }

   @Override
   public int getTotalPatternCapacity() {
      return ExtendedPatternProviderCapacity.slotsForPages(AE2LTCommonConfig.extendedPatternProviderPages());
   }

   @Override
   public String ae2lt$titleTranslationKey() {
      return "ae2lt.gui.title.extended_overloaded_pattern_provider";
   }

   @Override
   public ItemStack getMainMenuIcon() {
      return new ItemStack((ItemLike)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get());
   }

   @Override
   public AEItemKey getTerminalIcon() {
      return AEItemKey.of((ItemLike)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get());
   }

   @Override
   protected Item getItemFromBlockEntity() {
      return ((OverloadedPatternProviderBlock)ModBlocks.EXTENDED_OVERLOADED_PATTERN_PROVIDER.get()).m_5456_();
   }
}
