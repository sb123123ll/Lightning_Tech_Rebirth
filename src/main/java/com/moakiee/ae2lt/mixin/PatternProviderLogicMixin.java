package com.moakiee.ae2lt.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moakiee.ae2lt.logic.OverloadedPatternProviderLogic;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PatternProviderLogic.class})
public abstract class PatternProviderLogicMixin {
   @WrapOperation(
      method = {"updatePatterns"},
      at = {@At(
         value = "INVOKE",
         target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"
      )},
      remap = false
   )
   @Nullable
   private IPatternDetails ae2lt$rejectOverloadPatternsInNormalProviders(ItemStack stack, Level level, Operation<IPatternDetails> original) {
      IPatternDetails details = (IPatternDetails)original.call(new Object[]{stack, level});
      return details instanceof OverloadedProviderOnlyPatternDetails ? null : details;
   }

   @Inject(
      method = {"onStackReturnedToNetwork"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ae2lt$handleOverloadUnlockMatching(GenericStack genericStack, CallbackInfo ci) {
      if (this instanceof OverloadedPatternProviderLogic overloadedLogic && overloadedLogic.handleOverloadUnlockOnReturnedStack(genericStack)) {
         ci.cancel();
      }
   }
}
