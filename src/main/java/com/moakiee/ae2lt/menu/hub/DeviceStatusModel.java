package com.moakiee.ae2lt.menu.hub;

import com.moakiee.ae2lt.celestweave.ArmorEnergyBuffer;
import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmodule;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmoduleItem;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmoduleOptionUi;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.network.ArmorNetworkBinding;
import com.moakiee.ae2lt.device.network.BindingResolveResult;
import com.moakiee.ae2lt.item.railgun.RailgunExecutionMode;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import com.moakiee.ae2lt.item.railgun.RailgunModuleStorage;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.item.railgun.RailgunStructuralCore;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import com.moakiee.ae2lt.logic.railgun.RailgunBinding;
import com.moakiee.ae2lt.logic.railgun.RailgunEnergyBuffer;
import com.moakiee.ae2lt.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record DeviceStatusModel(
   String displayName,
   boolean hasCore,
   boolean powered,
   List<DeviceStatusModel.ModuleInfo> modules,
   int selectedModuleIndex,
   List<DeviceStatusModel.ModuleConfigInfo> moduleConfigs,
   boolean terrainDestruction,
   boolean pvp,
   boolean soundEnabled,
   boolean chainDamage,
   RailgunExecutionMode executionMode,
   boolean chargedSplash
) {
   public static final String RAILGUN_OVERLOAD_MODULE_KEY = "ae2lt.device_hub.module.railgun.overload_execution";
   public static final String RAILGUN_MULTIDIMENSIONAL_MODULE_KEY = "ae2lt.device_hub.module.railgun.multidimensional_execution";
   public static final DeviceStatusModel EMPTY = new DeviceStatusModel(
      "", false, false, List.of(), -1, List.of(), false, false, false, false, RailgunExecutionMode.NORMAL, false
   );

   public static DeviceStatusModel fromArmorStack(ItemStack armor, ServerPlayer player) {
      return fromArmorStack(armor, player, 0);
   }

   public static DeviceStatusModel fromArmorStack(ItemStack armor, ServerPlayer player, int selectedModuleIndex) {
      if (armor != null && !armor.m_41619_() && armor.m_41720_() instanceof BaseCelestweaveArmorItem armorItem) {
         String var17 = armor.m_41786_().getString();
         BindingResolveResult resolve = ArmorNetworkBinding.INSTANCE.resolve(armor, player);
         boolean gridReachable = resolve.success();
         boolean appFlux = AppFluxBridge.isAvailable();
         long stored = ArmorEnergyBuffer.read(armor, player.m_9236_().m_9598_());
         CelestweaveArmorState.Snapshot snapshot = CelestweaveArmorState.snapshot(player, armor, player.m_9236_().m_9598_(), true);
         boolean powered = DeviceHubDisplayRules.powerAvailable(stored, gridReachable, appFlux);
         ArrayList modules = new ArrayList();

         for (ItemStack stack : CelestweaveArmorState.loadModuleStacks(armor, player.m_9236_().m_9598_())) {
            if (stack.m_41720_() instanceof CelestweaveArmorSubmoduleItem provider) {
               int count = Math.max(1, stack.m_41613_());
               provider.collectSubmodules(stack, sub -> {
                  boolean enabled = CelestweaveArmorState.isSubmoduleEnabled(armor, sub);
                  modules.add(new DeviceStatusModel.ModuleInfo(sub.nameKey(), count, enabled));
               });
            }
         }

         int clampedModuleIndex = modules.isEmpty() ? -1 : Math.max(0, Math.min(modules.size() - 1, selectedModuleIndex));
         List<DeviceStatusModel.ModuleConfigInfo> moduleConfigs = moduleConfigs(armor, player, clampedModuleIndex);
         return new DeviceStatusModel(
            var17, snapshot.hasCore(), powered, modules, clampedModuleIndex, moduleConfigs, false, false, false, false, RailgunExecutionMode.NORMAL, false
         );
      } else {
         return EMPTY;
      }
   }

   public static DeviceStatusModel fromRailgunStack(ItemStack railgun, ServerPlayer player) {
      return fromRailgunStack(railgun, player, -1);
   }

   public static DeviceStatusModel fromRailgunStack(ItemStack railgun, ServerPlayer player, int selectedModuleIndex) {
      if (railgun != null && !railgun.m_41619_()) {
         String name = railgun.m_41786_().getString();
         RailgunBinding.Result resolve = RailgunBinding.resolve(railgun, player);
         boolean gridReachable = resolve.success();
         boolean appFlux = AppFluxBridge.isAvailable();
         long stored = RailgunEnergyBuffer.read(railgun);
         boolean powered = DeviceHubDisplayRules.powerAvailable(stored, gridReachable, appFlux);
         RailgunModuleEntries entries = RailgunModuleStorage.entryData(railgun);
         boolean hasStructuralCore = RailgunStructuralCore.hasCore(railgun);
         List<DeviceStatusModel.ModuleInfo> modules = new ArrayList<>();
         if (entries.hasCore()) {
            modules.add(new DeviceStatusModel.ModuleInfo("ae2lt.device_hub.module.railgun.core", 1, true));
         }

         if (entries.computeCount() > 0) {
            modules.add(new DeviceStatusModel.ModuleInfo("ae2lt.device_hub.module.railgun.compute", entries.computeCount(), true));
         }

         if (entries.accelerationCount() > 0) {
            modules.add(new DeviceStatusModel.ModuleInfo("ae2lt.device_hub.module.railgun.acceleration", entries.accelerationCount(), true));
         }

         if (entries.hasOverloadExecution()) {
            modules.add(new DeviceStatusModel.ModuleInfo("ae2lt.device_hub.module.railgun.overload_execution", 1, true));
         }

         if (entries.hasMultidimensionalExecution()) {
            modules.add(new DeviceStatusModel.ModuleInfo("ae2lt.device_hub.module.railgun.multidimensional_execution", 1, true));
         }

         RailgunSettings settings = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
         boolean terrainAllowed = AE2LTCommonConfig.railgunTerrainDestructionEnabled();
         return new DeviceStatusModel(
            name,
            hasStructuralCore,
            powered,
            modules,
            -1,
            List.of(),
            terrainAllowed && settings.terrainDestruction(),
            settings.pvp(),
            settings.soundEnabled(),
            settings.chainDamage(),
            settings.executionMode(),
            settings.chargedSplash()
         );
      } else {
         return EMPTY;
      }
   }

   private static List<DeviceStatusModel.ModuleConfigInfo> moduleConfigs(ItemStack armor, ServerPlayer player, int selectedModuleIndex) {
      List<CelestweaveArmorSubmodule> submodules = CelestweaveArmorState.collectSubmodules(armor, player.m_9236_().m_9598_());
      return selectedModuleIndex >= 0 && selectedModuleIndex < submodules.size()
         ? submodules.get(selectedModuleIndex).getConfigUI(armor).stream().map(DeviceStatusModel::moduleConfigInfo).toList()
         : List.of();
   }

   private static DeviceStatusModel.ModuleConfigInfo moduleConfigInfo(CelestweaveArmorSubmoduleOptionUi option) {
      return new DeviceStatusModel.ModuleConfigInfo(option.key(), option.label().getString(), option.value().getString(), option.editable());
   }

   public static record ModuleConfigInfo(String key, String label, String value, boolean editable) {
   }

   public static record ModuleInfo(String nameKey, int count, boolean enabled) {
   }
}
