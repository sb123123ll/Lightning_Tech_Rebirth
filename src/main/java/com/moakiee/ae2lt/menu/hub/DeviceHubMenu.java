package com.moakiee.ae2lt.menu.hub;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmodule;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmoduleConfig;
import com.moakiee.ae2lt.celestweave.module.PhaseLockSubmodule;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.celestweave.phase.PhaseLockService;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.item.railgun.RailgunExecutionMode;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.hub.DeviceHubSyncPacket;
import com.moakiee.ae2lt.registry.ModDataComponents;
import java.util.List;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import org.jetbrains.annotations.Nullable;

public class DeviceHubMenu extends AbstractContainerMenu {
   public static final int DATA_SELECTED_TAB = 0;
   public static final int DATA_TAB_AVAILABILITY = 1;
   public static final int DATA_COUNT = 2;
   public static final int TAB_HELMET = 0;
   public static final int TAB_CHESTPLATE = 1;
   public static final int TAB_LEGGINGS = 2;
   public static final int TAB_BOOTS = 3;
   public static final int TAB_RAILGUN = 4;
   public static final int TAB_COUNT = 5;
   public static final MenuType<DeviceHubMenu> TYPE = IForgeMenuType.create(DeviceHubMenu::new);
   public final ContainerData data;
   private String deviceName = "";
   private boolean hasCore;
   private boolean powered;
   private boolean terrainDestruction;
   private boolean pvp;
   private boolean soundEnabled;
   private boolean chainDamage;
   private RailgunExecutionMode executionMode = RailgunExecutionMode.NORMAL;
   private boolean chargedSplash;
   private List<String> moduleNameKeys = List.of();
   private List<Integer> moduleCounts = List.of();
   private List<Boolean> moduleEnabled = List.of();
   private int selectedModuleIndex = -1;
   private List<String> moduleConfigKeys = List.of();
   private List<String> moduleConfigLabels = List.of();
   private List<String> moduleConfigValues = List.of();
   private List<Boolean> moduleConfigEditable = List.of();
   private int selectedTab;
   private int lastSyncedTab = -1;
   @Nullable
   private DeviceHubSyncPacket lastSyncPacket;
   private Player trackedPlayer;

   public DeviceHubMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
      super(TYPE, containerId);
      this.selectedTab = buf.m_130242_();
      this.data = new SimpleContainerData(2);
      this.data.m_8050_(0, this.selectedTab);
      this.m_38884_(this.data);
   }

   public DeviceHubMenu(int containerId, Inventory inv, int defaultTab) {
      super(TYPE, containerId);
      this.selectedTab = defaultTab;
      this.trackedPlayer = inv.f_35978_;
      this.data = new DeviceHubMenu.ServerData();
      this.m_38884_(this.data);
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   public boolean m_6875_(Player player) {
      return true;
   }

   public void m_38946_() {
      if (!(this.getPlayer() instanceof ServerPlayer serverPlayer)) {
         super.m_38946_();
      } else {
         int var7 = 0;

         for (int deviceStack = 0; deviceStack < 5; deviceStack++) {
            if (!findDevice(serverPlayer, deviceStack).m_41619_()) {
               var7 |= 1 << deviceStack;
            }
         }

         if ((var7 & 1 << this.selectedTab) == 0) {
            for (int t = 0; t < 5; t++) {
               if ((var7 & 1 << t) != 0) {
                  this.selectedTab = t;
                  break;
               }
            }
         }

         ItemStack deviceStackx = findDevice(serverPlayer, this.selectedTab);
         DeviceStatusModel status;
         if (deviceStackx.m_41619_()) {
            status = DeviceStatusModel.EMPTY;
         } else if (this.selectedTab == 4) {
            status = DeviceStatusModel.fromRailgunStack(deviceStackx, serverPlayer, this.selectedModuleIndex);
         } else {
            status = DeviceStatusModel.fromArmorStack(deviceStackx, serverPlayer, this.selectedModuleIndex);
         }

         this.selectedModuleIndex = status.selectedModuleIndex();
         DeviceHubMenu.ServerData sd = (DeviceHubMenu.ServerData)this.data;
         sd.values[0] = this.selectedTab;
         sd.values[1] = var7;
         super.m_38946_();
         DeviceHubSyncPacket syncPacket = this.toSyncPacket(status);
         if (this.selectedTab != this.lastSyncedTab || !syncPacket.equals(this.lastSyncPacket)) {
            this.lastSyncedTab = this.selectedTab;
            this.lastSyncPacket = syncPacket;
            NetworkInit.sendToPlayer(serverPlayer, syncPacket);
         }
      }
   }

   private DeviceHubSyncPacket toSyncPacket(DeviceStatusModel status) {
      List<String> nameKeys = status.modules().stream().map(DeviceStatusModel.ModuleInfo::nameKey).toList();
      List<Integer> counts = status.modules().stream().map(DeviceStatusModel.ModuleInfo::count).toList();
      List<Boolean> enabled = status.modules().stream().map(DeviceStatusModel.ModuleInfo::enabled).toList();
      List<String> moduleConfigKeys = status.moduleConfigs().stream().map(DeviceStatusModel.ModuleConfigInfo::key).toList();
      List<String> moduleConfigLabels = status.moduleConfigs().stream().map(DeviceStatusModel.ModuleConfigInfo::label).toList();
      List<String> moduleConfigValues = status.moduleConfigs().stream().map(DeviceStatusModel.ModuleConfigInfo::value).toList();
      List<Boolean> moduleConfigEditable = status.moduleConfigs().stream().map(DeviceStatusModel.ModuleConfigInfo::editable).toList();
      return new DeviceHubSyncPacket(
         this.f_38840_,
         status.displayName(),
         status.hasCore(),
         status.powered(),
         status.terrainDestruction(),
         status.pvp(),
         status.soundEnabled(),
         status.chainDamage(),
         status.executionMode(),
         status.chargedSplash(),
         nameKeys,
         counts,
         enabled,
         status.selectedModuleIndex(),
         moduleConfigKeys,
         moduleConfigLabels,
         moduleConfigValues,
         moduleConfigEditable
      );
   }

   @Nullable
   private Player getPlayer() {
      return this.trackedPlayer;
   }

   public void m_182410_(int stateId, List<ItemStack> items, ItemStack carried) {
      super.m_182410_(stateId, items, carried);
   }

   public void setPlayer(Player player) {
      this.trackedPlayer = player;
   }

   public void receiveSync(
      String name,
      boolean hasCore,
      boolean powered,
      boolean terrainDestruction,
      boolean pvp,
      boolean soundEnabled,
      boolean chainDamage,
      RailgunExecutionMode executionMode,
      boolean chargedSplash,
      List<String> nameKeys,
      List<Integer> counts,
      List<Boolean> enabled,
      int selectedModuleIndex,
      List<String> moduleConfigKeys,
      List<String> moduleConfigLabels,
      List<String> moduleConfigValues,
      List<Boolean> moduleConfigEditable
   ) {
      this.deviceName = name;
      this.hasCore = hasCore;
      this.powered = powered;
      this.terrainDestruction = terrainDestruction;
      this.pvp = pvp;
      this.soundEnabled = soundEnabled;
      this.chainDamage = chainDamage;
      this.executionMode = executionMode;
      this.chargedSplash = chargedSplash;
      this.moduleNameKeys = List.copyOf(nameKeys);
      this.moduleCounts = List.copyOf(counts);
      this.moduleEnabled = List.copyOf(enabled);
      this.selectedModuleIndex = selectedModuleIndex;
      this.moduleConfigKeys = List.copyOf(moduleConfigKeys);
      this.moduleConfigLabels = List.copyOf(moduleConfigLabels);
      this.moduleConfigValues = List.copyOf(moduleConfigValues);
      this.moduleConfigEditable = List.copyOf(moduleConfigEditable);
   }

   public int getSelectedTab() {
      return this.data.m_6413_(0);
   }

   public int getTabAvailability() {
      return this.data.m_6413_(1);
   }

   public String getDeviceName() {
      return this.deviceName;
   }

   public List<String> getModuleNameKeys() {
      return this.moduleNameKeys;
   }

   public List<Integer> getModuleCounts() {
      return this.moduleCounts;
   }

   public List<Boolean> getModuleEnabled() {
      return this.moduleEnabled;
   }

   public boolean hasCore() {
      return this.hasCore;
   }

   public boolean isPowered() {
      return this.powered;
   }

   public boolean isTerrainDestruction() {
      return this.terrainDestruction;
   }

   public boolean isPvp() {
      return this.pvp;
   }

   public boolean isSoundEnabled() {
      return this.soundEnabled;
   }

   public boolean isChainDamage() {
      return this.chainDamage;
   }

   public RailgunExecutionMode getExecutionMode() {
      return this.executionMode;
   }

   public boolean isChargedSplash() {
      return this.chargedSplash;
   }

   public int getSelectedModuleIndex() {
      return this.selectedModuleIndex;
   }

   public List<String> getModuleConfigKeys() {
      return this.moduleConfigKeys;
   }

   public List<String> getModuleConfigLabels() {
      return this.moduleConfigLabels;
   }

   public List<String> getModuleConfigValues() {
      return this.moduleConfigValues;
   }

   public List<Boolean> getModuleConfigEditable() {
      return this.moduleConfigEditable;
   }

   public void selectTab(int tab) {
      if (tab >= 0 && tab < 5) {
         if (this.selectedTab != tab) {
            this.selectedModuleIndex = -1;
         }

         this.selectedTab = tab;
      }
   }

   public void selectModule(int moduleIndex) {
      if (moduleIndex >= 0) {
         this.selectedModuleIndex = this.selectedTab == 4 && this.selectedModuleIndex == moduleIndex ? -1 : moduleIndex;
      }
   }

   public void toggleModule(int moduleIndex) {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack deviceStack = findDevice(player, this.selectedTab);
         if (!deviceStack.m_41619_()) {
            if (this.selectedTab != 4) {
               List<CelestweaveArmorSubmodule> submodules = CelestweaveArmorState.collectSubmodules(deviceStack, player.m_9236_().m_9598_());
               if (moduleIndex >= 0 && moduleIndex < submodules.size()) {
                  CelestweaveArmorSubmodule sub = submodules.get(moduleIndex);
                  boolean current = CelestweaveArmorState.isSubmoduleEnabled(deviceStack, sub);
                  CelestweaveArmorState.setSubmoduleEnabled(deviceStack, sub, !current);
                  if (current && PhaseLockSubmodule.INSTANCE.id().equals(sub.id())) {
                     PhaseLockService.release(player);
                  }
               }
            }
         }
      }
   }

   public void toggleRailgunTerrain() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         if (AE2LTCommonConfig.railgunTerrainDestructionEnabled()) {
            ItemStack railgun = findDevice(player, 4);
            if (!railgun.m_41619_()) {
               RailgunSettings s = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
               ModDataComponents.RAILGUN_SETTINGS.set(railgun, s.withTerrain(!s.terrainDestruction()));
            }
         }
      }
   }

   public void toggleRailgunPvp() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack railgun = findDevice(player, 4);
         if (!railgun.m_41619_()) {
            RailgunSettings s = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
            ModDataComponents.RAILGUN_SETTINGS.set(railgun, s.withPvp(!s.pvp()));
         }
      }
   }

   public void toggleRailgunSound() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack railgun = findDevice(player, 4);
         if (!railgun.m_41619_()) {
            RailgunSettings s = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
            ModDataComponents.RAILGUN_SETTINGS.set(railgun, s.withSound(!s.soundEnabled()));
         }
      }
   }

   public void toggleRailgunChainDamage() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack railgun = findDevice(player, 4);
         if (!railgun.m_41619_()) {
            RailgunSettings s = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
            ModDataComponents.RAILGUN_SETTINGS.set(railgun, s.withChainDamage(!s.chainDamage()));
         }
      }
   }

   public void cycleRailgunExecutionMode() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack railgun = findDevice(player, 4);
         if (!railgun.m_41619_()) {
            RailgunSettings s = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
            ModDataComponents.RAILGUN_SETTINGS.set(railgun, s.withExecutionMode(s.executionMode().next()));
         }
      }
   }

   public void toggleRailgunChargedSplash() {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack railgun = findDevice(player, 4);
         if (!railgun.m_41619_()) {
            RailgunSettings s = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(railgun, RailgunSettings.DEFAULT);
            ModDataComponents.RAILGUN_SETTINGS.set(railgun, s.withChargedSplash(!s.chargedSplash()));
         }
      }
   }

   public void cycleSelectedModuleConfig(int optionIndex) {
      if (this.getPlayer() instanceof ServerPlayer player) {
         ItemStack deviceStack = findDevice(player, this.selectedTab);
         if (!deviceStack.m_41619_()) {
            if (this.selectedTab != 4) {
               List<CelestweaveArmorSubmodule> submodules = CelestweaveArmorState.collectSubmodules(deviceStack, player.m_9236_().m_9598_());
               if (this.selectedModuleIndex >= 0 && this.selectedModuleIndex < submodules.size()) {
                  CelestweaveArmorSubmodule submodule = submodules.get(this.selectedModuleIndex);
                  List<CelestweaveArmorSubmoduleConfig> configs = submodule.getConfigs(deviceStack);
                  if (optionIndex >= 0 && optionIndex < configs.size()) {
                     CelestweaveArmorSubmoduleConfig config = configs.get(optionIndex);
                     Tag next = config.nextValue();
                     if (next != null) {
                        submodule.setConfig(deviceStack, config.key(), next);
                        if ("flight_inertia".equals(config.key())
                           || "speed_multiplier".equals(config.key())
                           || "phase_mode".equals(config.key())
                           || "flight_lock".equals(config.key())) {
                           CelestweaveArmorState.syncFlightSettingsToClient(player);
                        }

                        if ("phase_block_external_forces".equals(config.key()) || "phase_block_external_teleports".equals(config.key())) {
                           CelestweaveArmorState.syncPhaseLockProtectionToClient(player, deviceStack);
                        }

                        if ("phase_armor_lock".equals(config.key()) && !PhaseLockSubmodule.isArmorLockEnabled(deviceStack)) {
                           PhaseLockService.release(player);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static ItemStack findDevice(Player player, int tab) {
      return switch (tab) {
         case 0 -> findArmor(player, EquipmentSlot.HEAD);
         case 1 -> findArmor(player, EquipmentSlot.CHEST);
         case 2 -> findArmor(player, EquipmentSlot.LEGS);
         case 3 -> findArmor(player, EquipmentSlot.FEET);
         case 4 -> findRailgun(player);
         default -> ItemStack.f_41583_;
      };
   }

   private static ItemStack findArmor(Player player, EquipmentSlot slot) {
      return CelestweaveEquipmentAccess.findArmor(player, slot);
   }

   private static ItemStack findRailgun(Player player) {
      ItemStack main = player.m_21205_();
      if (main.m_41720_() instanceof ElectromagneticRailgunItem) {
         return main;
      } else {
         ItemStack off = player.m_21206_();
         return off.m_41720_() instanceof ElectromagneticRailgunItem ? off : ItemStack.f_41583_;
      }
   }

   private static class ServerData implements ContainerData {
      final int[] values = new int[2];

      public int m_6413_(int index) {
         return index >= 0 && index < this.values.length ? this.values[index] : 0;
      }

      public void m_8050_(int index, int value) {
         if (index >= 0 && index < this.values.length) {
            this.values[index] = value;
         }
      }

      public int m_6499_() {
         return 2;
      }
   }
}
