package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.config.PhaseLockTeleportMode;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PhaseFlightMovementGuard {
   private static final Map<UUID, PhaseFlightMovementGuard.ServerSettings> SERVER_SETTINGS = new ConcurrentHashMap<>();
   private static final Map<UUID, PhaseFlightMovementGuard.BlockedTeleportNotice> LAST_BLOCKED_TELEPORT_NOTICE = new ConcurrentHashMap<>();
   private static final ThreadLocal<IdentityHashMap<Player, Integer>> SELF_MOVEMENT_DEPTH = ThreadLocal.withInitial(IdentityHashMap::new);
   private static final ThreadLocal<ServerPlayer> MAIN_THREAD_PAYLOAD_PLAYER = new ThreadLocal<>();
   private static final ThreadLocal<IdentityHashMap<Player, Integer>> MOVEMENT_POSITION_UPDATE_DEPTH = ThreadLocal.withInitial(IdentityHashMap::new);
   private static final ThreadLocal<IdentityHashMap<Player, Integer>> VANILLA_TRAVEL_MOVEMENT_DEPTH = ThreadLocal.withInitial(IdentityHashMap::new);
   private static final ThreadLocal<IdentityHashMap<Player, Integer>> VANILLA_TRAVEL_SCOPE_DEPTH = ThreadLocal.withInitial(IdentityHashMap::new);
   private static final ThreadLocal<Player> MOVEMENT_PACKET_PLAYER = new ThreadLocal<>();
   private static final ThreadLocal<Player> CUSTOM_PAYLOAD_PLAYER = new ThreadLocal<>();
   private static final ThreadLocal<CommandSourceStack> COMMAND_SOURCE = new ThreadLocal<>();

   private PhaseFlightMovementGuard() {
   }

   public static void updatePhaseFlightState(Player player, boolean phaseModeEnabled, boolean phaseTraversalActive) {
      if (player != null && !player.m_9236_().m_5776_()) {
         SERVER_SETTINGS.compute(player.m_20148_(), (id, current) -> currentFor(player, current).withPhaseFlight(phaseModeEnabled, phaseTraversalActive));
      }
   }

   public static void clearPhaseFlightState(Player player) {
      clearContribution(player, current -> current.withPhaseFlight(false, false));
   }

   public static void updatePhaseLockProtection(Player player, boolean blockForces, boolean blockTeleports) {
      if (player != null && !player.m_9236_().m_5776_()) {
         SERVER_SETTINGS.compute(player.m_20148_(), (id, current) -> currentFor(player, current).withPhaseLockProtection(blockForces, blockTeleports));
         if (!blockTeleports) {
            LAST_BLOCKED_TELEPORT_NOTICE.remove(player.m_20148_());
         }
      }
   }

   public static void clearPhaseLockProtection(Player player) {
      clearContribution(player, current -> current.withPhaseLockProtection(false, false));
      if (player != null) {
         LAST_BLOCKED_TELEPORT_NOTICE.remove(player.m_20148_());
      }
   }

   private static void clearContribution(Player player, UnaryOperator<PhaseFlightMovementGuard.ServerSettings> clearOperation) {
      if (player != null) {
         SERVER_SETTINGS.computeIfPresent(player.m_20148_(), (id, current) -> {
            if (current.owner() != player) {
               return null;
            } else {
               PhaseFlightMovementGuard.ServerSettings updated = clearOperation.apply(current);
               return updated.isEmpty() ? null : updated;
            }
         });
      }
   }

   private static PhaseFlightMovementGuard.ServerSettings currentFor(Player player, PhaseFlightMovementGuard.ServerSettings current) {
      return current != null && current.owner() == player ? current : PhaseFlightMovementGuard.ServerSettings.empty(player);
   }

   public static void clear(Player player) {
      if (player != null) {
         SERVER_SETTINGS.remove(player.m_20148_());
         LAST_BLOCKED_TELEPORT_NOTICE.remove(player.m_20148_());
         SELF_MOVEMENT_DEPTH.get().remove(player);
         if (MAIN_THREAD_PAYLOAD_PLAYER.get() == player) {
            MAIN_THREAD_PAYLOAD_PLAYER.remove();
         }

         MOVEMENT_POSITION_UPDATE_DEPTH.get().remove(player);
         VANILLA_TRAVEL_MOVEMENT_DEPTH.get().remove(player);
         VANILLA_TRAVEL_SCOPE_DEPTH.get().remove(player);
         if (MOVEMENT_PACKET_PLAYER.get() == player) {
            MOVEMENT_PACKET_PLAYER.remove();
         }

         if (CUSTOM_PAYLOAD_PLAYER.get() == player) {
            CUSTOM_PAYLOAD_PLAYER.remove();
         }
      }
   }

   public static boolean blocksExternalForces(Player player) {
      if (player == null) {
         return false;
      } else if (player.m_9236_().m_5776_()) {
         return CelestweaveArmorState.getClientPhaseLockBlockExternalForces();
      } else {
         PhaseFlightMovementGuard.ServerSettings settings = SERVER_SETTINGS.get(player.m_20148_());
         return settings != null && settings.owner() == player && settings.blockForces();
      }
   }

   public static boolean blocksExternalTeleports(Player player) {
      if (player != null && !player.m_9236_().m_5776_()) {
         PhaseFlightMovementGuard.ServerSettings settings = SERVER_SETTINGS.get(player.m_20148_());
         if (settings != null && settings.owner() == player && settings.blockTeleports()) {
            PhaseLockTeleportMode mode = AE2LTCommonConfig.overloadArmorPhaseLockTeleportMode();
            return mode.disablesProtection() ? false : !mode.ignoresPrivilegedCommands() || !isPrivilegedCommandExecution();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isPhaseFlightActive(Player player) {
      if (player == null) {
         return false;
      } else if (player.m_9236_().m_5776_()) {
         return isPhaseModeEnabled(player) && PhaseWingFlight.isFlightActive(player);
      } else {
         PhaseFlightMovementGuard.ServerSettings settings = SERVER_SETTINGS.get(player.m_20148_());
         return settings != null && settings.owner() == player && settings.phaseModeEnabled() && settings.phaseTraversalActive();
      }
   }

   public static boolean isPhaseModeEnabled(Player player) {
      if (player == null) {
         return false;
      } else if (player.m_9236_().m_5776_()) {
         return CelestweaveArmorState.isAnyClientFlightControlActive()
            && CelestweaveArmorState.getClientPhaseMode().allows(PhaseFlightPlayerState.isFlying(player), PhaseWingFlight.isFlightActive(player));
      } else {
         PhaseFlightMovementGuard.ServerSettings settings = SERVER_SETTINGS.get(player.m_20148_());
         return settings != null && settings.owner() == player && settings.phaseModeEnabled();
      }
   }

   public static boolean isSelfMovementAuthorized(Player player) {
      if (player == null) {
         return false;
      } else if (SELF_MOVEMENT_DEPTH.get().getOrDefault(player, Integer.valueOf(0)) > 0) {
         return true;
      } else {
         return consumeVanillaTravelMovement(player) ? true : isCurrentMovementPacket(player);
      }
   }

   public static boolean isSelfTeleportAuthorized(Player player) {
      if (player == null) {
         return false;
      } else if (SELF_MOVEMENT_DEPTH.get().getOrDefault(player, Integer.valueOf(0)) <= 0 && MAIN_THREAD_PAYLOAD_PLAYER.get() != player) {
         CommandSourceStack commandSource = COMMAND_SOURCE.get();
         return commandSource != null && commandSource.m_81373_() == player ? true : isCurrentMovementPacket(player) || isCurrentCustomPayload(player);
      } else {
         return true;
      }
   }

   private static boolean isPrivilegedCommandExecution() {
      CommandSourceStack commandSource = COMMAND_SOURCE.get();
      return commandSource != null && commandSource.m_6761_(2);
   }

   public static void beginMovementPacket(Player player) {
      if (player != null && !player.m_9236_().m_5776_()) {
         MOVEMENT_PACKET_PLAYER.set(player);
      }
   }

   public static void endMovementPacket(Player player) {
      if (MOVEMENT_PACKET_PLAYER.get() == player) {
         MOVEMENT_PACKET_PLAYER.remove();
      }
   }

   public static void beginCustomPayload(Player player) {
      if (player != null && !player.m_9236_().m_5776_()) {
         CUSTOM_PAYLOAD_PLAYER.set(player);
      }
   }

   public static void endCustomPayload(Player player) {
      if (CUSTOM_PAYLOAD_PLAYER.get() == player) {
         CUSTOM_PAYLOAD_PLAYER.remove();
      }
   }

   private static boolean isCurrentMovementPacket(Player player) {
      return player instanceof ServerPlayer && MOVEMENT_PACKET_PLAYER.get() == player;
   }

   private static boolean isCurrentCustomPayload(Player player) {
      return player instanceof ServerPlayer && CUSTOM_PAYLOAD_PLAYER.get() == player;
   }

   public static boolean isMovementPositionUpdate(Player player) {
      return player != null && MOVEMENT_POSITION_UPDATE_DEPTH.get().getOrDefault(player, Integer.valueOf(0)) > 0;
   }

   public static void beginSelfMovement(Player player) {
      if (player != null) {
         SELF_MOVEMENT_DEPTH.get().merge(player, Integer.valueOf(1), Integer::sum);
      }
   }

   public static void endSelfMovement(Player player) {
      if (player != null) {
         IdentityHashMap<Player, Integer> depths = SELF_MOVEMENT_DEPTH.get();
         int next = depths.getOrDefault(player, Integer.valueOf(0)) - 1;
         if (next <= 0) {
            depths.remove(player);
            if (depths.isEmpty()) {
               SELF_MOVEMENT_DEPTH.remove();
            }
         } else {
            depths.put(player, next);
         }
      }
   }

   public static void runAsSelfMovement(Player player, Runnable movement) {
      beginSelfMovement(player);

      try {
         movement.run();
      } finally {
         endSelfMovement(player);
      }
   }

   public static void runAsVanillaTravelMovement(Player player, Runnable movement) {
      if (player == null) {
         movement.run();
      } else {
         IdentityHashMap<Player, Integer> permits = VANILLA_TRAVEL_MOVEMENT_DEPTH.get();
         int previous = permits.getOrDefault(player, Integer.valueOf(0));
         permits.put(player, previous + 1);

         try {
            movement.run();
         } finally {
            if (previous == 0) {
               permits.remove(player);
               if (permits.isEmpty()) {
                  VANILLA_TRAVEL_MOVEMENT_DEPTH.remove();
               }
            } else {
               permits.put(player, previous);
            }
         }
      }
   }

   public static void runInVanillaTravelScope(Player player, Runnable travel) {
      if (player == null) {
         travel.run();
      } else {
         IdentityHashMap<Player, Integer> depths = VANILLA_TRAVEL_SCOPE_DEPTH.get();
         depths.merge(player, Integer.valueOf(1), Integer::sum);

         try {
            travel.run();
         } finally {
            int next = depths.getOrDefault(player, Integer.valueOf(0)) - 1;
            if (next <= 0) {
               depths.remove(player);
               if (depths.isEmpty()) {
                  VANILLA_TRAVEL_SCOPE_DEPTH.remove();
               }
            } else {
               depths.put(player, next);
            }
         }
      }
   }

   public static boolean isVanillaTravelScopeActive(Player player) {
      return player != null && VANILLA_TRAVEL_SCOPE_DEPTH.get().getOrDefault(player, Integer.valueOf(0)) > 0;
   }

   private static boolean consumeVanillaTravelMovement(Player player) {
      IdentityHashMap<Player, Integer> permits = VANILLA_TRAVEL_MOVEMENT_DEPTH.get();
      int remaining = permits.getOrDefault(player, Integer.valueOf(0));
      if (remaining <= 0) {
         return false;
      } else {
         if (remaining == 1) {
            permits.remove(player);
            if (permits.isEmpty()) {
               VANILLA_TRAVEL_MOVEMENT_DEPTH.remove();
            }
         } else {
            permits.put(player, remaining - 1);
         }

         return true;
      }
   }

   public static void runAsMovementPositionUpdate(Player player, Runnable movement) {
      if (player == null) {
         movement.run();
      } else {
         IdentityHashMap<Player, Integer> depths = MOVEMENT_POSITION_UPDATE_DEPTH.get();
         depths.merge(player, Integer.valueOf(1), Integer::sum);

         try {
            movement.run();
         } finally {
            int next = depths.getOrDefault(player, Integer.valueOf(0)) - 1;
            if (next <= 0) {
               depths.remove(player);
               if (depths.isEmpty()) {
                  MOVEMENT_POSITION_UPDATE_DEPTH.remove();
               }
            } else {
               depths.put(player, next);
            }
         }
      }
   }

   public static void runAsPlayerPayloadHandler(ServerPlayer player, Runnable action) {
      ServerPlayer previous = MAIN_THREAD_PAYLOAD_PLAYER.get();
      MAIN_THREAD_PAYLOAD_PLAYER.set(player);

      try {
         action.run();
      } finally {
         if (previous == null) {
            MAIN_THREAD_PAYLOAD_PLAYER.remove();
         } else {
            MAIN_THREAD_PAYLOAD_PLAYER.set(previous);
         }
      }
   }

   public static void runAsCommandExecution(CommandSourceStack source, Runnable action) {
      runAsCommandExecution(source, () -> {
         action.run();
         return null;
      });
   }

   public static <T> T runAsCommandExecution(CommandSourceStack source, Supplier<T> action) {
      CommandSourceStack previous = COMMAND_SOURCE.get();
      COMMAND_SOURCE.set(source);

      Object var3;
      try {
         var3 = action.get();
      } finally {
         if (previous == null) {
            COMMAND_SOURCE.remove();
         } else {
            COMMAND_SOURCE.set(previous);
         }
      }

      return (T)var3;
   }

   public static void notifyBlockedTeleport(ServerPlayer player, Vec3 target) {
      notifyBlockedTeleport(player, player.m_284548_(), target, false);
   }

   public static void notifyBlockedDimensionTeleport(ServerPlayer player, ServerLevel targetLevel, Vec3 target) {
      notifyBlockedTeleport(player, targetLevel, target, true);
   }

   private static void notifyBlockedTeleport(ServerPlayer player, ServerLevel targetLevel, Vec3 target, boolean includeDimension) {
      if (player.f_8906_ != null) {
         String dimension = targetLevel.m_46472_().m_135782_().toString();
         String position = formatPosition(target);
         PhaseFlightMovementGuard.BlockedTeleportNotice notice = new PhaseFlightMovementGuard.BlockedTeleportNotice(dimension, position, includeDimension);
         PhaseFlightMovementGuard.BlockedTeleportNotice previous = LAST_BLOCKED_TELEPORT_NOTICE.put(player.m_20148_(), notice);
         if (!notice.equals(previous)) {
            Component message = includeDimension
               ? Component.m_237110_("ae2lt.celestweave.phase_flight.teleport_blocked.dimension", new Object[]{dimension, position})
               : Component.m_237110_("ae2lt.celestweave.phase_flight.teleport_blocked", new Object[]{position});
            player.m_5661_(message, true);
         }
      }
   }

   private static String formatPosition(Vec3 target) {
      return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", target.f_82479_, target.f_82480_, target.f_82481_);
   }

   private static record BlockedTeleportNotice(String dimension, String position, boolean includeDimension) {
   }

   private static record ServerSettings(Player owner, boolean phaseModeEnabled, boolean phaseTraversalActive, boolean blockForces, boolean blockTeleports) {
      private static PhaseFlightMovementGuard.ServerSettings empty(Player owner) {
         return new PhaseFlightMovementGuard.ServerSettings(owner, false, false, false, false);
      }

      private PhaseFlightMovementGuard.ServerSettings withPhaseFlight(boolean phaseModeEnabled, boolean phaseTraversalActive) {
         return new PhaseFlightMovementGuard.ServerSettings(this.owner, phaseModeEnabled, phaseTraversalActive, this.blockForces, this.blockTeleports);
      }

      private PhaseFlightMovementGuard.ServerSettings withPhaseLockProtection(boolean blockForces, boolean blockTeleports) {
         return new PhaseFlightMovementGuard.ServerSettings(this.owner, this.phaseModeEnabled, this.phaseTraversalActive, blockForces, blockTeleports);
      }

      private boolean isEmpty() {
         return !this.phaseModeEnabled && !this.phaseTraversalActive && !this.blockForces && !this.blockTeleports;
      }
   }
}
