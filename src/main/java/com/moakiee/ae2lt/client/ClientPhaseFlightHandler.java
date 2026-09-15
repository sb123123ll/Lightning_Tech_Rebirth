package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.PhaseFlightInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class ClientPhaseFlightHandler {
   private static boolean lastJumpHeld;
   private static long lastFlightControlGeneration = CelestweaveArmorState.getClientFlightControlGeneration();

   private ClientPhaseFlightHandler() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.START) {
         Minecraft minecraft = Minecraft.m_91087_();
         if (minecraft.f_91074_ != null && event.player == minecraft.f_91074_) {
            LocalPlayer player = minecraft.f_91074_;
            boolean flightModuleActive = CelestweaveArmorState.isAnyClientFlightControlActive();
            if (!flightModuleActive && !PhaseFlightPlayerState.isFlightLocked(player)) {
               PhaseFlightPlayerState.endControl(player);
            } else {
               PhaseFlightPlayerState.activate(player);
            }

            syncJumpInput(minecraft, flightModuleActive);
            PhaseWingFlight.tickThrust(player);
            if (isClientPhaseActive(player)) {
               PhaseFlightSubmodule.applyTransientPhaseState(player);
            } else {
               if (PhaseFlightSubmodule.hasTransientPhaseState(player)) {
                  if (PhaseFlightControlRules.intersectsWorldCollision(player)) {
                     PhaseFlightSubmodule.applyTransientPhaseState(player);
                     return;
                  }

                  PhaseFlightSubmodule.clearTransientPhaseState(player);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      CelestweaveArmorState.clearClientActiveCache();
      resetJumpInputSync();
      PhaseFlightMovementGuard.clear(event.getPlayer());
      PhaseFlightPlayerState.endControl(event.getPlayer());
      if (event.getPlayer() != null && PhaseFlightSubmodule.hasTransientPhaseState(event.getPlayer())) {
         PhaseFlightSubmodule.clearTransientPhaseState(event.getPlayer());
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      CelestweaveArmorState.clearClientActiveCache();
      resetJumpInputSync();
      PhaseFlightMovementGuard.clear(event.getOldPlayer());
      PhaseFlightMovementGuard.clear(event.getNewPlayer());
      PhaseFlightPlayerState.endControl(event.getOldPlayer());
      PhaseFlightPlayerState.endControl(event.getNewPlayer());
      if (PhaseFlightSubmodule.hasTransientPhaseState(event.getOldPlayer())) {
         PhaseFlightSubmodule.clearTransientPhaseState(event.getOldPlayer());
      }

      if (PhaseFlightSubmodule.hasTransientPhaseState(event.getNewPlayer())) {
         PhaseFlightSubmodule.clearTransientPhaseState(event.getNewPlayer());
      }
   }

   private static boolean isClientPhaseActive(Player player) {
      return PhaseFlightMovementGuard.isPhaseFlightActive(player);
   }

   private static void syncJumpInput(Minecraft minecraft, boolean flightModuleActive) {
      boolean jumpHeld = flightModuleActive && minecraft.f_91066_.f_92089_.m_90857_();
      PhaseFlightPlayerState.setJumpHeld(minecraft.f_91074_, jumpHeld);
      long controlGeneration = CelestweaveArmorState.getClientFlightControlGeneration();
      if (PhaseFlightControlRules.shouldSyncJumpInput(jumpHeld, lastJumpHeld, controlGeneration, lastFlightControlGeneration)) {
         lastJumpHeld = jumpHeld;
         lastFlightControlGeneration = controlGeneration;
         NetworkInit.sendToServer(PhaseFlightInputPacket.jump(jumpHeld));
      }
   }

   private static void resetJumpInputSync() {
      lastJumpHeld = false;
      lastFlightControlGeneration = CelestweaveArmorState.getClientFlightControlGeneration();
   }
}
