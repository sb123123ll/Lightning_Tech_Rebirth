package com.moakiee.ae2lt.mixin.thunderbolt.accessor;

import appeng.api.stacks.AEKey;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   value = {CraftingCpuLogic.class},
   remap = false
)
public interface CraftingCpuLogicAccessor {
   @Accessor("job")
   @Nullable
   ExecutingCraftingJob ae2lt$getJob();

   @Invoker("finishJob")
   void ae2lt$finishJob(boolean var1);

   @Invoker("postChange")
   void ae2lt$postChange(AEKey var1);
}
