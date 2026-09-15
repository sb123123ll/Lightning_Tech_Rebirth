package com.moakiee.ae2lt.mixin;

import appeng.api.networking.pathing.ControllerState;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.pathfinding.ControllerValidator;
import com.moakiee.ae2lt.blockentity.OverloadedControllerBlockEntity;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ControllerValidator.class},
   remap = false
)
public abstract class ControllerValidatorMixin {
   @Inject(
      method = {"calculateState"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void ae2lt$forceMixedConflict(Collection<ControllerBlockEntity> controllers, CallbackInfoReturnable<ControllerState> cir) {
      if (cir.getReturnValue() != ControllerState.NO_CONTROLLER) {
         boolean hasOverloaded = false;
         boolean hasVanilla = false;

         for (ControllerBlockEntity c : controllers) {
            if (c instanceof OverloadedControllerBlockEntity) {
               hasOverloaded = true;
            } else {
               hasVanilla = true;
            }

            if (hasOverloaded && hasVanilla) {
               cir.setReturnValue(ControllerState.CONTROLLER_CONFLICT);
               return;
            }
         }
      }
   }
}
