package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.AE2LTCapabilities;
import com.moakiee.ae2lt.block.TeslaCoilBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.machine.common.LightningCollapseMatrixHost;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilAutomationInventory;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilEnergyStorage;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilInventory;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilLogic;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilStatus;
import com.moakiee.ae2lt.me.GridLightningEnergyHandler;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.menu.TeslaCoilMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public class TeslaCoilBlockEntity extends AENetworkBlockEntity implements IActionHost, FrequencyBindingHost, LightningCollapseMatrixHost {
   public static final int ENERGY_CAPACITY = 16000000;
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_ENERGY = "Energy";
   private static final String TAG_CONSUMED_ENERGY = "ConsumedEnergy";
   private static final String TAG_PROCESSING_TICKS = "ProcessingTicks";
   private static final String TAG_SELECTED_MODE = "SelectedMode";
   private static final String TAG_LOCKED_MODE = "LockedMode";
   private static final String TAG_LOCKED_BATCH_SIZE = "LockedBatchSize";
   private final TeslaCoilInventory inventory = new TeslaCoilInventory(this::onInventoryChanged);
   private final TeslaCoilAutomationInventory automationInventory = new TeslaCoilAutomationInventory(this.inventory);
   private final TeslaCoilEnergyStorage energyStorage = new TeslaCoilEnergyStorage(16000000L, this::onEnergyChanged);
   private final TeslaCoilLogic logic;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private TeslaCoilMode selectedMode = TeslaCoilMode.HIGH_VOLTAGE;
   private TeslaCoilMode lockedMode;
   private long lockedBatchSize = 0L;
   private long consumedEnergy;
   private int processingTicksSpent;
   private boolean working;

   public TeslaCoilBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.TESLA_COIL.get(), pos, blockState);
      this.logic = new TeslaCoilLogic(this);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this.logic);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TeslaCoilBlockEntity be) {
      if (!level.m_5776_()) {
         be.frequencyBinding.serverTick();
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

   public TeslaCoilInventory getInventory() {
      return this.inventory;
   }

   @Override
   public IItemHandlerModifiable getMatrixInventory() {
      return this.inventory;
   }

   @Override
   public int getMatrixSlot() {
      return 1;
   }

   public IItemHandlerModifiable getAutomationInventory() {
      return this.automationInventory;
   }

   public IEnergyStorage getEnergyStorageCapability(Direction side) {
      return this.energyStorage;
   }

   public TeslaCoilEnergyStorage getEnergyStorage() {
      return this.energyStorage;
   }

   public TeslaCoilLogic getLogic() {
      return this.logic;
   }

   public TeslaCoilMode getSelectedMode() {
      return this.selectedMode;
   }

   public long getCurrentTotalEnergy() {
      TeslaCoilMode mode = this.lockedMode != null ? this.lockedMode : this.selectedMode;
      long batchSize = this.lockedMode != null ? this.lockedBatchSize : this.getBatchSizeForMode(mode);
      return this.getTotalEnergyFor(mode, batchSize);
   }

   public void cycleMode() {
      this.selectedMode = this.selectedMode.next();
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   public boolean hasLockedMode() {
      return this.lockedMode != null;
   }

   public boolean lockSelectedMode() {
      if (this.lockedMode == null && this.canStartSelectedMode()) {
         this.lockedMode = this.selectedMode;
         this.lockedBatchSize = this.getBatchSizeForMode(this.selectedMode);
         if (this.lockedBatchSize <= 0L) {
            this.lockedMode = null;
            return false;
         } else {
            this.saveChanges();
            this.markForUpdate();
            this.logic.onStateChanged();
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean hasLocalStartPrerequisites() {
      return this.hasLocalPrerequisites(this.selectedMode, this.getBatchSizeForMode(this.selectedMode));
   }

   public boolean hasLocalResourcesForMinimumOperation() {
      return switch (this.selectedMode) {
         case HIGH_VOLTAGE -> {
            int dustPerOp = this.selectedMode.requiredDust();
            yield dustPerOp > 0 && this.inventory.getStackInSlot(0).m_41613_() >= dustPerOp;
         }
         case EXTREME_HIGH_VOLTAGE -> this.inventory.hasMatrix();
      };
   }

   public boolean hasLockedModeLocalPrerequisites() {
      return this.lockedMode != null && this.hasLocalPrerequisites(this.lockedMode, this.lockedBatchSize);
   }

   public boolean canStartSelectedMode() {
      long batchSize = this.getBatchSizeForMode(this.selectedMode);
      return batchSize > 0L && this.hasLocalPrerequisites(this.selectedMode, batchSize) && this.canCommitAgainstNetwork(this.selectedMode, batchSize);
   }

   public boolean hasEnoughEnergyForSelectedStart() {
      long batchSize = this.getBatchSizeForMode(this.selectedMode);
      return batchSize > 0L
         && this.energyStorage.getStoredEnergyLong() >= this.selectedMode.requiredEnergyForTick(0, 0L, this.getTotalEnergyFor(this.selectedMode, batchSize));
   }

   public long getConsumedEnergy() {
      return this.consumedEnergy;
   }

   public int getProcessingTicksSpent() {
      return this.processingTicksSpent;
   }

   public long getRequiredEnergyForNextTick() {
      return this.lockedMode == null ? 0L : this.lockedMode.requiredEnergyForTick(this.processingTicksSpent, this.consumedEnergy, this.getLockedTotalEnergy());
   }

   public double getProgress() {
      long totalEnergy = this.getLockedTotalEnergy();
      return this.lockedMode != null && totalEnergy > 0L ? Math.min(1.0, (double)this.consumedEnergy / (double)totalEnergy) : 0.0;
   }

   public boolean isReadyToCommit() {
      long totalEnergy = this.getLockedTotalEnergy();
      return this.lockedMode != null && this.lockedBatchSize > 0L && this.processingTicksSpent >= 5 && this.consumedEnergy >= totalEnergy;
   }

   public void advanceProgress(long amount) {
      if (this.lockedMode != null && amount > 0L) {
         this.consumedEnergy = Math.min(this.getLockedTotalEnergy(), this.consumedEnergy + amount);
         this.processingTicksSpent = Math.min(5, this.processingTicksSpent + 1);
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public TeslaCoilStatus getStatus() {
      if (this.lockedMode == null) {
         return TeslaCoilStatus.IDLE;
      } else if (this.isReadyToCommit()) {
         if (!this.hasLocalPrerequisites(this.lockedMode, this.lockedBatchSize)) {
            return TeslaCoilStatus.WAITING_INPUTS;
         } else {
            return this.canCommitAgainstNetwork(this.lockedMode, this.lockedBatchSize) ? TeslaCoilStatus.READY : TeslaCoilStatus.WAITING_NETWORK;
         }
      } else if (!this.hasLocalPrerequisites(this.lockedMode, this.lockedBatchSize)) {
         return TeslaCoilStatus.WAITING_INPUTS;
      } else {
         long required = this.getRequiredEnergyForNextTick();
         if (required <= 0L) {
            return TeslaCoilStatus.READY;
         } else {
            return this.energyStorage.getStoredEnergyLong() >= required ? TeslaCoilStatus.CHARGING : TeslaCoilStatus.WAITING_FE;
         }
      }
   }

   public long getAvailableHighVoltage() {
      return this.getAvailableLightning(LightningKey.HIGH_VOLTAGE);
   }

   public long getAvailableExtremeHighVoltage() {
      return this.getAvailableLightning(LightningKey.EXTREME_HIGH_VOLTAGE);
   }

   public boolean isMatrixInstalled() {
      return this.inventory.hasMatrix();
   }

   public boolean commitLockedMode() {
      if (this.lockedMode != null
         && this.lockedBatchSize > 0L
         && this.canCommitAgainstNetwork(this.lockedMode, this.lockedBatchSize)
         && this.hasLocalPrerequisites(this.lockedMode, this.lockedBatchSize)) {
         boolean committed = switch (this.lockedMode) {
            case HIGH_VOLTAGE -> this.commitHighVoltage();
            case EXTREME_HIGH_VOLTAGE -> this.commitExtremeHighVoltage();
         };
         if (!committed) {
            return false;
         } else {
            this.lockedMode = null;
            this.lockedBatchSize = 0L;
            this.consumedEnergy = 0L;
            this.processingTicksSpent = 0;
            this.saveChanges();
            this.markForUpdate();
            this.logic.onStateChanged();
            return true;
         }
      } else {
         return false;
      }
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(TeslaCoilMenu.TYPE, player, locator);
   }

   public boolean isWorking() {
      return this.working;
   }

   public void setWorking(boolean working) {
      boolean changed = this.working != working;
      this.working = working;
      if (this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         if (state.m_60713_((Block)ModBlocks.TESLA_COIL.get())
            && this.f_58857_.m_7702_(this.f_58858_) == this
            && state.m_61138_(TeslaCoilBlock.WORKING)
            && (Boolean)state.m_61143_(TeslaCoilBlock.WORKING) != working) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(TeslaCoilBlock.WORKING, working), 3);
         } else if (changed) {
            this.markForUpdate();
         }
      }
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.setWorking(this.hasLockedMode());
      if (this.f_58857_ != null) {
         TeslaCoilUpperBlockEntity.ensurePresent(this.f_58857_, this.f_58858_);
      }
   }

   public void m_7651_() {
      this.frequencyBinding.setRemoved();
      super.m_7651_();
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      super.onChunkUnloaded();
   }

   public void m_6339_() {
      super.m_6339_();
      this.frequencyBinding.clearRemoved();
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      this.inventory.saveToTag(data, "Inventory");
      data.m_128356_("Energy", this.energyStorage.getStoredEnergyLong());
      data.m_128356_("ConsumedEnergy", this.consumedEnergy);
      data.m_128405_("ProcessingTicks", this.processingTicksSpent);
      data.m_128359_("SelectedMode", this.selectedMode.m_7912_());
      if (this.lockedMode != null) {
         data.m_128359_("LockedMode", this.lockedMode.m_7912_());
         data.m_128356_("LockedBatchSize", this.lockedBatchSize);
      } else {
         data.m_128473_("LockedMode");
         data.m_128473_("LockedBatchSize");
      }

      this.frequencyBinding.save(data);
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      this.inventory.loadFromTag(data, "Inventory");
      this.energyStorage.loadStoredEnergy(data.m_128454_("Energy"));
      this.selectedMode = TeslaCoilMode.fromName(data.m_128461_("SelectedMode"));
      this.lockedMode = data.m_128441_("LockedMode") ? TeslaCoilMode.fromName(data.m_128461_("LockedMode")) : null;
      this.lockedBatchSize = Math.max(0L, data.m_128454_("LockedBatchSize"));
      this.consumedEnergy = Math.max(0L, data.m_128454_("ConsumedEnergy"));
      this.processingTicksSpent = Math.max(0, data.m_128451_("ProcessingTicks"));
      this.frequencyBinding.load(data);
      if (this.lockedMode == null) {
         this.lockedBatchSize = 0L;
         this.consumedEnergy = 0L;
         this.processingTicksSpent = 0;
      } else {
         if (this.lockedBatchSize <= 0L) {
            this.lockedBatchSize = 1L;
         }

         this.consumedEnergy = Math.min(this.consumedEnergy, this.getLockedTotalEnergy());
         this.processingTicksSpent = Math.min(this.processingTicksSpent, 5);
      }

      this.working = this.lockedMode != null;
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);

      for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
         ItemStack stack = this.inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            NativeStackDropHelper.addDrops(drops, stack);
         }
      }
   }

   public void m_6211_() {
      super.m_6211_();
      this.inventory.clear();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      MemoryCardConfigSupport.exportMemoryCardSettings(mode, output, tag -> {
         MemoryCardConfigSupport.writeEnum(tag, "SelectedMode", this.selectedMode);
         FrequencyBindingHelper.writeMemoryFrequency(tag, this.getFrequencyId());
         MemoryCardConfigSupport.writeMatrixCount(tag, this);
      });
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      MemoryCardConfigSupport.importMemoryCardSettings(mode, input, tag -> {
         TeslaCoilMode mode2 = MemoryCardConfigSupport.readEnum(tag, "SelectedMode", TeslaCoilMode.class, this.selectedMode);
         this.selectedMode = mode2;
         FrequencyBindingHelper.importMemoryFrequency(tag, this::setFrequency);
         MemoryCardConfigSupport.restoreMatrixCount(tag, player, this);
         this.saveChanges();
         this.markForUpdate();
         this.logic.onStateChanged();
      });
   }

   protected Item getItemFromBlockEntity() {
      return ((TeslaCoilBlock)ModBlocks.TESLA_COIL.get()).m_5456_();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.of(orientation.getSide(RelativeSide.BOTTOM));
   }

   private boolean hasLocalPrerequisites(TeslaCoilMode mode, long batchSize) {
      if (batchSize <= 0L) {
         return false;
      } else {
         return switch (mode) {
            case HIGH_VOLTAGE -> this.inventory.hasRequiredDust(this.getRequiredDustForBatch(mode, batchSize));
            case EXTREME_HIGH_VOLTAGE -> this.inventory.hasMatrix();
         };
      }
   }

   private boolean canCommitAgainstNetwork(TeslaCoilMode mode, long batchSize) {
      if (batchSize <= 0L || this.simulateInsert(mode.outputKey(), batchSize) < batchSize) {
         return false;
      } else if (mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE) {
         long requiredHighVoltage = this.getRequiredHighVoltageForBatch(mode, batchSize);
         return this.simulateExtract(LightningKey.HIGH_VOLTAGE, requiredHighVoltage) >= requiredHighVoltage;
      } else {
         return true;
      }
   }

   private long getAvailableLightning(LightningKey key) {
      return this.simulateExtract(key, Long.MAX_VALUE);
   }

   private boolean commitHighVoltage() {
      int requiredDust = this.getRequiredDustForBatch(this.lockedMode, this.lockedBatchSize);
      ItemStack extractedDust = this.inventory.extractItem(0, requiredDust, false);
      if (extractedDust.m_41613_() != requiredDust) {
         return false;
      } else {
         long inserted = this.insert(this.lockedMode.outputKey(), this.lockedBatchSize);
         if (inserted < this.lockedBatchSize) {
            this.inventory.insertItem(0, extractedDust, false);
            return false;
         } else {
            return true;
         }
      }
   }

   private boolean commitExtremeHighVoltage() {
      long requiredHighVoltage = this.getRequiredHighVoltageForBatch(this.lockedMode, this.lockedBatchSize);
      long extracted = this.extract(LightningKey.HIGH_VOLTAGE, requiredHighVoltage);
      if (extracted < requiredHighVoltage) {
         if (extracted > 0L) {
            this.insert(LightningKey.HIGH_VOLTAGE, extracted);
         }

         return false;
      } else {
         long inserted = this.insert(this.lockedMode.outputKey(), this.lockedBatchSize);
         if (inserted < this.lockedBatchSize) {
            this.insert(LightningKey.HIGH_VOLTAGE, extracted);
            return false;
         } else {
            return true;
         }
      }
   }

   private long simulateInsert(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().insert(key, amount, Actionable.SIMULATE, IActionSource.ofMachine(this));
      }
   }

   private long insert(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, IActionSource.ofMachine(this));
      }
   }

   private long simulateExtract(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().extract(key, amount, Actionable.SIMULATE, IActionSource.ofMachine(this));
      }
   }

   private long extract(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().extract(key, amount, Actionable.MODULATE, IActionSource.ofMachine(this));
      }
   }

   private void onInventoryChanged() {
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private void onEnergyChanged() {
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private long getBatchSizeForMode(TeslaCoilMode mode) {
      if (mode == TeslaCoilMode.HIGH_VOLTAGE) {
         int dustPerOperation = mode.requiredDust();
         if (dustPerOperation <= 0) {
            return 0L;
         } else {
            long craftable = (long)(this.inventory.getStackInSlot(0).m_41613_() / dustPerOperation);
            return craftable <= 0L ? 0L : Math.min(craftable, this.simulateInsert(mode.outputKey(), craftable));
         }
      } else {
         return this.hasLocalPrerequisites(mode, 1L) && this.canCommitAgainstNetwork(mode, 1L) ? 1L : 0L;
      }
   }

   private long getTotalEnergyFor(TeslaCoilMode mode, long batchSize) {
      return mode != null && batchSize > 0L ? Math.multiplyExact(mode.totalEnergy(), batchSize) : 0L;
   }

   private long getLockedTotalEnergy() {
      return this.getTotalEnergyFor(this.lockedMode, this.lockedBatchSize);
   }

   private int getRequiredDustForBatch(TeslaCoilMode mode, long batchSize) {
      return Math.toIntExact(Math.multiplyExact((long)mode.requiredDust(), batchSize));
   }

   private long getRequiredHighVoltageForBatch(TeslaCoilMode mode, long batchSize) {
      return Math.multiplyExact(mode.requiredHighVoltage(), batchSize);
   }

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      if (cap == ForgeCapabilities.ITEM_HANDLER) {
         return LazyOptional.of(this::getAutomationInventory).cast();
      } else if (cap == ForgeCapabilities.ENERGY) {
         return LazyOptional.of(() -> this.getEnergyStorageCapability(side)).cast();
      } else {
         return cap == AE2LTCapabilities.LIGHTNING_ENERGY_BLOCK
            ? LazyOptional.of(() -> new GridLightningEnergyHandler(this)).cast()
            : super.getCapability(cap, side);
      }
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.SMART;
   }
}
