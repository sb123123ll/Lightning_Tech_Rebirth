package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.ArmorFlightSpeedRules;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import java.util.List;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class FlightSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final FlightSubmodule INSTANCE = new FlightSubmodule();
   public static final String INSTALL_GROUP = "flight";
   public static final String INERTIA_CONFIG_KEY = "flight_inertia";
   private static final String LEGACY_TAG_HAD_MAYFLY = "FlightHadMayfly";
   private static final String LEGACY_TAG_WAS_FLYING = "FlightWasFlying";
   private static final String TAG_PREVIOUS_SPEED = "FlightPreviousFlyingSpeed";
   private static final String LEGACY_TAG_HAD_GAME_MODE_FLIGHT = "FlightHadGameModeFlight";
   private static final float SPEED_EPSILON = 1.0E-6F;

   private FlightSubmodule() {
   }

   @Override
   public String id() {
      return "flight";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.flight.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.flight.desc";
   }

   @Override
   public boolean defaultEnabled() {
      return true;
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
         grantFlight(player, armor);
      }
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         revokeFlight(player, armor);
      }
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         maintainFlight(player, armor);
         PhaseWingFlight.tickThrust(player);
      }

      return 0;
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      return List.of(this.speedConfig(armor), this.inertiaConfig(armor));
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
      } else {
         return false;
      }
   }

   public static float flightSpeed(ItemStack armor) {
      return selectedSpeed(armor).flyingSpeed();
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

   private FlightSpeedOption getSelectedSpeed(ItemStack armor) {
      CompoundTag options = this.getOptions(armor);
      return FlightSpeedOption.fromTag(options.m_128423_("speed_multiplier"));
   }

   private static void grantFlight(Player player, ItemStack armor) {
      Abilities abilities = player.m_150110_();
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
      ForgeFlightPermissionHandoff.cancelRelease(player);
      PhaseFlightPlayerState.activate(player);
      PhaseFlightPlayerState.setFlightLocked(player, PhaseLockSubmodule.isFlightLockEnabled(player));
      if (!data.m_128425_("FlightPreviousFlyingSpeed", 5)) {
         data.m_128350_("FlightPreviousFlyingSpeed", abilities.m_35942_());
      }

      clearLegacyFlightCapture(data);
      CelestweaveArmorState.setSubmoduleData(armor, INSTANCE, data);
      updateAbilitiesIfChanged(player, true, PhaseFlightPlayerState.isFlying(player), ArmorFlightSpeedRules.activeFlightSpeed(armor));
   }

   private static void maintainFlight(Player player, ItemStack armor) {
      ForgeFlightPermissionHandoff.cancelRelease(player);
      PhaseFlightPlayerState.activate(player);
      PhaseFlightPlayerState.setFlightLocked(player, PhaseLockSubmodule.isFlightLockEnabled(player));
      updateAbilitiesIfChanged(player, true, PhaseFlightPlayerState.isFlying(player), ArmorFlightSpeedRules.activeFlightSpeed(armor));
   }

   private static void revokeFlight(Player player, ItemStack armor) {
      PhaseFlightPlayerState.setFlightLocked(player, false);
      restoreStoredAbilities(player, armor);
      PhaseFlightPlayerState.endControl(player);
   }

   private static void restoreStoredAbilities(Player player, ItemStack armor) {
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
      float previousSpeed = data.m_128425_("FlightPreviousFlyingSpeed", 5) ? data.m_128457_("FlightPreviousFlyingSpeed") : 0.05F;
      data.m_128473_("FlightPreviousFlyingSpeed");
      clearLegacyFlightCapture(data);
      CelestweaveArmorState.setSubmoduleData(armor, INSTANCE, data);
      boolean siblingFlightActive = CelestweaveArmorState.isSubmoduleRuntimeActive(armor, PhaseFlightSubmodule.INSTANCE.id());
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
      data.m_128473_("FlightHadMayfly");
      data.m_128473_("FlightWasFlying");
      data.m_128473_("FlightHadGameModeFlight");
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
