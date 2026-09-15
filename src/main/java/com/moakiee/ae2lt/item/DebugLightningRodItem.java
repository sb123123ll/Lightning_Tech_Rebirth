package com.moakiee.ae2lt.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DebugLightningRodItem extends AE2LTItem {
   public DebugLightningRodItem(Properties properties) {
      super(properties);
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      BlockPos pos = context.m_8083_();
      BlockState state = level.m_8055_(pos);
      if (!state.m_60713_(Blocks.f_152587_)) {
         return InteractionResult.PASS;
      } else {
         if (level instanceof ServerLevel serverLevel) {
            LightningBolt bolt = (LightningBolt)EntityType.f_20465_.m_20615_(serverLevel);
            if (bolt == null) {
               return InteractionResult.FAIL;
            }

            Vec3 target = Vec3.m_82539_(pos.m_7494_());
            bolt.m_6027_(target.f_82479_, target.f_82480_, target.f_82481_);
            if (context.m_43723_() instanceof ServerPlayer serverPlayer) {
               bolt.m_20879_(serverPlayer);
            }

            bolt.getPersistentData().m_128379_("ae2lt.natural_weather_lightning", true);
            serverLevel.m_7967_(bolt);
         }

         Player player = context.m_43723_();
         if (player != null && !player.m_150110_().f_35937_) {
            context.m_43722_().m_41774_(1);
         }

         return InteractionResult.m_19078_(level.m_5776_());
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, AE2LTItem.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
      tooltip.add(Component.m_237115_("item.ae2lt.debug_lightning_rod.tooltip").m_130940_(ChatFormatting.GRAY));
      super.appendHoverText(stack, context, tooltip, tooltipFlag);
   }
}
