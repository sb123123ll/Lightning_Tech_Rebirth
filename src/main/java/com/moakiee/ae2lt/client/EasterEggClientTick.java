package com.moakiee.ae2lt.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class EasterEggClientTick {
   private EasterEggClientTick() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      EasterEggOverlay.tick();
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      EasterEggOverlay.reset();
   }
}
