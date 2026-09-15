package com.moakiee.ae2lt.blockentity;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AECableType;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.patternprovider.WirelessPatternProviderHost;
import com.moakiee.ae2lt.block.OverloadedPatternProviderBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.logic.EjectModeRegistry;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.logic.OverloadedPatternProviderLogic;
import com.moakiee.ae2lt.logic.ProviderTarget;
import com.moakiee.ae2lt.logic.WirelessConnectionLists;
import com.moakiee.ae2lt.logic.WirelessConnectionRef;
import com.moakiee.ae2lt.logic.WirelessConnectionValidator;
import com.moakiee.ae2lt.logic.WirelessPatternContainerGroupSelector;
import com.moakiee.ae2lt.menu.OverloadedPatternProviderMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OverloadedPatternProviderBlockEntity extends PatternProviderBlockEntity implements FrequencyBindingHost, WirelessPatternProviderHost {
   public static final int SLOTS_PER_PAGE = 36;
   public static final int MAX_WIRELESS_CONNECTIONS = 1024;
   private static final double IDLE_BASE = 5.0;
   private static final double IDLE_WIRELESS_BONUS = 5.0;
   private static final double IDLE_PER_CONNECTION = 1.0;
   private static final double IDLE_FAST_MULTIPLIER = 1.5;
   private OverloadedPatternProviderBlockEntity.ProviderMode providerMode = OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL;
   private OverloadedPatternProviderBlockEntity.ReturnMode returnMode = OverloadedPatternProviderBlockEntity.ReturnMode.OFF;
   private OverloadedPatternProviderBlockEntity.WirelessDispatchMode wirelessDispatchMode = OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION;
   private OverloadedPatternProviderBlockEntity.WirelessSpeedMode wirelessSpeedMode = OverloadedPatternProviderBlockEntity.WirelessSpeedMode.NORMAL;
   private OverloadedPatternProviderBlockEntity.BlockingMode blockingMode = OverloadedPatternProviderBlockEntity.BlockingMode.NORMAL;
   private boolean filteredImport = false;
   private boolean adaptiveBatchEnabled = false;
   private final List<OverloadedPatternProviderBlockEntity.WirelessConnection> connections = new ArrayList<>();
   private int invalidConnectionScanCursor;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private static final String TAG_PROVIDER_MODE = "OverloadMode";
   private static final String TAG_AUTO_RETURN = "AutoReturn";
   private static final String TAG_RETURN_MODE = "ReturnMode";
   private static final String TAG_WIRELESS_DISPATCH_MODE = "WirelessDispatchMode";
   private static final String TAG_WIRELESS_SPEED_MODE = "WirelessSpeedMode";
   private static final String TAG_BLOCKING_MODE = "BlockingMode";
   private static final String TAG_FILTERED_IMPORT = "FilteredImport";
   private static final String TAG_ADAPTIVE_BATCH_ENABLED = "AdaptiveBatchEnabled";
   private static final String TAG_CONNECTIONS = "WirelessConnections";
   private boolean unloadingChunk = false;

   public OverloadedPatternProviderBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.OVERLOADED_PATTERN_PROVIDER.get(), pos, blockState);
   }

   protected OverloadedPatternProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
      super(type, pos, blockState);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, OverloadedPatternProviderBlockEntity be) {
      if (!level.m_5776_()) {
         be.frequencyBinding.serverTick();
         if (level instanceof ServerLevel serverLevel) {
            be.tickWirelessConnectionCleanup(serverLevel);
         }

         OverloadedPatternProviderLogic logic = be.getOverloadedLogic();
         if (logic != null) {
            logic.tickOverflowRetries();
         }
      }
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

   public void onMainNodeStateChanged(State reason) {
      super.onMainNodeStateChanged(reason);
      this.frequencyBinding.onMainNodeStateChanged(reason);
   }

   public int getTotalPatternCapacity() {
      return 36;
   }

   protected PatternProviderLogic createLogic() {
      return new OverloadedPatternProviderLogic(this.getMainNode(), this, this.getTotalPatternCapacity());
   }

   public void onReady() {
      OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
      if (logic != null) {
         logic.onBlockEntityReady();
      }

      super.onReady();
      this.frequencyBinding.onReady();
      this.recomputeIdlePower();
   }

   void recomputeIdlePower() {
      double idle = 5.0;
      if (this.providerMode == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS) {
         idle += 5.0;
         idle += (double)this.connections.size() * 1.0;
         if (this.wirelessSpeedMode == OverloadedPatternProviderBlockEntity.WirelessSpeedMode.FAST) {
            idle *= 1.5;
         }
      }

      this.getMainNode().setIdlePowerUsage(idle);
   }

   @Nullable
   private OverloadedPatternProviderLogic getOverloadedLogic() {
      return this.getLogic() instanceof OverloadedPatternProviderLogic overloadedLogic ? overloadedLogic : null;
   }

   private void notifyLogicStateChanged() {
      OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
      if (logic != null) {
         logic.onHostStateChanged();
      }
   }

   public void onNeighborChanged() {
      OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
      if (logic != null) {
         logic.onNeighborChanged();
      }
   }

   public void saveChanges() {
      super.saveChanges();
      Level level = this.m_58904_();
      if (level != null && !level.f_46443_) {
         OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
         if (logic != null) {
            logic.onPersistentStateChanged();
         }
      }
   }

   public EnumSet<Direction> getTargets() {
      return this.providerMode == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS ? EnumSet.noneOf(Direction.class) : super.getTargets();
   }

   public OverloadedPatternProviderBlockEntity.ProviderMode getProviderMode() {
      return this.providerMode;
   }

   public void setProviderMode(OverloadedPatternProviderBlockEntity.ProviderMode providerMode) {
      if (this.providerMode != providerMode) {
         this.providerMode = providerMode;
         this.recomputeIdlePower();
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public OverloadedPatternProviderBlockEntity.ReturnMode getReturnMode() {
      return this.returnMode;
   }

   public void setReturnMode(OverloadedPatternProviderBlockEntity.ReturnMode mode) {
      if (this.returnMode != mode) {
         this.returnMode = mode;
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public boolean isAutoReturn() {
      return this.returnMode != OverloadedPatternProviderBlockEntity.ReturnMode.OFF;
   }

   public OverloadedPatternProviderBlockEntity.WirelessDispatchMode getWirelessDispatchMode() {
      return this.wirelessDispatchMode;
   }

   public void setWirelessDispatchMode(OverloadedPatternProviderBlockEntity.WirelessDispatchMode wirelessDispatchMode) {
      if (this.wirelessDispatchMode != wirelessDispatchMode) {
         this.wirelessDispatchMode = wirelessDispatchMode;
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public OverloadedPatternProviderBlockEntity.WirelessSpeedMode getWirelessSpeedMode() {
      return this.wirelessSpeedMode;
   }

   public void setWirelessSpeedMode(OverloadedPatternProviderBlockEntity.WirelessSpeedMode wirelessSpeedMode) {
      if (this.wirelessSpeedMode != wirelessSpeedMode) {
         this.wirelessSpeedMode = wirelessSpeedMode;
         this.recomputeIdlePower();
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public OverloadedPatternProviderBlockEntity.BlockingMode getBlockingMode() {
      return this.blockingMode;
   }

   public void setBlockingMode(OverloadedPatternProviderBlockEntity.BlockingMode blockingMode) {
      if (this.blockingMode != blockingMode) {
         this.blockingMode = blockingMode;
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public boolean isFilteredImport() {
      return this.filteredImport;
   }

   public void setFilteredImport(boolean filteredImport) {
      if (this.filteredImport != filteredImport) {
         this.filteredImport = filteredImport;
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public boolean isAdaptiveBatchEnabled() {
      return this.adaptiveBatchEnabled;
   }

   public void setAdaptiveBatchEnabled(boolean adaptiveBatchEnabled) {
      if (this.adaptiveBatchEnabled != adaptiveBatchEnabled) {
         this.adaptiveBatchEnabled = adaptiveBatchEnabled;
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public InternalInventory getExposedPatternInventory() {
      return this.getLogic().getPatternInv();
   }

   @Override
   public boolean addOrUpdateConnection(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace) {
      if (!this.isLocalDimension(dimension)) {
         return false;
      } else {
         int index = WirelessConnectionLists.indexOf(this.connections, dimension, pos);
         if (index >= 0) {
            OverloadedPatternProviderBlockEntity.WirelessConnection updated = new OverloadedPatternProviderBlockEntity.WirelessConnection(
               dimension, pos, boundFace
            );
            if (this.connections.get(index).equals(updated)) {
               return true;
            } else {
               this.connections.set(index, updated);
               this.invalidConnectionScanCursor = 0;
               this.notifyLogicStateChanged();
               this.saveChanges();
               this.markForUpdate();
               return true;
            }
         } else if (this.connections.size() >= 1024) {
            return false;
         } else {
            this.connections.add(new OverloadedPatternProviderBlockEntity.WirelessConnection(dimension, pos, boundFace));
            this.invalidConnectionScanCursor = 0;
            this.recomputeIdlePower();
            this.notifyLogicStateChanged();
            this.saveChanges();
            this.markForUpdate();
            return true;
         }
      }
   }

   @Override
   public boolean removeConnection(ResourceKey<Level> dimension, BlockPos pos) {
      int index = WirelessConnectionLists.indexOf(this.connections, dimension, pos);
      if (index < 0) {
         return false;
      } else {
         this.connections.remove(index);
         this.invalidConnectionScanCursor = 0;
         this.recomputeIdlePower();
         this.notifyLogicStateChanged();
         this.saveChanges();
         this.markForUpdate();
         return true;
      }
   }

   @Override
   public List<OverloadedPatternProviderBlockEntity.WirelessConnection> getConnections() {
      return Collections.unmodifiableList(this.connections);
   }

   @Override
   public BlockPos getProviderPos() {
      return this.m_58899_();
   }

   @Override
   public boolean isWirelessProvider() {
      return this.providerMode == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS;
   }

   @Override
   public int getMaxWirelessConnections() {
      return 1024;
   }

   public int clearInvalidConnections() {
      return this.pruneInvalidConnections(Integer.MAX_VALUE);
   }

   public int pruneInvalidConnections(int maxChecks) {
      if (this.m_58904_() instanceof ServerLevel serverLevel && maxChecks > 0 && !this.connections.isEmpty()) {
         WirelessConnectionLists.PruneResult result = WirelessConnectionLists.pruneInvalid(
            this.connections, this.invalidConnectionScanCursor, maxChecks, serverLevel, this.f_58858_, this::canRemoveInvalidConnection
         );
         this.invalidConnectionScanCursor = result.nextCursor();
         if (result.removed() > 0) {
            this.recomputeIdlePower();
            this.notifyLogicStateChanged();
            this.saveChanges();
            this.markForUpdate();
         }

         return result.removed();
      }

      return 0;
   }

   private void tickWirelessConnectionCleanup(ServerLevel level) {
      if (!this.connections.isEmpty() && WirelessConnectionValidator.shouldRunPeriodicPrune(level, this.f_58858_)) {
         this.pruneInvalidConnections(64);
      }
   }

   private boolean canRemoveInvalidConnection(OverloadedPatternProviderBlockEntity.WirelessConnection conn) {
      OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
      return logic == null || logic.prepareInvalidConnectionRemoval(conn);
   }

   private boolean isLocalDimension(ResourceKey<Level> dimension) {
      return WirelessConnectionLists.isLocalDimension(this.f_58857_, dimension);
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);
      data.writeByte(this.providerMode.ordinal());
      data.writeByte(this.returnMode.ordinal());
      data.writeByte(this.wirelessDispatchMode.ordinal());
      data.writeByte(this.wirelessSpeedMode.ordinal());
      data.writeByte(this.blockingMode.ordinal());
      data.writeBoolean(this.filteredImport);
      data.writeBoolean(this.adaptiveBatchEnabled);
      data.m_130130_(this.connections.size());

      for (OverloadedPatternProviderBlockEntity.WirelessConnection conn : this.connections) {
         data.m_130085_(conn.dimension().m_135782_());
         data.m_130064_(conn.pos());
         data.writeByte(conn.boundFace().m_122411_());
      }
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);
      byte modeOrd = data.readByte();
      OverloadedPatternProviderBlockEntity.ProviderMode newMode = modeOrd >= 0 && modeOrd < OverloadedPatternProviderBlockEntity.ProviderMode.values().length
         ? OverloadedPatternProviderBlockEntity.ProviderMode.values()[modeOrd]
         : OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL;
      byte rmOrd = data.readByte();
      OverloadedPatternProviderBlockEntity.ReturnMode newReturnMode = rmOrd >= 0 && rmOrd < OverloadedPatternProviderBlockEntity.ReturnMode.values().length
         ? OverloadedPatternProviderBlockEntity.ReturnMode.values()[rmOrd]
         : OverloadedPatternProviderBlockEntity.ReturnMode.OFF;
      byte dispatchOrd = data.readByte();
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode newDispatchMode = dispatchOrd >= 0
            && dispatchOrd < OverloadedPatternProviderBlockEntity.WirelessDispatchMode.values().length
         ? OverloadedPatternProviderBlockEntity.WirelessDispatchMode.values()[dispatchOrd]
         : OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION;
      byte speedOrd = data.readByte();
      OverloadedPatternProviderBlockEntity.WirelessSpeedMode newSpeedMode = speedOrd >= 0
            && speedOrd < OverloadedPatternProviderBlockEntity.WirelessSpeedMode.values().length
         ? OverloadedPatternProviderBlockEntity.WirelessSpeedMode.values()[speedOrd]
         : OverloadedPatternProviderBlockEntity.WirelessSpeedMode.NORMAL;
      byte blockingOrd = data.readByte();
      OverloadedPatternProviderBlockEntity.BlockingMode newBlockingMode = blockingOrd >= 0
            && blockingOrd < OverloadedPatternProviderBlockEntity.BlockingMode.values().length
         ? OverloadedPatternProviderBlockEntity.BlockingMode.values()[blockingOrd]
         : OverloadedPatternProviderBlockEntity.BlockingMode.NORMAL;
      boolean newFilteredImport = data.readBoolean();
      boolean newAdaptiveBatchEnabled = data.readBoolean();
      int count = data.m_130242_();
      ArrayList<OverloadedPatternProviderBlockEntity.WirelessConnection> newConns = new ArrayList<>(Math.min(count, 1024));

      for (int i = 0; i < count; i++) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, data.m_130281_());
         BlockPos pos = data.m_130135_();
         Direction face = Direction.m_122376_(data.readByte());
         WirelessConnectionLists.addOrReplace(newConns, new OverloadedPatternProviderBlockEntity.WirelessConnection(dim, pos, face), 1024);
      }

      if (newMode != this.providerMode
         || newReturnMode != this.returnMode
         || newDispatchMode != this.wirelessDispatchMode
         || newSpeedMode != this.wirelessSpeedMode
         || newBlockingMode != this.blockingMode
         || newFilteredImport != this.filteredImport
         || newAdaptiveBatchEnabled != this.adaptiveBatchEnabled
         || !newConns.equals(this.connections)) {
         this.providerMode = newMode;
         this.returnMode = newReturnMode;
         this.wirelessDispatchMode = newDispatchMode;
         this.wirelessSpeedMode = newSpeedMode;
         this.blockingMode = newBlockingMode;
         this.filteredImport = newFilteredImport;
         this.adaptiveBatchEnabled = newAdaptiveBatchEnabled;
         this.connections.clear();
         this.connections.addAll(newConns);
         this.invalidConnectionScanCursor = 0;
         this.recomputeIdlePower();
         this.notifyLogicStateChanged();
         changed = true;
      }

      return changed;
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      data.m_128359_("OverloadMode", this.providerMode.name());
      data.m_128359_("ReturnMode", this.returnMode.name());
      data.m_128359_("WirelessDispatchMode", this.wirelessDispatchMode.name());
      data.m_128359_("WirelessSpeedMode", this.wirelessSpeedMode.name());
      data.m_128359_("BlockingMode", this.blockingMode.name());
      data.m_128379_("FilteredImport", this.filteredImport);
      data.m_128379_("AdaptiveBatchEnabled", this.adaptiveBatchEnabled);
      data.m_128365_("WirelessConnections", WirelessConnectionLists.writeTagList(this.connections));
      this.frequencyBinding.save(data);
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      if (data.m_128441_("OverloadMode")) {
         try {
            this.providerMode = OverloadedPatternProviderBlockEntity.ProviderMode.valueOf(data.m_128461_("OverloadMode"));
         } catch (IllegalArgumentException var7) {
            this.providerMode = OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL;
         }
      }

      if (data.m_128441_("ReturnMode")) {
         try {
            this.returnMode = OverloadedPatternProviderBlockEntity.ReturnMode.valueOf(data.m_128461_("ReturnMode"));
         } catch (IllegalArgumentException var6) {
            this.returnMode = OverloadedPatternProviderBlockEntity.ReturnMode.OFF;
         }
      } else if (data.m_128441_("AutoReturn")) {
         this.returnMode = data.m_128471_("AutoReturn")
            ? OverloadedPatternProviderBlockEntity.ReturnMode.AUTO
            : OverloadedPatternProviderBlockEntity.ReturnMode.OFF;
      }

      if (data.m_128441_("WirelessDispatchMode")) {
         try {
            this.wirelessDispatchMode = OverloadedPatternProviderBlockEntity.WirelessDispatchMode.valueOf(data.m_128461_("WirelessDispatchMode"));
         } catch (IllegalArgumentException var5) {
            this.wirelessDispatchMode = OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION;
         }
      }

      if (data.m_128441_("WirelessSpeedMode")) {
         try {
            this.wirelessSpeedMode = OverloadedPatternProviderBlockEntity.WirelessSpeedMode.valueOf(data.m_128461_("WirelessSpeedMode"));
         } catch (IllegalArgumentException var4) {
            this.wirelessSpeedMode = OverloadedPatternProviderBlockEntity.WirelessSpeedMode.NORMAL;
         }
      }

      if (data.m_128441_("BlockingMode")) {
         try {
            this.blockingMode = OverloadedPatternProviderBlockEntity.BlockingMode.valueOf(data.m_128461_("BlockingMode"));
         } catch (IllegalArgumentException var3) {
            this.blockingMode = OverloadedPatternProviderBlockEntity.BlockingMode.NORMAL;
         }
      }

      this.filteredImport = data.m_128471_("FilteredImport");
      this.adaptiveBatchEnabled = data.m_128471_("AdaptiveBatchEnabled");
      WirelessConnectionLists.readTagList(data, "WirelessConnections", this.connections, 1024, OverloadedPatternProviderBlockEntity.WirelessConnection::fromTag);
      this.invalidConnectionScanCursor = 0;
      this.frequencyBinding.load(data);
      this.recomputeIdlePower();
      this.notifyLogicStateChanged();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      MemoryCardConfigSupport.exportMemoryCardSettings(mode, output, tag -> {
         MemoryCardConfigSupport.writeEnum(tag, "OverloadMode", this.providerMode);
         MemoryCardConfigSupport.writeEnum(tag, "ReturnMode", this.returnMode);
         MemoryCardConfigSupport.writeEnum(tag, "WirelessDispatchMode", this.wirelessDispatchMode);
         MemoryCardConfigSupport.writeEnum(tag, "WirelessSpeedMode", this.wirelessSpeedMode);
         MemoryCardConfigSupport.writeEnum(tag, "BlockingMode", this.blockingMode);
         tag.m_128379_("FilteredImport", this.filteredImport);
         tag.m_128379_("AdaptiveBatchEnabled", this.adaptiveBatchEnabled);
         FrequencyBindingHelper.writeMemoryFrequency(tag, this.getFrequencyId());
      });
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      MemoryCardConfigSupport.importMemoryCardSettings(
         mode,
         input,
         tag -> {
            this.providerMode = MemoryCardConfigSupport.readEnum(
               tag, "OverloadMode", OverloadedPatternProviderBlockEntity.ProviderMode.class, this.providerMode
            );
            this.returnMode = MemoryCardConfigSupport.readEnum(tag, "ReturnMode", OverloadedPatternProviderBlockEntity.ReturnMode.class, this.returnMode);
            this.wirelessDispatchMode = MemoryCardConfigSupport.readEnum(
               tag, "WirelessDispatchMode", OverloadedPatternProviderBlockEntity.WirelessDispatchMode.class, this.wirelessDispatchMode
            );
            this.wirelessSpeedMode = MemoryCardConfigSupport.readEnum(
               tag, "WirelessSpeedMode", OverloadedPatternProviderBlockEntity.WirelessSpeedMode.class, this.wirelessSpeedMode
            );
            this.blockingMode = MemoryCardConfigSupport.readEnum(
               tag, "BlockingMode", OverloadedPatternProviderBlockEntity.BlockingMode.class, this.blockingMode
            );
            MemoryCardConfigSupport.ifBoolean(tag, "FilteredImport", v -> this.filteredImport = v);
            MemoryCardConfigSupport.ifBoolean(tag, "AdaptiveBatchEnabled", v -> this.adaptiveBatchEnabled = v);
            FrequencyBindingHelper.importMemoryFrequency(tag, this::setFrequency);
            this.recomputeIdlePower();
            this.notifyLogicStateChanged();
            this.saveChanges();
            this.markForUpdate();
         }
      );
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      super.onChunkUnloaded();
      this.unloadingChunk = true;
      OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
      if (logic != null) {
         logic.flushWirelessEnergyBuffer();
      }
   }

   public void m_7651_() {
      this.frequencyBinding.setRemoved();
      if (!this.unloadingChunk) {
         List<EjectModeRegistry.DimPos> removed = EjectModeRegistry.unregisterAll(this, true);
         if (this.f_58857_ instanceof ServerLevel sl) {
            MinecraftServer server = sl.m_7654_();

            for (EjectModeRegistry.DimPos dp : removed) {
               ServerLevel targetLevel = server.m_129880_(dp.dimension());
               if (targetLevel != null) {
               }
            }
         }
      }

      OverloadedPatternProviderLogic logic = this.getOverloadedLogic();
      if (logic != null) {
         logic.flushWirelessEnergyBuffer();
      }

      super.m_7651_();
   }

   public void m_6339_() {
      super.m_6339_();
      this.frequencyBinding.clearRemoved();
   }

   public void openMenu(Player player, MenuLocator locator) {
      if (this.f_58857_ instanceof ServerLevel) {
         this.clearInvalidConnections();
      }

      MenuOpener.open(OverloadedPatternProviderMenu.TYPE, player, locator);
   }

   public void returnToMainMenu(Player player, ISubMenu subMenu) {
      MenuOpener.returnTo(OverloadedPatternProviderMenu.TYPE, player, subMenu.getLocator());
   }

   public ItemStack getMainMenuIcon() {
      return new ItemStack((ItemLike)ModBlocks.OVERLOADED_PATTERN_PROVIDER.get());
   }

   public AEItemKey getTerminalIcon() {
      return AEItemKey.of((ItemLike)ModBlocks.OVERLOADED_PATTERN_PROVIDER.get());
   }

   public PatternContainerGroup getTerminalGroup() {
      if (this.providerMode == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS && !this.hasCustomTerminalName()) {
         PatternContainerGroup wirelessGroup = this.findMostFrequentWirelessTerminalGroup();
         return wirelessGroup != null ? wirelessGroup : super.getTerminalGroup();
      } else {
         return super.getTerminalGroup();
      }
   }

   private boolean hasCustomTerminalName() {
      return this.m_8077_();
   }

   @Nullable
   private PatternContainerGroup findMostFrequentWirelessTerminalGroup() {
      if (this.f_58857_ instanceof ServerLevel hostLevel && !this.connections.isEmpty()) {
         ArrayList<PatternContainerGroup> groups = new ArrayList<>(this.connections.size());

         for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : this.connections) {
            ServerLevel targetLevel = this.resolveTerminalGroupTargetLevel(hostLevel, connection);
            if (targetLevel != null) {
               PatternContainerGroup group = PatternContainerGroup.fromMachine(targetLevel, connection.pos(), connection.boundFace());
               if (group != null) {
                  groups.add(group);
               }
            }
         }

         return WirelessPatternContainerGroupSelector.selectMostFrequent(groups).orElse(null);
      }

      return null;
   }

   @Nullable
   private ServerLevel resolveTerminalGroupTargetLevel(ServerLevel hostLevel, OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      if (WirelessConnectionValidator.validate(hostLevel, this.f_58858_, connection) != WirelessConnectionValidator.Status.VALID) {
         return null;
      } else {
         ServerLevel targetLevel = hostLevel.m_7654_().m_129880_(connection.dimension());
         return targetLevel != null && targetLevel.m_46749_(connection.pos()) ? targetLevel : null;
      }
   }

   protected Item getItemFromBlockEntity() {
      return ((OverloadedPatternProviderBlock)ModBlocks.OVERLOADED_PATTERN_PROVIDER.get()).m_5456_();
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.SMART;
   }

   public static enum BlockingMode {
      NORMAL,
      SAME_PATTERN;
   }

   public static enum ProviderMode {
      NORMAL,
      WIRELESS;
   }

   public static enum ReturnMode {
      OFF,
      AUTO,
      EJECT;
   }

   public static final class WirelessConnection extends ProviderTarget implements WirelessConnectionRef {
      private static final String TAG_DIM = "Dim";
      private static final String TAG_POS = "Pos";
      private static final String TAG_FACE = "Face";

      public WirelessConnection(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace) {
         super(dimension, pos, boundFace);
      }

      @Override
      public CompoundTag toTag() {
         CompoundTag tag = new CompoundTag();
         tag.m_128359_("Dim", this.dimension().m_135782_().toString());
         tag.m_128356_("Pos", this.pos().m_121878_());
         tag.m_128405_("Face", this.boundFace().m_122411_());
         return tag;
      }

      public static OverloadedPatternProviderBlockEntity.WirelessConnection fromTag(CompoundTag tag) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(tag.m_128461_("Dim")));
         BlockPos pos = BlockPos.m_122022_(tag.m_128454_("Pos"));
         Direction face = Direction.m_122376_(tag.m_128451_("Face"));
         return new OverloadedPatternProviderBlockEntity.WirelessConnection(dim, pos, face);
      }
   }

   public static enum WirelessDispatchMode {
      SINGLE_TARGET,
      EVEN_DISTRIBUTION;
   }

   public static enum WirelessSpeedMode {
      NORMAL,
      FAST;
   }
}
