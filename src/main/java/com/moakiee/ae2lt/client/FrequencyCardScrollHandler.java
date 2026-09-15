package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.ToggleFrequencyCardAutoConnectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class FrequencyCardScrollHandler {
   private FrequencyCardScrollHandler() {
   }

   @SubscribeEvent
   public static void onMouseScroll(MouseScrollingEvent event) {
      if (event.getScrollDelta() != 0.0 && Screen.m_96638_()) {
         LocalPlayer player = Minecraft.m_91087_().f_91074_;
         if (player != null) {
            InteractionHand hand = null;
            if (player.m_21205_().m_41720_() instanceof OverloadedFrequencyCardItem) {
               hand = InteractionHand.MAIN_HAND;
            } else if (player.m_21206_().m_41720_() instanceof OverloadedFrequencyCardItem) {
               hand = InteractionHand.OFF_HAND;
            }

            if (hand != null) {
               NetworkInit.sendToServer(ToggleFrequencyCardAutoConnectPacket.forHand(hand));
               event.setCanceled(true);
            }
         }
      }
   }
}
