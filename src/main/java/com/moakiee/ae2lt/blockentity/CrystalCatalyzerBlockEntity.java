package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionHost;
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
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.block.CrystalCatalyzerBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.logic.AdjacentItemAutoExportHelper;
import com.moakiee.ae2lt.logic.FluidStackHelper;
import com.moakiee.ae2lt.logic.FluidTankInteractionHelper;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.machine.common.GridRecipeMachineHost;
import com.moakiee.ae2lt.machine.common.LightningCollapseMatrixHost;
import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerAutomationInventory;
import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerFluidHandler;
import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerInventory;
import com.moakiee.ae2lt.machine.crystalcatalyzer.CrystalCatalyzerLogic;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerLockedRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipe;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipeCandidate;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.CrystalCatalyzerRecipeService;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
import com.moakiee.ae2lt.machine.overloadfactory.NotifyingFluidTank;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryEnergyStorage;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.menu.CrystalCatalyzerMenu;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public class CrystalCatalyzerBlockEntity
   extends AENetworkBlockEntity
   implements IActionHost,
   IUpgradeableObject,
   FrequencyBindingHost,
   LightningCollapseMatrixHost,
   GridRecipeMachineHost<CrystalCatalyzerLockedRecipe, CrystalCatalyzerRecipeCandidate> {
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_TANK = "Tank";
   private static final String TAG_ENERGY = "Energy";
   private static final String TAG_CONSUMED_ENERGY = "ConsumedEnergy";
   private static final String TAG_PROCESSING_TICKS = "ProcessingTicks";
   private static final String TAG_LOCKED_RECIPE = "LockedRecipe";
   private static final String TAG_AUTO_EXPORT = "AutoExport";
   private static final String TAG_ALLOWED_OUTPUTS = "AllowedOutputs";
   private static final String TAG_MODE = "Mode";
   public static final int ENERGY_CAPACITY = 1000000;
   public static final int FLUID_TANK_CAPACITY_MB = 16000;
   public static final int MATRIX_OUTPUT_MULTIPLIER = 4;
   public static final int FIXED_FLUID_PER_CYCLE_MB = 1000;
   private Mode mode = Mode.CRYSTAL;
   private final CrystalCatalyzerInventory inventory = new CrystalCatalyzerInventory(this::onInventoryChanged, this::getMode);
   private final CrystalCatalyzerAutomationInventory automationInventory = new CrystalCatalyzerAutomationInventory(this.inventory);
   private final NotifyingFluidTank tank = new NotifyingFluidTank(16000, this::onTankChanged);
   private final CrystalCatalyzerFluidHandler fluidHandler = new CrystalCatalyzerFluidHandler(this.tank);
   private final OverloadProcessingFactoryEnergyStorage energyStorage = new OverloadProcessingFactoryEnergyStorage(1000000L, this::onEnergyChanged);
   private final IUpgradeInventory upgrades = UpgradeInventories.forMachine((ItemLike)ModBlocks.CRYSTAL_CATALYZER.get(), 0, this::onUpgradesChanged);
   private final CrystalCatalyzerLogic logic;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private CrystalCatalyzerLockedRecipe lockedRecipe;
   private long consumedEnergy;
   private int processingTicksSpent;
   private boolean autoExport;
   private EnumSet<RelativeSide> allowedOutputs = EnumSet.noneOf(RelativeSide.class);
   private final AdjacentItemAutoExportHelper.DirectionalTargetCache exportTargetCache = new AdjacentItemAutoExportHelper.DirectionalTargetCache();

   public static FluidStack getFixedFluidPerCycle() {
      return new FluidStack(Fluids.f_76193_, 1000);
   }

   public CrystalCatalyzerBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.CRYSTAL_CATALYZER.get(), pos, blockState);
      this.logic = new CrystalCatalyzerLogic(this);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this.logic).addService(IStorageWatcherNode.class, new IStorageWatcherNode() {
         public void updateWatcher(IStackWatcher newWatcher) {
            CrystalCatalyzerBlockEntity.this.configureLightningWatcher(newWatcher);
         }

         public void onStackChange(AEKey what, long amount) {
            CrystalCatalyzerBlockEntity.this.onLightningStackChanged(what);
         }
      });
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalCatalyzerBlockEntity be) {
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

   public CrystalCatalyzerInventory getInventory() {
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

   public NotifyingFluidTank getTank() {
      return this.tank;
   }

   public FluidStack getFluid() {
      return this.tank.getFluid().copy();
   }

   public boolean tryInsertFluidFromCarried(Player player) {
      boolean changed = FluidTankInteractionHelper.insertFromCarried(player, this.tank);
      if (changed) {
         this.saveChanges();
      }

      return changed;
   }

   public boolean tryExtractFluidToCarried(Player player) {
      boolean changed = FluidTankInteractionHelper.extractToCarried(player, this.tank);
      if (changed) {
         this.saveChanges();
      }

      return changed;
   }

   public void clearFluidTank() {
      if (!this.tank.getFluid().isEmpty()) {
         FluidTankInteractionHelper.clear(this.tank);
         this.saveChanges();
      }
   }

   public IFluidHandler getFluidHandlerCapability(Direction side) {
      return this.fluidHandler;
   }

   public IEnergyStorage getEnergyStorageCapability(Direction side) {
      return this.energyStorage;
   }

   public OverloadProcessingFactoryEnergyStorage getEnergyStorage() {
      return this.energyStorage;
   }

   public CrystalCatalyzerLogic getLogic() {
      return this.logic;
   }

   public IUpgradeInventory getUpgrades() {
      return this.upgrades;
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(CrystalCatalyzerMenu.TYPE, player, locator);
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.inventory.setLevel(this.f_58857_);
      this.setWorking(this.lockedRecipe != null);
   }

   public void m_6339_() {
      super.m_6339_();
      this.inventory.setLevel(this.f_58857_);
      this.frequencyBinding.clearRemoved();
   }

   public void m_7651_() {
      this.frequencyBinding.setRemoved();
      super.m_7651_();
      this.inventory.setLevel(null);
   }

   public void onChunkUnloaded() {
      this.frequencyBinding.onChunkUnloaded();
      super.onChunkUnloaded();
   }

   public Optional<CrystalCatalyzerRecipeCandidate> findProcessableRecipe() {
      if (!this.hasEnoughFixedFluid()) {
         return Optional.empty();
      } else {
         Optional<CrystalCatalyzerRecipeCandidate> candidate = CrystalCatalyzerRecipeService.findRecipe(this.f_58857_, this.inventory, this.mode);
         if (candidate.isEmpty()) {
            return Optional.empty();
         } else {
            return this.canAcceptRecipeOutput(candidate.get()) ? candidate : Optional.empty();
         }
      }
   }

   public Mode getMode() {
      return this.mode;
   }

   public void cycleMode() {
      Mode previous = this.mode;
      this.mode = previous.next();
      this.abortProcessing();
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private boolean hasEnoughFixedFluid() {
      FluidStack required = getFixedFluidPerCycle();
      FluidStack current = this.tank.getFluid();
      return current.isEmpty() ? false : FluidStackHelper.sameFluidAndTag(current, required) && current.getAmount() >= required.getAmount();
   }

   private boolean canAcceptRecipeOutput(CrystalCatalyzerRecipeCandidate candidate) {
      return this.canAcceptRecipeOutput(candidate.recipe().getOutputTemplate(), this.getCurrentOutputMultiplier(candidate));
   }

   public boolean canAcceptLockedRecipeOutput(CrystalCatalyzerLockedRecipe lockedRecipe) {
      return !this.getLockedRecipeOutputStack(lockedRecipe).m_41619_();
   }

   public boolean canAdvanceLockedRecipe(CrystalCatalyzerLockedRecipe lockedRecipe) {
      LightningKey lightningKey = LightningKey.of(lockedRecipe.lightningTier());
      long lightningCost = (long)lockedRecipe.lightningCost();
      return this.hasEnoughFixedFluid() && this.simulateLightningExtract(lightningKey, lightningCost) >= lightningCost;
   }

   private boolean canAcceptRecipeOutput(ItemStack template, int multiplier) {
      long outputCount = (long)template.m_41613_() * (long)multiplier;
      return outputCount > 0L && outputCount <= 2147483647L ? this.inventory.canAcceptRecipeOutput(template.m_255036_((int)outputCount)) : false;
   }

   private ItemStack getLockedRecipeOutputStack(CrystalCatalyzerLockedRecipe lockedRecipe) {
      ItemStack template = lockedRecipe.output();
      long outputCount = (long)template.m_41613_() * (long)lockedRecipe.outputMultiplier();
      if (outputCount > 0L && outputCount <= 2147483647L) {
         ItemStack resultStack = template.m_255036_((int)outputCount);
         return this.inventory.canAcceptRecipeOutput(resultStack) ? resultStack : ItemStack.f_41583_;
      } else {
         return ItemStack.f_41583_;
      }
   }

   private int getCurrentOutputMultiplier() {
      return this.inventory.hasLightningCollapseMatrix() ? 4 : 1;
   }

   private int getCurrentOutputMultiplier(CrystalCatalyzerRecipeCandidate candidate) {
      int matrix = this.inventory.hasLightningCollapseMatrix() ? 4 : 1;
      int parallel = this.computeParallel(candidate);
      long multiplier = (long)Math.max(1, parallel) * (long)matrix;
      return (int)Math.min(multiplier, 2147483647L);
   }

   private int computeParallel(CrystalCatalyzerRecipeCandidate candidate) {
      if (candidate == null) {
         return 1;
      } else {
         CrystalCatalyzerRecipe recipe = candidate.recipe();
         int perInstance = recipe.catalystCount();
         if (perInstance <= 0) {
            return 1;
         } else {
            int amount = this.inventory.getStackInSlot(0).m_41613_();
            return Math.max(1, amount / perInstance);
         }
      }
   }

   @Override
   public boolean hasLockedRecipe() {
      return this.lockedRecipe != null;
   }

   @Override
   public Optional<CrystalCatalyzerLockedRecipe> getLockedRecipe() {
      return Optional.ofNullable(this.lockedRecipe);
   }

   @Override
   public Optional<CrystalCatalyzerLockedRecipe> lockCurrentRecipe() {
      if (this.lockedRecipe != null) {
         return Optional.of(this.lockedRecipe);
      } else {
         Optional<CrystalCatalyzerRecipeCandidate> candidate = this.findProcessableRecipe();
         if (candidate.isEmpty()) {
            return Optional.empty();
         } else {
            this.lockedRecipe = CrystalCatalyzerLockedRecipe.fromCandidate(candidate.get(), this.getCurrentOutputMultiplier(candidate.get()));
            this.saveChanges();
            return Optional.of(this.lockedRecipe);
         }
      }
   }

   public void clearLockedRecipe() {
      boolean hadLockedRecipe = this.lockedRecipe != null;
      boolean hadProgress = this.consumedEnergy != 0L || this.processingTicksSpent != 0;
      if (hadLockedRecipe || hadProgress) {
         this.lockedRecipe = null;
         this.resetProgressState();
         if (hadLockedRecipe && !hadProgress) {
            this.saveChanges();
         }
      }
   }

   @Override
   public void abortProcessing() {
      this.clearLockedRecipe();
      this.setWorking(false);
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

   private void addConsumedEnergy(long amount) {
      if (amount > 0L) {
         if (amount > Long.MAX_VALUE - this.consumedEnergy) {
            this.consumedEnergy = Long.MAX_VALUE;
         } else {
            this.consumedEnergy += amount;
         }
      }
   }

   private void incrementProcessingTicksSpent() {
      this.processingTicksSpent++;
   }

   @Override
   public void resetProgressState() {
      boolean changed = this.consumedEnergy != 0L || this.processingTicksSpent != 0;
      this.consumedEnergy = 0L;
      this.processingTicksSpent = 0;
      if (changed) {
         this.saveChanges();
      }
   }

   @Override
   public boolean pushOutResult() {
      return this.hasAutoExportWork() && this.f_58857_ instanceof ServerLevel serverLevel
         ? AdjacentItemAutoExportHelper.pushOutResult(
            this,
            this.getOrientation(),
            this.allowedOutputs,
            2,
            1,
            this.inventory::getStackInSlot,
            (slot, amount) -> this.inventory.extractItem(slot, amount, false),
            remainder -> {
               ItemStack leftover = this.inventory.insertRecipeOutput(remainder, false);
               if (!leftover.m_41619_() && this.f_58857_ != null) {
                  NativeStackDropHelper.popResource(this.f_58857_, this.f_58858_, leftover);
               }
            },
            direction -> this.exportTargetCache.resolve(serverLevel, this.f_58858_, direction)
         )
         : false;
   }

   @Override
   public boolean hasAutoExportWork() {
      return !this.allowedOutputs.isEmpty() && AdjacentItemAutoExportHelper.hasAnyOutput(this.autoExport, 2, 1, this.inventory::getStackInSlot);
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
      this.exportTargetCache.invalidate();
      this.saveChanges();
      this.logic.onStateChanged();
   }

   public void onNeighborChanged(BlockPos changedPos) {
      if (changedPos != null && this.f_58858_.m_123333_(changedPos) == 1) {
         this.exportTargetCache.invalidate();
      }
   }

   public long getAvailableHighVoltage() {
      return this.simulateLightningExtract(LightningKey.HIGH_VOLTAGE, Long.MAX_VALUE);
   }

   public long getAvailableExtremeHighVoltage() {
      return this.simulateLightningExtract(LightningKey.EXTREME_HIGH_VOLTAGE, Long.MAX_VALUE);
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

   protected void onOrientationChanged(BlockOrientation orientation) {
      super.onOrientationChanged(orientation);
      this.exportTargetCache.invalidate();
   }

   public boolean completeLockedRecipe(CrystalCatalyzerLockedRecipe lockedRecipe, CrystalCatalyzerRecipeCandidate candidate) {
      ItemStack resultStack = this.getLockedRecipeOutputStack(lockedRecipe);
      if (resultStack.m_41619_()) {
         return false;
      } else {
         FluidStack requiredFluid = getFixedFluidPerCycle();
         FluidStack currentFluid = this.tank.getFluid();
         if (!currentFluid.isEmpty() && FluidStackHelper.sameFluidAndTag(currentFluid, requiredFluid) && currentFluid.getAmount() >= requiredFluid.getAmount()) {
            LightningKey lightningKey = LightningKey.of(lockedRecipe.lightningTier());
            long lightningCost = (long)lockedRecipe.lightningCost();
            if (this.simulateLightningExtract(lightningKey, lightningCost) < lightningCost) {
               return false;
            } else {
               long extractedLightning = this.extractLightning(lightningKey, lightningCost);
               if (extractedLightning < lightningCost) {
                  if (extractedLightning > 0L) {
                     this.insertLightning(lightningKey, extractedLightning);
                  }

                  return false;
               } else {
                  FluidStack drained = this.tank.drain(requiredFluid, FluidAction.EXECUTE);
                  if (drained.getAmount() != requiredFluid.getAmount()) {
                     if (!drained.isEmpty()) {
                        this.tank.fill(drained, FluidAction.EXECUTE);
                     }

                     this.insertLightning(lightningKey, extractedLightning);
                     return false;
                  } else {
                     ItemStack leftover = this.inventory.insertRecipeOutput(resultStack, false);
                     if (!leftover.m_41619_()) {
                        this.tank.fill(drained, FluidAction.EXECUTE);
                        this.insertLightning(lightningKey, extractedLightning);
                        return false;
                     } else {
                        this.clearLockedRecipe();
                        this.pushOutResult();
                        return true;
                     }
                  }
               }
            }
         } else {
            return false;
         }
      }
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
      this.addConsumedEnergy((long)consumed);
      this.incrementProcessingTicksSpent();
      this.saveChanges();
   }

   public boolean isWorking() {
      BlockState state = this.m_58900_();
      return state.m_61138_(CrystalCatalyzerBlock.WORKING) && (Boolean)state.m_61143_(CrystalCatalyzerBlock.WORKING);
   }

   @Override
   public void setWorking(boolean working) {
      if (this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         if (state.m_60713_((Block)ModBlocks.CRYSTAL_CATALYZER.get())
            && this.f_58857_.m_7702_(this.f_58858_) == this
            && state.m_61138_(CrystalCatalyzerBlock.WORKING)
            && (Boolean)state.m_61143_(CrystalCatalyzerBlock.WORKING) != working) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(CrystalCatalyzerBlock.WORKING, working), 3);
         }
      }
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      this.inventory.saveToTag(data, "Inventory");
      data.m_128365_("Tank", this.tank.writeToNBT(new CompoundTag()));
      data.m_128356_("Energy", this.energyStorage.getStoredEnergyLong());
      data.m_128356_("ConsumedEnergy", this.consumedEnergy);
      data.m_128405_("ProcessingTicks", this.processingTicksSpent);
      data.m_128379_("AutoExport", this.autoExport);
      ListTag outputTags = new ListTag();

      for (RelativeSide side : this.allowedOutputs) {
         outputTags.add(StringTag.m_129297_(side.name()));
      }

      data.m_128365_("AllowedOutputs", outputTags);
      data.m_128359_("Mode", this.mode.m_7912_());
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
      this.tank.readFromNBT(data.m_128469_("Tank"));
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
         } catch (IllegalArgumentException var8) {
         }
      }

      if (data.m_128425_("Mode", 8)) {
         String modeName = data.m_128461_("Mode");
         this.mode = Mode.CRYSTAL;

         for (Mode m : Mode.values()) {
            if (m.m_7912_().equals(modeName)) {
               this.mode = m;
               break;
            }
         }
      } else {
         this.mode = Mode.CRYSTAL;
      }

      if (data.m_128425_("LockedRecipe", 10)) {
         this.lockedRecipe = CrystalCatalyzerLockedRecipe.fromTag(data.m_128469_("LockedRecipe"), this.getCurrentOutputMultiplier());
      } else {
         this.lockedRecipe = null;
      }

      if (this.lockedRecipe == null) {
         this.consumedEnergy = 0L;
         this.processingTicksSpent = 0;
      } else {
         this.consumedEnergy = Math.min(this.consumedEnergy, this.lockedRecipe.totalEnergy());
      }

      this.exportTargetCache.invalidate();
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);

      for (int slot = 0; slot <= 1; slot++) {
         LargeStackStreamCodecs.writeItemStack(data, this.inventory.getStackInSlot(slot));
      }
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);

      for (int slot = 0; slot <= 1; slot++) {
         ItemStack oldStack = this.inventory.getStackInSlot(slot);
         ItemStack newStack = LargeStackStreamCodecs.readItemStack(data);
         if (!ItemStack.m_41728_(oldStack, newStack)) {
            this.inventory.setClientRenderStack(slot, newStack);
            changed = true;
         }
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
   }

   public void m_6211_() {
      super.m_6211_();
      this.abortProcessing();
      this.inventory.clear();
      this.tank.setFluid(FluidStack.EMPTY);
      this.energyStorage.loadStoredEnergy(0L);
      this.autoExport = false;
      this.allowedOutputs.clear();
      this.exportTargetCache.invalidate();
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      MemoryCardConfigSupport.exportAutoExportSettings(mode, output, this.autoExport, this.allowedOutputs, tag -> {
         MemoryCardConfigSupport.writeEnum(tag, "Mode", this.mode);
         FrequencyBindingHelper.writeMemoryFrequency(tag, this.getFrequencyId());
         MemoryCardConfigSupport.writeMatrixCount(tag, this);
      });
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      MemoryCardConfigSupport.importAutoExportSettings(mode, input, v -> this.autoExport = v, sides -> this.allowedOutputs = sides, tag -> {
         Mode importedMode = MemoryCardConfigSupport.readEnum(tag, "Mode", Mode.class, this.mode);
         if (this.mode != importedMode) {
            this.mode = importedMode;
            this.abortProcessing();
         }

         FrequencyBindingHelper.importMemoryFrequency(tag, this::setFrequency);
         MemoryCardConfigSupport.restoreMatrixCount(tag, player, this);
      }, () -> {
         this.exportTargetCache.invalidate();
         this.saveChanges();
         this.markForUpdate();
      });
   }

   protected Item getItemFromBlockEntity() {
      return ((CrystalCatalyzerBlock)ModBlocks.CRYSTAL_CATALYZER.get()).m_5456_();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.allOf(Direction.class);
   }

   private void onInventoryChanged() {
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private void onTankChanged() {
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private void onEnergyChanged() {
      this.saveChanges();
      this.logic.onStateChanged();
   }

   private void onUpgradesChanged() {
      this.saveChanges();
      this.logic.onStateChanged();
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
      } else if (cap == ForgeCapabilities.FLUID_HANDLER) {
         return LazyOptional.of(() -> this.getFluidHandlerCapability(side)).cast();
      } else {
         return cap == ForgeCapabilities.ENERGY ? LazyOptional.of(() -> this.getEnergyStorageCapability(side)).cast() : super.getCapability(cap, side);
      }
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.SMART;
   }
}
