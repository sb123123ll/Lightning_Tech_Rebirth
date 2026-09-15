package com.moakiee.ae2lt.event.railgun;

import com.moakiee.ae2lt.logic.railgun.RailgunRecoilService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class RailgunFallDamageHandler {
   private RailgunFallDamageHandler() {
   }

   @SubscribeEvent
   public static void onFall(LivingFallEvent e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         if (RailgunRecoilService.inRecoilGrace(p)) {
            e.setCanceled(true);
         }
      }
   }
}
