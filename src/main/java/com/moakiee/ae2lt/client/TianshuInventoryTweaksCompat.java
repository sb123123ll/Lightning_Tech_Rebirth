package com.moakiee.ae2lt.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class TianshuInventoryTweaksCompat {
   private TianshuInventoryTweaksCompat() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onMousePressed(Pre event) {
      if (event.getButton() == 2
         && ModList.get().isLoaded("invtweaks")
         && event.getScreen() instanceof TianshuPatternEncodingTermScreen<?> screen
         && screen.m_6375_(event.getMouseX(), event.getMouseY(), event.getButton())) {
         event.setCanceled(true);
      }
   }
}
