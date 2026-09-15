package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.blockentity.AdvancedWirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.grid.FrequencyDisplayName;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

public record FrequencyCardUsePacket(InteractionHand hand, BlockPos pos, Direction face, double hitX, double hitY, double hitZ, boolean shiftDown) {
   public static FrequencyCardUsePacket decode(FriendlyByteBuf buf) {
      return new FrequencyCardUsePacket(
         (InteractionHand)buf.m_130066_(InteractionHand.class),
         buf.m_130135_(),
         (Direction)buf.m_130066_(Direction.class),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readBoolean()
      );
   }

   public void write(FriendlyByteBuf buf) {
      buf.m_130068_(this.hand);
      buf.m_130064_(this.pos);
      buf.m_130068_(this.face);
      buf.writeDouble(this.hitX);
      buf.writeDouble(this.hitY);
      buf.writeDouble(this.hitZ);
      buf.writeBoolean(this.shiftDown);
   }

   public static void handle(FrequencyCardUsePacket payload, Supplier<Context> context) {
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
      if (player.m_9236_() instanceof ServerLevel level) {
         if (level.m_46749_(this.pos)) {
            if (!(player.m_20275_((double)this.pos.m_123341_() + 0.5, (double)this.pos.m_123342_() + 0.5, (double)this.pos.m_123343_() + 0.5) > 36.0)) {
               ItemStack stack = player.m_21120_(this.hand);
               if (stack.m_41720_() instanceof OverloadedFrequencyCardItem) {
                  BlockEntity be = level.m_7702_(this.pos);
                  if (!this.shiftDown && !player.m_6144_()) {
                     tryLinkWithCard(player, stack, level, this.pos, this.face, new Vec3(this.hitX, this.hitY, this.hitZ));
                  } else {
                     OverloadedFrequencyCardData data = OverloadedFrequencyCardItem.getData(stack);
                     if (data.isBound() && !data.canBeUsedBy(player.m_20148_())) {
                        message(player, "ae2lt.frequency_card.card_owner_mismatch", ChatFormatting.RED);
                     } else {
                        if (be instanceof AdvancedWirelessOverloadedControllerBlockEntity controller) {
                           bindController(player, stack, level, controller);
                        } else if (be instanceof WirelessOverloadedControllerBlockEntity) {
                           message(player, "ae2lt.frequency_card.bind_requires_advanced", ChatFormatting.RED);
                        } else {
                           message(player, "ae2lt.frequency_card.not_advanced_controller", ChatFormatting.RED);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void tryLinkWithCard(ServerPlayer player, ItemStack card, ServerLevel level, BlockPos pos, Direction face, Vec3 hitVec) {
      OverloadedFrequencyCardData data = OverloadedFrequencyCardItem.getData(card);
      if (!data.isBound()) {
         message(player, "ae2lt.frequency_card.unbound_message", ChatFormatting.RED);
      } else if (!data.canBeUsedBy(player.m_20148_())) {
         message(player, "ae2lt.frequency_card.card_owner_mismatch", ChatFormatting.RED);
      } else {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         WirelessFrequency frequency = manager == null ? null : manager.getFrequency(data.frequencyId());
         if (frequency != null && frequency.canPlayerAccess(player, "")) {
            WirelessLinkRegistry.ActionFeedback feedback = WirelessLinkRegistry.get(level.m_7654_())
               .handleManualUse(player, data.frequencyId(), level, pos, face, hitVec);
            player.m_5661_(Component.m_237110_(feedback.translationKey(), feedback.args()).m_130940_(feedback.style()), true);
         } else {
            message(player, "ae2lt.frequency_card.no_frequency_permission", ChatFormatting.RED);
         }
      }
   }

   private static void bindController(ServerPlayer player, ItemStack stack, ServerLevel level, AdvancedWirelessOverloadedControllerBlockEntity controller) {
      int frequencyId = controller.getFrequencyId();
      if (frequencyId <= 0) {
         message(player, "ae2lt.frequency_card.controller_no_frequency", ChatFormatting.RED);
      } else {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         WirelessFrequency frequency = manager == null ? null : manager.getFrequency(frequencyId);
         if (frequency != null && frequency.canPlayerAccess(player, "")) {
            OverloadedFrequencyCardItem.bindFrequency(stack, frequencyId, level.m_46472_(), controller.m_58899_(), player.m_20148_());
            player.m_5661_(
               Component.m_237110_("ae2lt.frequency_card.bound", new Object[]{FrequencyDisplayName.of(frequencyId, frequency.getName())})
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         } else {
            message(player, "ae2lt.frequency_card.no_frequency_permission", ChatFormatting.RED);
         }
      }
   }

   private static void message(ServerPlayer player, String key, ChatFormatting style) {
      player.m_5661_(Component.m_237115_(key).m_130940_(style), true);
   }
}
