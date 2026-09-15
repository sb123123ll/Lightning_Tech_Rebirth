package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class ClientFlightInertiaHandler {
   private static final double INERTIA_OFF_DECAY = 0.1;

   private ClientFlightInertiaHandler() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.START) {
         Minecraft minecraft = Minecraft.m_91087_();
         if (minecraft.f_91074_ != null && event.player == minecraft.f_91074_) {
            LocalPlayer player = minecraft.f_91074_;
            if (player.m_150110_().f_35935_) {
               if (!CelestweaveArmorState.getClientFlightInertia()) {
                  Input input = player.f_108618_;
                  Vec3 motion = player.m_20184_();
                  double x = motion.f_82479_;
                  double y = motion.f_82480_;
                  double z = motion.f_82481_;
                  boolean anyHorizontalInput = input.f_108567_ != 0.0F || input.f_108566_ != 0.0F;
                  if (!anyHorizontalInput) {
                     x *= 0.1;
                     z *= 0.1;
                  }

                  boolean verticalInput = minecraft.f_91066_.f_92089_.m_90857_() || minecraft.f_91066_.f_92090_.m_90857_();
                  if (!verticalInput) {
                     y *= 0.1;
                  }

                  if (x != motion.f_82479_ || y != motion.f_82480_ || z != motion.f_82481_) {
                     boolean wasNoPhysics = player.f_19794_;
                     if (wasNoPhysics) {
                        player.f_19794_ = false;
                     }

                     Vec3 adjusted = new Vec3(x, y, z);
                     PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_20256_(adjusted));
                     if (wasNoPhysics) {
                        player.f_19794_ = true;
                     }
                  }
               }
            }
         }
      }
   }
}
