package com.moakiee.ae2lt.event;

import appeng.api.util.DimensionalBlockPos;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.moakiee.ae2lt.block.WrenchDisassemblableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class MultiblockWrenchHandler {
   private MultiblockWrenchHandler() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onWrenchDisassemble(RightClickBlock event) {
      Player player = event.getEntity();
      Level level = event.getLevel();
      BlockPos pos = event.getPos();
      BlockState state = level.m_8055_(pos);
      if (!event.isCanceled()
         && !player.m_5833_()
         && event.getHand() == InteractionHand.MAIN_HAND
         && InteractionUtil.isInAlternateUseMode(player)
         && InteractionUtil.canWrenchDisassemble(event.getItemStack())
         && supportsWrenchDisassembly(state.m_60734_())) {
         event.setCanceled(true);
         if (!Platform.hasPermissions(new DimensionalBlockPos(level, pos), player)) {
            event.setCancellationResult(InteractionResult.FAIL);
         } else {
            event.setCancellationResult(InteractionResult.m_19078_(level.m_5776_()));
            if (level instanceof ServerLevel serverLevel) {
               BlockEntity blockEntity = level.m_7702_(pos);

               for (ItemStack drop : Block.m_49874_(state, serverLevel, pos, blockEntity, player, event.getItemStack())) {
                  player.m_150109_().m_150079_(drop);
               }

               Block block = state.m_60734_();
               block.m_5707_(level, pos, state, player);
               level.m_7471_(pos, false);
               block.m_6786_(level, pos, state);
            }

            level.m_5594_(player, pos, SoundEvents.f_12016_, SoundSource.BLOCKS, 0.7F, 1.0F);
         }
      }
   }

   static boolean supportsWrenchDisassembly(Block block) {
      return block instanceof WrenchDisassemblableBlock;
   }
}
