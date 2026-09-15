package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorUndyingHandler;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerGamePacketListenerImpl.class})
public abstract class ServerCommonPacketListenerUndyingMixin {
   @Inject(
      method = {"send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$suppressProtectedPlayerDeathPacket(Packet<?> packet, PacketSendListener sendListener, CallbackInfo ci) {
      if (packet instanceof ClientboundPlayerCombatKillPacket
         && this instanceof ServerGamePacketListenerImpl gameListener
         && CelestweaveArmorUndyingHandler.protectBeforeDeathSideEffect(gameListener.f_9743_)) {
         ci.cancel();
      }
   }
}
