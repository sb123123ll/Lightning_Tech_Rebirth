package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.ArmorFlightSpeedRules;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightControlRules;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import com.moakiee.ae2lt.celestweave.service.ArmorLightningService;
import com.moakiee.ae2lt.celestweave.service.ArmorResourceFeedback;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class PhaseFlightSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final PhaseFlightSubmodule INSTANCE = new PhaseFlightSubmodule();
   public static final String INERTIA_CONFIG_KEY = "flight_inertia";
   public static final String PHASE_MODE_CONFIG_KEY = "phase_mode";
   private static final String LEGACY_TAG_HAD_MAYFLY = "PhaseHadMayfly";
   private static final String LEGACY_TAG_WAS_FLYING = "PhaseWasFlying";
   private static final String TAG_PREVIOUS_SPEED = "PhasePreviousFlyingSpeed";
   private static final String LEGACY_TAG_HAD_GAME_MODE_FLIGHT = "PhaseHadGameModeFlight";
   private static final String PLAYER_PHASE_TAG = "ae2lt.phase_flight.active";
   private static final String PLAYER_ESCAPE_TICKS_TAG = "ae2lt.phase_flight.escape_ticks";
   private static final float DEFAULT_FLYING_SPEED = 0.05F;
   private static final float SPEED_EPSILON = 1.0E-6F;
   private static final int ESCAPE_PHASE_TICKS = 40;

   private PhaseFlightSubmodule() {
   }

   @Override
   public String id() {
      return "phase_flight";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.phase_flight.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.phase_flight.desc";
   }

   @Override
   public boolean defaultEnabled() {
      return false;
   }

   @Override
   public int getMaxInstallAmount() {
      return 1;
   }

   @Override
   public String installGroupId() {
      return "flight";
   }

   @Override
   public void onActivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         grantPhaseFlight(player, armor);
      }
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         revokePhaseFlight(player, armor);
      }
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         maintainPhaseFlight(player, armor);
         PhaseWingFlight.tickThrust(player);
         return 0;
      } else {
         return 0;
      }
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      return List.of(this.speedConfig(armor), this.inertiaConfig(armor), this.phaseModeConfig(armor));
   }

   @Override
   public boolean setConfig(ItemStack armor, String key, @Nullable Tag value) {
      if ("speed_multiplier".equals(key)) {
         FlightSpeedOption option = FlightSpeedOption.fromTag(value);
         CompoundTag options = this.getOptions(armor);
         options.m_128365_("speed_multiplier", option.toTag());
         this.setOptions(armor, options);
         return true;
      } else if ("flight_inertia".equals(key)) {
         CompoundTag options = this.getOptions(armor);
         options.m_128365_("flight_inertia", value instanceof ByteTag bt ? bt : ByteTag.m_128273_(true));
         this.setOptions(armor, options);
         return true;
      } else if ("phase_mode".equals(key)) {
         PhaseFlightMode mode = PhaseFlightMode.fromTag(value);
         CompoundTag options = this.getOptions(armor);
         options.m_128365_(key, mode.toTag());
         this.setOptions(armor, options);
         return true;
      } else {
         return false;
      }
   }

   public static double phaseSpeed(ItemStack armor) {
      return (double)selectedSpeed(armor).flyingSpeed();
   }

   public static FlightSpeedOption selectedSpeed(ItemStack armor) {
      return INSTANCE.getSelectedSpeed(armor);
   }

   private CelestweaveArmorSubmoduleConfig speedConfig(ItemStack armor) {
      return this.config(
         "speed_multiplier",
         Component.m_237115_("ae2lt.celestweave.config.speed_multiplier"),
         this.getSelectedSpeed(armor).toTag(),
         this.speedChoices(),
         Component.m_237115_("ae2lt.celestweave.config.speed_multiplier.hint")
      );
   }

   private List<CelestweaveArmorSubmoduleConfigChoice> speedChoices() {
      return List.of(
         this.choice(FlightSpeedOption.ONE.toTag(), Component.m_237113_(FlightSpeedOption.ONE.label())),
         this.choice(FlightSpeedOption.TWO.toTag(), Component.m_237113_(FlightSpeedOption.TWO.label())),
         this.choice(FlightSpeedOption.FOUR.toTag(), Component.m_237113_(FlightSpeedOption.FOUR.label()))
      );
   }

   private CelestweaveArmorSubmoduleConfig inertiaConfig(ItemStack armor) {
      return this.config(
         "flight_inertia",
         Component.m_237115_("ae2lt.celestweave.config.flight_inertia"),
         ByteTag.m_128273_(isInertiaEnabled(armor)),
         this.booleanChoices(),
         Component.m_237115_("ae2lt.celestweave.config.flight_inertia.hint")
      );
   }

   public static boolean isInertiaEnabled(ItemStack armor) {
      CompoundTag options = INSTANCE.getOptions(armor);
      return !options.m_128425_("flight_inertia", 1) ? true : options.m_128471_("flight_inertia");
   }

   public static boolean isPhaseModeEnabled(ItemStack armor) {
      return selectedPhaseMode(armor) != PhaseFlightMode.OFF;
   }

   public static PhaseFlightMode selectedPhaseMode(ItemStack armor) {
      return PhaseFlightMode.fromTag(INSTANCE.getOptions(armor).m_128423_("phase_mode"));
   }

   private CelestweaveArmorSubmoduleConfig phaseModeConfig(ItemStack armor) {
      PhaseFlightMode selected = selectedPhaseMode(armor);
      return this.config(
         "phase_mode",
         Component.m_237115_("ae2lt.celestweave.config.phase_mode"),
         selected.toTag(),
         this.phaseModeChoices(),
         Component.m_237115_("ae2lt.celestweave.config.phase_mode.hint")
      );
   }

   private List<CelestweaveArmorSubmoduleConfigChoice> phaseModeChoices() {
      return List.of(
         this.phaseModeChoice(PhaseFlightMode.OFF), this.phaseModeChoice(PhaseFlightMode.CREATIVE_FLIGHT_ONLY), this.phaseModeChoice(PhaseFlightMode.ALL)
      );
   }

   private CelestweaveArmorSubmoduleConfigChoice phaseModeChoice(PhaseFlightMode mode) {
      return this.choice(mode.toTag(), Component.m_237115_("ae2lt.celestweave.config.phase_mode." + mode.id()));
   }

   private static boolean booleanOption(ItemStack armor, String key, boolean defaultValue) {
      CompoundTag options = INSTANCE.getOptions(armor);
      return options.m_128425_(key, 1) ? options.m_128471_(key) : defaultValue;
   }

   private FlightSpeedOption getSelectedSpeed(ItemStack armor) {
      CompoundTag options = this.getOptions(armor);
      return FlightSpeedOption.fromTag(options.m_128423_("speed_multiplier"));
   }

   private static void grantPhaseFlight(Player player, ItemStack armor) {
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
      Abilities abilities = player.m_150110_();
      ForgeFlightPermissionHandoff.cancelRelease(player);
      PhaseFlightPlayerState.activate(player);
      PhaseFlightPlayerState.setFlightLocked(player, PhaseLockSubmodule.isFlightLockEnabled(player));
      updateMovementGuards(player, armor);
      if (!data.m_128425_("PhasePreviousFlyingSpeed", 5)) {
         data.m_128350_("PhasePreviousFlyingSpeed", abilities.m_35942_());
      }

      clearLegacyFlightCapture(data);
      CelestweaveArmorState.setSubmoduleData(armor, INSTANCE, data);
      updateAbilitiesIfChanged(player, true, PhaseFlightPlayerState.isFlying(player), ArmorFlightSpeedRules.activeFlightSpeed(armor));
      updatePhaseTraversal(player, armor);
   }

   private static void maintainPhaseFlight(Player player, ItemStack armor) {
      ForgeFlightPermissionHandoff.cancelRelease(player);
      PhaseFlightPlayerState.activate(player);
      PhaseFlightPlayerState.setFlightLocked(player, PhaseLockSubmodule.isFlightLockEnabled(player));
      updateMovementGuards(player, armor);
      updateAbilitiesIfChanged(player, true, PhaseFlightPlayerState.isFlying(player), ArmorFlightSpeedRules.activeFlightSpeed(armor));
      updatePhaseTraversal(player, armor);
   }

   private static void revokePhaseFlight(Player player, ItemStack armor) {
      PhaseFlightMovementGuard.clearPhaseFlightState(player);
      stopPhaseTraversal(player);
      PhaseFlightPlayerState.setFlightLocked(player, false);
      restoreStoredAbilities(player, armor);
      PhaseFlightPlayerState.endControl(player);
   }

   public static boolean shouldUsePhaseTraversal(Player player, ItemStack armor) {
      return player != null
         && AE2LTCommonConfig.overloadArmorPhaseFlightEnabled()
         && selectedPhaseMode(armor).allows(PhaseFlightPlayerState.isFlying(player), PhaseWingFlight.isFlightActive(player));
   }

   private static void updatePhaseTraversal(Player player, ItemStack armor) {
      if (shouldUsePhaseTraversal(player, armor)) {
         clearEscapePhaseIfPresent(player);
         applyTransientPhaseState(player);
      } else if (player.getPersistentData().m_128451_("ae2lt.phase_flight.escape_ticks") > 0) {
         tickEscapePhase(player, armor);
      } else {
         stopPhaseTraversal(player);
      }
   }

   private static void stopPhaseTraversal(Player player) {
      if (hasTransientPhaseState(player)) {
         if (PhaseFlightControlRules.intersectsWorldCollision(player) && !escapeFromBlocks(player)) {
            beginEscapePhase(player);
         } else {
            clearTransientPhaseState(player);
         }
      }
   }

   private static void restoreStoredAbilities(Player player, ItemStack armor) {
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
      float previousSpeed = data.m_128425_("PhasePreviousFlyingSpeed", 5) ? data.m_128457_("PhasePreviousFlyingSpeed") : 0.05F;
      data.m_128473_("PhasePreviousFlyingSpeed");
      clearLegacyFlightCapture(data);
      CelestweaveArmorState.setSubmoduleData(armor, INSTANCE, data);
      boolean siblingFlightActive = CelestweaveArmorState.isSubmoduleRuntimeActive(armor, FlightSubmodule.INSTANCE.id());
      if (!siblingFlightActive && !player.m_7500_() && !player.m_5833_()) {
         ForgeFlightPermissionHandoff.beginRelease(player);
      } else {
         ForgeFlightPermissionHandoff.cancelRelease(player);
      }

      updateAbilitiesIfChanged(
         player,
         true,
         PhaseFlightPlayerState.isFlying(player),
         siblingFlightActive ? ArmorFlightSpeedRules.activeFlightSpeed(armor) : (previousSpeed > 0.0F ? previousSpeed : 0.05F)
      );
   }

   private static void clearLegacyFlightCapture(CompoundTag data) {
      data.m_128473_("PhaseHadMayfly");
      data.m_128473_("PhaseWasFlying");
      data.m_128473_("PhaseHadGameModeFlight");
   }

   private static boolean escapeFromBlocks(Player player) {
      Level level = player.m_9236_();
      BlockPos origin = player.m_20183_();
      List<BlockPos> preferred = List.of(
         origin,
         origin.m_7494_(),
         origin.m_7495_(),
         origin.m_122012_(),
         origin.m_122019_(),
         origin.m_122029_(),
         origin.m_122024_(),
         origin.m_7494_().m_122012_(),
         origin.m_7494_().m_122019_(),
         origin.m_7494_().m_122029_(),
         origin.m_7494_().m_122024_()
      );

      for (BlockPos candidate : preferred) {
         if (tryTeleportToCollisionFree(player, candidate)) {
            return true;
         }
      }

      for (int radius = 0; radius <= 3; radius++) {
         for (BlockPos candidatex : BlockPos.m_121940_(origin.m_7918_(-radius, -radius, -radius), origin.m_7918_(radius, radius, radius))) {
            if (!preferred.contains(candidatex) && tryTeleportToCollisionFree(player, candidatex)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean tryTeleportToCollisionFree(Player player, BlockPos candidate) {
      Level level = player.m_9236_();
      Vec3 target = Vec3.m_82539_(candidate);
      if (!level.m_45756_(player, player.m_20191_().m_82383_(target.m_82546_(player.m_20182_())))) {
         return false;
      } else {
         PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_6021_(target.f_82479_, target.f_82480_, target.f_82481_));
         return true;
      }
   }

   public static boolean hasTransientPhaseState(Player player) {
      return player.getPersistentData().m_128471_("ae2lt.phase_flight.active");
   }

   public static void applyTransientPhaseState(Player player) {
      player.f_19794_ = true;
      player.m_20242_(!player.m_21255_());
      player.m_6853_(false);
      player.f_19789_ = 0.0F;
      player.getPersistentData().m_128379_("ae2lt.phase_flight.active", true);
   }

   public static void clearTransientPhaseState(Player player) {
      player.f_19794_ = player.m_5833_();
      player.m_20242_(player.m_5833_());
      player.getPersistentData().m_128473_("ae2lt.phase_flight.active");
      clearEscapePhase(player);
   }

   public static boolean tickEscapePhase(Player player, @Nullable ItemStack armor) {
      int ticks = player.getPersistentData().m_128451_("ae2lt.phase_flight.escape_ticks");
      if (ticks <= 0) {
         return false;
      } else if (!PhaseFlightControlRules.intersectsWorldCollision(player)) {
         clearTransientPhaseState(player);
         return false;
      } else {
         if (player instanceof ServerPlayer serverPlayer && !ArmorLightningService.consume(serverPlayer, armor, LightningKey.EXTREME_HIGH_VOLTAGE, 8L)) {
            ArmorResourceFeedback.noExtremeHighVoltage(serverPlayer);
            clearTransientPhaseState(player);
            return false;
         }

         applyTransientPhaseState(player);
         player.getPersistentData().m_128405_("ae2lt.phase_flight.escape_ticks", ticks - 1);
         if (ticks <= 1) {
            clearTransientPhaseState(player);
            return false;
         } else {
            return true;
         }
      }
   }

   private static void beginEscapePhase(Player player) {
      applyTransientPhaseState(player);
      player.getPersistentData().m_128405_("ae2lt.phase_flight.escape_ticks", 40);
   }

   private static void updateMovementGuards(Player player, ItemStack armor) {
      boolean phaseTraversalActive = shouldUsePhaseTraversal(player, armor);
      PhaseFlightMovementGuard.updatePhaseFlightState(player, phaseTraversalActive, phaseTraversalActive);
   }

   private static void clearEscapePhase(Player player) {
      player.getPersistentData().m_128473_("ae2lt.phase_flight.escape_ticks");
   }

   private static void clearEscapePhaseIfPresent(Player player) {
      if (player.getPersistentData().m_128441_("ae2lt.phase_flight.escape_ticks")) {
         clearEscapePhase(player);
      }
   }

   private static boolean updateAbilitiesIfChanged(Player player, boolean mayfly, boolean flying, float desiredSpeed) {
      Abilities abilities = player.m_150110_();
      boolean changed = false;
      if (abilities.f_35936_ != mayfly) {
         abilities.f_35936_ = mayfly;
         changed = true;
      }

      if (abilities.f_35935_ != flying) {
         abilities.f_35935_ = flying;
         changed = true;
      }

      if (Math.abs(abilities.m_35942_() - desiredSpeed) > 1.0E-6F) {
         abilities.m_35943_(desiredSpeed);
         changed = true;
      }

      if (changed) {
         player.m_6885_();
      }

      return changed;
   }
}
