package com.moakiee.ae2lt.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorUndyingHandler;
import com.moakiee.ae2lt.event.LightningItemTransformationHandler;
import com.moakiee.ae2lt.event.NaturalLightningTransformationHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerLevel.class})
public abstract class ServerLevelMixin {
   @ModifyArg(
      method = {"tickChunk"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
         ordinal = 1
      )
   )
   private Entity ae2lt$markNaturalWeatherLightning(Entity entity) {
      if (entity instanceof LightningBolt lightningBolt) {
         lightningBolt.getPersistentData().m_128379_("ae2lt.natural_weather_lightning", true);
      }

      return entity;
   }

   @ModifyReceiver(
      method = {"tickNonPassenger"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;tick()V"
      )}
   )
   private Entity ae2lt$handleLightningTick(Entity entity) {
      if (entity instanceof LightningBolt lightningBolt) {
         NaturalLightningTransformationHandler.handleLightningTick(lightningBolt);
         LightningItemTransformationHandler.handleLightningTick(lightningBolt);
      }

      return entity;
   }

   @Inject(
      method = {"broadcastEntityEvent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$suppressProtectedCopiedDeathAnimation(Entity entity, byte eventId, CallbackInfo ci) {
      if (eventId == 3 && entity instanceof ServerPlayer player && CelestweaveArmorUndyingHandler.protectBeforeDeathSideEffect(player)) {
         ci.cancel();
      }
   }
}
