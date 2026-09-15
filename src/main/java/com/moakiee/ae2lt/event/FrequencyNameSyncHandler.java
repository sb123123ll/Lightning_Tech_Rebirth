package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.SyncFrequencyListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class FrequencyNameSyncHandler {
   private FrequencyNameSyncHandler() {
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof FakePlayer)) {
         NetworkInit.sendToPlayer(player, SyncFrequencyListPacket.fromServer());
      }
   }
}
