package com.moakiee.ae2lt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class ShieldHitFeedbackClientState {
   private static final int SUPPRESSION_TICKS = 20;
   private static int suppressEntityId = -1;
   private static int suppressTicks;

   private ShieldHitFeedbackClientState() {
   }

   public static void suppress(int entityId) {
      suppressEntityId = entityId;
      suppressTicks = 20;
      if ((Minecraft.m_91087_().f_91073_ != null ? Minecraft.m_91087_().f_91073_.m_6815_(entityId) : null) instanceof LivingEntity living) {
         clearFeedback(living);
      }
   }

   public static boolean suppressHurtAnimation(Entity entity) {
      if (!shouldSuppress(entity)) {
         return false;
      } else {
         if (entity instanceof LivingEntity living) {
            clearFeedback(living);
         }

         return true;
      }
   }

   public static void clearAfterHealthSync(LocalPlayer player) {
      if (shouldSuppress(player)) {
         clearFeedback(player);
         suppressTicks = Math.min(suppressTicks, 2);
      }
   }

   private static boolean shouldSuppress(Entity entity) {
      return entity != null && suppressTicks > 0 && entity.m_19879_() == suppressEntityId;
   }

   private static void clearFeedback(LivingEntity entity) {
      entity.f_20916_ = 0;
      entity.f_20917_ = 0;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (suppressTicks > 0) {
            suppressTicks--;
            if (suppressTicks <= 0) {
               suppressEntityId = -1;
            }
         }
      }
   }
}
