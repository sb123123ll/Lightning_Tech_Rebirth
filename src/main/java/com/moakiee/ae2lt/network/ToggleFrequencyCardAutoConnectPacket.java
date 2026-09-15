package com.moakiee.ae2lt.network;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.menu.AEBaseMenu;
import com.moakiee.ae2lt.item.FrequencyCardCandidateSelector;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.item.TerminalCardAccess;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public record ToggleFrequencyCardAutoConnectPacket(Optional<InteractionHand> hand, boolean terminalCard) {
   public static ToggleFrequencyCardAutoConnectPacket forHand(InteractionHand hand) {
      return new ToggleFrequencyCardAutoConnectPacket(Optional.of(hand), false);
   }

   public static ToggleFrequencyCardAutoConnectPacket forPreferredCard() {
      return new ToggleFrequencyCardAutoConnectPacket(Optional.empty(), false);
   }

   public static ToggleFrequencyCardAutoConnectPacket forTerminalCard() {
      return new ToggleFrequencyCardAutoConnectPacket(Optional.empty(), true);
   }

   public static ToggleFrequencyCardAutoConnectPacket decode(FriendlyByteBuf buf) {
      boolean terminalCard = buf.readBoolean();
      Optional<InteractionHand> hand = buf.readBoolean() ? Optional.of((InteractionHand)buf.m_130066_(InteractionHand.class)) : Optional.empty();
      return new ToggleFrequencyCardAutoConnectPacket(hand, terminalCard);
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeBoolean(this.terminalCard);
      buf.writeBoolean(this.hand.isPresent());
      this.hand.ifPresent(value -> buf.m_130068_(value));
   }

   public static void handle(ToggleFrequencyCardAutoConnectPacket payload, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            payload.handleOnServer(player);
         }
      });
      ctx.setPacketHandled(true);
   }

   private void handleOnServer(ServerPlayer player) {
      if (this.terminalCard) {
         this.handleTerminalCard(player);
      } else {
         ItemStack stack;
         if (this.hand.isPresent()) {
            stack = player.m_21120_(this.hand.get());
         } else {
            FrequencyCardCandidateSelector.Selection<ItemStack> selection = OverloadedFrequencyCardItem.selectToggleCard(player);
            if (selection.ambiguous()) {
               player.m_5661_(Component.m_237115_("ae2lt.frequency_card.auto_ambiguous").m_130940_(ChatFormatting.RED), true);
               return;
            }

            stack = selection.selected().orElse(ItemStack.f_41583_);
            if (stack.m_41619_()) {
               player.m_5661_(Component.m_237115_("ae2lt.frequency_card.no_toggle_candidate").m_130940_(ChatFormatting.RED), true);
               return;
            }
         }

         if (stack.m_41720_() instanceof OverloadedFrequencyCardItem) {
            OverloadedFrequencyCardData data = OverloadedFrequencyCardItem.getData(stack);
            if (data.isBound() && !data.canBeUsedBy(player.m_20148_())) {
               player.m_5661_(Component.m_237115_("ae2lt.frequency_card.card_owner_mismatch").m_130940_(ChatFormatting.RED), true);
            } else {
               boolean enabled = OverloadedFrequencyCardItem.toggleAutoConnect(stack);
               messageAutoConnectState(player, enabled);
            }
         }
      }
   }

   private void handleTerminalCard(ServerPlayer player) {
      if (!(player.f_36096_ instanceof AEBaseMenu aeMenu) || !aeMenu.m_6875_(player)) {
         player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
         return;
      }

      if (aeMenu.getTarget() instanceof ItemMenuHost terminalHost) {
         IUpgradeInventory upgrades = terminalHost.getUpgrades();
         if (!TerminalCardAccess.hasCard(upgrades)) {
            player.m_5661_(Component.m_237115_("ae2lt.frequency_card.terminal_no_card").m_130940_(ChatFormatting.RED), true);
            return;
         }

         OverloadedFrequencyCardData data = TerminalCardAccess.readCardData(upgrades);
         if (data.isBound() && !data.canBeUsedBy(player.m_20148_())) {
            player.m_5661_(Component.m_237115_("ae2lt.frequency_card.card_owner_mismatch").m_130940_(ChatFormatting.RED), true);
            return;
         }

         if (!TerminalCardAccess.updateCard(upgrades, cardData -> cardData.toggleAutoConnect())) {
            player.m_5661_(Component.m_237115_("ae2lt.frequency_card.terminal_no_card").m_130940_(ChatFormatting.RED), true);
            return;
         }

         messageAutoConnectState(player, TerminalCardAccess.readCardData(upgrades).autoConnect());
         return;
      }

      player.m_5661_(Component.m_237115_("ae2lt.gui.error.rejected").m_130940_(ChatFormatting.RED), true);
   }

   private static void messageAutoConnectState(ServerPlayer player, boolean enabled) {
      player.m_5661_(
         Component.m_237115_(enabled ? "ae2lt.frequency_card.auto_enabled" : "ae2lt.frequency_card.auto_disabled")
            .m_130940_(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
         true
      );
   }
}
