package com.moakiee.ae2lt.mixin.recipeviewer.emi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   targets = {"appeng.integration.modules.emi.AbstractRecipeHandler$Result"},
   remap = false
)
public interface EmiRecipeTransferResultAccessor {
   @Invoker("canCraft")
   boolean ae2lt$canCraft();
}
