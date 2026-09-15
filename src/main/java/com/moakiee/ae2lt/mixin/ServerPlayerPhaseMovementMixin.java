package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerPhaseMovementMixin {
   @Inject(
      method = {"changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ae2lt$blockExternalPhaseDimensionChange(ServerLevel destination, ITeleporter teleporter, CallbackInfoReturnable<Entity> cir) {
      ServerPlayer player = (ServerPlayer)this;
      if (PhaseFlightMovementGuard.blocksExternalTeleports(player) && !PhaseFlightMovementGuard.isSelfTeleportAuthorized(player)) {
         PhaseFlightMovementGuard.notifyBlockedDimensionTeleport(player, destination, player.m_20182_());
         cir.setReturnValue(null);
      }
   }
}
