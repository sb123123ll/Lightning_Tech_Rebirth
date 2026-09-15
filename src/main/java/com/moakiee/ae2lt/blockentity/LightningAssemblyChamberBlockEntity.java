package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.storage.CompositeStorage;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.AE2LTCapabilities;
import com.moakiee.ae2lt.block.LightningAssemblyChamberBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.logic.AdjacentItemAutoExportHelper;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.machine.common.GridRecipeMachineHost;
import com.moakiee.ae2lt.machine.common.LightningCollapseMatrixHost;
import com.moakiee.ae2lt.machine.common.SingleOutputLightningRecipeExecutor;
import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberAutomationInventory;
import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberEnergyStorage;
import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberInventory;
import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberLogic;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyLockedRecipe;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipeCandidate;
import com.moakiee.ae2lt.machine.lightningassembly.recipe.LightningAssemblyRecipeService;
import com.moakiee.ae2lt.me.GridLightningEnergyHandler;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.menu.LightningAssemblyChamberMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.util.LargeStackStreamCodecs;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
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

public class LightningAssemblyChamberBlockEntity
   extends AENetworkBlockEntity
   implements IUpgradeableObject,
   FrequencyBindingHost,
   LightningCollapseMatrixHost,
   GridRecipeMachineHost<LightningAssemblyLockedRecipe, LightningAssemblyRecipeCandidate> {
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_LOCKED_RECIPE = "LockedRecipe";
   private static final String TAG_UPGRADES = "Upgrades";
   private static final String TAG_ENERGY = "Energy";
   private static final String TAG_CONSUMED_ENERGY = "ConsumedEnergy";
   private static final String TAG_PROCESSING_TICKS = "ProcessingTicks";
   private static final String TAG_AUTO_EXPORT = "AutoExport";
   private static final String TAG_ALLOWED_OUTPUTS = "AllowedOutputs";
   public static final int ENERGY_CAPACITY = 1000000;
   public static final int SPEED_CARD_SLOTS = 4;
   private final LightningAssemblyChamberInventory inventory = new LightningAssemblyChamberInventory(this::onInventoryChanged);
   private final LightningAssemblyChamberAutomationInventory automationInventory = new LightningAssemblyChamberAutomationInventory(this.inventory);
   private final LightningAssemblyChamberEnergyStorage energyStorage = new LightningAssemblyChamberEnergyStorage(1000000L, this::onEnergyChanged);
   private final IUpgradeInventory upgrades = UpgradeInventories.forMachine((ItemLike)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get(), 4, this::onUpgradesChanged);
   private final LightningAssemblyChamberLogic logic;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private LightningAssemblyLockedRecipe lockedRecipe;
   private long consumedEnergy;
   private int processingTicksSpent;
   private boolean working;
   private boolean powered;
   private boolean autoExport;
   private boolean removing;
   private ItemStack clientRecipeResult = ItemStack.f_41583_;
   private EnumSet<RelativeSide> allowedOutputs = EnumSet.noneOf(RelativeSide.class);
   private final AdjacentItemAutoExportHelper.DirectionalTargetCache exportTargetCache = new AdjacentItemAutoExportHelper.DirectionalTargetCache();

   public LightningAssemblyChamberBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.LIGHTNING_ASSEMBLY_CHAMBER.get(), pos, blockState);
      this.logic = new LightningAssemblyChamberLogic(this);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this.logic).addService(IStorageWatcherNode.class, new IStorageWatcherNode() {
         public void updateWatcher(IStackWatcher newWatcher) {
            LightningAssemblyChamberBlockEntity.this.configureLightningWatcher(newWatcher);
         }

         public void onStackChange(AEKey what, long amount) {
            LightningAssemblyChamberBlockEntity.this.onLightningStackChanged(what);
         }
      });
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, LightningAssemblyChamberBlockEntity be) {
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

   public LightningAssemblyChamberInventory getInventory() {
      return this.inventory;
   }

   @Override
   public IItemHandlerModifiable getMatrixInventory() {
      return this.inventory;
   }

   @Override
   public int getMatrixSlot() {
      return 9;
   }

   public IItemHandlerModifiable getAutomationInventory() {
      return this.automationInventory;
   }

   public IEnergyStorage getEnergyStorageCapability(Direction side) {
      return this.energyStorage;
   }

   public LightningAssemblyChamberEnergyStorage getEnergyStorage() {
      return this.energyStorage;
   }

   public LightningAssemblyChamberLogic getLogic() {
      return this.logic;
   }

   public Optional<LightningAssemblyRecipeCandidate> findProcessableRecipe() {
      return LightningAssemblyRecipeService.findFirstProcessable(
         this.m_58904_(), this.inventory, this.getAvailableHighVoltage(), this.getAvailableExtremeHighVoltage()
      );
   }

   @Override
   public long getConsumedEnergy() {
      return this.consumedEnergy;
   }

   @Override
   public int getProcessingTicksSpent() {
      return this.processingTicksSpent;
   }

   public double getProgress() {
      return this.lockedRecipe != null && this.lockedRecipe.totalEnergy() > 0L
         ? Math.min(1.0, (double)this.consumedEnergy / (double)this.lockedRecipe.totalEnergy())
         : 0.0;
   }

   public void addConsumedEnergy(long amount) {
      if (this.addConsumedEnergyUnchecked(amount)) {
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public void incrementProcessingTicksSpent() {
      this.processingTicksSpent++;
      this.saveChanges();
   }

   @Override
   public void resetProgressState() {
      boolean changed = this.consumedEnergy != 0L || this.processingTicksSpent != 0;
      this.consumedEnergy = 0L;
      this.processingTicksSpent = 0;
      if (changed) {
         this.saveChanges();
         this.markForUpdate();
      }
   }

   @Override
   public boolean hasLockedRecipe() {
      return this.lockedRecipe != null;
   }

   @Override
   public Optional<LightningAssemblyLockedRecipe> getLockedRecipe() {
      return Optional.ofNullable(this.lockedRecipe);
   }

   @Override
   public Optional<LightningAssemblyLockedRecipe> lockCurrentRecipe() {
      if (this.lockedRecipe != null) {
         return Optional.of(this.lockedRecipe);
      } else {
         Optional<LightningAssemblyRecipeCandidate> candidate = this.findProcessableRecipe();
         if (candidate.isEmpty()) {
            return Optional.empty();
         } else {
            this.lockedRecipe = LightningAssemblyLockedRecipe.fromCandidate(candidate.get());
            this.saveChanges();
            return Optional.of(this.lockedRecipe);
         }
      }
   }

   public void clearLockedRecipe() {
      if (this.lockedRecipe != null) {
         this.lockedRecipe = null;
         this.saveChanges();
      }
   }

   @Override
   public void abortProcessing() {
      this.clearLockedRecipe();
      this.resetProgressState();
      this.setWorking(false);
   }

   public boolean isAutoExportEnabled() {
      return this.autoExport;
   }

   public void setAutoExportEnabled(boolean autoExport) {
      if (this.autoExport != autoExport) {
         this.autoExport = autoExport;
         this.saveChanges();
         this.logic.onStateChanged();
      }
   }

   public EnumSet<RelativeSide> getAllowedOutputs() {
      return this.allowedOutputs.isEmpty() ? EnumSet.noneOf(RelativeSide.class) : EnumSet.copyOf(this.allowedOutputs);
   }

   public void updateOutputSides(EnumSet<RelativeSide> allowedOutputs) {
      this.allowedOutputs = allowedOutputs.isEmpty() ? EnumSet.noneOf(RelativeSide.class) : EnumSet.copyOf(allowedOutputs);
      this.invalidateExportTargets();
      this.saveChanges();
      this.logic.onStateChanged();
   }

   @Override
   public boolean hasAutoExportWork() {
      return !this.allowedOutputs.isEmpty() && AdjacentItemAutoExportHelper.hasAnyOutput(this.autoExport, 10, 1, this.inventory::getStackInSlot);
   }

   @Override
   public boolean pushOutResult() {
      return !this.allowedOutputs.isEmpty() && this.hasAutoExportWork() && this.f_58857_ instanceof ServerLevel serverLevel
         ? AdjacentItemAutoExportHelper.pushOutResult(
            this,
            this.getOrientation(),
            this.allowedOutputs,
            10,
            1,
            this.inventory::getStackInSlot,
            (slot, amount) -> this.inventory.extractItem(slot, amount, false),
            remainder -> {
               ItemStack leftover = this.inventory.insertRecipeOutput(remainder, false);
               if (!leftover.m_41619_() && this.f_58857_ != null) {
                  NativeStackDropHelper.popResource(this.f_58857_, this.f_58858_, leftover);
               }
            },
            direction -> this.getExportTarget(serverLevel, direction)
         )
         : false;
   }

   public void onNeighborChanged(BlockPos changedPos) {
      if (changedPos != null && this.f_58858_.m_123333_(changedPos) == 1) {
         this.invalidateExportTargets();
      }
   }

   public long getAvailableHighVoltage() {
      return this.simulateLightningExtract(LightningKey.HIGH_VOLTAGE, Long.MAX_VALUE);
   }

   public long getAvailableExtremeHighVoltage() {
      return this.simulateLightningExtract(LightningKey.EXTREME_HIGH_VOLTAGE, Long.MAX_VALUE);
   }

   public boolean hasLightningCollapseMatrix() {
      return this.inventory.hasLightningCollapseMatrix();
   }

   public boolean completeLockedRecipe(LightningAssemblyLockedRecipe lockedRecipe, LightningAssemblyRecipeCandidate candidate) {
      boolean completed = SingleOutputLightningRecipeExecutor.complete(
         0,
         8,
         candidate.match()::getConsumptionForSlot,
         candidate.recipe().getResultStack(),
         () -> LightningAssemblyRecipeService.resolveLightningConsumption(
                  this.inventory,
                  lockedRecipe.lightningTier(),
                  lockedRecipe.lightningCost(),
                  this.getAvailableHighVoltage(),
                  this.getAvailableExtremeHighVoltage()
               )
               .map(plan -> new SingleOutputLightningRecipeExecutor.LightningPlan(plan.key(), plan.amount())),
         new SingleOutputLightningRecipeExecutor.InventoryAdapter() {
            @Override
            public boolean canAcceptOutput(ItemStack result) {
               return LightningAssemblyChamberBlockEntity.this.inventory.canAcceptRecipeOutput(result);
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
               return LightningAssemblyChamberBlockEntity.this.inventory.getStackInSlot(slot);
            }

            @Override
            public ItemStack extractItem(int slot, int amount) {
               return LightningAssemblyChamberBlockEntity.this.inventory.extractItem(slot, amount, false);
            }

            @Override
            public ItemStack insertOutput(ItemStack stack) {
               return LightningAssemblyChamberBlockEntity.this.inventory.insertRecipeOutput(stack, false);
            }

            @Override
            public void insertInput(int slot, ItemStack stack) {
               LightningAssemblyChamberBlockEntity.this.inventory.insertItem(slot, stack, false);
            }
         },
         new SingleOutputLightningRecipeExecutor.LightningAdapter() {
            @Override
            public long simulateExtract(LightningKey key, long amount) {
               return LightningAssemblyChamberBlockEntity.this.simulateLightningExtract(key, amount);
            }

            @Override
            public long extract(LightningKey key, long amount) {
               return LightningAssemblyChamberBlockEntity.this.extractLightning(key, amount);
            }

            @Override
            public long insert(LightningKey key, long amount) {
               return LightningAssemblyChamberBlockEntity.this.insertLightning(key, amount);
            }
         }
      );
      if (!completed) {
         return false;
      } else {
         this.clearLockedRecipe();
         this.resetProgressState();
         this.pushOutResult();
         return true;
      }
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(LightningAssemblyChamberMenu.TYPE, player, locator);
   }

   public IUpgradeInventory getUpgrades() {
      return this.upgrades;
   }

   public boolean isWorking() {
      return this.working;
   }

   public boolean isPowered() {
      return this.powered;
   }

   public ItemStack getClientRecipeResult() {
      return this.clientRecipeResult;
   }

   @Override
   public void setWorking(boolean working) {
      boolean changed = this.working != working;
      this.working = working;
      if (this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         if (state.m_60713_((Block)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get())
            && this.f_58857_.m_7702_(this.f_58858_) == this
            && state.m_61138_(LightningAssemblyChamberBlock.WORKING)
            && (Boolean)state.m_61143_(LightningAssemblyChamberBlock.WORKING) != working) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(LightningAssemblyChamberBlock.WORKING, working), 3);
         } else if (changed) {
            this.markForUpdate();
         }
      }
   }

   public void onMainNodeStateChanged(State reason) {
      this.frequencyBinding.onMainNodeStateChanged(reason);
      if (!this.removing && reason != State.GRID_BOOT) {
         this.refreshPoweredState();
      }
   }

   private void refreshPoweredState() {
      boolean newState = false;
      IGrid grid = this.getMainNode().getGrid();
      if (grid != null
         && this.getMainNode().isPowered()
         && grid.getEnergyService().extractAEPower(1.0, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 1.0E-4
         && this.energyStorage.getStoredEnergyLong() > 0L) {
         newState = true;
      }

      if (newState != this.powered) {
         this.powered = newState;
         this.markForUpdate();
      }
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.setWorking(this.hasLockedRecipe());
   }

   public void m_7651_() {
      this.removing = true;
      this.frequencyBinding.setRemoved();
      super.m_7651_();
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      super.onChunkUnloaded();
   }

   public void m_6339_() {
      super.m_6339_();
      this.removing = false;
      this.frequencyBinding.clearRemoved();
   }

   private void onInventoryChanged() {
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private void onEnergyChanged() {
      this.saveChanges();
      this.logic.onStateChanged();
      this.refreshPoweredState();
   }

   private void onUpgradesChanged() {
      this.saveChanges();
      this.logic.onStateChanged();
   }

   private void configureLightningWatcher(IStackWatcher watcher) {
      watcher.reset();
      watcher.add(LightningKey.HIGH_VOLTAGE);
      watcher.add(LightningKey.EXTREME_HIGH_VOLTAGE);
   }

   private void onLightningStackChanged(AEKey what) {
      if (LightningKey.HIGH_VOLTAGE.equals(what) || LightningKey.EXTREME_HIGH_VOLTAGE.equals(what)) {
         this.logic.onStateChanged();
      }
   }

   private CompositeStorage getExportTarget(ServerLevel level, Direction direction) {
      return this.exportTargetCache.resolve(level, this.f_58858_, direction);
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      this.inventory.saveToTag(data, "Inventory");
      this.upgrades.writeToNBT(data, "Upgrades");
      data.m_128356_("Energy", this.energyStorage.getStoredEnergyLong());
      data.m_128356_("ConsumedEnergy", this.consumedEnergy);
      data.m_128405_("ProcessingTicks", this.processingTicksSpent);
      data.m_128379_("AutoExport", this.autoExport);
      ListTag outputTags = new ListTag();

      for (RelativeSide side : this.allowedOutputs) {
         outputTags.add(StringTag.m_129297_(side.name()));
      }

      data.m_128365_("AllowedOutputs", outputTags);
      if (this.lockedRecipe != null) {
         data.m_128365_("LockedRecipe", this.lockedRecipe.toTag());
      } else {
         data.m_128473_("LockedRecipe");
      }

      this.frequencyBinding.save(data);
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      this.inventory.loadFromTag(data, "Inventory");
      this.upgrades.readFromNBT(data, "Upgrades");
      this.energyStorage.loadStoredEnergy(data.m_128454_("Energy"));
      this.consumedEnergy = Math.max(0L, data.m_128454_("ConsumedEnergy"));
      this.processingTicksSpent = Math.max(0, data.m_128451_("ProcessingTicks"));
      this.frequencyBinding.load(data);
      this.autoExport = data.m_128471_("AutoExport");
      this.allowedOutputs.clear();
      ListTag outputTags = data.m_128437_("AllowedOutputs", 8);

      for (int i = 0; i < outputTags.size(); i++) {
         try {
            this.allowedOutputs.add(RelativeSide.valueOf(outputTags.m_128778_(i)));
         } catch (IllegalArgumentException var5) {
         }
      }

      if (data.m_128425_("LockedRecipe", 10)) {
         this.lockedRecipe = LightningAssemblyLockedRecipe.fromTag(data.m_128469_("LockedRecipe"));
      } else {
         this.lockedRecipe = null;
      }

      if (this.lockedRecipe == null) {
         this.consumedEnergy = 0L;
         this.processingTicksSpent = 0;
      } else {
         this.consumedEnergy = Math.min(this.consumedEnergy, this.lockedRecipe.totalEnergy());
      }

      this.working = this.lockedRecipe != null;
      this.invalidateExportTargets();
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);

      for (int slot = 0; slot <= 8; slot++) {
         LargeStackStreamCodecs.writeItemStack(data, this.inventory.getStackInSlot(slot));
      }

      LargeStackStreamCodecs.writeItemStack(data, this.lockedRecipe != null ? this.lockedRecipe.result() : ItemStack.f_41583_);
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);

      for (int slot = 0; slot <= 8; slot++) {
         ItemStack oldStack = this.inventory.getStackInSlot(slot);
         ItemStack newStack = LargeStackStreamCodecs.readItemStack(data);
         if (!ItemStack.m_41728_(oldStack, newStack)) {
            this.inventory.setClientRenderStack(slot, newStack);
            changed = true;
         }
      }

      ItemStack newResult = LargeStackStreamCodecs.readItemStack(data);
      if (!ItemStack.m_41728_(this.clientRecipeResult, newResult)) {
         this.clientRecipeResult = newResult;
         changed = true;
      }

      return changed;
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);

      for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
         ItemStack stack = this.inventory.getStackInSlot(slot);
         if (!stack.m_41619_()) {
            NativeStackDropHelper.addDrops(drops, stack);
         }
      }

      for (ItemStack upgrade : this.upgrades) {
         if (!upgrade.m_41619_()) {
            NativeStackDropHelper.addDrops(drops, upgrade);
         }
      }
   }

   public void m_6211_() {
      super.m_6211_();
      this.inventory.clear();
      this.upgrades.clear();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      MemoryCardConfigSupport.exportAutoExportSettings(mode, output, this.autoExport, this.allowedOutputs, tag -> {
         FrequencyBindingHelper.writeMemoryFrequency(tag, this.getFrequencyId());
         MemoryCardConfigSupport.writeMatrixCount(tag, this);
      });
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      MemoryCardConfigSupport.importAutoExportSettings(mode, input, v -> this.autoExport = v, sides -> this.allowedOutputs = sides, tag -> {
         FrequencyBindingHelper.importMemoryFrequency(tag, this::setFrequency);
         MemoryCardConfigSupport.restoreMatrixCount(tag, player, this);
      }, () -> {
         this.invalidateExportTargets();
         this.saveChanges();
         this.markForUpdate();
      });
   }

   protected Item getItemFromBlockEntity() {
      return ((LightningAssemblyChamberBlock)ModBlocks.LIGHTNING_ASSEMBLY_CHAMBER.get()).m_5456_();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.allOf(Direction.class);
   }

   private void invalidateExportTargets() {
      this.exportTargetCache.invalidate();
   }

   @Override
   public long getMachineStoredEnergy() {
      return this.energyStorage.getStoredEnergyLong();
   }

   @Override
   public IEnergyStorage getMachineEnergyStorage() {
      return this.energyStorage;
   }

   @Override
   public int extractMachineEnergy(long amount) {
      return this.energyStorage.extractInternal(amount, false);
   }

   @Override
   public void onEnergyConsumed(int consumed) {
      if (consumed > 0) {
         this.addConsumedEnergyUnchecked((long)consumed);
         this.processingTicksSpent++;
         this.saveChanges();
         this.markForUpdate();
      }
   }

   private boolean addConsumedEnergyUnchecked(long amount) {
      if (amount <= 0L) {
         return false;
      } else {
         if (amount > Long.MAX_VALUE - this.consumedEnergy) {
            this.consumedEnergy = Long.MAX_VALUE;
         } else {
            this.consumedEnergy += amount;
         }

         return true;
      }
   }

   private long simulateLightningExtract(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().extract(key, amount, Actionable.SIMULATE, IActionSource.ofMachine(this));
      }
   }

   private long extractLightning(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().extract(key, amount, Actionable.MODULATE, IActionSource.ofMachine(this));
      }
   }

   private long insertLightning(LightningKey key, long amount) {
      if (amount <= 0L) {
         return 0L;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().insert(key, amount, Actionable.MODULATE, IActionSource.ofMachine(this));
      }
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
