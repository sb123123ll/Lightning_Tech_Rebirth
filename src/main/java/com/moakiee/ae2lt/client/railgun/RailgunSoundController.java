package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class RailgunSoundController {
   private static RailgunBeamLoopSound beamSound;
   private static RailgunChargeRampSound chargeRampSound;
   private static RailgunChargeLoopSound chargeSound;

   private RailgunSoundController() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent e) {
      if (e.phase == Phase.END) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ != null && e.player == mc.f_91074_) {
            LocalPlayer player = mc.f_91074_;
            ItemStack localRailgun = localRailgunStack(player);
            boolean beamFiring = RailgunBeamRenderClient.isLocalFiring() && !localRailgun.m_41619_() && RailgunSettings.soundEnabled(localRailgun);
            if (beamFiring) {
               if (beamSound == null || beamSound.m_7801_()) {
                  beamSound = new RailgunBeamLoopSound(player);
                  mc.m_91106_().m_120367_(beamSound);
               }
            } else if (beamSound != null) {
               beamSound.requestStop();
               if (beamSound.m_7801_()) {
                  beamSound = null;
               }
            }

            ItemStack using = player.m_21211_();
            boolean charging = player.m_6117_() && using.m_41720_() instanceof ElectromagneticRailgunItem && RailgunSettings.soundEnabled(using);
            if (charging) {
               long chargeTicks = ModDataComponents.RAILGUN_CHARGE_TICKS.getOrDefault(using, 0L);
               RailgunChargeSoundPhase phase = RailgunChargeSoundPhase.fromChargeTicks(chargeTicks, 40L);
               if (phase == RailgunChargeSoundPhase.RAMP) {
                  if (chargeSound != null) {
                     chargeSound.requestStop();
                  }

                  if (chargeRampSound == null || chargeRampSound.m_7801_()) {
                     chargeRampSound = new RailgunChargeRampSound(player);
                     mc.m_91106_().m_120367_(chargeRampSound);
                  }
               } else if (phase == RailgunChargeSoundPhase.SUSTAIN) {
                  if (chargeRampSound != null) {
                     chargeRampSound.requestStop();
                  }

                  if (chargeSound == null || chargeSound.m_7801_()) {
                     chargeSound = new RailgunChargeLoopSound(player);
                     mc.m_91106_().m_120367_(chargeSound);
                  }
               }
            } else {
               if (chargeRampSound != null) {
                  chargeRampSound.requestStop();
                  if (chargeRampSound.m_7801_()) {
                     chargeRampSound = null;
                  }
               }

               if (chargeSound != null) {
                  chargeSound.requestStop();
                  if (chargeSound.m_7801_()) {
                     chargeSound = null;
                  }
               }
            }
         }
      }
   }

   private static ItemStack localRailgunStack(LocalPlayer player) {
      ItemStack main = player.m_21205_();
      if (main.m_41720_() instanceof ElectromagneticRailgunItem) {
         return main;
      } else {
         ItemStack offhand = player.m_21206_();
         return offhand.m_41720_() instanceof ElectromagneticRailgunItem ? offhand : ItemStack.f_41583_;
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut e) {
      forceStopAll();
   }

   @SubscribeEvent
   public static void onLogin(LoggingIn e) {
      forceStopAll();
   }

   private static void forceStopAll() {
      if (beamSound != null) {
         beamSound.requestStop();
         beamSound = null;
      }

      if (chargeRampSound != null) {
         chargeRampSound.requestStop();
         chargeRampSound = null;
      }

      if (chargeSound != null) {
         chargeSound.requestStop();
         chargeSound = null;
      }
   }
}
