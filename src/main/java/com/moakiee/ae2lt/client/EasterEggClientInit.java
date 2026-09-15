package com.moakiee.ae2lt.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class EasterEggClientInit {
   private EasterEggClientInit() {
   }

   @SubscribeEvent
   public static void registerOverlay(RegisterGuiOverlaysEvent event) {
      event.registerAbove(VanillaGuiOverlay.CHAT_PANEL.id(), "easter_egg", EasterEggOverlay.INSTANCE);
   }
}
