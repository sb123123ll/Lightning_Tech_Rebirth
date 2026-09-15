package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.PhaseFlightPlayerState;
import com.moakiee.ae2lt.celestweave.PhaseWingFlight;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import java.util.List;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class PhaseLockSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final PhaseLockSubmodule INSTANCE = new PhaseLockSubmodule();
   public static final String ARMOR_LOCK_CONFIG_KEY = "phase_armor_lock";
   public static final String FLIGHT_LOCK_CONFIG_KEY = "flight_lock";
   public static final String BLOCK_EXTERNAL_FORCES_CONFIG_KEY = "phase_block_external_forces";
   public static final String BLOCK_EXTERNAL_TELEPORTS_CONFIG_KEY = "phase_block_external_teleports";

   private PhaseLockSubmodule() {
   }

   @Override
   public String id() {
      return "phase_lock";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.phase_lock.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.phase_lock.desc";
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
   public void onActivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         updateMovementProtection(player, armor);
         updateFlightLock(player);
      }
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         PhaseFlightMovementGuard.clearPhaseLockProtection(player);
         PhaseFlightPlayerState.setFlightLocked(player, false);
         if (!PhaseWingFlight.canUse(player)) {
            PhaseFlightPlayerState.endControl(player);
         }
      }
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         updateMovementProtection(player, armor);
         updateFlightLock(player);
      }

      return 0;
   }

   @Override
   public List<CelestweaveArmorSubmoduleConfig> getConfigs(ItemStack armor) {
      return List.of(this.armorLockConfig(armor), this.flightLockConfig(armor), this.blockExternalForcesConfig(armor), this.blockExternalTeleportsConfig(armor));
   }

   @Override
   public boolean setConfig(ItemStack armor, String key, @Nullable Tag value) {
      if (!"phase_armor_lock".equals(key)
         && !"flight_lock".equals(key)
         && !"phase_block_external_forces".equals(key)
         && !"phase_block_external_teleports".equals(key)) {
         return false;
      } else {
         CompoundTag options = this.getOptions(armor);
         if (value == null) {
            options.m_128473_(key);
         } else if (value instanceof ByteTag byteTag) {
            options.m_128365_(key, ByteTag.m_128273_(byteTag.m_7063_() != 0));
         } else {
            options.m_128365_(key, ByteTag.m_128273_(defaultValue(key)));
         }

         this.setOptions(armor, options);
         return true;
      }
   }

   public static boolean isArmorLockEnabled(ItemStack armor) {
      return booleanOption(armor, "phase_armor_lock");
   }

   public static boolean isFlightLockEnabled(ItemStack armor) {
      return booleanOption(armor, "flight_lock");
   }

   public static boolean isFlightLockEnabled(Player player) {
      return isFlightLockConfigured(player) && hasFlightSource(player);
   }

   public static boolean hasFlightSource(Player player) {
      return player != null && (player.m_150110_().f_35936_ || ForgeFlightPermissionHandoff.isReleasePending(player) || PhaseWingFlight.canUse(player));
   }

   public static boolean isFlightLockConfigured(Player player) {
      if (player == null) {
         return false;
      } else {
         ItemStack chest = CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.CHEST);
         return !chest.m_41619_() && CelestweaveArmorState.isSubmoduleRuntimeActive(chest, INSTANCE.id()) && isFlightLockEnabled(chest);
      }
   }

   public static boolean blocksExternalForces(ItemStack armor) {
      return booleanOption(armor, "phase_block_external_forces");
   }

   public static boolean blocksExternalTeleports(ItemStack armor) {
      return booleanOption(armor, "phase_block_external_teleports");
   }

   private CelestweaveArmorSubmoduleConfig armorLockConfig(ItemStack armor) {
      return this.booleanConfig("phase_armor_lock", "ae2lt.celestweave.config.phase_armor_lock", isArmorLockEnabled(armor));
   }

   private CelestweaveArmorSubmoduleConfig flightLockConfig(ItemStack armor) {
      return this.booleanConfig("flight_lock", "ae2lt.celestweave.config.flight_lock", isFlightLockEnabled(armor));
   }

   private CelestweaveArmorSubmoduleConfig blockExternalForcesConfig(ItemStack armor) {
      return this.booleanConfig("phase_block_external_forces", "ae2lt.celestweave.config.phase_block_external_forces", blocksExternalForces(armor));
   }

   private CelestweaveArmorSubmoduleConfig blockExternalTeleportsConfig(ItemStack armor) {
      return this.booleanConfig("phase_block_external_teleports", "ae2lt.celestweave.config.phase_block_external_teleports", blocksExternalTeleports(armor));
   }

   private CelestweaveArmorSubmoduleConfig booleanConfig(String key, String translationKey, boolean value) {
      return this.config(
         key, Component.m_237115_(translationKey), ByteTag.m_128273_(value), this.booleanChoices(), Component.m_237115_(translationKey + ".hint")
      );
   }

   private static boolean booleanOption(ItemStack armor, String key) {
      CompoundTag options = INSTANCE.getOptions(armor);
      return options.m_128425_(key, 1) ? options.m_128471_(key) : defaultValue(key);
   }

   private static boolean defaultValue(String key) {
      return switch (key) {
         case "phase_armor_lock", "flight_lock", "phase_block_external_forces", "phase_block_external_teleports" -> true;
         default -> false;
      };
   }

   private static void updateMovementProtection(Player player, ItemStack armor) {
      PhaseFlightMovementGuard.updatePhaseLockProtection(player, blocksExternalForces(armor), blocksExternalTeleports(armor));
   }

   private static void updateFlightLock(Player player) {
      boolean wasFlightLocked = PhaseFlightPlayerState.isFlightLocked(player);
      boolean flightSourceAvailable = hasFlightSource(player);
      boolean flightLockEnabled = isFlightLockEnabled(player);
      if (flightLockEnabled) {
         PhaseFlightPlayerState.activate(player);
      }

      PhaseFlightPlayerState.setFlightLocked(player, flightLockEnabled);
      if (!flightLockEnabled && !PhaseWingFlight.canUse(player)) {
         if (!flightSourceAvailable && PhaseFlightPlayerState.isControlled(player)) {
            PhaseFlightPlayerState.synchronizeFlying(player, false);
         }

         PhaseFlightPlayerState.endControl(player);
      }

      if (wasFlightLocked != flightLockEnabled && player instanceof ServerPlayer serverPlayer) {
         CelestweaveArmorState.syncFlightSettingsToClient(serverPlayer);
      }
   }

   static void reconcileFlightLock(Player player) {
      updateFlightLock(player);
   }
}
