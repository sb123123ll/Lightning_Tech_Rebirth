package com.moakiee.ae2lt.mixin.mekanism;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moakiee.ae2lt.integration.mekanism.MekanismArmorIntegration;
import mekanism.api.math.FloatingLong;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"mekanism.common.tile.laser.TileEntityBasicLaser"},
   remap = false
)
public abstract class TileEntityBasicLaserMixin {
   @WrapOperation(
      method = {"onUpdateServer"},
      at = {@At(
         value = "INVOKE",
         target = "Lmekanism/api/math/FloatingLong;timesEqual(Lmekanism/api/math/FloatingLong;)Lmekanism/api/math/FloatingLong;"
      )},
      require = 1
   )
   private FloatingLong ae2lt$chargeCelestweaveFromAbsorbedLaser(
      FloatingLong remainingEnergy, FloatingLong retainedFraction, Operation<FloatingLong> original, @Local LivingEntity target
   ) {
      FloatingLong energyBeforeDissipation = remainingEnergy.copy();
      FloatingLong retainedEnergy = (FloatingLong)original.call(new Object[]{remainingEnergy, retainedFraction});
      MekanismArmorIntegration.absorbLaserEnergy(target, energyBeforeDissipation.subtract(retainedEnergy));
      return retainedEnergy;
   }
}
