package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.celestweave.FlightSneakMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityFlightSneakMixin {
   @Inject(
      method = {"getFrictionInfluencedSpeed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$useSneakHoverSpeed(float friction, CallbackInfoReturnable<Float> cir) {
      if (this instanceof Player player && FlightSneakMovement.isActive(player)) {
         cir.setReturnValue(FlightSneakMovement.movementSpeed(player.m_6113_()));
      }
   }
}
