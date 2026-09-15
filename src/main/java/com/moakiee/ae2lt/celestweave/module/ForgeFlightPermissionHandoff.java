package com.moakiee.ae2lt.celestweave.module;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class ForgeFlightPermissionHandoff {
   private static final Map<ServerPlayer, ForgeFlightPermissionHandoff.ProbeState> PENDING = new IdentityHashMap<>();

   private ForgeFlightPermissionHandoff() {
   }

   static void beginRelease(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (hasGameModeFlight(serverPlayer)) {
            PENDING.remove(serverPlayer);
         } else {
            serverPlayer.m_150110_().f_35936_ = true;
            PENDING.put(serverPlayer, ForgeFlightPermissionHandoff.ProbeState.WAITING_FOR_NEXT_TICK);
         }
      }
   }

   static void cancelRelease(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         PENDING.remove(serverPlayer);
      }
   }

   static boolean isReleasePending(Player player) {
      if (player instanceof ServerPlayer serverPlayer && PENDING.containsKey(serverPlayer)) {
         return true;
      }

      return false;
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerTickStart(PlayerTickEvent event) {
      if (event.phase == Phase.START && event.player instanceof ServerPlayer player) {
         ForgeFlightPermissionHandoff.ProbeState state = PENDING.get(player);
         if (state == ForgeFlightPermissionHandoff.ProbeState.WAITING_FOR_NEXT_TICK) {
            if (hasGameModeFlight(player)) {
               finishProbe(player);
            } else {
               player.m_150110_().f_35936_ = false;
               PENDING.put(player, ForgeFlightPermissionHandoff.ProbeState.PROBING);
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onPlayerTickEnd(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         if (PENDING.get(player) == ForgeFlightPermissionHandoff.ProbeState.PROBING) {
            finishProbe(player);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PENDING.remove(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      if (event.getOriginal() instanceof ServerPlayer original) {
         PENDING.remove(original);
      }

      if (event.getEntity() instanceof ServerPlayer player) {
         PENDING.remove(player);
      }
   }

   private static void finishProbe(ServerPlayer player) {
      Abilities abilities = player.m_150110_();
      FlightAbilityRestoreRules.Target target = FlightAbilityRestoreRules.targetAfterReleaseProbe(
         hasGameModeFlight(player), abilities.f_35936_, abilities.f_35935_
      );
      PENDING.remove(player);
      abilities.f_35936_ = target.mayfly();
      abilities.f_35935_ = target.flying();
      PhaseLockSubmodule.reconcileFlightLock(player);
      player.m_6885_();
   }

   private static boolean hasGameModeFlight(Player player) {
      return player.m_7500_() || player.m_5833_();
   }

   private static enum ProbeState {
      WAITING_FOR_NEXT_TICK,
      PROBING;
   }
}
