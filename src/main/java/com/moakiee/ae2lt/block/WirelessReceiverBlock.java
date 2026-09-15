package com.moakiee.ae2lt.block;

import com.moakiee.ae2lt.blockentity.WirelessReceiverBlockEntity;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class WirelessReceiverBlock extends AE2LTBaseEntityBlock<WirelessReceiverBlockEntity> {
   public WirelessReceiverBlock() {
      super(metalProps());
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.m_7702_(pos) instanceof WirelessReceiverBlockEntity be) {
         if (!level.f_46443_ && player instanceof ServerPlayer sp) {
            int freqId = be.getFrequencyId();
            if (freqId > 0) {
               WirelessFrequencyManager manager = WirelessFrequencyManager.get();
               WirelessFrequency freq = manager == null ? null : manager.getFrequency(freqId);
               if (freq != null && !freq.getPlayerAccess(sp).canUse() && freq.getSecurity() != FrequencySecurityLevel.ENCRYPTED) {
                  sp.m_5661_(Component.m_237115_("ae2lt.gui.error.no_access").m_130940_(ChatFormatting.RED), true);
                  return InteractionResult.m_19078_(false);
               }
            }

            NetworkHooks.openScreen(
               sp,
               new SimpleMenuProvider((id, inv, p) -> new FrequencyMenu(id, inv, be), be.m_58900_().m_60734_().m_49954_()),
               buf -> FrequencyMenu.writeExtraData(buf, be, false)
            );
         }

         return InteractionResult.m_19078_(level.f_46443_);
      } else {
         return super.useWithoutItem(state, level, pos, player, hitResult);
      }
   }
}
