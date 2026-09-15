package com.moakiee.ae2lt.network;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingMenuHost;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.item.TerminalCardAccess;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public record OpenFrequencyMenuPacket(boolean cardMode) {
   public static OpenFrequencyMenuPacket forBlock() {
      return new OpenFrequencyMenuPacket(false);
   }

   public static OpenFrequencyMenuPacket forCard() {
      return new OpenFrequencyMenuPacket(true);
   }

   public static void encode(OpenFrequencyMenuPacket pkt, FriendlyByteBuf buf) {
      buf.writeBoolean(pkt.cardMode);
   }

   public static OpenFrequencyMenuPacket decode(FriendlyByteBuf buf) {
      return new OpenFrequencyMenuPacket(buf.readBoolean());
   }

   public static void handle(OpenFrequencyMenuPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (pkt.cardMode) {
               handleCardMode(player);
            } else {
               handleBlockMode(player);
            }
         }
      });
      ctx.setPacketHandled(true);
   }

   private static void handleBlockMode(ServerPlayer player) {
      if (player.f_36096_ instanceof AEBaseMenu parentMenu && parentMenu instanceof FrequencyBindingMenuHost && parentMenu.m_6875_(player)) {
         MenuLocator parentLocator = parentMenu.getLocator();
         if (parentLocator == null) {
            reject(player);
            return;
         }

         FrequencyBindingHost bindingHost = (FrequencyBindingHost)parentLocator.locate(player, FrequencyBindingHost.class);
         if (bindingHost == null) {
            reject(player);
            return;
         }

         int freqId = bindingHost.getFrequencyId();
         if (freqId > 0) {
            WirelessFrequencyManager manager = WirelessFrequencyManager.get();
            WirelessFrequency freq = manager == null ? null : manager.getFrequency(freqId);
            if (freq != null && !freq.getPlayerAccess(player).canUse() && freq.getSecurity() != FrequencySecurityLevel.ENCRYPTED) {
               player.m_5661_(Component.m_237115_("ae2lt.gui.error.no_access").m_130940_(ChatFormatting.RED), true);
               return;
            }
         }

         if (!MenuOpener.open(FrequencyMenu.TYPE, player, parentLocator)) {
            reject(player);
         }

         return;
      }

      reject(player);
   }

   private static void handleCardMode(ServerPlayer player) {
      if (player.f_36096_ instanceof AEBaseMenu aeMenu && aeMenu.getLocator() != null && aeMenu.m_6875_(player)) {
         MenuLocator locator = aeMenu.getLocator();
         ItemMenuHost terminalHost = (ItemMenuHost)locator.locate(player, ItemMenuHost.class);
         if (terminalHost == null) {
            reject(player);
            return;
         }

         ItemStack terminal = terminalHost.getItemStack();
         if (!TerminalCardAccess.hasCard(terminal)) {
            player.m_5661_(Component.m_237115_("ae2lt.frequency_card.terminal_no_card").m_130940_(ChatFormatting.RED), true);
            return;
         }

         if (!MenuOpener.open(FrequencyMenu.TYPE, player, locator)) {
            reject(player);
         }

         return;
      }

      reject(player);
   }

   private static void reject(ServerPlayer player) {
      player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
   }
}
