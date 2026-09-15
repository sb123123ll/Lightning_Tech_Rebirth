package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.ToggleFrequencyCardAutoConnectPacket;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class FrequencyCardKeyMappings {
   private static final String CATEGORY = "key.categories.ae2lt";
   private static final KeyMapping TOGGLE_AUTO_CONNECT = new KeyMapping("key.ae2lt.toggle_frequency_card_auto_connect", -1, "key.categories.ae2lt");

   private FrequencyCardKeyMappings() {
   }

   @SubscribeEvent
   public static void register(RegisterKeyMappingsEvent event) {
      event.register(TOGGLE_AUTO_CONNECT);
   }

   @EventBusSubscriber(
      modid = "ae2lt",
      bus = Bus.FORGE,
      value = {Dist.CLIENT}
   )
   public static final class RuntimeHandler {
      private RuntimeHandler() {
      }

      @SubscribeEvent
      public static void onClientTick(ClientTickEvent event) {
         if (event.phase == Phase.END) {
            while (FrequencyCardKeyMappings.TOGGLE_AUTO_CONNECT.m_90859_()) {
               NetworkInit.sendToServer(ToggleFrequencyCardAutoConnectPacket.forPreferredCard());
            }
         }
      }
   }
}
