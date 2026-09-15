package com.moakiee.ae2lt.integration.emi;

import net.minecraftforge.client.event.ScreenEvent.MouseDragged.Pre;
import net.minecraftforge.common.MinecraftForge;

final class EmiMultiblockInputEvents {
   private static boolean registered;

   static synchronized void register() {
      if (!registered) {
         registered = true;
         MinecraftForge.EVENT_BUS.addListener(EmiMultiblockInputEvents::onMouseDragged);
         MinecraftForge.EVENT_BUS.addListener(EmiMultiblockInputEvents::onMouseScrolled);
      }
   }

   private static void onMouseDragged(Pre event) {
      if (EmiInteractiveMultiblockWidget.routeMouseDragged(
         event.getScreen(), event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY()
      )) {
         event.setCanceled(true);
      }
   }

   private static void onMouseScrolled(net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre event) {
      double delta = event.getScrollDelta();
      if (EmiInteractiveMultiblockWidget.routeMouseScrolled(event.getScreen(), event.getMouseX(), event.getMouseY(), 0.0, delta)) {
         event.setCanceled(true);
      }
   }

   private EmiMultiblockInputEvents() {
   }
}
