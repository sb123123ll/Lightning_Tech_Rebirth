package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.FirmamentConversionCoreBlockEntity;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FirmamentConversionCoreBlock extends Block implements EntityBlock {
   public FirmamentConversionCoreBlock(Properties properties) {
      super(properties);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack stack = player.m_21120_(hand);
      if (stack.m_41619_()) {
         if (level.m_7702_(pos) instanceof FirmamentConversionCoreBlockEntity be) {
            if (!level.m_5776_()) {
               be.extractToPlayer(player);
            }

            return InteractionResult.m_19078_(level.m_5776_());
         } else {
            return InteractionResult.PASS;
         }
      } else if (level.m_7702_(pos) instanceof FirmamentConversionCoreBlockEntity be) {
         return !level.m_5776_() && !be.insertHeldItem(player, hand) ? InteractionResult.PASS : InteractionResult.m_19078_(level.m_5776_());
      } else {
         return InteractionResult.PASS;
      }
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new FirmamentConversionCoreBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      return !level.m_5776_() && blockEntityType == ModBlockEntities.FIRMAMENT_CONVERSION_CORE.get()
         ? (l, p, s, be) -> FirmamentConversionCoreBlockEntity.serverTick(l, p, s, (FirmamentConversionCoreBlockEntity)be)
         : null;
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.m_5776_() && !state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof FirmamentConversionCoreBlockEntity be) {
         be.dropContents(level, pos);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public List<ItemStack> m_49635_(BlockState state, Builder builder) {
      List<ItemStack> drops = new ArrayList<>(super.m_49635_(state, builder));
      BlockEntity blockEntity = (BlockEntity)builder.m_287159_(LootContextParams.f_81462_);
      if (blockEntity instanceof FirmamentConversionCoreBlockEntity be) {
         be.addDrops(drops);
      }

      return drops;
   }
}
