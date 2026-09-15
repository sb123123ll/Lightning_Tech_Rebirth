package com.moakiee.ae2lt.client.compat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class TianshuPolymorphCompatBootstrap {
   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      if (ModList.get().isLoaded("polymorph")) {
         event.enqueueWork(TianshuPolymorphClientCompat::register);
      }
   }

   private TianshuPolymorphCompatBootstrap() {
   }
}
