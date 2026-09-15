package com.moakiee.ae2lt.network;

import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.TerminalCardAccess;
import com.moakiee.ae2lt.menu.FrequencyMenu;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent.Context;

public record SelectFrequencyPacket(int token, BlockPos blockPos, int frequencyId, String password) {
   public static void encode(SelectFrequencyPacket pkt, FriendlyByteBuf buf) {
      buf.m_130130_(pkt.token);
      buf.m_130064_(pkt.blockPos);
      buf.writeInt(pkt.frequencyId);
      buf.m_130072_(pkt.password, 16);
   }

   public static SelectFrequencyPacket decode(FriendlyByteBuf buf) {
      return new SelectFrequencyPacket(buf.m_130242_(), buf.m_130135_(), buf.readInt(), buf.m_130136_(16));
   }

   public static void handle(SelectFrequencyPacket pkt, Supplier<Context> ctxSupplier) {
      Context ctx = ctxSupplier.get();
      ctx.enqueueWork(
         () -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
               FrequencyMenu menu = FrequencyMenu.validateToken(player, pkt.token);
               if (menu == null) {
                  NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
               } else if (menu.isCardMode()) {
                  handleCardSelect(player, menu, pkt.frequencyId, pkt.password);
               } else if (!menu.getBlockPos().equals(pkt.blockPos)) {
                  NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
               } else {
                  ServerLevel level = player.m_284548_();
                  BlockEntity be = level.m_7702_(pkt.blockPos);
                  int currentFreqId;
                  if (be instanceof WirelessOverloadedControllerBlockEntity ctrl) {
                     currentFreqId = ctrl.getFrequencyId();
                  } else {
                     if (!(be instanceof FrequencyBindingHost bindingHost)) {
                        NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
                        return;
                     }

                     currentFreqId = bindingHost.getFrequencyId();
                  }

                  WirelessFrequencyManager manager = WirelessFrequencyManager.get();
                  if (manager != null) {
                     boolean changingBinding = pkt.frequencyId != currentFreqId;
                     if (changingBinding && currentFreqId > 0) {
                        WirelessFrequency currentFreq = manager.getFrequency(currentFreqId);
                        if (currentFreq != null && !currentFreq.canPlayerAccess(player, "")) {
                           NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                           return;
                        }
                     }

                     if (pkt.frequencyId <= 0) {
                        if (be instanceof WirelessOverloadedControllerBlockEntity ctrl) {
                           ctrl.clearFrequency();
                        } else {
                           ((FrequencyBindingHost)be).clearFrequency();
                        }
                     } else {
                        WirelessFrequency freq = manager.getFrequency(pkt.frequencyId);
                        if (freq == null) {
                           NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(3));
                        } else if (freq.canPlayerAccess(player, pkt.password)) {
                           if (!freq.isMember(player) && freq.enrollAsUser(player)) {
                              manager.markModified();
                              SyncFrequencyDetailPacket.broadcastMembersTo(player.m_20194_(), pkt.frequencyId);
                           }

                           if (be instanceof WirelessOverloadedControllerBlockEntity
                              && !manager.canRegisterTransmitter(pkt.frequencyId, level.m_46472_(), pkt.blockPos)) {
                              NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(5));
                           } else {
                              if (be instanceof WirelessOverloadedControllerBlockEntity ctrl) {
                                 ctrl.setFrequency(pkt.frequencyId);
                              } else {
                                 ((FrequencyBindingHost)be).setFrequency(pkt.frequencyId);
                              }
                           }
                        } else {
                           if (freq.getSecurity() == FrequencySecurityLevel.ENCRYPTED && !freq.getPlayerAccess(player).canUse() && pkt.password.isBlank()) {
                              NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(1));
                           } else if (freq.getSecurity() == FrequencySecurityLevel.ENCRYPTED && !freq.getPlayerAccess(player).canUse()) {
                              NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
                           } else {
                              NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                           }
                        }
                     }
                  }
               }
            }
         }
      );
      ctx.setPacketHandled(true);
   }

   private static void handleCardSelect(ServerPlayer player, FrequencyMenu menu, int targetFreqId, String password) {
      ItemStack terminal = menu.resolveTerminalStack();
      if (!TerminalCardAccess.hasCard(terminal)) {
         NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
      } else {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager != null) {
            int currentFreqId = TerminalCardAccess.readCardData(terminal).frequencyId();
            boolean changingBinding = targetFreqId != currentFreqId;
            if (changingBinding && currentFreqId > 0) {
               WirelessFrequency currentFreq = manager.getFrequency(currentFreqId);
               if (currentFreq != null && !currentFreq.canPlayerAccess(player, "")) {
                  NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                  return;
               }
            }

            if (targetFreqId <= 0) {
               TerminalCardAccess.updateCard(terminal, OverloadedFrequencyCardData::clearFrequency);
            } else {
               WirelessFrequency freq = manager.getFrequency(targetFreqId);
               if (freq == null) {
                  NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(3));
               } else if (freq.canPlayerAccess(player, password)) {
                  if (!freq.isMember(player) && freq.enrollAsUser(player)) {
                     manager.markModified();
                     SyncFrequencyDetailPacket.broadcastMembersTo(player.m_20194_(), targetFreqId);
                  }

                  TerminalCardAccess.updateCard(
                     terminal,
                     data -> data.bindFrequency(
                           targetFreqId, player.m_9236_().m_46472_().m_135782_().toString(), player.m_20183_().m_121878_(), player.m_20148_()
                        )
                  );
               } else {
                  if (freq.getSecurity() == FrequencySecurityLevel.ENCRYPTED && !freq.getPlayerAccess(player).canUse() && password.isBlank()) {
                     NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(1));
                  } else if (freq.getSecurity() == FrequencySecurityLevel.ENCRYPTED && !freq.getPlayerAccess(player).canUse()) {
                     NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(4));
                  } else {
                     NetworkInit.sendToPlayer(player, new FrequencyResponsePacket(2));
                  }
               }
            }
         }
      }
   }
}
