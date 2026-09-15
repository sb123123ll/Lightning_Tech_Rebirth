package com.moakiee.ae2lt.client.railgun;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class RailgunCameraShake {
   private static float intensity = 0.0F;
   private static int decayTicks = 0;

   private RailgunCameraShake() {
   }

   public static void clear() {
      intensity = 0.0F;
      decayTicks = 0;
   }

   public static void applyRecoil(float pitchUp, int tierOrdinal) {
      LocalPlayer lp = Minecraft.m_91087_().f_91074_;
      if (lp != null) {
         lp.m_146926_(lp.m_146909_() - pitchUp);
         lp.f_19860_ = lp.m_146909_();
      }

      intensity = Math.max(intensity, 0.3F + (float)tierOrdinal * 0.2F);
      decayTicks = 6 + tierOrdinal * 2;
   }

   @SubscribeEvent
   public static void onCameraAngles(ComputeCameraAngles e) {
      if (!(intensity <= 0.0F)) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         float dx = (float)((rng.nextDouble() - 0.5) * (double)intensity * 4.0);
         float dy = (float)((rng.nextDouble() - 0.5) * (double)intensity * 4.0);
         e.setPitch(e.getPitch() + dx);
         e.setYaw(e.getYaw() + dy);
         intensity *= 0.85F;
         if (--decayTicks <= 0) {
            intensity = 0.0F;
         }
      }
   }
}
