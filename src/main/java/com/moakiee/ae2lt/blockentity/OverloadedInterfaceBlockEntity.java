package com.moakiee.ae2lt.blockentity;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.InterfaceLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.google.common.util.concurrent.Runnables;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.item.OverloadedFilterComponentItem;
import com.moakiee.ae2lt.logic.AppFluxHelper;
import com.moakiee.ae2lt.logic.ConnectionEndpoints;
import com.moakiee.ae2lt.logic.EjectModeRegistry;
import com.moakiee.ae2lt.logic.FilteredInsertGenericInv;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.logic.OverloadedInterfaceLogic;
import com.moakiee.ae2lt.logic.OverloadedInterfaceTickDecider;
import com.moakiee.ae2lt.logic.WirelessConnectionLists;
import com.moakiee.ae2lt.logic.WirelessConnectionRange;
import com.moakiee.ae2lt.logic.WirelessConnectionRef;
import com.moakiee.ae2lt.logic.WirelessConnectionValidator;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import com.moakiee.ae2lt.logic.energy.PowerCostUtil;
import com.moakiee.ae2lt.logic.energy.TargetAccess;
import com.moakiee.ae2lt.logic.energy.WirelessEnergyAPI;
import com.moakiee.ae2lt.logic.energy.WirelessEnergyDistributor;
import com.moakiee.ae2lt.menu.OverloadedInterfaceMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OverloadedInterfaceBlockEntity extends InterfaceBlockEntity implements FrequencyBindingHost {
   public static final int SLOT_COUNT = 36;
   public static final int MAX_WIRELESS_CONNECTIONS = 1024;
   private static final double IDLE_BASE = 5.0;
   private static final double IDLE_WIRELESS_BONUS = 5.0;
   private static final double IDLE_PER_CONNECTION = 1.0;
   private static final double IDLE_FAST_MULTIPLIER = 1.5;
   private static final String TAG_INTERFACE_MODE = "InterfaceMode";
   private static final String TAG_EXPORT_MODE = "ExportMode";
   private static final String TAG_IMPORT_MODE = "ImportMode";
   private static final String TAG_IO_SPEED_MODE = "IOSpeedMode";
   private static final String TAG_CONNECTIONS = "WirelessConnections";
   private static final String TAG_ENERGY_DIR = "EnergyDir";
   private static final String TAG_UNLIMITED_SLOTS = "UnlimitedSlots";
   private static final String TAG_FILTER_INV = "FilterInv";
   private static final String TAG_IMPORT_BUFFER = "ae2ltImportBuffer";
   private static final String TAG_IMPORT_FLUSH_TICK = "ae2ltImportFlushTick";
   private static final List<Direction> ALL_NORMAL_IO_DIRECTIONS = List.of(Direction.values());
   private static final int WRAPPER_REFRESH_TICKS = 20;
   private static final int NORMAL_CD_INIT = 5;
   private static final int NORMAL_CD_MIN = 5;
   private static final int NORMAL_CD_MAX = 80;
   private static final int FAST_CD_INIT = 5;
   private static final int FAST_CD_MIN = 1;
   private static final int FAST_CD_MAX = 40;
   private static final float[] PROBE_LEVELS = new float[]{5.0F, 3.0F, 2.0F, 1.0F, 0.5F, 0.3F, 0.1F};
   private static final int IMPORT_FLUSH_INTERVAL = 5;
   private static final int STOP_IMPORT_TTL = 20;
   private static final double NORMAL_TARGET_FILL = 0.85;
   private static final double RATE_EMA_ALPHA = 0.2;
   private static final int IO_WHEEL_SLOTS = 128;
   private static final int IMPORT_KEY_CACHE_TTL = 40;
   private static final int IMPORT_EMPTY_KEY_CACHE_TTL = 20;
   private static final int IMPORT_KEY_CACHE_MAX_KEYS = 256;
   private static final int IMPORT_KEY_CACHE_TRUNCATED_TTL = 5;
   private static final int EXPORT_REJECT_BACKOFF_INIT = 10;
   private static final int EXPORT_REJECT_BACKOFF_MAX = 80;
   private static final int EXPORT_REJECT_BACKOFF_MAX_KEYS = 128;
   private static final long IMPORT_TRANSFER_LIMIT = Long.MAX_VALUE;
   private KeyCounter scanBuffer = new KeyCounter();
   private final Map<AEKey, Long> importBuffer = new LinkedHashMap<>();
   private final Map<AEKeyType, Long> keyTypeLockUntil = new IdentityHashMap<>();
   private final Map<AEKeyType, List<OverloadedInterfaceBlockEntity.ExportConfigEntry>> exportConfigCache = new IdentityHashMap<>();
   private long importBufferLastFlushTick = Long.MIN_VALUE;
   private long exportConfigCacheTick = Long.MIN_VALUE;
   private int exportConfigCacheHash;
   private boolean exportConfigCacheValid;
   private long lastEnergyTickGameTime = -1L;
   private static final int VALIDATE_INTERVAL = 20;
   private final Map<OverloadedInterfaceBlockEntity.WirelessConnection, OverloadedInterfaceBlockEntity.ConnectionState> connectionStates = new HashMap<>();
   private final Map<Direction, OverloadedInterfaceBlockEntity.ConnectionState> normalConnectionStates = new EnumMap<>(Direction.class);
   private List<OverloadedInterfaceBlockEntity.WirelessConnection> validConnectionsCache = List.of();
   private long validConnectionsCacheTick = -1L;
   private boolean connectionsDirty = true;
   private int invalidConnectionScanCursor;
   private List<WirelessEnergyAPI.Target> validEnergyTargetsCache = List.of();
   private int validEnergyTargetsVersion;
   private final List<OverloadedInterfaceBlockEntity.IoScheduledEntry>[] ioWheel = new ArrayList[128];
   private final Map<OverloadedInterfaceBlockEntity.IoEntryKey, OverloadedInterfaceBlockEntity.IoScheduledEntry> ioEntries;
   private final List<OverloadedInterfaceBlockEntity.IoScheduledEntry> dueIoEntries;
   private long lastIOWheelTick;
   private long lastIOEntryRefreshTick;
   private int ioScheduleGeneration;
   private boolean ioWheelDirty;
   private OverloadedInterfaceBlockEntity.InterfaceMode interfaceMode;
   private OverloadedInterfaceBlockEntity.IOSpeedMode ioSpeedMode;
   private OverloadedInterfaceBlockEntity.ExportMode exportMode;
   private OverloadedInterfaceBlockEntity.ImportMode importMode;
   @Nullable
   private Direction energyOutputDir;
   private final boolean[] unlimitedSlots;
   private final List<OverloadedInterfaceBlockEntity.WirelessConnection> connections;
   private final FrequencyBindingHelper frequencyBinding;
   private final IActionSource machineSource;
   private final InternalInventoryHost filterInvHost;
   private final AppEngInternalInventory filterInv;
   @Nullable
   private GenericInternalInventory exposedGenericInv;
   private final WirelessEnergyDistributor wirelessDistributor;
   @Nullable
   private Set<AEKey> importFilterKeys;
   @Nullable
   private FuzzyMode importFilterFuzzyMode;
   private boolean importFilterInverted;
   private boolean inductionCardCacheDirty;
   private boolean inductionCardInstalledCache;
   private boolean unloadingChunk;
   private transient int lastViewedPage;
   private Set<AEKey> exportBlacklistCache;
   private long exportBlacklistTick;

   public int getLastViewedPage() {
      return this.lastViewedPage;
   }

   public void setLastViewedPage(int p) {
      this.lastViewedPage = p;
   }

   public OverloadedInterfaceBlockEntity(BlockEntityType<?> betype, BlockPos pos, BlockState state) {
      super(betype, pos, state);

      for (int i = 0; i < 128; i++) {
         this.ioWheel[i] = new ArrayList<>();
      }

      this.ioEntries = new HashMap<>();
      this.dueIoEntries = new ArrayList<>();
      this.lastIOWheelTick = -1L;
      this.lastIOEntryRefreshTick = Long.MIN_VALUE;
      this.ioScheduleGeneration = 1;
      this.ioWheelDirty = true;
      this.interfaceMode = OverloadedInterfaceBlockEntity.InterfaceMode.NORMAL;
      this.ioSpeedMode = OverloadedInterfaceBlockEntity.IOSpeedMode.NORMAL;
      this.exportMode = OverloadedInterfaceBlockEntity.ExportMode.OFF;
      this.importMode = OverloadedInterfaceBlockEntity.ImportMode.OFF;
      this.energyOutputDir = null;
      this.unlimitedSlots = new boolean[36];
      this.connections = new ArrayList<>();
      this.frequencyBinding = new FrequencyBindingHelper(this);
      this.machineSource = IActionSource.ofMachine(this);
      this.filterInvHost = new InternalInventoryHost() {
         public void saveChanges() {
            OverloadedInterfaceBlockEntity.this.saveChanges();
            OverloadedInterfaceBlockEntity.this.markForUpdate();
         }

         public void onChangeInventory(InternalInventory inv, int slot) {
            OverloadedInterfaceBlockEntity.this.rebuildFilter();
         }

         public boolean isClientSide() {
            return OverloadedInterfaceBlockEntity.this.f_58857_ != null && OverloadedInterfaceBlockEntity.this.f_58857_.m_5776_();
         }
      };
      this.filterInv = new AppEngInternalInventory(this.filterInvHost, 1) {
         public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.m_41619_() && stack.m_41720_() instanceof OverloadedFilterComponentItem;
         }
      };
      this.wirelessDistributor = new WirelessEnergyDistributor(new OverloadedInterfaceBlockEntity.DistributorHost());
      this.inductionCardCacheDirty = true;
      this.inductionCardInstalledCache = false;
      this.unloadingChunk = false;
      this.lastViewedPage = 0;
      this.exportBlacklistCache = Set.of();
      this.exportBlacklistTick = -1L;
   }

   public OverloadedInterfaceBlockEntity(BlockPos pos, BlockState state) {
      this((BlockEntityType<?>)ModBlockEntities.OVERLOADED_INTERFACE.get(), pos, state);
   }

   @Override
   public FrequencyBindingHelper getFrequencyBinding() {
      return this.frequencyBinding;
   }

   @Override
   public AENetworkBlockEntity getFrequencyBindingBlockEntity() {
      return this;
   }

   @Override
   public void saveFrequencyBindingChanges() {
      this.saveChanges();
   }

   @Override
   public void markFrequencyBindingForUpdate() {
      this.markForUpdate();
   }

   public void onLoad() {
      super.onLoad();
      this.unloadingChunk = false;
      this.recomputeIdlePower();
      if (this.f_58857_ != null && !this.f_58857_.m_5776_() && this.importMode == OverloadedInterfaceBlockEntity.ImportMode.EJECT) {
         this.refreshEjectRegistrations();
      }
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
   }

   public void onMainNodeStateChanged(State reason) {
      super.onMainNodeStateChanged(reason);
      this.frequencyBinding.onMainNodeStateChanged(reason);
   }

   protected InterfaceLogic createLogic() {
      return new OverloadedInterfaceLogic(this.getMainNode(), this, this.getItemFromBlockEntity().m_5456_(), 36);
   }

   public void openMenu(Player player, MenuLocator locator) {
      if (this.f_58857_ instanceof ServerLevel) {
         this.clearInvalidConnections();
      }

      MenuOpener.open(OverloadedInterfaceMenu.TYPE, player, locator);
   }

   public void returnToMainMenu(Player player, ISubMenu subMenu) {
      MenuOpener.returnTo(OverloadedInterfaceMenu.TYPE, player, subMenu.getLocator());
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.DENSE_SMART;
   }

   @Nullable
   public GenericInternalInventory getExposedGenericInv() {
      if (this.exposedGenericInv == null && this.getInterfaceLogic() instanceof OverloadedInterfaceLogic ol) {
         this.exposedGenericInv = new FilteredInsertGenericInv(ol.getProxiedStorage(), this::isInsertAllowedByFilter);
      }

      return this.exposedGenericInv;
   }

   public AppEngInternalInventory getFilterInv() {
      return this.filterInv;
   }

   public void rebuildFilter() {
      this.wakeWirelessIo();
      ItemStack filterStack = this.filterInv.getStackInSlot(0);
      if (!filterStack.m_41619_() && filterStack.m_41720_() instanceof ICellWorkbenchItem cwi) {
         ConfigInventory var8 = cwi.getConfigInventory(filterStack);
         HashSet keys = new HashSet();

         for (int upgrades = 0; upgrades < var8.size(); upgrades++) {
            AEKey k = var8.getKey(upgrades);
            if (k != null) {
               keys.add(k);
            }
         }

         if (keys.isEmpty()) {
            this.importFilterKeys = null;
            this.importFilterFuzzyMode = null;
            this.importFilterInverted = false;
         } else {
            IUpgradeInventory upgradesx = cwi.getUpgrades(filterStack);
            boolean hasFuzzy = upgradesx.getInstalledUpgrades(AEItems.FUZZY_CARD) > 0;
            boolean hasInverter = upgradesx.getInstalledUpgrades(AEItems.INVERTER_CARD) > 0;
            this.importFilterKeys = Set.copyOf(keys);
            this.importFilterFuzzyMode = hasFuzzy ? cwi.getFuzzyMode(filterStack) : null;
            this.importFilterInverted = hasInverter;
         }
      } else {
         this.importFilterKeys = null;
         this.importFilterFuzzyMode = null;
         this.importFilterInverted = false;
      }
   }

   public OverloadedInterfaceBlockEntity.InterfaceMode getInterfaceMode() {
      return this.interfaceMode;
   }

   public void setInterfaceMode(OverloadedInterfaceBlockEntity.InterfaceMode m) {
      if (this.interfaceMode != m) {
         this.interfaceMode = m;
         this.invalidateConnectionCache();
         this.refreshEjectRegistrations();
         this.recomputeIdlePower();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public OverloadedInterfaceBlockEntity.IOSpeedMode getIOSpeedMode() {
      return this.ioSpeedMode;
   }

   public void setIOSpeedMode(OverloadedInterfaceBlockEntity.IOSpeedMode m) {
      if (this.ioSpeedMode != m) {
         this.ioSpeedMode = m;
         this.wakeWirelessIo();
         this.recomputeIdlePower();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public OverloadedInterfaceBlockEntity.ExportMode getExportMode() {
      return this.exportMode;
   }

   public void setExportMode(OverloadedInterfaceBlockEntity.ExportMode m) {
      if (this.exportMode != m) {
         this.exportMode = m;
         this.wakeWirelessIo();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public OverloadedInterfaceBlockEntity.ImportMode getImportMode() {
      return this.importMode;
   }

   public void setImportMode(OverloadedInterfaceBlockEntity.ImportMode m) {
      if (this.importMode != m) {
         OverloadedInterfaceBlockEntity.ImportMode old = this.importMode;
         this.importMode = m;
         if (old == OverloadedInterfaceBlockEntity.ImportMode.EJECT != (m == OverloadedInterfaceBlockEntity.ImportMode.EJECT)) {
            this.refreshEjectRegistrations();
         }

         this.wakeWirelessIo();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public boolean isSlotUnlimited(int slot) {
      return slot >= 0 && slot < 36 && this.unlimitedSlots[slot];
   }

   public void setSlotUnlimited(int slot, boolean unlimited) {
      if (slot >= 0 && slot < 36) {
         if (this.unlimitedSlots[slot] != unlimited) {
            this.unlimitedSlots[slot] = unlimited;
            this.invalidateExportConfigCache();
            this.saveChanges();
            this.markForUpdate();
         }
      }
   }

   public void onGridIoConfigChanged() {
      this.invalidateExportConfigCache();
      this.wakeWirelessIo();
   }

   @Nullable
   public Direction getEnergyOutputDir() {
      return this.energyOutputDir;
   }

   public void setEnergyOutputDir(@Nullable Direction d) {
      if (this.energyOutputDir != d) {
         this.energyOutputDir = d;
         this.normalConnectionStates.clear();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public void invalidateInductionCardCache() {
      this.inductionCardCacheDirty = true;
      this.wakeWirelessIo();
   }

   void recomputeIdlePower() {
      double idle = 5.0;
      if (this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
         idle += 5.0;
         idle += (double)this.connections.size() * 1.0;
      }

      if (this.ioSpeedMode == OverloadedInterfaceBlockEntity.IOSpeedMode.FAST) {
         idle *= 1.5;
      }

      this.getMainNode().setIdlePowerUsage(idle);
   }

   public List<OverloadedInterfaceBlockEntity.WirelessConnection> getConnections() {
      return Collections.unmodifiableList(this.connections);
   }

   public boolean addOrUpdateConnection(OverloadedInterfaceBlockEntity.WirelessConnection conn) {
      if (!this.isLocalDimension(conn.dimension())) {
         return false;
      } else {
         int index = this.indexOfConnectionEndpoint(conn.dimension(), conn.pos(), conn.boundFace());
         if (index >= 0) {
            if (this.connections.get(index).equals(conn)) {
               return true;
            } else {
               this.connections.set(index, conn);
               this.invalidConnectionScanCursor = 0;
               this.invalidateConnectionCache();
               this.refreshEjectRegistrations();
               this.recomputeIdlePower();
               this.saveChanges();
               this.markForUpdate();
               return true;
            }
         } else if (this.connections.size() >= 1024) {
            return false;
         } else {
            this.connections.add(conn);
            this.invalidConnectionScanCursor = 0;
            this.invalidateConnectionCache();
            this.refreshEjectRegistrations();
            this.recomputeIdlePower();
            this.saveChanges();
            this.markForUpdate();
            return true;
         }
      }
   }

   public boolean removeConnection(ResourceKey<Level> dim, BlockPos pos, Direction face) {
      int index = this.indexOfConnectionEndpoint(dim, pos, face);
      if (index < 0) {
         return false;
      } else {
         this.connections.remove(index);
         this.invalidConnectionScanCursor = 0;
         this.invalidateConnectionCache();
         this.refreshEjectRegistrations();
         this.recomputeIdlePower();
         this.saveChanges();
         this.markForUpdate();
         return true;
      }
   }

   public boolean removeConnection(ResourceKey<Level> dim, BlockPos pos) {
      int index = WirelessConnectionLists.indexOf(this.connections, dim, pos);
      if (index < 0) {
         return false;
      } else {
         this.connections.remove(index);
         this.invalidConnectionScanCursor = 0;
         this.invalidateConnectionCache();
         this.refreshEjectRegistrations();
         this.recomputeIdlePower();
         this.saveChanges();
         this.markForUpdate();
         return true;
      }
   }

   private int indexOfConnectionEndpoint(ResourceKey<Level> dimension, BlockPos pos, Direction face) {
      return ConnectionEndpoints.indexOfEndpoint(
         this.connections,
         dimension,
         pos,
         face,
         OverloadedInterfaceBlockEntity.WirelessConnection::dimension,
         OverloadedInterfaceBlockEntity.WirelessConnection::pos,
         OverloadedInterfaceBlockEntity.WirelessConnection::boundFace
      );
   }

   public int clearInvalidConnections() {
      return this.pruneInvalidConnections(Integer.MAX_VALUE);
   }

   public int pruneInvalidConnections(int maxChecks) {
      if (this.f_58857_ instanceof ServerLevel serverLevel && maxChecks > 0 && !this.connections.isEmpty()) {
         WirelessConnectionLists.PruneResult result = WirelessConnectionLists.pruneInvalid(
            this.connections, this.invalidConnectionScanCursor, maxChecks, serverLevel, this.m_58899_()
         );
         this.invalidConnectionScanCursor = result.nextCursor();
         if (result.removed() > 0) {
            this.invalidateConnectionCache();
            this.refreshEjectRegistrations();
            this.recomputeIdlePower();
            this.saveChanges();
            this.markForUpdate();
         }

         return result.removed();
      }

      return 0;
   }

   private void tickWirelessConnectionCleanup(ServerLevel level) {
      if (!this.connections.isEmpty() && WirelessConnectionValidator.shouldRunPeriodicPrune(level, this.m_58899_())) {
         this.pruneInvalidConnections(64);
      }
   }

   private boolean isLocalDimension(ResourceKey<Level> dimension) {
      return WirelessConnectionLists.isLocalDimension(this.f_58857_, dimension);
   }

   private OverloadedInterfaceBlockEntity.ConnectionState getOrCreateState(OverloadedInterfaceBlockEntity.WirelessConnection conn) {
      return this.connectionStates.computeIfAbsent(conn, k -> new OverloadedInterfaceBlockEntity.ConnectionState());
   }

   private void invalidateConnectionCache() {
      this.connectionsDirty = true;
      this.validConnectionsCache = List.of();
      this.validConnectionsCacheTick = -1L;
      if (!this.validEnergyTargetsCache.isEmpty()) {
         this.validEnergyTargetsCache = List.of();
      }

      this.validEnergyTargetsVersion++;
      this.connectionStates.clear();
      this.normalConnectionStates.clear();
      this.resetIOWheel();
      this.wirelessDistributor.clearTickState(true);
      this.alertGridTicker();
   }

   private void resetIOWheel() {
      for (List<OverloadedInterfaceBlockEntity.IoScheduledEntry> slot : this.ioWheel) {
         slot.clear();
      }

      this.dueIoEntries.clear();
      this.ioEntries.clear();
      this.lastIOWheelTick = -1L;
      this.lastIOEntryRefreshTick = Long.MIN_VALUE;
      this.ioScheduleGeneration++;
      this.ioWheelDirty = true;
   }

   private void wakeWirelessIo() {
      for (OverloadedInterfaceBlockEntity.ConnectionState state : this.connectionStates.values()) {
         state.resetWirelessIo(this.ioSpeedMode);
      }

      for (OverloadedInterfaceBlockEntity.ConnectionState state : this.normalConnectionStates.values()) {
         state.resetWirelessIo(this.ioSpeedMode);
      }

      this.keyTypeLockUntil.clear();
      this.resetIOWheel();
      this.alertGridTicker();
   }

   private void alertGridTicker() {
      this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }

   private void invalidateExportConfigCache() {
      this.exportConfigCache.clear();
      this.exportConfigCacheTick = Long.MIN_VALUE;
      this.exportConfigCacheValid = false;
      this.exportBlacklistCache = Set.of();
      this.exportBlacklistTick = -1L;
      this.alertGridTicker();
   }

   private List<OverloadedInterfaceBlockEntity.WirelessConnection> getOrRefreshValidConnections(ServerLevel sl, long gameTick) {
      if (!this.connectionsDirty && gameTick - this.validConnectionsCacheTick < 20L) {
         return this.validConnectionsCache;
      } else {
         this.clearInvalidConnections();
         ArrayList<OverloadedInterfaceBlockEntity.WirelessConnection> valid = new ArrayList<>();

         for (OverloadedInterfaceBlockEntity.WirelessConnection c : this.connections) {
            if (WirelessConnectionValidator.validate(sl, this.m_58899_(), c) == WirelessConnectionValidator.Status.VALID) {
               valid.add(c);
            }
         }

         List<OverloadedInterfaceBlockEntity.WirelessConnection> newCache = List.copyOf(valid);
         if (!newCache.equals(this.validConnectionsCache)) {
            this.validConnectionsCache = newCache;
            this.rebuildEnergyTargets();
         }

         this.validConnectionsCacheTick = gameTick;
         this.connectionsDirty = false;
         return this.validConnectionsCache;
      }
   }

   private void rebuildEnergyTargets() {
      if (this.validConnectionsCache.isEmpty()) {
         this.validEnergyTargetsCache = List.of();
      } else {
         ArrayList<WirelessEnergyAPI.Target> snapshot = new ArrayList<>(this.validConnectionsCache.size());

         for (OverloadedInterfaceBlockEntity.WirelessConnection conn : this.validConnectionsCache) {
            snapshot.add(new WirelessEnergyAPI.Target(conn.dimension(), conn.pos(), conn.boundFace()));
         }

         this.validEnergyTargetsCache = List.copyOf(snapshot);
      }

      this.validEnergyTargetsVersion++;
   }

   @Nullable
   private ServerLevel resolveTargetLevel(ServerLevel origin, OverloadedInterfaceBlockEntity.WirelessConnection conn) {
      if (!conn.dimension().equals(origin.m_46472_())) {
         return null;
      } else if (!WirelessConnectionRange.isConnectorLinkInRange(origin.m_46472_(), this.m_58899_(), conn.dimension(), conn.pos())) {
         return null;
      } else {
         ServerLevel tl = origin.m_7654_().m_129880_(conn.dimension());
         return tl != null && tl.m_46749_(conn.pos()) ? tl : null;
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, OverloadedInterfaceBlockEntity be) {
      if (level != null && !level.m_5776_()) {
         if (level instanceof ServerLevel sl) {
            be.frequencyBinding.serverTick();
            be.tickWirelessConnectionCleanup(sl);
            if (be.hasServerEnergyWork()) {
               be.tickEnergyTransfer(sl);
            }
         }
      }
   }

   private boolean hasServerEnergyWork() {
      boolean wirelessMode = this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS;
      boolean hasConnections = !this.connections.isEmpty();
      boolean hasEnergyOutput = this.energyOutputDir != null;
      boolean hasFeKey = AppFluxHelper.FE_KEY != null;
      boolean mayTransferEnergy = wirelessMode && hasConnections || hasEnergyOutput;
      boolean hasInduction = mayTransferEnergy && hasFeKey && this.hasInductionCard();
      return OverloadedInterfaceTickDecider.hasServerEnergyWork(wirelessMode, hasConnections, hasEnergyOutput, hasFeKey, hasInduction);
   }

   public boolean hasGridItemIoWork() {
      return OverloadedInterfaceTickDecider.hasGridItemIoWork(
         this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS,
         !this.importBuffer.isEmpty(),
         !this.connections.isEmpty(),
         this.importMode == OverloadedInterfaceBlockEntity.ImportMode.AUTO,
         this.exportMode == OverloadedInterfaceBlockEntity.ExportMode.AUTO
      );
   }

   public void tickGridItemIo() {
      if (this.f_58857_ instanceof ServerLevel sl && this.getMainNode().isActive()) {
         if (this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
            this.tickWirelessIO(sl);
         } else {
            this.tickNormalIO(sl);
         }

         return;
      }
   }

   private void tickNormalIO(ServerLevel sl) {
      if (this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.NORMAL) {
         IGrid grid = this.getMainNode().getGrid();
         if (grid != null) {
            long now = sl.m_46467_();
            MEStorage meStorage = grid.getStorageService().getInventory();
            IActionSource source = this.machineSource;
            this.flushImportBuffer(meStorage, source, now);
            boolean activeImport = this.importMode == OverloadedInterfaceBlockEntity.ImportMode.AUTO;
            boolean activeExport = this.exportMode == OverloadedInterfaceBlockEntity.ExportMode.AUTO;
            if (activeImport || activeExport) {
               for (Direction direction : this.normalIoDirections()) {
                  OverloadedInterfaceBlockEntity.WirelessConnection conn = new OverloadedInterfaceBlockEntity.WirelessConnection(
                     sl.m_46472_(), this.m_58899_().m_121945_(direction), direction.m_122424_()
                  );
                  OverloadedInterfaceBlockEntity.ConnectionState state = this.normalConnectionStates
                     .computeIfAbsent(direction, ignored -> new OverloadedInterfaceBlockEntity.ConnectionState());
                  Map<AEKeyType, MEStorage> wrappers = state.resolveWrappers(sl, conn);
                  if (wrappers != null) {
                     for (Entry<AEKeyType, MEStorage> wrapperEntry : wrappers.entrySet()) {
                        AEKeyType keyType = wrapperEntry.getKey();
                        if (isWirelessIoKeyType(keyType)) {
                           MEStorage wrapper = wrapperEntry.getValue();
                           if (activeImport) {
                              this.runNormalImportIfDue(state, keyType, wrapper, source, now);
                           }

                           if (activeExport) {
                              this.runNormalExportIfDue(state, keyType, wrapper, meStorage, source, now);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private List<Direction> normalIoDirections() {
      return OverloadedInterfaceTickDecider.normalIoDirectionCount(this.energyOutputDir != null) == 1 && this.energyOutputDir != null
         ? List.of(this.energyOutputDir)
         : ALL_NORMAL_IO_DIRECTIONS;
   }

   private void runNormalImportIfDue(OverloadedInterfaceBlockEntity.ConnectionState state, AEKeyType keyType, MEStorage wrapper, IActionSource source, long now) {
      OverloadedInterfaceBlockEntity.CooldownTracker cd = state.cdFor(keyType, OverloadedInterfaceBlockEntity.IoDirection.IMPORT);
      if (cd.cooldownUntil() <= now) {
         long locked = this.lockedUntil(keyType, now);
         if (locked > now) {
            cd.cooldownUntil = locked;
         } else {
            this.runExtract(state, keyType, wrapper, source, now, Long.MAX_VALUE);
         }
      }
   }

   private void runNormalExportIfDue(
      OverloadedInterfaceBlockEntity.ConnectionState state, AEKeyType keyType, MEStorage wrapper, MEStorage meStorage, IActionSource source, long now
   ) {
      OverloadedInterfaceBlockEntity.CooldownTracker cd = state.cdFor(keyType, OverloadedInterfaceBlockEntity.IoDirection.EXPORT);
      if (cd.cooldownUntil() <= now) {
         this.runExport(state, keyType, wrapper, meStorage, source, now);
      }
   }

   private void tickWirelessIO(ServerLevel sl) {
      if (this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
         IGrid grid = this.getMainNode().getGrid();
         if (grid != null) {
            long now = sl.m_46467_();
            MEStorage meStorage = grid.getStorageService().getInventory();
            IActionSource source = this.machineSource;
            this.flushImportBuffer(meStorage, source, now);
            boolean activeImport = this.importMode == OverloadedInterfaceBlockEntity.ImportMode.AUTO;
            boolean activeExport = this.exportMode == OverloadedInterfaceBlockEntity.ExportMode.AUTO;
            if (activeImport || activeExport) {
               List<OverloadedInterfaceBlockEntity.WirelessConnection> valid = this.getOrRefreshValidConnections(sl, now);
               if (!valid.isEmpty()) {
                  this.refreshIOWheel(sl, valid, now, activeImport, activeExport);
                  this.pollIOWheel(now);

                  for (OverloadedInterfaceBlockEntity.IoScheduledEntry entry : this.dueIoEntries) {
                     if (this.isEntryStillValid(entry)) {
                        ServerLevel targetLevel = this.resolveTargetLevel(sl, entry.conn);
                        if (targetLevel == null) {
                           entry.state.cdFor(entry.keyType, entry.direction).onFail(now, this.ioSpeedMode);
                           this.rescheduleEntry(entry, now);
                        } else {
                           Map<AEKeyType, MEStorage> wrappers = entry.state.resolveWrappers(targetLevel, entry.conn);
                           MEStorage wrapper = wrappers != null ? wrappers.get(entry.keyType) : null;
                           if (wrapper == null) {
                              entry.state.cdFor(entry.keyType, entry.direction).onFail(now, this.ioSpeedMode);
                              this.rescheduleEntry(entry, now);
                           } else {
                              if (entry.direction == OverloadedInterfaceBlockEntity.IoDirection.IMPORT) {
                                 long lockedUntil = this.lockedUntil(entry.keyType, now);
                                 if (lockedUntil > now) {
                                    this.scheduleEntryAt(entry, lockedUntil);
                                    continue;
                                 }

                                 if (entry.phase == OverloadedInterfaceBlockEntity.IoPhase.PROBE) {
                                    this.runProbe(entry.state, entry.keyType, wrapper, source, now);
                                 } else {
                                    this.runExtract(entry.state, entry.keyType, wrapper, source, now, Long.MAX_VALUE);
                                 }
                              } else {
                                 this.runExport(entry.state, entry.keyType, wrapper, meStorage, source, now);
                              }

                              this.rescheduleEntry(entry, now);
                           }
                        }
                     }
                  }

                  this.dueIoEntries.clear();
               }
            }
         }
      }
   }

   private void refreshIOWheel(
      ServerLevel sl, List<OverloadedInterfaceBlockEntity.WirelessConnection> valid, long now, boolean activeImport, boolean activeExport
   ) {
      if (this.ioWheelDirty || this.lastIOEntryRefreshTick == Long.MIN_VALUE || now - this.lastIOEntryRefreshTick >= 20L) {
         this.lastIOEntryRefreshTick = now;

         for (OverloadedInterfaceBlockEntity.WirelessConnection conn : valid) {
            OverloadedInterfaceBlockEntity.ConnectionState state = this.getOrCreateState(conn);
            ServerLevel targetLevel = this.resolveTargetLevel(sl, conn);
            if (targetLevel != null) {
               Map<AEKeyType, MEStorage> wrappers = state.resolveWrappers(targetLevel, conn);
               if (wrappers != null) {
                  for (AEKeyType keyType : wrappers.keySet()) {
                     if (isWirelessIoKeyType(keyType)) {
                        if (activeImport) {
                           this.ensureIOEntry(conn, state, keyType, OverloadedInterfaceBlockEntity.IoDirection.IMPORT, now);
                        }

                        if (activeExport) {
                           this.ensureIOEntry(conn, state, keyType, OverloadedInterfaceBlockEntity.IoDirection.EXPORT, now);
                        }
                     }
                  }
               }
            }
         }

         this.ioWheelDirty = false;
      }
   }

   private void ensureIOEntry(
      OverloadedInterfaceBlockEntity.WirelessConnection conn,
      OverloadedInterfaceBlockEntity.ConnectionState state,
      AEKeyType keyType,
      OverloadedInterfaceBlockEntity.IoDirection direction,
      long now
   ) {
      OverloadedInterfaceBlockEntity.IoEntryKey key = new OverloadedInterfaceBlockEntity.IoEntryKey(conn, keyType, direction);
      if (!this.ioEntries.containsKey(key)) {
         OverloadedInterfaceBlockEntity.IoScheduledEntry entry = new OverloadedInterfaceBlockEntity.IoScheduledEntry(
            conn, state, keyType, direction, this.ioScheduleGeneration
         );
         entry.state.cdFor(keyType, direction).reset(this.ioSpeedMode);
         this.ioEntries.put(key, entry);
         this.scheduleEntryAt(entry, now + 1L);
      }
   }

   private static boolean isWirelessIoKeyType(AEKeyType keyType) {
      return AppFluxHelper.FE_KEY == null || keyType != AppFluxHelper.FE_KEY.getType();
   }

   private void pollIOWheel(long now) {
      this.dueIoEntries.clear();
      long start;
      if (this.lastIOWheelTick < 0L) {
         start = now;
      } else if (now - this.lastIOWheelTick >= 128L) {
         start = now - 128L + 1L;
      } else {
         start = this.lastIOWheelTick + 1L;
      }

      for (long tick = start; tick <= now; tick++) {
         List<OverloadedInterfaceBlockEntity.IoScheduledEntry> slot = this.ioWheel[(int)(tick % 128L)];
         if (!slot.isEmpty()) {
            this.dueIoEntries.addAll(slot);
            slot.clear();
         }
      }

      this.lastIOWheelTick = now;
   }

   private boolean isEntryStillValid(OverloadedInterfaceBlockEntity.IoScheduledEntry entry) {
      if (entry.generation != this.ioScheduleGeneration) {
         return false;
      } else if (!isWirelessIoKeyType(entry.keyType)) {
         return false;
      } else if (this.connectionStates.get(entry.conn) != entry.state) {
         return false;
      } else {
         return entry.direction == OverloadedInterfaceBlockEntity.IoDirection.IMPORT
            ? this.importMode == OverloadedInterfaceBlockEntity.ImportMode.AUTO
            : this.exportMode == OverloadedInterfaceBlockEntity.ExportMode.AUTO;
      }
   }

   private void scheduleEntryAt(OverloadedInterfaceBlockEntity.IoScheduledEntry entry, long dueTick) {
      long target = Math.max(1L, dueTick);
      this.ioWheel[(int)(target % 128L)].add(entry);
   }

   private void rescheduleEntry(OverloadedInterfaceBlockEntity.IoScheduledEntry entry, long now) {
      OverloadedInterfaceBlockEntity.CooldownTracker cd = entry.state.cdFor(entry.keyType, entry.direction);
      if (entry.direction == OverloadedInterfaceBlockEntity.IoDirection.EXPORT) {
         entry.phase = OverloadedInterfaceBlockEntity.IoPhase.EXTRACT;
         this.scheduleEntryAt(entry, nextCooldownTick(cd, now));
      } else {
         OverloadedInterfaceBlockEntity.ProbeState probe = entry.state.probeStateFor(entry.keyType);
         if (entry.phase == OverloadedInterfaceBlockEntity.IoPhase.PROBE) {
            boolean probeHit = checkProbeSuccess(entry.state.modelFor(entry.keyType));
            entry.phase = OverloadedInterfaceBlockEntity.IoPhase.EXTRACT;
            if (probeHit) {
               probe.reset();
               this.scheduleEntryAt(entry, now + 1L);
            } else {
               probe.levelIdx = Math.min(probe.levelIdx + 1, PROBE_LEVELS.length - 1);
               this.scheduleEntryAt(entry, nextCooldownTick(cd, now));
            }
         } else {
            long cdUntil = nextCooldownTick(cd, now);
            long probeAt = this.computeProbeInsertTick(probe, cdUntil, now);
            if (probeAt > now && probeAt < cdUntil && cdUntil - now >= (long)probeEnableThreshold(entry.keyType)) {
               entry.phase = OverloadedInterfaceBlockEntity.IoPhase.PROBE;
               this.scheduleEntryAt(entry, probeAt);
            } else {
               entry.phase = OverloadedInterfaceBlockEntity.IoPhase.EXTRACT;
               this.scheduleEntryAt(entry, cdUntil);
            }
         }
      }
   }

   private static long nextCooldownTick(OverloadedInterfaceBlockEntity.CooldownTracker cd, long now) {
      long until = cd.cooldownUntil();
      return until > now ? until : now + 1L;
   }

   private long computeProbeInsertTick(OverloadedInterfaceBlockEntity.ProbeState probe, long cdUntil, long now) {
      float level = PROBE_LEVELS[probe.levelIdx];
      if (level >= 1.0F) {
         return cdUntil - (long)level;
      } else {
         int interval = Math.round(1.0F / level);
         probe.skipCounter++;
         if (probe.skipCounter >= interval) {
            probe.skipCounter = 0;
            return cdUntil - 1L;
         } else {
            return -1L;
         }
      }
   }

   private static int probeEnableThreshold(AEKeyType type) {
      return type == AEKeyType.items() ? 10 : 5;
   }

   private static boolean checkProbeSuccess(OverloadedInterfaceBlockEntity.KeyModel model) {
      return model.effectiveMax > 0L && model.lastAvail >= (long)((double)model.effectiveMax * 0.85);
   }

   private long runProbe(OverloadedInterfaceBlockEntity.ConnectionState state, AEKeyType keyType, MEStorage wrapper, IActionSource src, long now) {
      long totalAvail = this.observeImportAvailable(state, keyType, wrapper, src, now, Long.MAX_VALUE);
      state.modelFor(keyType).onProbe(totalAvail, now);
      return totalAvail;
   }

   private long runExtract(
      OverloadedInterfaceBlockEntity.ConnectionState state, AEKeyType keyType, MEStorage wrapper, IActionSource src, long now, long transferLimit
   ) {
      Set<AEKey> exactFilterKeys = this.getExactImportFilterKeys();
      OverloadedInterfaceBlockEntity.ImportResult result;
      if (exactFilterKeys != null) {
         result = this.extractExactImportKeys(keyType, wrapper, src, transferLimit, exactFilterKeys);
      } else {
         OverloadedInterfaceBlockEntity.ImportKeyCache cache = state.importKeyCacheFor(keyType);
         if (cache.isScanFresh(now) && cache.keys.isEmpty()) {
            result = new OverloadedInterfaceBlockEntity.ImportResult(0L, 0L);
         } else {
            result = this.scanImportKeys(keyType, wrapper, src, cache, now, transferLimit, true);
         }
      }

      OverloadedInterfaceBlockEntity.KeyModel model = state.modelFor(keyType);
      model.onExtract(result.totalAvail(), result.moved(), now);
      OverloadedInterfaceBlockEntity.CooldownTracker cd = state.cdFor(keyType, OverloadedInterfaceBlockEntity.IoDirection.IMPORT);
      if (result.moved() > 0L) {
         this.saveChanges();
         cd.onSuccess(now, this.ioSpeedMode, model);
      } else {
         cd.onFail(now, this.ioSpeedMode);
      }

      return result.moved();
   }

   private long observeImportAvailable(
      OverloadedInterfaceBlockEntity.ConnectionState state, AEKeyType keyType, MEStorage wrapper, IActionSource src, long now, long probeLimit
   ) {
      Set<AEKey> exactFilterKeys = this.getExactImportFilterKeys();
      if (exactFilterKeys != null) {
         return this.observeExactImportAvailable(keyType, wrapper, src, probeLimit, exactFilterKeys);
      } else {
         OverloadedInterfaceBlockEntity.ImportKeyCache cache = state.importKeyCacheFor(keyType);
         return cache.isScanFresh(now) && cache.keys.isEmpty() ? 0L : this.scanImportKeys(keyType, wrapper, src, cache, now, probeLimit, false).totalAvail();
      }
   }

   private OverloadedInterfaceBlockEntity.ImportResult extractExactImportKeys(
      AEKeyType keyType, MEStorage wrapper, IActionSource src, long transferLimit, Set<AEKey> exactFilterKeys
   ) {
      long budget = transferLimit;
      long totalAvail = 0L;
      long moved = 0L;

      for (AEKey key : exactFilterKeys) {
         if (key.getType() == keyType) {
            long available = wrapper.extract(key, Math.max(1L, transferLimit), Actionable.SIMULATE, src);
            if (available > 0L) {
               totalAvail += available;
               if (budget > 0L) {
                  long extracted = this.importExtractToBuffer(key, Math.min(available, budget), wrapper, src);
                  moved += extracted;
                  budget -= extracted;
               }
            }
         }
      }

      return new OverloadedInterfaceBlockEntity.ImportResult(totalAvail, moved);
   }

   private long observeExactImportAvailable(AEKeyType keyType, MEStorage wrapper, IActionSource src, long probeLimit, Set<AEKey> exactFilterKeys) {
      long total = 0L;

      for (AEKey key : exactFilterKeys) {
         if (key.getType() == keyType) {
            long amount = wrapper.extract(key, Math.max(1L, probeLimit), Actionable.SIMULATE, src);
            if (amount > 0L) {
               total += amount;
            }
         }
      }

      return total;
   }

   private OverloadedInterfaceBlockEntity.ImportResult scanImportKeys(
      AEKeyType keyType,
      MEStorage wrapper,
      IActionSource src,
      OverloadedInterfaceBlockEntity.ImportKeyCache cache,
      long now,
      long transferLimit,
      boolean extract
   ) {
      KeyCounter buffer = this.freshScanBuffer();
      wrapper.getAvailableStacks(buffer);
      ArrayList<AEKey> scannedKeys = new ArrayList<>();
      boolean truncated = false;
      long budget = transferLimit;
      long total = 0L;
      long moved = 0L;

      for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> available : buffer) {
         AEKey key = (AEKey)available.getKey();
         if (key.getType() == keyType && this.isImportAllowed(key)) {
            long amount = available.getLongValue();
            if (amount > 0L) {
               total += amount;
               if (scannedKeys.size() < 256) {
                  scannedKeys.add(key);
               } else {
                  truncated = true;
               }

               if (extract && budget > 0L) {
                  long extracted = this.importExtractToBuffer(key, Math.min(amount, budget), wrapper, src);
                  moved += extracted;
                  budget -= extracted;
               }
            }
         }
      }

      cache.update(scannedKeys, truncated, now);
      return new OverloadedInterfaceBlockEntity.ImportResult(total, moved);
   }

   private KeyCounter freshScanBuffer() {
      this.scanBuffer = new KeyCounter();
      return this.scanBuffer;
   }

   private long importExtractToBuffer(AEKey key, long amount, MEStorage wrapper, IActionSource src) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         long affordable = PowerCostUtil.maxAffordable(grid, key, amount);
         if (affordable <= 0L) {
            return 0L;
         } else {
            long extracted = wrapper.extract(key, affordable, Actionable.MODULATE, src);
            if (extracted > 0L) {
               PowerCostUtil.consume(grid, key, extracted);
               this.addToImportBuffer(key, extracted);
            }

            return extracted;
         }
      }
   }

   @Nullable
   private Set<AEKey> getExactImportFilterKeys() {
      if (this.importFilterInverted) {
         return null;
      } else {
         return this.importFilterFuzzyMode == null ? this.importFilterKeys : null;
      }
   }

   private Set<AEKey> getExportBlacklist() {
      if (this.f_58857_ != null && this.f_58857_.m_46467_() == this.exportBlacklistTick) {
         return this.exportBlacklistCache;
      } else {
         ConfigInventory config = this.getInterfaceLogic().getConfig();
         HashSet<AEKey> set = new HashSet<>();

         for (int i = 0; i < config.size(); i++) {
            AEKey key = config.getKey(i);
            if (key != null) {
               set.add(key);
            }
         }

         this.exportBlacklistCache = set;
         if (this.f_58857_ != null) {
            this.exportBlacklistTick = this.f_58857_.m_46467_();
         }

         return set;
      }
   }

   private boolean isImportAllowed(AEKey key) {
      return this.getExportBlacklist().contains(key) ? false : this.isInsertAllowedByFilter(key);
   }

   public boolean isInsertAllowedByFilter(AEKey key) {
      Set<AEKey> keys = this.importFilterKeys;
      if (keys != null && !keys.isEmpty()) {
         FuzzyMode fuzzyMode = this.importFilterFuzzyMode;
         boolean matches;
         if (fuzzyMode == null) {
            matches = keys.contains(key);
         } else {
            matches = false;

            for (AEKey filterKey : keys) {
               if (key.equals(filterKey) || key.fuzzyEquals(filterKey, fuzzyMode)) {
                  matches = true;
                  break;
               }
            }
         }

         return matches != this.importFilterInverted;
      } else {
         return true;
      }
   }

   private long runExport(OverloadedInterfaceBlockEntity.ConnectionState state, AEKeyType keyType, MEStorage wrapper, MEStorage me, IActionSource src, long now) {
      List<OverloadedInterfaceBlockEntity.ExportConfigEntry> entries = this.exportEntriesForType(keyType, now);
      long moved = 0L;
      boolean overflowed = false;
      IGrid grid = this.getMainNode().getGrid();

      for (OverloadedInterfaceBlockEntity.ExportConfigEntry entry : entries) {
         AEKey key = entry.key();
         if (!state.isExportRejected(key, now)) {
            long toMove = entry.maxAmount();
            long available = me.extract(key, toMove, Actionable.SIMULATE, src);
            if (available > 0L) {
               long requested = Math.min(toMove, available);
               long canAccept = wrapper.insert(key, requested, Actionable.SIMULATE, src);
               if (canAccept <= 0L) {
                  state.onExportRejected(key, now);
               } else {
                  long target = Math.min(requested, canAccept);
                  long affordable = PowerCostUtil.maxAffordable(grid, key, target);
                  if (affordable > 0L) {
                     long extracted = me.extract(key, affordable, Actionable.MODULATE, src);
                     if (extracted > 0L) {
                        long inserted = wrapper.insert(key, extracted, Actionable.MODULATE, src);
                        if (inserted > 0L) {
                           PowerCostUtil.consume(grid, key, inserted);
                           state.onExportAccepted(key);
                           moved += inserted;
                        } else {
                           state.onExportRejected(key, now);
                        }

                        long overflow = extracted - inserted;
                        if (overflow > 0L) {
                           this.addToImportBuffer(key, overflow);
                           overflowed = true;
                        }
                     }
                  }
               }
            }
         }
      }

      OverloadedInterfaceBlockEntity.CooldownTracker cd = state.cdFor(keyType, OverloadedInterfaceBlockEntity.IoDirection.EXPORT);
      if (moved > 0L) {
         cd.onSuccess(now, this.ioSpeedMode, null);
      } else {
         cd.onFail(now, this.ioSpeedMode);
      }

      if (overflowed) {
         this.saveChanges();
      }

      return moved;
   }

   private List<OverloadedInterfaceBlockEntity.ExportConfigEntry> exportEntriesForType(AEKeyType keyType, long now) {
      this.refreshExportConfigCache(now);
      List<OverloadedInterfaceBlockEntity.ExportConfigEntry> entries = this.exportConfigCache.get(keyType);
      return entries != null ? entries : List.of();
   }

   private void refreshExportConfigCache(long now) {
      if (this.exportConfigCacheTick != now) {
         this.exportConfigCacheTick = now;
         int hash = this.computeExportConfigHash();
         if (!this.exportConfigCacheValid || hash != this.exportConfigCacheHash) {
            this.exportConfigCacheHash = hash;
            this.exportConfigCacheValid = true;
            this.exportConfigCache.clear();
            ConfigInventory config = this.getInterfaceLogic().getConfig();

            for (int ci = 0; ci < config.size(); ci++) {
               AEKey key = config.getKey(ci);
               if (key != null && isWirelessIoKeyType(key.getType())) {
                  long maxAmount = this.unlimitedSlots[ci] ? Long.MAX_VALUE : config.getAmount(ci);
                  if (maxAmount <= 0L) {
                     maxAmount = Long.MAX_VALUE;
                  }

                  this.exportConfigCache
                     .computeIfAbsent(key.getType(), ignored -> new ArrayList<>())
                     .add(new OverloadedInterfaceBlockEntity.ExportConfigEntry(key, maxAmount));
               }
            }

            this.exportConfigCache.replaceAll((ignored, entries) -> List.copyOf(entries));
         }
      }
   }

   private int computeExportConfigHash() {
      ConfigInventory config = this.getInterfaceLogic().getConfig();
      int hash = config.size();

      for (int ci = 0; ci < config.size(); ci++) {
         AEKey key = config.getKey(ci);
         hash = 31 * hash + (key != null ? key.hashCode() : 0);
         hash = 31 * hash + (this.unlimitedSlots[ci] ? 1 : 0);
         if (key != null && !this.unlimitedSlots[ci]) {
            hash = 31 * hash + Long.hashCode(config.getAmount(ci));
         }
      }

      return hash;
   }

   private void addToImportBuffer(AEKey key, long amount) {
      if (amount > 0L) {
         this.importBuffer.merge(key, amount, (oldAmount, added) -> oldAmount > Long.MAX_VALUE - added ? Long.MAX_VALUE : oldAmount + added);
         this.alertGridTicker();
      }
   }

   private void flushImportBuffer(MEStorage me, IActionSource src, long now) {
      if (!this.importBuffer.isEmpty()) {
         if (this.importBufferLastFlushTick == Long.MIN_VALUE || now - this.importBufferLastFlushTick >= 5L) {
            this.importBufferLastFlushTick = now;
            IdentityHashMap<AEKeyType, Boolean> typeProgressed = new IdentityHashMap<>();
            IdentityHashMap<AEKeyType, Boolean> typeFullyRejected = new IdentityHashMap<>();
            boolean changed = false;
            Iterator<Entry<AEKey, Long>> it = this.importBuffer.entrySet().iterator();

            while (it.hasNext()) {
               Entry<AEKey, Long> buffered = it.next();
               AEKey key = buffered.getKey();
               long amount = buffered.getValue();
               if (amount <= 0L) {
                  it.remove();
                  changed = true;
               } else {
                  long inserted = me.insert(key, amount, Actionable.MODULATE, src);
                  AEKeyType type = key.getType();
                  if (inserted >= amount) {
                     it.remove();
                     typeProgressed.put(type, true);
                     changed = true;
                  } else if (inserted > 0L) {
                     buffered.setValue(amount - inserted);
                     typeProgressed.put(type, true);
                     changed = true;
                  } else {
                     typeFullyRejected.putIfAbsent(type, Boolean.valueOf(true));
                  }
               }
            }

            for (AEKeyType type : typeProgressed.keySet()) {
               if (this.keyTypeLockUntil.remove(type) != null) {
                  changed = true;
               }
            }

            for (AEKeyType typex : typeFullyRejected.keySet()) {
               if (!typeProgressed.getOrDefault(typex, Boolean.valueOf(false))) {
                  this.keyTypeLockUntil.put(typex, now + 20L);
               }
            }

            if (changed) {
               this.saveChanges();
            }
         }
      }
   }

   private long lockedUntil(AEKeyType type, long now) {
      long until = this.keyTypeLockUntil.getOrDefault(type, 0L);
      if (until <= 0L) {
         return 0L;
      } else if (now >= until) {
         this.keyTypeLockUntil.remove(type);
         return 0L;
      } else {
         return until;
      }
   }

   public void addImportBufferDrops(List<ItemStack> drops) {
      if (!this.importBuffer.isEmpty()) {
         for (Entry<AEKey, Long> buffered : this.importBuffer.entrySet()) {
            buffered.getKey().addDrops(buffered.getValue(), drops, this.m_58904_(), this.m_58899_());
         }

         this.importBuffer.clear();
      }
   }

   public void clearImportBuffer() {
      this.importBuffer.clear();
      this.keyTypeLockUntil.clear();
      this.importBufferLastFlushTick = Long.MIN_VALUE;
   }

   private boolean hasInductionCard() {
      if (!AppFluxHelper.isAvailable()) {
         return false;
      } else if (!this.inductionCardCacheDirty) {
         return this.inductionCardInstalledCache;
      } else {
         IUpgradeInventory u = this.getInterfaceLogic().getUpgrades();
         boolean installed = false;

         for (int i = 0; i < u.size(); i++) {
            if (AppFluxHelper.isInductionCard(u.getStackInSlot(i).m_41720_())) {
               installed = true;
               break;
            }
         }

         this.inductionCardInstalledCache = installed;
         this.inductionCardCacheDirty = false;
         return installed;
      }
   }

   private void tickEnergyTransfer(ServerLevel sl) {
      if (this.hasInductionCard()) {
         AEKey feKey = AppFluxHelper.FE_KEY;
         if (feKey != null) {
            IGrid grid = this.getMainNode().getGrid();
            if (grid != null) {
               if (this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
                  this.tickWirelessEnergy(sl, feKey);
               } else if (this.energyOutputDir != null) {
                  this.tickNormalEnergy(sl);
               }
            }
         }
      }
   }

   private void tickNormalEnergy(ServerLevel sl) {
      if (AppFluxBridge.canUseEnergyHandler()) {
         IGrid grid = this.getMainNode().getGrid();
         if (grid != null) {
            Object capCache = AppFluxBridge.createCapCache(sl, this.m_58899_(), () -> grid);
            TargetAccess target = WirelessEnergyAPI.resolveEnergyTarget(capCache, this.energyOutputDir.m_122424_());
            if (target != null) {
               IStorageService storage = grid.getStorageService();
               WirelessEnergyAPI.sendToTarget(target, storage, this.machineSource, AppFluxBridge.TRANSFER_RATE);
            }
         }
      }
   }

   private void tickWirelessEnergy(ServerLevel sl, AEKey feKey) {
      long gt = sl.m_46467_();
      if (gt != this.lastEnergyTickGameTime) {
         this.lastEnergyTickGameTime = gt;
         this.distributeWirelessEnergy(sl, gt, feKey);
      }
   }

   private void distributeWirelessEnergy(ServerLevel sl, long tick, AEKey feKey) {
      this.getOrRefreshValidConnections(sl, tick);
      this.wirelessDistributor.tickNormal(sl);
   }

   public void refreshEjectRegistrations() {
      if (this.f_58857_ == null || !this.f_58857_.m_5776_()) {
         this.unregisterEject();
         if (OverloadedInterfaceTickDecider.shouldRegisterEjectPorts(
               this.interfaceMode == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS, this.importMode == OverloadedInterfaceBlockEntity.ImportMode.EJECT
            )
            && this.f_58857_ != null) {
            MinecraftServer srv = this.f_58857_.m_7654_();
            if (srv != null) {
               for (OverloadedInterfaceBlockEntity.WirelessConnection c : this.connections) {
                  if (c.dimension().equals(this.f_58857_.m_46472_())) {
                     this.registerEjectAt(srv, c.dimension(), c.pos().m_121945_(c.boundFace()), c.boundFace().m_122424_());
                  }
               }
            }
         }
      }
   }

   private void registerEjectAt(MinecraftServer srv, ResourceKey<Level> dim, BlockPos ip, Direction iface) {
      ServerLevel tl = srv.m_129880_(dim);
      if (tl != null) {
         GhostOutputBlockEntity ghost = new GhostOutputBlockEntity(ip);
         ghost.m_142339_(tl);
         EjectModeRegistry.register(
            dim, ip.m_121878_(), iface, new EjectModeRegistry.EjectEntry(new WeakReference<>(this), ghost, this.f_58857_.m_46472_(), this.m_58899_())
         );
      }
   }

   private void unregisterEject() {
      if (this.f_58857_ != null && !this.f_58857_.m_5776_()) {
         List<EjectModeRegistry.DimPos> removed = EjectModeRegistry.unregisterAll(this, true);
         if (this.f_58857_ instanceof ServerLevel sl) {
            MinecraftServer srv = sl.m_7654_();

            for (EjectModeRegistry.DimPos dp : removed) {
               ServerLevel t = srv.m_129880_(dp.dimension());
               if (t != null) {
               }
            }
         }
      }
   }

   public void m_7651_() {
      this.frequencyBinding.setRemoved();
      if (!this.unloadingChunk) {
         this.unregisterEject();
      }

      this.wirelessDistributor.flushBufferToNetwork();
      super.m_7651_();
   }

   public void m_6339_() {
      super.m_6339_();
      this.frequencyBinding.clearRemoved();
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      this.unloadingChunk = true;
      this.wirelessDistributor.flushBufferToNetwork();
      super.onChunkUnloaded();
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);
      data.writeByte(this.interfaceMode.ordinal());
      data.writeByte(this.ioSpeedMode.ordinal());
      data.writeByte(this.exportMode.ordinal());
      data.writeByte(this.importMode.ordinal());
      data.writeByte(this.energyOutputDir != null ? this.energyOutputDir.m_122411_() : -1);
      long bits = 0L;

      for (int i = 0; i < 36; i++) {
         if (this.unlimitedSlots[i]) {
            bits |= 1L << i;
         }
      }

      data.writeLong(bits);
      data.m_130130_(this.connections.size());

      for (OverloadedInterfaceBlockEntity.WirelessConnection c : this.connections) {
         data.m_130085_(c.dimension().m_135782_());
         data.m_130064_(c.pos());
         data.writeByte(c.boundFace().m_122411_());
      }
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);
      int interfaceOrd = data.readByte();
      OverloadedInterfaceBlockEntity.InterfaceMode newInterfaceMode = interfaceOrd >= 0
            && interfaceOrd < OverloadedInterfaceBlockEntity.InterfaceMode.values().length
         ? OverloadedInterfaceBlockEntity.InterfaceMode.values()[interfaceOrd]
         : OverloadedInterfaceBlockEntity.InterfaceMode.NORMAL;
      int speedOrd = data.readByte();
      OverloadedInterfaceBlockEntity.IOSpeedMode newIoSpeedMode = speedOrd >= 0 && speedOrd < OverloadedInterfaceBlockEntity.IOSpeedMode.values().length
         ? OverloadedInterfaceBlockEntity.IOSpeedMode.values()[speedOrd]
         : OverloadedInterfaceBlockEntity.IOSpeedMode.NORMAL;
      int exportOrd = data.readByte();
      OverloadedInterfaceBlockEntity.ExportMode newExportMode = exportOrd >= 0 && exportOrd < OverloadedInterfaceBlockEntity.ExportMode.values().length
         ? OverloadedInterfaceBlockEntity.ExportMode.values()[exportOrd]
         : OverloadedInterfaceBlockEntity.ExportMode.OFF;
      int importOrd = data.readByte();
      OverloadedInterfaceBlockEntity.ImportMode newImportMode = importOrd >= 0 && importOrd < OverloadedInterfaceBlockEntity.ImportMode.values().length
         ? OverloadedInterfaceBlockEntity.ImportMode.values()[importOrd]
         : OverloadedInterfaceBlockEntity.ImportMode.OFF;
      int energyOrd = data.readByte();
      Direction newEnergyDir = energyOrd >= 0 && energyOrd < 6 ? Direction.m_122376_(energyOrd) : null;
      long newBits = data.readLong();
      boolean[] newUnlimitedSlots = new boolean[36];

      for (int i = 0; i < 36; i++) {
         newUnlimitedSlots[i] = (newBits & 1L << i) != 0L;
      }

      int count = data.m_130242_();
      ArrayList<OverloadedInterfaceBlockEntity.WirelessConnection> newConnections = new ArrayList<>(Math.min(count, 1024));

      for (int i = 0; i < count; i++) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, data.m_130281_());
         BlockPos pos = data.m_130135_();
         Direction face = Direction.m_122376_(data.readByte());
         OverloadedInterfaceBlockEntity.WirelessConnection connection = new OverloadedInterfaceBlockEntity.WirelessConnection(dim, pos, face);
         if (ConnectionEndpoints.indexOfEndpoint(
                  newConnections,
                  connection.dimension(),
                  connection.pos(),
                  connection.boundFace(),
                  OverloadedInterfaceBlockEntity.WirelessConnection::dimension,
                  OverloadedInterfaceBlockEntity.WirelessConnection::pos,
                  OverloadedInterfaceBlockEntity.WirelessConnection::boundFace
               )
               < 0
            && newConnections.size() < 1024) {
            newConnections.add(connection);
         }
      }

      boolean unlimitedChanged = false;

      for (int ix = 0; ix < 36; ix++) {
         if (this.unlimitedSlots[ix] != newUnlimitedSlots[ix]) {
            unlimitedChanged = true;
            break;
         }
      }

      if (newInterfaceMode != this.interfaceMode
         || newIoSpeedMode != this.ioSpeedMode
         || newExportMode != this.exportMode
         || newImportMode != this.importMode
         || newEnergyDir != this.energyOutputDir
         || unlimitedChanged
         || !newConnections.equals(this.connections)) {
         this.interfaceMode = newInterfaceMode;
         this.ioSpeedMode = newIoSpeedMode;
         this.exportMode = newExportMode;
         this.importMode = newImportMode;
         this.energyOutputDir = newEnergyDir;
         System.arraycopy(newUnlimitedSlots, 0, this.unlimitedSlots, 0, 36);
         this.invalidateExportConfigCache();
         this.connections.clear();
         this.connections.addAll(newConnections);
         this.invalidConnectionScanCursor = 0;
         changed = true;
      }

      return changed;
   }

   public void m_183515_(CompoundTag d) {
      super.m_183515_(d);
      d.m_128359_("InterfaceMode", this.interfaceMode.name());
      d.m_128359_("IOSpeedMode", this.ioSpeedMode.name());
      d.m_128359_("ExportMode", this.exportMode.name());
      d.m_128359_("ImportMode", this.importMode.name());
      d.m_128405_("EnergyDir", this.energyOutputDir != null ? this.energyOutputDir.m_122411_() : -1);
      long bits = 0L;

      for (int i = 0; i < 36; i++) {
         if (this.unlimitedSlots[i]) {
            bits |= 1L << i;
         }
      }

      d.m_128356_("UnlimitedSlots", bits);
      d.m_128365_("WirelessConnections", WirelessConnectionLists.writeTagList(this.connections));
      this.filterInv.writeToNBT(d, "FilterInv");
      if (!this.importBuffer.isEmpty()) {
         ListTag buffered = new ListTag();

         for (Entry<AEKey, Long> entry : this.importBuffer.entrySet()) {
            buffered.add(GenericStack.writeTag(new GenericStack(entry.getKey(), entry.getValue())));
         }

         d.m_128365_("ae2ltImportBuffer", buffered);
      }

      d.m_128356_("ae2ltImportFlushTick", this.importBufferLastFlushTick);
      this.frequencyBinding.save(d);
   }

   public void loadTag(CompoundTag d) {
      super.loadTag(d);
      if (d.m_128441_("InterfaceMode")) {
         try {
            this.interfaceMode = OverloadedInterfaceBlockEntity.InterfaceMode.valueOf(d.m_128461_("InterfaceMode"));
         } catch (IllegalArgumentException var11) {
            this.interfaceMode = OverloadedInterfaceBlockEntity.InterfaceMode.NORMAL;
         }
      }

      if (d.m_128441_("IOSpeedMode")) {
         try {
            this.ioSpeedMode = OverloadedInterfaceBlockEntity.IOSpeedMode.valueOf(d.m_128461_("IOSpeedMode"));
         } catch (IllegalArgumentException var10) {
            this.ioSpeedMode = OverloadedInterfaceBlockEntity.IOSpeedMode.NORMAL;
         }
      }

      if (d.m_128441_("ExportMode")) {
         try {
            this.exportMode = OverloadedInterfaceBlockEntity.ExportMode.valueOf(d.m_128461_("ExportMode"));
         } catch (IllegalArgumentException var9) {
            this.exportMode = OverloadedInterfaceBlockEntity.ExportMode.OFF;
         }
      }

      if (d.m_128441_("ImportMode")) {
         try {
            this.importMode = OverloadedInterfaceBlockEntity.ImportMode.valueOf(d.m_128461_("ImportMode"));
         } catch (IllegalArgumentException var8) {
            this.importMode = OverloadedInterfaceBlockEntity.ImportMode.OFF;
         }
      }

      long bits = d.m_128454_("UnlimitedSlots");

      for (int i = 0; i < 36; i++) {
         this.unlimitedSlots[i] = (bits & 1L << i) != 0L;
      }

      int ev = d.m_128441_("EnergyDir") ? d.m_128451_("EnergyDir") : -1;
      this.energyOutputDir = ev >= 0 && ev < 6 ? Direction.m_122376_(ev) : null;
      WirelessConnectionLists.readTagList(d, "WirelessConnections", this.connections, 1024, OverloadedInterfaceBlockEntity.WirelessConnection::fromTag);
      this.invalidConnectionScanCursor = 0;
      this.filterInv.readFromNBT(d, "FilterInv");
      this.rebuildFilter();
      this.importBuffer.clear();
      if (d.m_128425_("ae2ltImportBuffer", 9)) {
         ListTag buffered = d.m_128437_("ae2ltImportBuffer", 10);

         for (int i = 0; i < buffered.size(); i++) {
            GenericStack stack = GenericStack.readTag(buffered.m_128728_(i));
            if (stack != null && stack.amount() > 0L) {
               this.importBuffer
                  .merge(stack.what(), stack.amount(), (oldAmount, added) -> oldAmount > Long.MAX_VALUE - added ? Long.MAX_VALUE : oldAmount + added);
            }
         }
      }

      this.importBufferLastFlushTick = d.m_128441_("ae2ltImportFlushTick") ? d.m_128454_("ae2ltImportFlushTick") : Long.MIN_VALUE;
      this.keyTypeLockUntil.clear();
      this.invalidateConnectionCache();
      this.refreshEjectRegistrations();
      this.frequencyBinding.load(d);
      this.recomputeIdlePower();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      MemoryCardConfigSupport.exportMemoryCardSettings(mode, output, tag -> {
         MemoryCardConfigSupport.writeEnum(tag, "InterfaceMode", this.interfaceMode);
         MemoryCardConfigSupport.writeEnum(tag, "IOSpeedMode", this.ioSpeedMode);
         MemoryCardConfigSupport.writeEnum(tag, "ExportMode", this.exportMode);
         MemoryCardConfigSupport.writeEnum(tag, "ImportMode", this.importMode);
         MemoryCardConfigSupport.writeDirection(tag, "EnergyDir", this.energyOutputDir);
         long bits = 0L;

         for (int i = 0; i < 36; i++) {
            if (this.unlimitedSlots[i]) {
               bits |= 1L << i;
            }
         }

         tag.m_128356_("UnlimitedSlots", bits);
         FrequencyBindingHelper.writeMemoryFrequency(tag, this.getFrequencyId());
      });
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      MemoryCardConfigSupport.importMemoryCardSettings(mode, input, tag -> {
         this.interfaceMode = MemoryCardConfigSupport.readEnum(tag, "InterfaceMode", OverloadedInterfaceBlockEntity.InterfaceMode.class, this.interfaceMode);
         this.ioSpeedMode = MemoryCardConfigSupport.readEnum(tag, "IOSpeedMode", OverloadedInterfaceBlockEntity.IOSpeedMode.class, this.ioSpeedMode);
         this.exportMode = MemoryCardConfigSupport.readEnum(tag, "ExportMode", OverloadedInterfaceBlockEntity.ExportMode.class, this.exportMode);
         this.importMode = MemoryCardConfigSupport.readEnum(tag, "ImportMode", OverloadedInterfaceBlockEntity.ImportMode.class, this.importMode);
         if (tag.m_128441_("EnergyDir")) {
            this.energyOutputDir = MemoryCardConfigSupport.readDirection(tag, "EnergyDir");
         }

         if (tag.m_128441_("UnlimitedSlots")) {
            long bits = tag.m_128454_("UnlimitedSlots");

            for (int i = 0; i < 36; i++) {
               this.unlimitedSlots[i] = (bits & 1L << i) != 0L;
            }
         }

         FrequencyBindingHelper.importMemoryFrequency(tag, this::setFrequency);
         this.invalidateConnectionCache();
         this.refreshEjectRegistrations();
         this.recomputeIdlePower();
         this.saveChanges();
         this.markForUpdate();
      });
   }

   static final class ConnectionState {
      final Map<AEKeyType, OverloadedInterfaceBlockEntity.CooldownTracker> importCDs = new IdentityHashMap<>();
      final Map<AEKeyType, OverloadedInterfaceBlockEntity.CooldownTracker> exportCDs = new IdentityHashMap<>();
      final Map<AEKeyType, OverloadedInterfaceBlockEntity.ProbeState> importProbeStates = new IdentityHashMap<>();
      final Map<AEKeyType, OverloadedInterfaceBlockEntity.KeyModel> keyModels = new IdentityHashMap<>();
      final Map<AEKeyType, OverloadedInterfaceBlockEntity.ImportKeyCache> importKeyCaches = new IdentityHashMap<>();
      final Map<AEKey, OverloadedInterfaceBlockEntity.ExportRejectState> exportRejects = new HashMap<>();
      @Nullable
      WeakReference<BlockEntity> storageBERef;
      @Nullable
      Map<AEKeyType, ExternalStorageStrategy> storageStrategies;
      @Nullable
      Map<AEKeyType, MEStorage> storageWrappers;
      long storageWrapperTick = -1L;

      OverloadedInterfaceBlockEntity.CooldownTracker cdFor(AEKeyType type, OverloadedInterfaceBlockEntity.IoDirection direction) {
         Map<AEKeyType, OverloadedInterfaceBlockEntity.CooldownTracker> cds = direction == OverloadedInterfaceBlockEntity.IoDirection.IMPORT
            ? this.importCDs
            : this.exportCDs;
         return cds.computeIfAbsent(type, ignored -> new OverloadedInterfaceBlockEntity.CooldownTracker());
      }

      OverloadedInterfaceBlockEntity.ProbeState probeStateFor(AEKeyType type) {
         return this.importProbeStates.computeIfAbsent(type, ignored -> new OverloadedInterfaceBlockEntity.ProbeState());
      }

      OverloadedInterfaceBlockEntity.KeyModel modelFor(AEKeyType type) {
         return this.keyModels.computeIfAbsent(type, ignored -> new OverloadedInterfaceBlockEntity.KeyModel());
      }

      OverloadedInterfaceBlockEntity.ImportKeyCache importKeyCacheFor(AEKeyType type) {
         return this.importKeyCaches.computeIfAbsent(type, ignored -> new OverloadedInterfaceBlockEntity.ImportKeyCache());
      }

      void resetWirelessIo(OverloadedInterfaceBlockEntity.IOSpeedMode mode) {
         this.importCDs.values().forEach(cd -> cd.reset(mode));
         this.exportCDs.values().forEach(cd -> cd.reset(mode));
         this.importProbeStates.values().forEach(OverloadedInterfaceBlockEntity.ProbeState::reset);
         this.keyModels.values().forEach(OverloadedInterfaceBlockEntity.KeyModel::resetCycle);
         this.importKeyCaches.values().forEach(OverloadedInterfaceBlockEntity.ImportKeyCache::clear);
         this.exportRejects.clear();
      }

      boolean isExportRejected(AEKey key, long now) {
         OverloadedInterfaceBlockEntity.ExportRejectState state = this.exportRejects.get(key);
         if (state == null) {
            return false;
         } else if (now >= state.untilTick) {
            this.exportRejects.remove(key);
            return false;
         } else {
            return true;
         }
      }

      void onExportRejected(AEKey key, long now) {
         OverloadedInterfaceBlockEntity.ExportRejectState state = this.exportRejects.get(key);
         if (state == null) {
            if (this.exportRejects.size() >= 128) {
               this.exportRejects.clear();
            }

            state = new OverloadedInterfaceBlockEntity.ExportRejectState();
            this.exportRejects.put(key, state);
         }

         state.reject(now);
      }

      void onExportAccepted(AEKey key) {
         this.exportRejects.remove(key);
      }

      @Nullable
      Map<AEKeyType, MEStorage> resolveWrappers(ServerLevel level, OverloadedInterfaceBlockEntity.WirelessConnection conn) {
         BlockEntity be = level.m_7702_(conn.pos());
         if (be == null) {
            this.storageBERef = null;
            this.storageStrategies = null;
            this.storageWrappers = null;
            return null;
         } else {
            if (this.storageBERef == null || this.storageBERef.get() != be || this.storageStrategies == null) {
               this.storageStrategies = StackWorldBehaviors.createExternalStorageStrategies(level, conn.pos(), conn.boundFace());
               this.storageBERef = new WeakReference<>(be);
               this.storageWrappers = null;
               this.storageWrapperTick = -1L;
            }

            if (this.storageStrategies.isEmpty()) {
               return null;
            } else {
               long gt = level.m_46467_();
               if (this.storageWrappers == null || gt - this.storageWrapperTick >= 20L) {
                  IdentityHashMap<AEKeyType, MEStorage> map = new IdentityHashMap<>(this.storageStrategies.size());

                  for (Entry<AEKeyType, ExternalStorageStrategy> e : this.storageStrategies.entrySet()) {
                     MEStorage w = e.getValue().createWrapper(false, Runnables.doNothing());
                     if (w != null) {
                        map.put(e.getKey(), w);
                     }
                  }

                  this.storageWrappers = map.isEmpty() ? null : map;
                  this.storageWrapperTick = gt;
               }

               return this.storageWrappers;
            }
         }
      }
   }

   static final class CooldownTracker {
      private OverloadedInterfaceBlockEntity.IOSpeedMode mode = OverloadedInterfaceBlockEntity.IOSpeedMode.NORMAL;
      private int cooldownN = 5;
      private long cooldownUntil = -1L;
      private long lastSuccessTick = -1L;
      private long lastSuccessInterval = -1L;

      long cooldownUntil() {
         return this.cooldownUntil;
      }

      void reset(OverloadedInterfaceBlockEntity.IOSpeedMode newMode) {
         this.mode = newMode;
         this.cooldownN = initialFor(newMode);
         this.cooldownUntil = -1L;
         this.lastSuccessTick = -1L;
         this.lastSuccessInterval = -1L;
      }

      void onSuccess(long now, OverloadedInterfaceBlockEntity.IOSpeedMode newMode, @Nullable OverloadedInterfaceBlockEntity.KeyModel model) {
         this.ensureMode(newMode);
         if (newMode == OverloadedInterfaceBlockEntity.IOSpeedMode.FAST) {
            if (this.lastSuccessTick >= 0L) {
               this.lastSuccessInterval = Math.max(1L, now - this.lastSuccessTick);
            }

            this.lastSuccessTick = now;
            this.cooldownN = 1;
         } else if (model == null) {
            this.cooldownN = 5;
         } else {
            this.cooldownN = predictNormalCooldown(model);
         }

         this.cooldownUntil = now + (long)this.cooldownN;
      }

      void onFail(long now, OverloadedInterfaceBlockEntity.IOSpeedMode newMode) {
         this.ensureMode(newMode);
         if (newMode == OverloadedInterfaceBlockEntity.IOSpeedMode.FAST) {
            int limit = this.lastSuccessInterval > 0L ? (int)Math.min(this.lastSuccessInterval, 40L) : 40;
            this.cooldownN = Math.min(Math.min(this.cooldownN + 1, limit), 40);
            this.cooldownN = Math.max(this.cooldownN, 1);
         } else {
            this.cooldownN = Math.max(5, this.cooldownN / 2);
         }

         this.cooldownUntil = now + (long)this.cooldownN;
      }

      private void ensureMode(OverloadedInterfaceBlockEntity.IOSpeedMode newMode) {
         if (this.mode != newMode) {
            this.reset(newMode);
         }
      }

      private static int initialFor(OverloadedInterfaceBlockEntity.IOSpeedMode mode) {
         return mode == OverloadedInterfaceBlockEntity.IOSpeedMode.FAST ? 5 : 5;
      }

      private static int predictNormalCooldown(@Nullable OverloadedInterfaceBlockEntity.KeyModel model) {
         if (model != null && model.effectiveMax > 0L) {
            long deficit = (long)((double)model.effectiveMax * 0.85) - model.lastAvail;
            if (deficit <= 0L) {
               return 5;
            } else if (model.rateEMA > 0.0) {
               long predicted = (long)Math.ceil((double)deficit / model.rateEMA);
               return (int)Math.max(5L, Math.min(80L, predicted));
            } else {
               return 80;
            }
         } else {
            return 80;
         }
      }
   }

   private final class DistributorHost implements WirelessEnergyDistributor.Host {
      @Override
      public IManagedGridNode getMainNode() {
         return OverloadedInterfaceBlockEntity.this.getMainNode();
      }

      @Override
      public IActionSource actionSource() {
         return OverloadedInterfaceBlockEntity.this.machineSource;
      }

      @Override
      public boolean isHostRemoved() {
         return OverloadedInterfaceBlockEntity.this.m_58901_();
      }

      @Override
      public List<WirelessEnergyAPI.Target> getValidTargets() {
         return OverloadedInterfaceBlockEntity.this.validEnergyTargetsCache;
      }

      @Override
      public int getValidTargetsVersion() {
         return OverloadedInterfaceBlockEntity.this.validEnergyTargetsVersion;
      }
   }

   private static record ExportConfigEntry(AEKey key, long maxAmount) {
   }

   public static enum ExportMode {
      OFF,
      AUTO;
   }

   static final class ExportRejectState {
      long untilTick;
      int failures;

      void reject(long now) {
         this.failures = Math.min(this.failures + 1, 6);
         int delay = Math.min(80, 10 << Math.min(this.failures - 1, 3));
         this.untilTick = now + (long)delay;
      }
   }

   public static enum IOSpeedMode {
      NORMAL,
      FAST;
   }

   static final class ImportKeyCache {
      final List<AEKey> keys = new ArrayList<>();
      long lastFullScanTick = Long.MIN_VALUE;
      boolean truncated;

      boolean isScanFresh(long now) {
         if (this.lastFullScanTick == Long.MIN_VALUE) {
            return false;
         } else {
            int ttl;
            if (this.keys.isEmpty()) {
               ttl = 20;
            } else if (this.truncated) {
               ttl = 5;
            } else {
               ttl = 40;
            }

            return now - this.lastFullScanTick < (long)ttl;
         }
      }

      void update(List<AEKey> scannedKeys, boolean wasTruncated, long now) {
         this.keys.clear();
         this.keys.addAll(scannedKeys);
         this.lastFullScanTick = now;
         this.truncated = wasTruncated;
      }

      void clear() {
         this.keys.clear();
         this.lastFullScanTick = Long.MIN_VALUE;
         this.truncated = false;
      }
   }

   public static enum ImportMode {
      OFF,
      AUTO,
      EJECT;
   }

   private static record ImportResult(long totalAvail, long moved) {
   }

   public static enum InterfaceMode {
      NORMAL,
      WIRELESS;
   }

   static enum IoDirection {
      IMPORT,
      EXPORT;
   }

   private static record IoEntryKey(
      OverloadedInterfaceBlockEntity.WirelessConnection conn, AEKeyType keyType, OverloadedInterfaceBlockEntity.IoDirection direction
   ) {
   }

   static enum IoPhase {
      PROBE,
      EXTRACT;
   }

   static final class IoScheduledEntry {
      final OverloadedInterfaceBlockEntity.WirelessConnection conn;
      final OverloadedInterfaceBlockEntity.ConnectionState state;
      final AEKeyType keyType;
      final OverloadedInterfaceBlockEntity.IoDirection direction;
      final int generation;
      OverloadedInterfaceBlockEntity.IoPhase phase;

      IoScheduledEntry(
         OverloadedInterfaceBlockEntity.WirelessConnection conn,
         OverloadedInterfaceBlockEntity.ConnectionState state,
         AEKeyType keyType,
         OverloadedInterfaceBlockEntity.IoDirection direction,
         int generation
      ) {
         this.conn = conn;
         this.state = state;
         this.keyType = keyType;
         this.direction = direction;
         this.generation = generation;
         this.phase = OverloadedInterfaceBlockEntity.IoPhase.EXTRACT;
      }
   }

   static final class KeyModel {
      long maxObserved;
      long effectiveMax;
      long lastAvail;
      long lastTick;
      double rateEMA;
      long postExtractAvail;
      long postExtractTick = -1L;
      long midProbeAvail;
      long midProbeTick = -1L;
      double half1Rate;

      void resetCycle() {
         this.postExtractTick = -1L;
         this.midProbeTick = -1L;
         this.half1Rate = 0.0;
      }

      void onProbe(long avail, long now) {
         if (this.postExtractTick > 0L && now > this.postExtractTick) {
            double dt = (double)(now - this.postExtractTick);
            this.half1Rate = (double)(avail - this.postExtractAvail) / dt;
         }

         this.midProbeAvail = avail;
         this.midProbeTick = now;
         this.updateRateEMA(avail, now);
      }

      void onExtract(long totalAvail, long totalExtracted, long now) {
         long currentAvail = Math.max(0L, totalAvail - totalExtracted);
         if (this.midProbeTick > 0L && this.postExtractTick > 0L && now > this.midProbeTick) {
            double dt = (double)(now - this.midProbeTick);
            double half2Rate = (double)(currentAvail - this.midProbeAvail) / dt;
            if (this.half1Rate > 0.0) {
               double ratio = half2Rate / this.half1Rate;
               if (ratio < 0.7) {
                  this.effectiveMax = Math.max(this.maxObserved / 4L, (long)((double)this.effectiveMax * 0.9));
               } else if (ratio > 1.1) {
                  this.effectiveMax = Math.min(this.maxObserved, (long)Math.ceil((double)this.effectiveMax * 1.05));
               }
            }
         }

         this.updateRateEMA(totalAvail, now);
         this.lastAvail = currentAvail;
         this.lastTick = now;
         this.postExtractAvail = currentAvail;
         this.postExtractTick = now;
         this.midProbeTick = -1L;
      }

      private void updateRateEMA(long totalAvail, long now) {
         if (this.lastTick > 0L && now > this.lastTick) {
            long dt = now - this.lastTick;
            long da = totalAvail - this.lastAvail;
            if (da >= 0L) {
               double instant = (double)da / (double)dt;
               this.rateEMA = 0.2 * instant + 0.8 * this.rateEMA;
            }
         }

         if (totalAvail > this.maxObserved) {
            this.maxObserved = totalAvail;
            if (this.effectiveMax < totalAvail) {
               this.effectiveMax = totalAvail;
            }
         }

         if (this.maxObserved > 0L) {
            this.effectiveMax = Math.max(this.maxObserved / 4L, Math.min(this.maxObserved, this.effectiveMax));
         }

         this.lastAvail = totalAvail;
         this.lastTick = now;
      }
   }

   static final class ProbeState {
      int levelIdx;
      int skipCounter;

      void reset() {
         this.levelIdx = 0;
         this.skipCounter = 0;
      }
   }

   public static record WirelessConnection(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace) implements WirelessConnectionRef {
      private static final String TAG_DIM = "Dim";
      private static final String TAG_POS = "Pos";
      private static final String TAG_FACE = "Face";

      @Override
      public CompoundTag toTag() {
         CompoundTag tag = new CompoundTag();
         tag.m_128359_("Dim", this.dimension.m_135782_().toString());
         tag.m_128356_("Pos", this.pos.m_121878_());
         tag.m_128405_("Face", this.boundFace.m_122411_());
         return tag;
      }

      public static OverloadedInterfaceBlockEntity.WirelessConnection fromTag(CompoundTag tag) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(tag.m_128461_("Dim")));
         return new OverloadedInterfaceBlockEntity.WirelessConnection(dim, BlockPos.m_122022_(tag.m_128454_("Pos")), Direction.m_122376_(tag.m_128451_("Face")));
      }
   }
}
