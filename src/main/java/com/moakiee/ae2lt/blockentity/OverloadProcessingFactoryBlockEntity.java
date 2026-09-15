package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
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
import com.moakiee.ae2lt.block.OverloadProcessingFactoryBlock;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.logic.AdjacentItemAutoExportHelper;
import com.moakiee.ae2lt.logic.FluidStackHelper;
import com.moakiee.ae2lt.logic.FluidTankInteractionHelper;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.machine.common.GridRecipeMachineHost;
import com.moakiee.ae2lt.machine.common.LightningCollapseMatrixHost;
import com.moakiee.ae2lt.machine.overloadfactory.NotifyingFluidTank;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryAutomationInventory;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryEnergyStorage;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryFluidHandler;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryLogic;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingLockedRecipe;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeCandidate;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeService;
import com.moakiee.ae2lt.me.GridLightningEnergyHandler;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public class OverloadProcessingFactoryBlockEntity
   extends AENetworkBlockEntity
   implements IUpgradeableObject,
   FrequencyBindingHost,
   LightningCollapseMatrixHost,
   GridRecipeMachineHost<OverloadProcessingLockedRecipe, OverloadProcessingRecipeCandidate> {
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_UPGRADES = "Upgrades";
   private static final String TAG_ENERGY = "Energy";
   private static final String TAG_INPUT_TANK = "InputTank";
   private static final String TAG_OUTPUT_TANK = "OutputTank";
   private static final String TAG_CONSUMED_ENERGY = "ConsumedEnergy";
   private static final String TAG_PROCESSING_TICKS = "ProcessingTicks";
   private static final String TAG_LOCKED_RECIPE = "LockedRecipe";
   private static final String TAG_AUTO_EXPORT = "AutoExport";
   private static final String TAG_ALLOWED_OUTPUTS = "AllowedOutputs";
   public static final int INPUT_TANK_CAPACITY = 1024000;
   public static final int OUTPUT_TANK_CAPACITY = 1024000;
   public static final int SPEED_CARD_SLOTS = 4;
   private final OverloadProcessingFactoryInventory inventory = new OverloadProcessingFactoryInventory(this::onInventoryChanged);
   private final OverloadProcessingFactoryAutomationInventory automationInventory = new OverloadProcessingFactoryAutomationInventory(this.inventory);
   private final NotifyingFluidTank inputTank = new NotifyingFluidTank(1024000, this::onTankChanged);
   private final NotifyingFluidTank outputTank = new NotifyingFluidTank(1024000, this::onTankChanged);
   private final OverloadProcessingFactoryFluidHandler fluidHandler = new OverloadProcessingFactoryFluidHandler(this.inputTank, this.outputTank);
   private final OverloadProcessingFactoryEnergyStorage energyStorage = new OverloadProcessingFactoryEnergyStorage(
      AE2LTCommonConfig.overloadFactoryEnergyCapacity(), this::onEnergyChanged
   );
   private final IUpgradeInventory upgrades = UpgradeInventories.forMachine((ItemLike)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get(), 4, this::onUpgradesChanged);
   private final OverloadProcessingFactoryLogic logic;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private OverloadProcessingLockedRecipe lockedRecipe;
   private long consumedEnergy;
   private int processingTicksSpent;
   private boolean working;
   private boolean autoExport;
   private EnumSet<RelativeSide> allowedOutputs = EnumSet.noneOf(RelativeSide.class);
   private final AdjacentItemAutoExportHelper.DirectionalTargetCache exportTargetCache = new AdjacentItemAutoExportHelper.DirectionalTargetCache();
   private long lastClientUpdateTick = Long.MIN_VALUE;

   public OverloadProcessingFactoryBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.OVERLOAD_PROCESSING_FACTORY.get(), pos, blockState);
      this.logic = new OverloadProcessingFactoryLogic(this);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this.logic);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, OverloadProcessingFactoryBlockEntity be) {
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

   public OverloadProcessingFactoryInventory getInventory() {
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

   public IFluidHandler getFluidHandlerCapability(Direction side) {
      return this.fluidHandler;
   }

   public IEnergyStorage getEnergyStorageCapability(Direction side) {
      return this.energyStorage;
   }

   public OverloadProcessingFactoryEnergyStorage getEnergyStorage() {
      return this.energyStorage;
   }

   public IUpgradeInventory getUpgrades() {
      return this.upgrades;
   }

   public FluidStack getInputFluid() {
      return this.inputTank.getFluid().copy();
   }

   public FluidStack getOutputFluid() {
      return this.outputTank.getFluid().copy();
   }

   public boolean tryInsertFluidFromCarried(Player player, int tankIndex) {
      NotifyingFluidTank target = this.resolveTankByIndex(tankIndex);
      if (target == null) {
         return false;
      } else {
         boolean changed = FluidTankInteractionHelper.insertFromCarried(player, target);
         if (changed) {
            this.saveChanges();
         }

         return changed;
      }
   }

   public boolean tryExtractFluidToCarried(Player player, int tankIndex) {
      NotifyingFluidTank target = this.resolveTankByIndex(tankIndex);
      if (target == null) {
         return false;
      } else {
         boolean changed = FluidTankInteractionHelper.extractToCarried(player, target);
         if (changed) {
            this.saveChanges();
         }

         return changed;
      }
   }

   public void clearFluidTank(int tankIndex) {
      NotifyingFluidTank target = this.resolveTankByIndex(tankIndex);
      if (target != null && !target.getFluid().isEmpty()) {
         FluidTankInteractionHelper.clear(target);
         this.saveChanges();
      }
   }

   private NotifyingFluidTank resolveTankByIndex(int tankIndex) {
      return switch (tankIndex) {
         case 0 -> this.inputTank;
         case 1 -> this.outputTank;
         default -> null;
      };
   }

   @Override
   public int getInstalledMatrixCount() {
      return this.inventory.getInstalledMatrixCount();
   }

   public int getInstalledParallelCapacity() {
      return this.inventory.getInstalledParallelCapacity();
   }

   public Optional<OverloadProcessingRecipeCandidate> findProcessableRecipe() {
      return OverloadProcessingRecipeService.findFirstProcessable(
         this.m_58904_(), this.inventory, this.getInputFluid(), this.getOutputFluid(), this.getAvailableHighVoltage(), this.getAvailableExtremeHighVoltage()
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
         this.requestClientUpdate();
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
         this.requestClientUpdate();
      }
   }

   @Override
   public boolean hasLockedRecipe() {
      return this.lockedRecipe != null;
   }

   @Override
   public Optional<OverloadProcessingLockedRecipe> getLockedRecipe() {
      return Optional.ofNullable(this.lockedRecipe);
   }

   @Override
   public Optional<OverloadProcessingLockedRecipe> lockCurrentRecipe() {
      if (this.lockedRecipe != null) {
         return Optional.of(this.lockedRecipe);
      } else {
         Optional<OverloadProcessingRecipeCandidate> candidate = this.findProcessableRecipe();
         if (candidate.isEmpty()) {
            return Optional.empty();
         } else {
            this.lockedRecipe = OverloadProcessingLockedRecipe.fromCandidate(candidate.get());
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
      if (this.allowedOutputs.isEmpty() || !this.autoExport) {
         return false;
      } else {
         return !this.outputTank.getFluid().isEmpty()
            ? true
            : AdjacentItemAutoExportHelper.hasAnyOutput(this.autoExport, 10, 1, this.inventory::getStackInSlot);
      }
   }

   @Override
   public boolean pushOutResult() {
      if (this.hasAutoExportWork() && this.f_58857_ instanceof ServerLevel serverLevel) {
         boolean hasItemOutput = AdjacentItemAutoExportHelper.hasAnyOutput(this.autoExport, 10, 1, this.inventory::getStackInSlot);
         if (!hasItemOutput && this.outputTank.getFluid().isEmpty()) {
            return false;
         } else {
            boolean pushedItem = hasItemOutput
               && AdjacentItemAutoExportHelper.pushOutResult(
                  this,
                  this.getOrientation(),
                  this.allowedOutputs,
                  10,
                  1,
                  this.inventory::getStackInSlot,
                  (slot, amount) -> this.inventory.extractItem(slot, amount, false),
                  remainder -> {
                     if (!this.inventory.insertRecipeOutputs(List.of(remainder)) && !remainder.m_41619_() && this.f_58857_ != null) {
                        NativeStackDropHelper.popResource(this.f_58857_, this.f_58858_, remainder);
                     }
                  },
                  direction -> this.getExportTarget(serverLevel, direction)
               );
            boolean pushedFluid = !this.outputTank.getFluid().isEmpty()
               && AdjacentItemAutoExportHelper.pushOutFluid(this, this.getOrientation(), this.allowedOutputs, this.outputTank::getFluid, amount -> {
                  FluidStack drained = this.outputTank.drain(amount, FluidAction.EXECUTE);
                  return drained.getAmount();
               }, direction -> this.getExportTarget(serverLevel, direction));
            return pushedItem || pushedFluid;
         }
      } else {
         return false;
      }
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

   public boolean completeLockedRecipe(OverloadProcessingLockedRecipe lockedRecipe, OverloadProcessingRecipeCandidate candidate) {
      if (candidate.parallel() != lockedRecipe.parallel()) {
         return false;
      } else if (!this.inventory.canAcceptRecipeOutputs(candidate.recipe().getScaledItemResults(candidate.parallel()))) {
         return false;
      } else {
         for (int slot = 0; slot <= 8; slot++) {
            int toConsume = candidate.match().getConsumptionForSlot(slot);
            if (toConsume > 0 && this.inventory.getStackInSlot(slot).m_41613_() < toConsume) {
               return false;
            }
         }

         FluidStack requiredInputFluid = candidate.recipe().fluidInput();
         int inputFluidCost = 0;
         if (!requiredInputFluid.isEmpty()) {
            long scaledInputFluidCost = (long)requiredInputFluid.getAmount() * (long)candidate.parallel();
            if (scaledInputFluidCost > 2147483647L) {
               return false;
            }

            inputFluidCost = (int)scaledInputFluidCost;
         }

         if (inputFluidCost > 0) {
            FluidStack currentInput = this.inputTank.getFluid();
            if (currentInput.isEmpty() || !FluidStackHelper.sameFluidAndTag(requiredInputFluid, currentInput) || currentInput.getAmount() < inputFluidCost) {
               return false;
            }
         }

         FluidStack scaledOutputFluid = candidate.recipe().getScaledFluidResult(candidate.parallel());
         if (!this.canAcceptFluidOutput(scaledOutputFluid)) {
            return false;
         } else {
            Optional<OverloadProcessingRecipeService.LightningConsumptionPlan> lightningPlan = OverloadProcessingRecipeService.resolveLightningConsumption(
               this.inventory,
               lockedRecipe.lightningTier(),
               lockedRecipe.totalLightningCost(),
               this.getAvailableHighVoltage(),
               this.getAvailableExtremeHighVoltage()
            );
            if (lightningPlan.isEmpty()) {
               return false;
            } else {
               OverloadProcessingRecipeService.LightningConsumptionPlan plan = lightningPlan.get();
               if (this.simulateLightningExtract(plan.primaryKey(), plan.primaryAmount()) < plan.primaryAmount()) {
                  return false;
               } else if (plan.hasSecondary() && this.simulateLightningExtract(plan.secondaryKey(), plan.secondaryAmount()) < plan.secondaryAmount()) {
                  return false;
               } else {
                  ItemStack[] extractedInputs = new ItemStack[9];

                  for (int slotx = 0; slotx <= 8; slotx++) {
                     int toConsume = candidate.match().getConsumptionForSlot(slotx);
                     if (toConsume > 0) {
                        ItemStack extracted = this.inventory.extractItem(slotx, toConsume, false);
                        if (extracted.m_41613_() != toConsume) {
                           this.rollbackInputs(extractedInputs);
                           return false;
                        }

                        extractedInputs[slotx] = extracted;
                     }
                  }

                  FluidStack drainedInput = inputFluidCost <= 0 ? FluidStack.EMPTY : this.inputTank.drain(inputFluidCost, FluidAction.EXECUTE);
                  if (inputFluidCost > 0 && drainedInput.getAmount() != inputFluidCost) {
                     this.rollbackInputs(extractedInputs);
                     if (!drainedInput.isEmpty()) {
                        this.inputTank.fill(drainedInput, FluidAction.EXECUTE);
                     }

                     return false;
                  } else {
                     long extractedPrimary = this.extractLightning(plan.primaryKey(), plan.primaryAmount());
                     if (extractedPrimary < plan.primaryAmount()) {
                        this.rollbackInputs(extractedInputs);
                        if (!drainedInput.isEmpty()) {
                           this.inputTank.fill(drainedInput, FluidAction.EXECUTE);
                        }

                        if (extractedPrimary > 0L) {
                           this.insertLightning(plan.primaryKey(), extractedPrimary);
                        }

                        return false;
                     } else {
                        long extractedSecondary = 0L;
                        if (plan.hasSecondary()) {
                           extractedSecondary = this.extractLightning(plan.secondaryKey(), plan.secondaryAmount());
                           if (extractedSecondary < plan.secondaryAmount()) {
                              this.insertLightning(plan.primaryKey(), extractedPrimary);
                              this.rollbackInputs(extractedInputs);
                              if (!drainedInput.isEmpty()) {
                                 this.inputTank.fill(drainedInput, FluidAction.EXECUTE);
                              }

                              if (extractedSecondary > 0L) {
                                 this.insertLightning(plan.secondaryKey(), extractedSecondary);
                              }

                              return false;
                           }
                        }

                        if (!this.inventory.insertRecipeOutputs(candidate.recipe().getScaledItemResults(candidate.parallel()))) {
                           this.insertLightning(plan.primaryKey(), extractedPrimary);
                           if (extractedSecondary > 0L) {
                              this.insertLightning(plan.secondaryKey(), extractedSecondary);
                           }

                           if (!drainedInput.isEmpty()) {
                              this.inputTank.fill(drainedInput, FluidAction.EXECUTE);
                           }

                           this.rollbackInputs(extractedInputs);
                           return false;
                        } else {
                           int filledFluid = scaledOutputFluid.isEmpty() ? 0 : this.outputTank.fill(scaledOutputFluid, FluidAction.EXECUTE);
                           if (!scaledOutputFluid.isEmpty() && filledFluid != scaledOutputFluid.getAmount()) {
                              if (filledFluid > 0) {
                                 this.outputTank.drain(filledFluid, FluidAction.EXECUTE);
                              }

                              this.insertLightning(plan.primaryKey(), extractedPrimary);
                              if (extractedSecondary > 0L) {
                                 this.insertLightning(plan.secondaryKey(), extractedSecondary);
                              }

                              if (!drainedInput.isEmpty()) {
                                 this.inputTank.fill(drainedInput, FluidAction.EXECUTE);
                              }

                              this.rollbackItemOutputs(candidate.recipe().getScaledItemResults(candidate.parallel()));
                              this.rollbackInputs(extractedInputs);
                              return false;
                           } else {
                              this.clearLockedRecipe();
                              this.resetProgressState();
                              this.pushOutResult();
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(OverloadProcessingFactoryMenu.TYPE, player, locator);
   }

   public boolean isWorking() {
      return this.working;
   }

   @Override
   public void setWorking(boolean working) {
      boolean changed = this.working != working;
      this.working = working;
      if (this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         if (state.m_60713_((Block)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get())
            && this.f_58857_.m_7702_(this.f_58858_) == this
            && state.m_61138_(OverloadProcessingFactoryBlock.WORKING)
            && (Boolean)state.m_61143_(OverloadProcessingFactoryBlock.WORKING) != working) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(OverloadProcessingFactoryBlock.WORKING, working), 3);
         } else if (changed) {
            this.requestClientUpdate();
         }
      }
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.setWorking(this.hasLockedRecipe());
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
      this.upgrades.writeToNBT(data, "Upgrades");
      data.m_128356_("Energy", this.energyStorage.getStoredEnergyLong());
      data.m_128365_("InputTank", this.inputTank.writeToNBT(new CompoundTag()));
      data.m_128365_("OutputTank", this.outputTank.writeToNBT(new CompoundTag()));
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
      this.inputTank.readFromNBT(data.m_128469_("InputTank"));
      this.outputTank.readFromNBT(data.m_128469_("OutputTank"));
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
         this.lockedRecipe = OverloadProcessingLockedRecipe.fromTag(data.m_128469_("LockedRecipe"));
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
      this.inputTank.setFluid(FluidStack.EMPTY);
      this.outputTank.setFluid(FluidStack.EMPTY);
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
      return ((OverloadProcessingFactoryBlock)ModBlocks.OVERLOAD_PROCESSING_FACTORY.get()).m_5456_();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.allOf(Direction.class);
   }

   protected void onOrientationChanged(BlockOrientation orientation) {
      super.onOrientationChanged(orientation);
      this.invalidateExportTargets();
   }

   private CompositeStorage getExportTarget(ServerLevel level, Direction direction) {
      return this.exportTargetCache.resolve(level, this.f_58858_, direction);
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
         this.requestClientUpdate();
      }
   }

   private boolean canAcceptFluidOutput(FluidStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         FluidStack current = this.outputTank.getFluid();
         return current.isEmpty()
            ? stack.getAmount() <= 1024000
            : FluidStackHelper.sameFluidAndTag(current, stack) && current.getAmount() + stack.getAmount() <= 1024000;
      }
   }

   private void rollbackInputs(ItemStack[] extractedInputs) {
      for (int slot = 0; slot <= 8; slot++) {
         ItemStack extracted = extractedInputs[slot];
         if (extracted != null && !extracted.m_41619_()) {
            this.inventory.insertItem(slot, extracted, false);
         }
      }
   }

   private void rollbackItemOutputs(List<ItemStack> outputs) {
      for (ItemStack output : outputs) {
         int remaining = output.m_41613_();

         for (int slot = 10; slot < 11 && remaining > 0; slot++) {
            ItemStack current = this.inventory.getStackInSlot(slot);
            if (!current.m_41619_() && ItemStack.m_150942_(current, output)) {
               int extracted = Math.min(remaining, current.m_41613_());
               this.inventory.extractItem(slot, extracted, false);
               remaining -= extracted;
            }
         }
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

   private void onInventoryChanged() {
      this.saveChanges();
      this.requestClientUpdate();
      this.logic.onStateChanged();
   }

   private void onTankChanged() {
      this.saveChanges();
      this.requestClientUpdate();
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

   private void requestClientUpdate() {
      if (this.f_58857_ == null) {
         this.markForUpdate();
      } else {
         long gameTime = this.f_58857_.m_46467_();
         if (this.lastClientUpdateTick != gameTime) {
            this.lastClientUpdateTick = gameTime;
            this.markForUpdate();
         }
      }
   }

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      if (cap == ForgeCapabilities.ITEM_HANDLER) {
         return LazyOptional.of(this::getAutomationInventory).cast();
      } else if (cap == ForgeCapabilities.FLUID_HANDLER) {
         return LazyOptional.of(() -> this.getFluidHandlerCapability(side)).cast();
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
