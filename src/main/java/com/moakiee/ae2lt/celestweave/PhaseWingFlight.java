package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PhaseWingFlight {
   private static final double FIREWORK_FORWARD_ACCELERATION = 0.1;
   private static final double FIREWORK_TARGET_SPEED = 1.5;
   private static final double FIREWORK_STEERING = 0.5;

   private PhaseWingFlight() {
   }

   public static boolean canUse(Player player) {
      if (player == null) {
         return false;
      } else if (player.m_9236_().m_5776_() && player.m_7578_()) {
         return CelestweaveArmorState.isAnyClientFlightControlActive();
      } else {
         for (ArmorCapabilityCollector.ActiveCapability active : ArmorCapabilityCollector.collectPerInstalledStack(player)) {
            if (active.capability() instanceof DeviceCapability.ElytraFlight) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean canElytraFly(LivingEntity entity) {
      if (entity instanceof Player player) {
         return player.m_9236_().m_5776_() && !player.m_7578_() ? player.m_21255_() : canUse(player) && !PhaseFlightPlayerState.isFlying(player);
      } else {
         return false;
      }
   }

   public static boolean elytraFlightTick(LivingEntity entity) {
      return canElytraFly(entity);
   }

   public static boolean isFlightActive(Player player) {
      return player != null && (PhaseFlightPlayerState.isFlying(player) || player.m_21255_() && canUse(player));
   }

   public static void tickThrust(Player player) {
      if (player != null && player.m_21255_() && PhaseFlightPlayerState.isJumpHeld(player) && canUse(player)) {
         double speedMultiplier = thrustMultiplier(player.m_150110_().m_35942_());
         Vec3 target = fireworkThrust(player.m_20184_(), player.m_20154_(), speedMultiplier);
         PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_20256_(target));
         player.f_19864_ = true;
      }
   }

   static double thrustMultiplier(float flyingSpeed) {
      return Math.max(1.0, (double)(flyingSpeed / 0.05F));
   }

   static Vec3 fireworkThrust(Vec3 motion, Vec3 look, double speedMultiplier) {
      double forwardAcceleration = 0.1 * speedMultiplier;
      double targetSpeed = 1.5 * speedMultiplier;
      return motion.m_82520_(
         look.f_82479_ * forwardAcceleration + (look.f_82479_ * targetSpeed - motion.f_82479_) * 0.5,
         look.f_82480_ * forwardAcceleration + (look.f_82480_ * targetSpeed - motion.f_82480_) * 0.5,
         look.f_82481_ * forwardAcceleration + (look.f_82481_ * targetSpeed - motion.f_82481_) * 0.5
      );
   }
}
