package com.moakiee.ae2lt.mixin.ae2wtlib.client;

import appeng.client.gui.AEBaseScreen;
import com.moakiee.ae2lt.client.ae2wtlib.FrequencyTerminalButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AEBaseScreen.class})
public abstract class AEBaseScreenFrequencyTerminalButtonMixin {
   @Unique
   private boolean ae2lt$frequencyTerminalButtonAdded;
   @Unique
   private FrequencyTerminalButton.ToolbarButtons ae2lt$frequencyTerminalButtons;

   @Inject(
      method = {"init"},
      at = {@At(
         value = "INVOKE",
         target = "Lappeng/client/gui/WidgetContainer;populateScreen(Ljava/util/function/Consumer;Lnet/minecraft/client/renderer/Rect2i;Lappeng/client/gui/AEBaseScreen;)V",
         remap = false
      )},
      require = 1
   )
   private void ae2lt$addFrequencyTerminalButton(CallbackInfo ci) {
      if (!this.ae2lt$frequencyTerminalButtonAdded) {
         AEBaseScreen<?> screen = (AEBaseScreen<?>)this;
         if (FrequencyTerminalButton.shouldInject(screen)) {
            this.ae2lt$frequencyTerminalButtons = FrequencyTerminalButton.addToToolbar(screen);
            this.ae2lt$frequencyTerminalButtonAdded = true;
         }
      }
   }

   @Inject(
      method = {"updateBeforeRender"},
      at = {@At("TAIL")},
      require = 1,
      remap = false
   )
   private void ae2lt$updateFrequencyTerminalButtons(CallbackInfo ci) {
      AEBaseScreen<?> screen = (AEBaseScreen<?>)this;
      if (this.ae2lt$frequencyTerminalButtons != null) {
         this.ae2lt$frequencyTerminalButtons.update(screen);
      }
   }
}
