package com.moakiee.ae2lt.client.railgun;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class RailgunClientSessionState {
   private RailgunClientSessionState() {
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      clear();
   }

   @SubscribeEvent
   public static void onLoggingIn(LoggingIn event) {
      clear();
   }

   private static void clear() {
      RailgunBeamInput.reset();
      RailgunBeamRenderClient.reset();
      RailgunArcRenderer.clear();
      RailgunShockwaveRenderer.clear();
      RailgunCameraShake.clear();
   }
}
