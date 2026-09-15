package com.moakiee.ae2lt.mixin.thunderbolt.accessor;

import appeng.api.stacks.GenericStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(
   targets = {"net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU"},
   remap = false
)
public interface AdvCraftingCpuAccessor {
   @Invoker("markDirty")
   void ae2lt$markDirty();

   @Invoker("updateOutput")
   void ae2lt$updateOutput(GenericStack var1);
}
