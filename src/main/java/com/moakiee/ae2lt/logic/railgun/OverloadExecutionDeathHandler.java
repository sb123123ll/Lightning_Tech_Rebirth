package com.moakiee.ae2lt.logic.railgun;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorUndyingHandler;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class OverloadExecutionDeathHandler {
   private OverloadExecutionDeathHandler() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST,
      receiveCanceled = true
   )
   public static void onLivingDeath(LivingDeathEvent event) {
      if (OverloadExecutionContext.contains(event.getEntity())) {
         if (!CelestweaveArmorUndyingHandler.wasProtectedThisTick(event.getEntity())) {
            event.setCanceled(false);
         }
      }
   }
}
