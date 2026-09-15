package com.moakiee.ae2lt.client.ctm;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class CtmGeometryLoaders {
   private CtmGeometryLoaders() {
   }

   @SubscribeEvent
   public static void registerGeometryLoaders(RegisterGeometryLoaders event) {
      event.register("connected_texture", new ConnectedTextureLoader());
   }
}
