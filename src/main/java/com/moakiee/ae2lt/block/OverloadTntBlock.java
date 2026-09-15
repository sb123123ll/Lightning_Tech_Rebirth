package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.entity.OverloadTntEntity;
import com.moakiee.ae2lt.logic.advancement.ProgressionAdvancementService;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class OverloadTntBlock extends TntBlock {
   public OverloadTntBlock(Properties properties) {
      super(properties);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      ItemStack stack = player.m_21120_(hand);
      if (!stack.m_150930_(Items.f_42409_) && !stack.m_150930_(Items.f_42613_)) {
         return super.m_6227_(state, level, pos, player, hand, hitResult);
      } else {
         prime(level, pos, player);
         level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 11);
         if (!player.m_150110_().f_35937_) {
            if (stack.m_150930_(Items.f_42409_)) {
               stack.m_41622_(1, player, brokenPlayer -> brokenPlayer.m_21190_(hand));
            } else {
               stack.m_41774_(1);
            }
         }

         player.m_36246_(Stats.f_12982_.m_12902_(stack.m_41720_()));
         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   public void m_6807_(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      if (!oldState.m_60713_(state.m_60734_())) {
         if (level.m_276867_(pos)) {
            prime(level, pos, null);
            level.m_7471_(pos, false);
         }
      }
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
      if (level.m_276867_(pos)) {
         prime(level, pos, null);
         level.m_7471_(pos, false);
      }
   }

   public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.m_5776_() && !player.m_7500_() && (Boolean)state.m_61143_(f_57419_)) {
         prime(level, pos, player);
      }

      super.m_5707_(level, pos, state, player);
   }

   public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
      prime(level, pos, igniter);
      level.m_7471_(pos, false);
   }

   public void m_7592_(Level level, BlockPos pos, Explosion explosion) {
      if (!level.f_46443_) {
         LivingEntity source = explosion.m_252906_();
         LivingEntity owner = source instanceof LivingEntity ? source : null;
         OverloadTntEntity tnt = new OverloadTntEntity(level, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, owner);
         int fuse = tnt.m_32100_();
         tnt.m_32085_((short)(level.f_46441_.m_188503_(fuse / 4) + fuse / 8));
         level.m_7967_(tnt);
      }
   }

   public boolean m_6903_(Explosion explosion) {
      return false;
   }

   private static void prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
      if (!level.f_46443_) {
         OverloadTntEntity tnt = new OverloadTntEntity(level, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, igniter);
         level.m_7967_(tnt);
         if (igniter instanceof ServerPlayer player) {
            ProgressionAdvancementService.awardOverloadTntIgnited(player);
         }

         level.m_6263_(null, tnt.m_20185_(), tnt.m_20186_(), tnt.m_20189_(), SoundEvents.f_12512_, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.m_142346_(igniter, GameEvent.f_157776_, pos);
      }
   }
}
