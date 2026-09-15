package com.moakiee.ae2lt.mixin.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.widgets.VerticalButtonBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   value = {AEBaseScreen.class},
   remap = false
)
public interface AEBaseScreenAccessor {
   @Accessor("verticalToolbar")
   VerticalButtonBar ae2lt$getVerticalToolbar();

   @Invoker("switchToScreen")
   void ae2lt$switchToScreen(AEBaseScreen<?> var1);
}
