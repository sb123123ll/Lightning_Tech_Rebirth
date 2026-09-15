package com.moakiee.ae2lt.mixin.thunderbolt.accessor;

import appeng.api.stacks.AEKeyType;
import appeng.crafting.execution.ElapsedTimeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   value = {ElapsedTimeTracker.class},
   remap = false
)
public interface ElapsedTimeTrackerAccessor {
   @Invoker("decrementItems")
   void ae2lt$decrementItems(long var1, AEKeyType var3);

   @Invoker("addMaxItems")
   void ae2lt$addMaxItems(long var1, AEKeyType var3);
}
