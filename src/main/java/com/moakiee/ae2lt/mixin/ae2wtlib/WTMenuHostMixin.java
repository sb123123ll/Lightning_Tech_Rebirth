package com.moakiee.ae2lt.mixin.ae2wtlib;

import appeng.api.networking.IGridNode;
import com.moakiee.ae2lt.integration.ae2wtlib.WirelessTerminalFrequencyLink;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {WTMenuHost.class},
   remap = false
)
public abstract class WTMenuHostMixin {
   @Shadow
   private boolean rangeCheck;

   @Inject(
      method = {"getActionableNode"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$redirectToFrequencyNode(CallbackInfoReturnable<IGridNode> cir) {
      WirelessTerminalFrequencyLink.Resolution route = this.ae2lt$resolveFrequencyRoute();
      if (route.usesFrequencyRoute()) {
         cir.setReturnValue(route.node());
      }
   }

   @Inject(
      method = {"rangeCheck"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ae2lt$validateFrequencyRange(CallbackInfoReturnable<Boolean> cir) {
      WirelessTerminalFrequencyLink.Resolution route = this.ae2lt$resolveFrequencyRoute();
      if (route.usesFrequencyRoute()) {
         this.rangeCheck = false;
         cir.setReturnValue(route.isNetworkPowered());
      }
   }

   @Unique
   private WirelessTerminalFrequencyLink.Resolution ae2lt$resolveFrequencyRoute() {
      WTMenuHost self = (WTMenuHost)this;
      return WirelessTerminalFrequencyLink.resolveRoute(self.getPlayer(), self.getUpgrades());
   }
}
