package com.moakiee.ae2lt.mixin;

import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockItem.class})
public abstract class BlockItemPhasePlacementMixin {
   @Shadow
   protected abstract boolean m_6652_();

   @Inject(
      method = {"canPlace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$ignorePhaseFlyingPlacers(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
      Player player = context.m_43723_();
      if (PhaseFlightMovementGuard.isPhaseFlightActive(player)) {
         Level level = context.m_43725_();
         BlockPos pos = context.m_8083_();
         if (this.m_6652_() && !state.m_60710_(level, pos)) {
            cir.setReturnValue(false);
         } else {
            VoxelShape localShape = state.m_60742_(level, pos, CollisionContext.m_82750_(player));
            cir.setReturnValue(
               localShape.m_83281_() || level.m_5450_(player, localShape.m_83216_((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_()))
            );
         }
      }
   }
}
