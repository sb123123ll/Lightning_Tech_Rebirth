package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmodule;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmoduleItem;
import com.moakiee.ae2lt.celestweave.module.FlightSubmodule;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightMode;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import com.moakiee.ae2lt.celestweave.module.PhaseLockSubmodule;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.celestweave.state.ArmorPersistentData;
import com.moakiee.ae2lt.celestweave.state.ArmorRuntimeRegistry;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import com.moakiee.ae2lt.network.CelestweaveSubmoduleActivePacket;
import com.moakiee.ae2lt.network.FlightInertiaSyncPacket;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.PhaseLockProtectionSyncPacket;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class CelestweaveArmorState {
   public static final int SLOT_CORE = 0;
   public static final int SLOT_COUNT = 1;
   private static volatile boolean CLIENT_FLIGHT_INERTIA = true;
   private static volatile UUID CLIENT_FLIGHT_INERTIA_ARMOR_ID = null;
   private static volatile PhaseFlightMode CLIENT_PHASE_MODE = PhaseFlightMode.ALL;
   private static volatile UUID CLIENT_PHASE_LOCK_ARMOR_ID = null;
   private static volatile boolean CLIENT_PHASE_LOCK_BLOCK_EXTERNAL_FORCES = false;
   private static final Set<CelestweaveArmorState.ClientFlightControlKey> CLIENT_FLIGHT_CONTROL_ACTIVE = ConcurrentHashMap.newKeySet();
   private static final AtomicLong CLIENT_FLIGHT_CONTROL_GENERATION = new AtomicLong();

   private CelestweaveArmorState() {
   }

   public static UUID ensureArmorId(ItemStack armor) {
      return ArmorPersistentData.ensureArmorId(armor);
   }

   @Nullable
   public static UUID getArmorId(ItemStack armor) {
      return ArmorPersistentData.armorId(armor).orElse(null);
   }

   public static long getCachedEnergyModuleCapacityFe(ItemStack armor) {
      return ArmorPersistentData.getCachedEnergyModuleCapacityFe(armor);
   }

   public static boolean hasCachedEnergyModuleCapacityFe(ItemStack armor) {
      return ArmorPersistentData.hasCachedEnergyModuleCapacityFe(armor);
   }

   public static void setCachedEnergyModuleCapacityFe(ItemStack armor, long capacityFe) {
      ArmorPersistentData.setCachedEnergyModuleCapacityFe(armor, capacityFe);
   }

   public static ItemStack getSlot(ItemStack armor, Provider registries, int slot) {
      return slot == 0 ? ArmorPersistentData.structuralCore(armor) : ItemStack.f_41583_;
   }

   public static void setSlot(ItemStack armor, Provider registries, int slot, ItemStack stack) {
      if (slot == 0) {
         ArmorPersistentData.setStructuralCore(armor, stack);
      }
   }

   public static boolean canInstallCore(ItemStack armor, Provider registries, ItemStack core) {
      return core != null && !core.m_41619_() && core.m_150930_((Item)ModItems.ULTIMATE_OVERLOAD_CORE.get());
   }

   public static boolean hasCore(ItemStack armor, Provider registries) {
      return ArmorPersistentData.hasStructuralCore(armor);
   }

   public static ArmorPart armorPart(ItemStack armor) {
      return armor != null && armor.m_41720_() instanceof BaseCelestweaveArmorItem item ? item.armorPart() : ArmorPart.CHEST;
   }

   public static boolean canInstallModule(ItemStack armor, Provider registries, ItemStack candidate) {
      if (candidate != null && !candidate.m_41619_()) {
         if (candidate.m_41720_() instanceof ArmorEnergyModuleItem energyModule) {
            ArmorPart part = armorPart(armor);
            return !energyModule.acceptableDevices().contains(part.deviceKind())
                  || ArmorEnergyModuleItem.acceptableSlotFor(part.deviceKind()) != part.moduleSlot()
               ? false
               : getInstalledAmount(armor, registries, "energy") < 1;
         } else if (!(candidate.m_41720_() instanceof OverloadDeviceModuleItem provider)) {
            return false;
         } else {
            ArmorPart var12 = armorPart(armor);
            if (!provider.accepts(var12.deviceKind(), var12.moduleSlot())) {
               return false;
            } else {
               String id = resolveSubmoduleId(candidate);
               if (id.isBlank()) {
                  return false;
               } else {
                  Set<String> groupIds = resolveSubmoduleGroupIds(candidate);
                  if (!groupIds.isEmpty()) {
                     for (ItemStack installed : loadModuleStacks(armor, registries)) {
                        if (!id.equals(resolveSubmoduleId(installed))) {
                           Set<String> installedGroups = resolveSubmoduleGroupIds(installed);
                           if (groupIds.stream().anyMatch(installedGroups::contains)) {
                              return false;
                           }
                        }
                     }
                  }

                  int current = getInstalledAmount(armor, registries, id);
                  int max = getSubmoduleMaxInstallAmountForStack(candidate);
                  return max <= 0 || current < max;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static boolean installOneModule(ItemStack armor, Provider registries, ItemStack candidate) {
      if (!canInstallModule(armor, registries, candidate)) {
         return false;
      } else {
         String id = resolveSubmoduleId(candidate);
         ArrayList<ItemStack> stacks = new ArrayList<>(loadModuleStacks(armor, registries));
         boolean merged = false;

         for (ItemStack stack : stacks) {
            if (id.equals(resolveSubmoduleId(stack))) {
               stack.m_41769_(1);
               merged = true;
               break;
            }
         }

         if (!merged) {
            stacks.add(candidate.m_255036_(1));
         }

         saveModuleStacks(armor, registries, stacks);
         return true;
      }
   }

   public static ItemStack uninstallOneModule(ItemStack armor, Provider registries, String submoduleId) {
      if (submoduleId != null && !submoduleId.isBlank()) {
         ArrayList<ItemStack> stacks = new ArrayList<>(loadModuleStacks(armor, registries));

         for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (submoduleId.equals(resolveSubmoduleId(stack))) {
               ItemStack detached = stack.m_255036_(1);
               if (stack.m_41613_() <= 1) {
                  stacks.remove(index);
               } else {
                  stack.m_41774_(1);
               }

               saveModuleStacks(armor, registries, stacks);
               pruneRemovedRuntime(getArmorId(armor), installedSubmoduleIds(armor, registries));
               return detached;
            }
         }

         return ItemStack.f_41583_;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public static ItemStack uninstallAllOfType(ItemStack armor, Provider registries, String submoduleId) {
      if (submoduleId != null && !submoduleId.isBlank()) {
         ArrayList<ItemStack> stacks = new ArrayList<>(loadModuleStacks(armor, registries));

         for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (submoduleId.equals(resolveSubmoduleId(stack))) {
               stacks.remove(index);
               saveModuleStacks(armor, registries, stacks);
               pruneRemovedRuntime(getArmorId(armor), installedSubmoduleIds(armor, registries));
               return stack.m_41777_();
            }
         }

         return ItemStack.f_41583_;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public static List<ItemStack> loadModuleStacks(ItemStack armor, Provider registries) {
      return ArmorPersistentData.loadModuleStacks(armor, registries);
   }

   private static void saveModuleStacks(ItemStack armor, Provider registries, List<ItemStack> stacks) {
      ArmorPersistentData.saveModuleStacks(armor, registries, stacks);
   }

   public static boolean hasAnyInstalledModule(ItemStack armor, Provider registries) {
      return !loadModuleStacks(armor, registries).isEmpty();
   }

   public static int getInstalledAmount(ItemStack armor, Provider registries, String submoduleId) {
      if (submoduleId != null && !submoduleId.isBlank()) {
         for (ItemStack stack : loadModuleStacks(armor, registries)) {
            if (submoduleId.equals(resolveSubmoduleId(stack))) {
               return stack.m_41613_();
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public static List<CelestweaveArmorSubmodule> collectSubmodules(ItemStack armor, Provider registries) {
      return collectInstalledSubmoduleEntries(armor, registries).stream().map(CelestweaveArmorState.InstalledSubmodule::submodule).toList();
   }

   public static String moduleTypeId(ItemStack stack) {
      return resolveSubmoduleId(stack);
   }

   public static int getSubmoduleMaxInstallAmountForStack(ItemStack stack) {
      if (stack != null && stack.m_41720_() instanceof ArmorEnergyModuleItem) {
         return 1;
      } else {
         int max = stack != null && stack.m_41720_() instanceof OverloadDeviceModuleItem provider ? Math.max(0, provider.getMaxInstallAmount()) : 0;
         if (stack != null && stack.m_41720_() instanceof CelestweaveArmorSubmoduleItem providerx) {
            ArrayList<Integer> values = new ArrayList<>();
            providerx.collectSubmodules(stack, submodule -> values.add(Math.max(0, submodule.getMaxInstallAmount())));

            for (int value : values) {
               if (value > 0) {
                  max = max == 0 ? value : Math.min(max, value);
               }
            }
         }

         return max;
      }
   }

   public static int getSubmoduleMaxInstallAmount(CelestweaveArmorSubmodule submodule) {
      return submodule == null ? 0 : Math.max(0, submodule.getMaxInstallAmount());
   }

   public static boolean isSubmoduleInstalled(ItemStack armor, Provider registries, String submoduleId) {
      return getInstalledAmount(armor, registries, submoduleId) > 0;
   }

   public static boolean isSubmoduleInstalled(ItemStack armor, String submoduleId) {
      return ArmorPersistentData.hasInstalledSubmodule(armor, submoduleId);
   }

   public static boolean isSubmoduleEnabled(ItemStack armor, CelestweaveArmorSubmodule submodule) {
      return submodule != null && isSubmoduleEnabled(armor, submodule.id(), submodule.defaultEnabled());
   }

   public static boolean isSubmoduleEnabled(ItemStack armor, String submoduleId, boolean defaultEnabled) {
      return ArmorPersistentData.getToggle(armor, submoduleId, defaultEnabled);
   }

   public static void setSubmoduleEnabled(ItemStack armor, CelestweaveArmorSubmodule submodule, boolean enabled) {
      if (submodule != null) {
         setSubmoduleEnabled(armor, submodule.id(), enabled, submodule.defaultEnabled());
      }
   }

   public static void setSubmoduleEnabled(ItemStack armor, String submoduleId, boolean enabled, boolean defaultEnabled) {
      ArmorPersistentData.setToggle(armor, submoduleId, enabled, defaultEnabled);
   }

   public static int buildSubmoduleMask(ItemStack armor, List<CelestweaveArmorSubmodule> submodules) {
      int mask = 0;
      int limit = Math.min(submodules.size(), 31);

      for (int i = 0; i < limit; i++) {
         if (isSubmoduleEnabled(armor, submodules.get(i))) {
            mask |= 1 << i;
         }
      }

      return mask;
   }

   public static CompoundTag getSubmoduleData(ItemStack armor, CelestweaveArmorSubmodule submodule) {
      return submodule == null ? new CompoundTag() : getStoredSubmoduleData(armor, submodule.id());
   }

   public static void setSubmoduleData(ItemStack armor, CelestweaveArmorSubmodule submodule, CompoundTag data) {
      if (submodule != null) {
         setStoredSubmoduleData(armor, submodule.id(), data);
      }
   }

   public static boolean isSubmoduleRuntimeActive(ItemStack armor, String submoduleId) {
      UUID id = getArmorId(armor);
      return id != null && ArmorRuntimeRegistry.isSubmoduleRuntimeActive(id, submoduleId);
   }

   public static boolean isSubmoduleActive(ItemStack armor, CelestweaveArmorSubmodule submodule, Provider registries, boolean equipped) {
      return submodule != null && isSubmoduleRuntimeActive(armor, submodule.id());
   }

   public static void syncSubmoduleActiveState(@Nullable Player player, ItemStack armor, Provider registries, boolean equipped, Dist dist) {
      syncSubmoduleActiveState(player, armor, registries, equipped, dist, false);
   }

   public static void syncSubmoduleActiveState(
      @Nullable Player player, ItemStack armor, Provider registries, boolean equipped, Dist dist, boolean forceClientSync
   ) {
      syncSubmoduleActiveState(player, armor, collectInstalledSubmoduleEntries(armor, registries), equipped, dist, forceClientSync);
   }

   public static void syncSubmoduleActiveState(
      @Nullable Player player, ItemStack armor, List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules, boolean equipped, Dist dist
   ) {
      syncSubmoduleActiveState(player, armor, installedSubmodules, equipped, dist, false);
   }

   public static void syncSubmoduleActiveState(
      @Nullable Player player,
      ItemStack armor,
      List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules,
      boolean equipped,
      Dist dist,
      boolean forceClientSync
   ) {
      UUID armorId = ensureArmorId(armor);
      boolean hasCore = equipped && ArmorPersistentData.hasStructuralCore(armor);
      boolean dedicatedServer = dist == Dist.DEDICATED_SERVER && player instanceof ServerPlayer;

      for (CelestweaveArmorState.InstalledSubmodule entry : installedSubmodules) {
         CelestweaveArmorSubmodule submodule = entry.submodule();
         boolean active = hasCore && isSubmoduleEnabled(armor, submodule);
         Boolean previous = setSubmoduleRuntimeActive(armor, submodule.id(), active);
         boolean changed = (previous != null && previous) != active;
         boolean flightModule = isFlightControlModule(submodule.id());
         if (dedicatedServer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (flightModule && (changed || forceClientSync)) {
               NetworkInit.sendToPlayer(serverPlayer, new CelestweaveSubmoduleActivePacket(armorId, submodule.id(), active));
            }

            if (PhaseLockSubmodule.INSTANCE.id().equals(submodule.id()) && (changed || forceClientSync)) {
               syncPhaseLockProtectionToClient(serverPlayer, armor, armorId, active);
               syncFlightSettingsToClient(serverPlayer, armorId);
            }

            if (active && FlightInertiaSyncRules.shouldSync(changed, active, forceClientSync, flightModule)) {
               syncFlightSettingsToClient(serverPlayer, armor, armorId);
            }
         }

         if (changed) {
            if (active) {
               submodule.onActivated(player, dist, armor);
            } else {
               submodule.onDeactivated(player, dist, armor);
               if (dedicatedServer && flightModule) {
                  syncFlightSettingsToClient((ServerPlayer)player, armor, armorId);
               }
            }
         }
      }
   }

   public static void reconcileInstalledSubmodules(@Nullable Player player, ItemStack armor, Provider registries, Dist dist) {
      ensureArmorId(armor);

      for (CelestweaveArmorSubmodule submodule : collectSubmodules(armor, registries)) {
         submodule.onInstalled(player, dist, armor);
      }
   }

   public static void tickActiveSubmodules(@Nullable Player player, ItemStack armor, Provider registries, Dist dist) {
      tickActiveSubmodules(player, armor, collectInstalledSubmoduleEntries(armor, registries), dist);
   }

   public static void tickActiveSubmodules(
      @Nullable Player player, ItemStack armor, List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules, Dist dist
   ) {
      for (CelestweaveArmorState.InstalledSubmodule entry : installedSubmodules) {
         CelestweaveArmorSubmodule submodule = entry.submodule();
         if (isSubmoduleRuntimeActive(armor, submodule.id())) {
            submodule.tickActive(player, dist, armor);
         }
      }
   }

   public static CelestweaveArmorState.Snapshot tickEquipped(Player player, ItemStack armor, Provider registries) {
      return tickEquipped(player, armor, collectInstalledSubmoduleEntries(armor, registries), registries);
   }

   public static CelestweaveArmorState.Snapshot tickEquipped(
      Player player, ItemStack armor, List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules, Provider registries
   ) {
      UUID id = ensureArmorId(armor);
      pruneRemovedRuntime(id, installedSubmoduleIds(installedSubmodules));
      return snapshot(player, armor, registries, true);
   }

   public static CelestweaveArmorState.Snapshot snapshot(ItemStack armor, Provider registries, boolean equipped) {
      return snapshot(null, armor, registries, equipped);
   }

   public static CelestweaveArmorState.Snapshot snapshot(@Nullable Player player, ItemStack armor, Provider registries, boolean equipped) {
      long stored = ArmorEnergyBuffer.read(armor, registries);
      long capacity = ArmorEnergyBuffer.capacity(armor, registries);
      boolean hasCore = hasCore(armor, registries);
      boolean hasEnergy = ArmorEnergyModuleStorage.capacityFe(armor, registries) > 0L;
      return new CelestweaveArmorState.Snapshot(equipped, hasCore, hasEnergy, stored, capacity);
   }

   public static long readPersistedStoredEnergy(ItemStack armor) {
      return ArmorEnergyBuffer.read(armor);
   }

   public static long addStoredEnergy(ItemStack armor, Provider registries, long amount) {
      return ArmorEnergyBuffer.receiveFe(armor, registries, amount, false);
   }

   public static boolean isSubmoduleActiveClient(ItemStack armor, CelestweaveArmorSubmodule submodule) {
      return submodule != null && ArmorPersistentData.hasStructuralCore(armor) && isModulesPowered(armor) && isSubmoduleEnabled(armor, submodule);
   }

   public static boolean isModulesPowered(ItemStack armor) {
      return armor != null && ModDataComponents.CELESTWEAVE_MODULES_POWERED.getOrDefault(armor, Boolean.TRUE);
   }

   public static void setModulesPowered(ItemStack armor, boolean powered) {
      if (armor != null && !armor.m_41619_() && isModulesPowered(armor) != powered) {
         if (powered) {
            ModDataComponents.CELESTWEAVE_MODULES_POWERED.remove(armor);
         } else {
            ModDataComponents.CELESTWEAVE_MODULES_POWERED.set(armor, Boolean.FALSE);
         }
      }
   }

   public static void markClientActive(UUID armorId, String submoduleId, boolean active) {
      if (armorId != null && isFlightControlModule(submoduleId)) {
         CelestweaveArmorState.ClientFlightControlKey key = new CelestweaveArmorState.ClientFlightControlKey(armorId, submoduleId);
         boolean changed = active ? CLIENT_FLIGHT_CONTROL_ACTIVE.add(key) : CLIENT_FLIGHT_CONTROL_ACTIVE.remove(key);
         if (changed) {
            CLIENT_FLIGHT_CONTROL_GENERATION.incrementAndGet();
         }
      }
   }

   public static boolean isAnyClientFlightControlActive() {
      return !CLIENT_FLIGHT_CONTROL_ACTIVE.isEmpty();
   }

   public static long getClientFlightControlGeneration() {
      return CLIENT_FLIGHT_CONTROL_GENERATION.get();
   }

   public static void clearClientActiveCache() {
      if (!CLIENT_FLIGHT_CONTROL_ACTIVE.isEmpty()) {
         CLIENT_FLIGHT_CONTROL_ACTIVE.clear();
         CLIENT_FLIGHT_CONTROL_GENERATION.incrementAndGet();
      }

      CLIENT_FLIGHT_INERTIA = true;
      CLIENT_FLIGHT_INERTIA_ARMOR_ID = null;
      CLIENT_PHASE_MODE = PhaseFlightMode.ALL;
      CLIENT_PHASE_LOCK_ARMOR_ID = null;
      CLIENT_PHASE_LOCK_BLOCK_EXTERNAL_FORCES = false;
   }

   public static void forgetSubmoduleActiveCache(UUID armorId) {
      ArmorRuntimeRegistry.clear(armorId);
      if (armorId != null) {
         if (CLIENT_FLIGHT_CONTROL_ACTIVE.removeIf(key -> key.armorId().equals(armorId))) {
            CLIENT_FLIGHT_CONTROL_GENERATION.incrementAndGet();
         }

         if (armorId.equals(CLIENT_PHASE_LOCK_ARMOR_ID)) {
            CLIENT_PHASE_LOCK_ARMOR_ID = null;
            CLIENT_PHASE_LOCK_BLOCK_EXTERNAL_FORCES = false;
         }
      }
   }

   public static void clearTransientRuntimeAndCaches(ItemStack armor) {
      UUID armorId = getArmorId(armor);
      if (armorId != null) {
         forgetSubmoduleActiveCache(armorId);
      }
   }

   private static String resolveSubmoduleId(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof OverloadDeviceModuleItem provider ? provider.moduleTypeId(stack) : "";
   }

   private static Set<String> resolveSubmoduleGroupIds(ItemStack stack) {
      if (stack != null && !stack.m_41619_() && stack.m_41720_() instanceof ArmorEnergyModuleItem) {
         return Set.of("energy");
      } else if (stack != null && !stack.m_41619_()) {
         if (stack.m_41720_() instanceof CelestweaveArmorSubmoduleItem provider) {
            HashSet var4 = new HashSet();
            provider.collectSubmodules(stack, submodule -> {
               if (submodule != null) {
                  submodule.installGroupIds().stream().filter(group -> group != null && !group.isBlank()).forEach(var4::add);
               }
            });
            return Set.copyOf(var4);
         } else {
            String id = resolveSubmoduleId(stack);
            return id.isBlank() ? Set.of() : Set.of(id);
         }
      } else {
         return Set.of();
      }
   }

   public static List<CelestweaveArmorState.InstalledSubmodule> collectInstalledSubmoduleEntries(ItemStack armor, Provider registries) {
      ArrayList<CelestweaveArmorState.InstalledSubmodule> result = new ArrayList<>();

      for (ItemStack stack : loadModuleStacks(armor, registries)) {
         if (stack.m_41720_() instanceof CelestweaveArmorSubmoduleItem provider) {
            int count = Math.max(1, stack.m_41613_());
            ItemStack installedStack = stack.m_255036_(count);
            provider.collectSubmodules(installedStack, submodule -> {
               if (submodule != null && !submodule.id().isBlank()) {
                  result.add(new CelestweaveArmorState.InstalledSubmodule(installedStack, submodule, count));
               }
            });
         }
      }

      return List.copyOf(result);
   }

   private static void pruneRemovedRuntime(UUID armorId, Set<String> installedIds) {
      for (String submoduleId : List.copyOf(ArmorRuntimeRegistry.submoduleIds(armorId))) {
         if (!installedIds.contains(submoduleId)) {
            ArmorRuntimeRegistry.removeSubmodule(armorId, submoduleId);
         }
      }
   }

   private static Set<String> installedSubmoduleIds(ItemStack armor, Provider registries) {
      return installedSubmoduleIds(collectInstalledSubmoduleEntries(armor, registries));
   }

   private static Set<String> installedSubmoduleIds(List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules) {
      HashSet<String> installedIds = new HashSet<>();

      for (CelestweaveArmorState.InstalledSubmodule entry : installedSubmodules) {
         installedIds.add(entry.submodule().id());
      }

      return installedIds;
   }

   private static Boolean setSubmoduleRuntimeActive(ItemStack armor, String submoduleId, boolean active) {
      UUID id = ensureArmorId(armor);
      return ArmorRuntimeRegistry.setSubmoduleRuntimeActive(id, submoduleId, active);
   }

   private static CompoundTag getStoredSubmoduleData(ItemStack armor, String submoduleId) {
      return ArmorPersistentData.getSubmoduleData(armor, submoduleId);
   }

   private static void setStoredSubmoduleData(ItemStack armor, String submoduleId, CompoundTag data) {
      ArmorPersistentData.setSubmoduleData(armor, submoduleId, data);
   }

   public static void setClientFlightSettings(UUID armorId, boolean inertiaEnabled, PhaseFlightMode phaseMode) {
      CLIENT_FLIGHT_INERTIA = inertiaEnabled;
      CLIENT_FLIGHT_INERTIA_ARMOR_ID = armorId;
      CLIENT_PHASE_MODE = phaseMode;
   }

   public static boolean getClientFlightInertia() {
      return CLIENT_FLIGHT_INERTIA;
   }

   public static PhaseFlightMode getClientPhaseMode() {
      return CLIENT_PHASE_MODE;
   }

   public static void setClientPhaseLockProtection(UUID armorId, boolean blockExternalForces) {
      CLIENT_PHASE_LOCK_ARMOR_ID = armorId;
      CLIENT_PHASE_LOCK_BLOCK_EXTERNAL_FORCES = blockExternalForces;
   }

   public static boolean getClientPhaseLockBlockExternalForces() {
      return CLIENT_PHASE_LOCK_BLOCK_EXTERNAL_FORCES;
   }

   private static void syncFlightSettingsToClient(ServerPlayer player, ItemStack flightArmor, UUID syncArmorId) {
      boolean phaseFlightActive = !flightArmor.m_41619_() && isSubmoduleRuntimeActive(flightArmor, PhaseFlightSubmodule.INSTANCE.id());
      boolean flightActive = !flightArmor.m_41619_() && isSubmoduleRuntimeActive(flightArmor, FlightSubmodule.INSTANCE.id());
      boolean flightControlActive = flightActive || phaseFlightActive;
      boolean flightLockActive = PhaseLockSubmodule.isFlightLockConfigured(player) && PhaseLockSubmodule.hasFlightSource(player);
      if (flightControlActive || flightLockActive) {
         PhaseFlightPlayerState.activate(player);
      }

      boolean inertia = FlightInertiaSyncRules.targetInertia(
         flightActive,
         flightActive && FlightSubmodule.isInertiaEnabled(flightArmor),
         phaseFlightActive,
         phaseFlightActive && PhaseFlightSubmodule.isInertiaEnabled(flightArmor)
      );
      boolean flying = PhaseFlightControlRules.handoffFlying(PhaseFlightPlayerState.isFlying(player), PhaseLockSubmodule.hasFlightSource(player));
      NetworkInit.sendToPlayer(
         player,
         new FlightInertiaSyncPacket(
            syncArmorId,
            inertia,
            flightControlActive,
            flying,
            phaseFlightActive && AE2LTCommonConfig.overloadArmorPhaseFlightEnabled()
               ? PhaseFlightSubmodule.selectedPhaseMode(flightArmor)
               : PhaseFlightMode.OFF,
            flightLockActive
         )
      );
   }

   public static void syncFlightSettingsToClient(ServerPlayer player) {
      syncFlightSettingsToClient(player, null);
   }

   private static void syncFlightSettingsToClient(ServerPlayer player, @Nullable UUID fallbackArmorId) {
      ItemStack flightArmor = CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.LEGS);
      UUID syncArmorId = flightArmor.m_41619_() ? null : getArmorId(flightArmor);
      if (syncArmorId == null) {
         ItemStack chest = CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.CHEST);
         syncArmorId = chest.m_41619_() ? null : getArmorId(chest);
      }

      if (syncArmorId == null) {
         syncArmorId = fallbackArmorId;
      }

      if (syncArmorId != null) {
         syncFlightSettingsToClient(player, flightArmor, syncArmorId);
      }
   }

   private static boolean isFlightControlModule(String submoduleId) {
      return FlightSubmodule.INSTANCE.id().equals(submoduleId) || PhaseFlightSubmodule.INSTANCE.id().equals(submoduleId);
   }

   private static void syncPhaseLockProtectionToClient(ServerPlayer player, ItemStack armor, UUID armorId, boolean active) {
      NetworkInit.sendToPlayer(player, new PhaseLockProtectionSyncPacket(armorId, active && PhaseLockSubmodule.blocksExternalForces(armor)));
   }

   public static void syncPhaseLockProtectionToClient(ServerPlayer player, ItemStack armor) {
      UUID armorId = getArmorId(armor);
      if (armorId != null) {
         syncPhaseLockProtectionToClient(player, armor, armorId, isSubmoduleRuntimeActive(armor, PhaseLockSubmodule.INSTANCE.id()));
      }
   }

   private static record ClientFlightControlKey(UUID armorId, String submoduleId) {
   }

   public static record InstalledSubmodule(ItemStack stack, CelestweaveArmorSubmodule submodule, int count) {
   }

   public static record Snapshot(boolean equipped, boolean hasCore, boolean hasEnergyModule, long storedEnergy, long energyCapacity) {
   }
}
