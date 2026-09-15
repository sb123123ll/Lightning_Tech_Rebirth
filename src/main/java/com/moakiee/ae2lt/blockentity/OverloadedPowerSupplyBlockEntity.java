package com.moakiee.ae2lt.blockentity;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.moakiee.ae2lt.block.OverloadedPowerSupplyBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.logic.OverloadedPowerSupplyLogic;
import com.moakiee.ae2lt.logic.WirelessConnectionLists;
import com.moakiee.ae2lt.logic.WirelessConnectionRef;
import com.moakiee.ae2lt.logic.WirelessConnectionValidator;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import com.moakiee.ae2lt.menu.OverloadedPowerSupplyMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OverloadedPowerSupplyBlockEntity extends AENetworkBlockEntity implements InternalInventoryHost, FrequencyBindingHost {
   public static final int MAX_WIRELESS_CONNECTIONS = 64;
   private static final String TAG_MODE = "Mode";
   private static final String TAG_CONNECTIONS = "WirelessConnections";
   private static final String TAG_CELL_INV = "CellInv";
   private final AppEngInternalInventory cellInv = new AppEngInternalInventory(this, 1, 1) {
      public boolean isItemValid(int slot, ItemStack stack) {
         return AppFluxBridge.isFluxCell(stack);
      }
   };
   private final List<OverloadedPowerSupplyBlockEntity.WirelessConnection> connections = new ArrayList<>();
   private final List<OverloadedPowerSupplyBlockEntity.WirelessConnection> readOnlyConnections = Collections.unmodifiableList(this.connections);
   private int invalidConnectionScanCursor;
   private int connectionVersion;
   private final OverloadedPowerSupplyLogic logic;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private OverloadedPowerSupplyBlockEntity.PowerMode mode = OverloadedPowerSupplyBlockEntity.PowerMode.NORMAL;
   @Nullable
   private StorageCell cachedCellView;
   private boolean cellViewDirty = true;
   private boolean cellCapacityDirty = true;
   private long cachedCellCapacity;

   public OverloadedPowerSupplyBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.OVERLOADED_POWER_SUPPLY.get(), pos, blockState);
      this.logic = new OverloadedPowerSupplyLogic(this);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this.logic).setFlags(new GridFlags[]{GridFlags.REQUIRE_CHANNEL});
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, OverloadedPowerSupplyBlockEntity be) {
      if (!level.m_5776_()) {
         be.frequencyBinding.serverTick();
         if (level instanceof ServerLevel serverLevel) {
            be.tickWirelessConnectionCleanup(serverLevel);
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

   protected IManagedGridNode createMainNode() {
      return super.createMainNode().setTagName("overloaded_power_supply").setVisualRepresentation((ItemLike)ModBlocks.OVERLOADED_POWER_SUPPLY.get());
   }

   protected Item getItemFromBlockEntity() {
      return ((OverloadedPowerSupplyBlock)ModBlocks.OVERLOADED_POWER_SUPPLY.get()).m_5456_();
   }

   public AppEngInternalInventory getCellInventory() {
      return this.cellInv;
   }

   public ItemStack getInstalledCell() {
      return this.cellInv.getStackInSlot(0);
   }

   public long getBufferCapacity() {
      if (this.cellCapacityDirty) {
         this.cachedCellCapacity = AppFluxBridge.getFluxCellCapacity(this.getInstalledCell());
         this.cellCapacityDirty = false;
      }

      return this.cachedCellCapacity;
   }

   @Nullable
   public MEStorage getInstalledCellStorage() {
      if (!this.cellViewDirty && this.cachedCellView != null) {
         return this.cachedCellView;
      } else {
         this.cachedCellView = null;
         this.cellViewDirty = false;
         ItemStack stack = this.getInstalledCell();
         if (!stack.m_41619_() && AppFluxBridge.isFluxCell(stack)) {
            this.cachedCellView = StorageCells.getCellInventory(stack, this::onCellInventoryChanged);
            return this.cachedCellView;
         } else {
            return null;
         }
      }
   }

   private void onCellInventoryChanged() {
      this.saveChanges();
   }

   public void persistCellStorage() {
      AppFluxBridge.persistCellStorage(this.cachedCellView);
      this.saveChanges();
   }

   public OverloadedPowerSupplyBlockEntity.PowerMode getMode() {
      return this.mode;
   }

   public void cycleMode() {
      this.mode = this.mode.next();
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
      BlockState state = this.m_58900_();
      boolean currentlyPowered = state.m_61138_(OverloadedPowerSupplyBlock.POWERED) && (Boolean)state.m_61143_(OverloadedPowerSupplyBlock.POWERED);
      this.updateVisualState(currentlyPowered, this.mode == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD);
   }

   public void updateVisualState(boolean powered, boolean overloaded) {
      if (this.f_58857_ != null && !this.f_58857_.m_5776_()) {
         BlockState state = this.m_58900_();
         if (state.m_61138_(OverloadedPowerSupplyBlock.POWERED) && state.m_61138_(OverloadedPowerSupplyBlock.OVERLOADED)) {
            boolean curPowered = (Boolean)state.m_61143_(OverloadedPowerSupplyBlock.POWERED);
            boolean curOverloaded = (Boolean)state.m_61143_(OverloadedPowerSupplyBlock.OVERLOADED);
            if (curPowered != powered || curOverloaded != overloaded) {
               this.f_58857_
                  .m_7731_(
                     this.f_58858_,
                     (BlockState)((BlockState)state.m_61124_(OverloadedPowerSupplyBlock.POWERED, powered))
                        .m_61124_(OverloadedPowerSupplyBlock.OVERLOADED, overloaded),
                     3
                  );
            }
         }
      }
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.updateVisualState(false, this.mode == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD);
   }

   public OverloadedPowerSupplyLogic getSupplyLogic() {
      return this.logic;
   }

   public boolean addOrUpdateConnection(ResourceKey<Level> dimension, BlockPos pos, Direction face) {
      if (!this.isLocalDimension(dimension)) {
         return false;
      } else {
         int index = WirelessConnectionLists.indexOf(this.connections, dimension, pos);
         if (index >= 0) {
            OverloadedPowerSupplyBlockEntity.WirelessConnection updated = new OverloadedPowerSupplyBlockEntity.WirelessConnection(
               dimension, pos.m_7949_(), face
            );
            if (this.connections.get(index).equals(updated)) {
               return false;
            } else {
               this.connections.set(index, updated);
               this.invalidConnectionScanCursor = 0;
               this.notifyConnectionsChanged();
               return true;
            }
         } else if (this.connections.size() >= 64) {
            return false;
         } else {
            this.connections.add(new OverloadedPowerSupplyBlockEntity.WirelessConnection(dimension, pos.m_7949_(), face));
            this.invalidConnectionScanCursor = 0;
            this.notifyConnectionsChanged();
            return true;
         }
      }
   }

   public OverloadedPowerSupplyBlockEntity.ConnectionEditResult editConnections(ResourceKey<Level> dimension, Collection<BlockPos> positions, Direction face) {
      if (positions.isEmpty()) {
         return new OverloadedPowerSupplyBlockEntity.ConnectionEditResult(List.of(), List.of(), List.of(), 0);
      } else if (!this.isLocalDimension(dimension)) {
         return new OverloadedPowerSupplyBlockEntity.ConnectionEditResult(List.of(), List.of(), List.of(), 0);
      } else {
         ArrayList<BlockPos> disconnected = new ArrayList<>();
         ArrayList<BlockPos> updated = new ArrayList<>();
         ArrayList<BlockPos> connected = new ArrayList<>();
         int skippedDueToLimit = 0;
         boolean changed = false;

         for (BlockPos rawPos : positions) {
            BlockPos targetPos = rawPos.m_7949_();
            int index = WirelessConnectionLists.indexOf(this.connections, dimension, targetPos);
            if (index >= 0) {
               OverloadedPowerSupplyBlockEntity.WirelessConnection existing = this.connections.get(index);
               if (existing.boundFace() == face) {
                  this.connections.remove(index);
                  disconnected.add(targetPos);
                  this.invalidConnectionScanCursor = 0;
                  changed = true;
               } else {
                  this.connections.set(index, new OverloadedPowerSupplyBlockEntity.WirelessConnection(dimension, targetPos, face));
                  updated.add(targetPos);
                  this.invalidConnectionScanCursor = 0;
                  changed = true;
               }
            } else if (this.connections.size() >= 64) {
               skippedDueToLimit++;
            } else {
               this.connections.add(new OverloadedPowerSupplyBlockEntity.WirelessConnection(dimension, targetPos, face));
               connected.add(targetPos);
               this.invalidConnectionScanCursor = 0;
               changed = true;
            }
         }

         if (changed) {
            this.notifyConnectionsChanged();
         }

         return new OverloadedPowerSupplyBlockEntity.ConnectionEditResult(
            List.copyOf(disconnected), List.copyOf(updated), List.copyOf(connected), skippedDueToLimit
         );
      }
   }

   public boolean removeConnection(ResourceKey<Level> dimension, BlockPos pos) {
      int index = WirelessConnectionLists.indexOf(this.connections, dimension, pos);
      if (index >= 0) {
         this.connections.remove(index);
         this.invalidConnectionScanCursor = 0;
         this.notifyConnectionsChanged();
         return true;
      } else {
         return false;
      }
   }

   public boolean removeConnections(Collection<OverloadedPowerSupplyBlockEntity.WirelessConnection> removedConnections) {
      if (removedConnections.isEmpty()) {
         return false;
      } else {
         boolean removed = this.connections.removeIf(removedConnections::contains);
         if (removed) {
            this.invalidConnectionScanCursor = 0;
            this.notifyConnectionsChanged();
         }

         return removed;
      }
   }

   public List<OverloadedPowerSupplyBlockEntity.WirelessConnection> getConnections() {
      return this.readOnlyConnections;
   }

   public int getConnectionVersion() {
      return this.connectionVersion;
   }

   public int clearInvalidConnections() {
      return this.pruneInvalidConnections(Integer.MAX_VALUE);
   }

   public int pruneInvalidConnections(int maxChecks) {
      if (this.m_58904_() instanceof ServerLevel serverLevel && maxChecks > 0 && !this.connections.isEmpty()) {
         WirelessConnectionLists.PruneResult result = WirelessConnectionLists.pruneInvalid(
            this.connections, this.invalidConnectionScanCursor, maxChecks, serverLevel, this.f_58858_
         );
         this.invalidConnectionScanCursor = result.nextCursor();
         if (result.removed() > 0) {
            this.notifyConnectionsChanged();
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

   private void notifyConnectionsChanged() {
      this.connectionVersion++;
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private boolean isLocalDimension(ResourceKey<Level> dimension) {
      return WirelessConnectionLists.isLocalDimension(this.f_58857_, dimension);
   }

   public void saveChanges() {
      super.saveChanges();
      this.markForUpdate();
   }

   public void onChangeInventory(InternalInventory inv, int slot) {
      if (inv == this.cellInv) {
         this.logic.flushBufferToNetwork();
         AppFluxBridge.persistCellStorage(this.cachedCellView);
         this.cellViewDirty = true;
         this.cellCapacityDirty = true;
         this.cachedCellView = null;
         this.logic.onStateChanged();
      }
   }

   public boolean isClientSide() {
      return this.f_58857_ != null && this.f_58857_.m_5776_();
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      this.logic.flushBufferToNetwork();
      AppFluxBridge.persistCellStorage(this.cachedCellView);
      super.addAdditionalDrops(level, pos, drops);
      ItemStack cell = this.cellInv.getStackInSlot(0);
      if (!cell.m_41619_()) {
         drops.add(cell.m_41777_());
      }
   }

   public void m_6211_() {
      super.m_6211_();
      this.cellInv.clear();
      this.connections.clear();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      FrequencyBindingHelper.exportMemorySettings(mode, output, this.getFrequencyId());
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      FrequencyBindingHelper.importMemorySettings(mode, input, this::setFrequency);
   }

   public void m_7651_() {
      this.frequencyBinding.setRemoved();
      this.logic.flushBufferToNetwork();
      super.m_7651_();
   }

   public void m_6339_() {
      super.m_6339_();
      this.frequencyBinding.clearRemoved();
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      this.logic.flushBufferToNetwork();
      super.onChunkUnloaded();
   }

   public void m_183515_(CompoundTag data) {
      AppFluxBridge.persistCellStorage(this.cachedCellView);
      super.m_183515_(data);
      data.m_128359_("Mode", this.mode.name());
      this.cellInv.writeToNBT(data, "CellInv");
      data.m_128365_("WirelessConnections", WirelessConnectionLists.writeTagList(this.connections));
      this.frequencyBinding.save(data);
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      if (data.m_128425_("Mode", 8)) {
         try {
            this.mode = OverloadedPowerSupplyBlockEntity.PowerMode.valueOf(data.m_128461_("Mode"));
         } catch (IllegalArgumentException var3) {
            this.mode = OverloadedPowerSupplyBlockEntity.PowerMode.NORMAL;
         }
      } else {
         this.mode = OverloadedPowerSupplyBlockEntity.PowerMode.NORMAL;
      }

      this.cellInv.readFromNBT(data, "CellInv");
      this.cellViewDirty = true;
      this.cellCapacityDirty = true;
      this.cachedCellView = null;
      WirelessConnectionLists.readTagList(data, "WirelessConnections", this.connections, 64, OverloadedPowerSupplyBlockEntity.WirelessConnection::fromTag);
      this.invalidConnectionScanCursor = 0;
      this.connectionVersion++;
      this.frequencyBinding.load(data);
      this.logic.onStateChanged();
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);
      data.writeByte(this.mode.ordinal());
      data.m_130130_(this.connections.size());

      for (OverloadedPowerSupplyBlockEntity.WirelessConnection connection : this.connections) {
         data.m_130085_(connection.dimension().m_135782_());
         data.m_130064_(connection.pos());
         data.writeByte(connection.boundFace().m_122411_());
      }
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);
      int modeOrdinal = data.readByte();
      OverloadedPowerSupplyBlockEntity.PowerMode newMode = modeOrdinal >= 0 && modeOrdinal < OverloadedPowerSupplyBlockEntity.PowerMode.values().length
         ? OverloadedPowerSupplyBlockEntity.PowerMode.values()[modeOrdinal]
         : OverloadedPowerSupplyBlockEntity.PowerMode.NORMAL;
      int count = data.m_130242_();
      ArrayList<OverloadedPowerSupplyBlockEntity.WirelessConnection> newConnections = new ArrayList<>(Math.min(count, 64));

      for (int i = 0; i < count; i++) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, data.m_130281_());
         BlockPos pos = data.m_130135_();
         int rawFace = data.readByte();
         Direction face = rawFace >= 0 && rawFace < Direction.values().length ? Direction.m_122376_(rawFace) : Direction.DOWN;
         OverloadedPowerSupplyBlockEntity.WirelessConnection connection = new OverloadedPowerSupplyBlockEntity.WirelessConnection(dim, pos, face);
         WirelessConnectionLists.addOrReplace(newConnections, connection, 64);
      }

      if (newMode != this.mode || !newConnections.equals(this.connections)) {
         this.mode = newMode;
         this.connections.clear();
         this.connections.addAll(newConnections);
         this.invalidConnectionScanCursor = 0;
         this.connectionVersion++;
         this.logic.onStateChanged();
         changed = true;
      }

      return changed;
   }

   public void openMenu(Player player, MenuLocator locator) {
      if (this.f_58857_ instanceof ServerLevel) {
         this.clearInvalidConnections();
      }

      MenuOpener.open(OverloadedPowerSupplyMenu.TYPE, player, locator);
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.SMART;
   }

   public static record ConnectionEditResult(List<BlockPos> disconnected, List<BlockPos> updated, List<BlockPos> connected, int skippedDueToLimit) {
      public boolean hasChanges() {
         return !this.disconnected.isEmpty() || !this.updated.isEmpty() || !this.connected.isEmpty();
      }
   }

   public static enum PowerMode {
      NORMAL,
      OVERLOAD;

      public OverloadedPowerSupplyBlockEntity.PowerMode next() {
         return this == NORMAL ? OVERLOAD : NORMAL;
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

      public static OverloadedPowerSupplyBlockEntity.WirelessConnection fromTag(CompoundTag tag) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(tag.m_128461_("Dim")));
         BlockPos pos = BlockPos.m_122022_(tag.m_128454_("Pos"));
         int rawFace = tag.m_128451_("Face");
         Direction face = rawFace >= 0 && rawFace < Direction.values().length ? Direction.m_122376_(rawFace) : Direction.DOWN;
         return new OverloadedPowerSupplyBlockEntity.WirelessConnection(dim, pos, face);
      }
   }
}
