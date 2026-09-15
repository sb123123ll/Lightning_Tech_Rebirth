package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.blockentity.GhostOutputBlockEntity;
import com.moakiee.ae2lt.logic.EjectModeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class})
public abstract class EjectGhostBEMixin {
   @Inject(
      method = {"getBlockEntity"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ae2lt$injectGhostBE(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
      if (cir.getReturnValue() == null) {
         if (this instanceof ServerLevel) {
            EjectModeRegistry.EjectEntry entry = EjectModeRegistry.lookupAny(((Level)this).m_46472_(), pos.m_121878_());
            if (entry != null) {
               GhostOutputBlockEntity ghost = entry.ghostBE();
               if (ghost.m_58904_() == null) {
                  ghost.m_142339_((Level)this);
               }

               cir.setReturnValue(ghost);
            }
         }
      }
   }
}
