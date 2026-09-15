package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorUndyingHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityUndyingMixin {
   @Inject(
      method = {"gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$suppressProtectedCopiedDeathGameEvent(GameEvent gameEvent, Entity sourceEntity, CallbackInfo ci) {
      Entity entity = (Entity)this;
      if (gameEvent == GameEvent.f_223707_ && entity instanceof ServerPlayer player && CelestweaveArmorUndyingHandler.protectBeforeDeathSideEffect(player)) {
         ci.cancel();
      }
   }
}
