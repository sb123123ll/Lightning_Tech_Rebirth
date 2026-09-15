package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.block.AtmosphericIonizerBlock;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.item.WeatherCondensateItem;
import com.moakiee.ae2lt.logic.WeatherControlHelper;
import com.moakiee.ae2lt.logic.research.ResearchRitualService;
import com.moakiee.ae2lt.machine.atmosphericionizer.AtmosphericIonizerInventory;
import com.moakiee.ae2lt.machine.atmosphericionizer.AtmosphericIonizerLogic;
import com.moakiee.ae2lt.machine.atmosphericionizer.AtmosphericIonizerStatus;
import com.moakiee.ae2lt.machine.common.InsertOnlyAutomationInventory;
import com.moakiee.ae2lt.menu.AtmosphericIonizerMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class AtmosphericIonizerBlockEntity extends AENetworkBlockEntity implements IActionHost, FrequencyBindingHost {
   private static final Logger LOG = LogUtils.getLogger();
   public static final int PROCESS_TICKS = 100;
   private static final double POWER_EPSILON = 0.01;
   private static final int RITUAL_STRIKE_HEIGHT_OFFSET = 2;
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_CONSUMED_ENERGY = "ConsumedEnergy";
   private static final String TAG_PROCESSING_TICKS = "ProcessingTicks";
   private static final String TAG_LOCKED_TYPE = "LockedType";
   private final AtmosphericIonizerInventory inventory = new AtmosphericIonizerInventory(this::onInventoryChanged);
   private final IItemHandlerModifiable automationInventory = new InsertOnlyAutomationInventory(this.inventory);
   private final AtmosphericIonizerLogic logic;
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private WeatherCondensateItem.Type lockedType;
   private long consumedEnergy;
   private int processingTicksSpent;
   private boolean working;

   public AtmosphericIonizerBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.ATMOSPHERIC_IONIZER.get(), pos, blockState);
      this.logic = new AtmosphericIonizerLogic(this);
      this.getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this.logic);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, AtmosphericIonizerBlockEntity be) {
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

   public AtmosphericIonizerInventory getInventory() {
      return this.inventory;
   }

   public IItemHandlerModifiable getAutomationInventory() {
      return this.automationInventory;
   }

   public WeatherCondensateItem.Type getSelectedType() {
      return this.lockedType != null ? this.lockedType : WeatherCondensateItem.getType(this.getInstalledCondensate());
   }

   public boolean hasLockedType() {
      return this.lockedType != null;
   }

   public ItemStack getInstalledCondensate() {
      return this.inventory.getStackInSlot(0);
   }

   public boolean hasLocalStartPrerequisites() {
      return this.getSelectedType() != null;
   }

   public boolean hasLockedCondensateInput() {
      return this.lockedType != null && this.lockedType == WeatherCondensateItem.getType(this.getInstalledCondensate());
   }

   public boolean canOperateInCurrentDimension() {
      if (this.f_58857_ instanceof ServerLevel serverLevel && WeatherControlHelper.supportsWeather(serverLevel)) {
         return true;
      }

      return false;
   }

   public boolean isSelectedWeatherAlreadyActive() {
      WeatherCondensateItem.Type selectedType = this.getSelectedType();
      return selectedType != null && this.isWeatherAlreadyActive(selectedType);
   }

   public boolean isLockedWeatherAlreadyActive() {
      return this.lockedType != null && this.isWeatherAlreadyActive(this.lockedType);
   }

   public boolean hasEnoughEnergyForSelectedStart() {
      return this.canExtractAEPower(this.requiredEnergyForTick(this.getSelectedType(), 0, 0L));
   }

   public long getConsumedEnergy() {
      return this.consumedEnergy;
   }

   public long getTotalEnergy() {
      WeatherCondensateItem.Type type = this.getSelectedType();
      return type == null ? 0L : type.totalEnergy();
   }

   public long getRequiredEnergyForNextTick() {
      return this.requiredEnergyForTick(this.lockedType, this.processingTicksSpent, this.consumedEnergy);
   }

   public boolean canExtractAEPower(long amount) {
      return this.extractAEPower(amount, Actionable.SIMULATE);
   }

   public boolean tryExtractAEPower(long amount) {
      return this.extractAEPower(amount, Actionable.MODULATE);
   }

   public boolean isReadyToCommit() {
      return this.lockedType != null && this.processingTicksSpent >= 100 && this.consumedEnergy >= this.lockedType.totalEnergy();
   }

   public boolean lockSelectedCondensate() {
      WeatherCondensateItem.Type selectedType = WeatherCondensateItem.getType(this.getInstalledCondensate());
      if (selectedType == null) {
         return false;
      } else {
         this.lockedType = selectedType;
         this.saveChanges();
         this.markForUpdate();
         this.logic.onStateChanged();
         return true;
      }
   }

   public void advanceProgress(long amount) {
      if (this.lockedType != null && amount > 0L) {
         this.consumedEnergy = Math.min(this.lockedType.totalEnergy(), this.consumedEnergy + amount);
         this.processingTicksSpent = Math.min(100, this.processingTicksSpent + 1);
         this.saveChanges();
         this.markForUpdate();
      }
   }

   public AtmosphericIonizerStatus getStatus() {
      WeatherCondensateItem.Type selectedType = this.getSelectedType();
      if (selectedType == null) {
         return AtmosphericIonizerStatus.IDLE;
      } else if (!this.canOperateInCurrentDimension()) {
         return AtmosphericIonizerStatus.INVALID_DIMENSION;
      } else if ((this.lockedType == null || !this.isLockedWeatherAlreadyActive()) && (this.lockedType != null || !this.isSelectedWeatherAlreadyActive())) {
         if (this.lockedType == null) {
            return this.hasEnoughEnergyForSelectedStart() ? AtmosphericIonizerStatus.READY : AtmosphericIonizerStatus.WAITING_AE;
         } else if (!this.hasLockedCondensateInput()) {
            return AtmosphericIonizerStatus.WAITING_INPUT;
         } else if (this.isReadyToCommit()) {
            return AtmosphericIonizerStatus.READY;
         } else {
            return this.canExtractAEPower(this.getRequiredEnergyForNextTick()) ? AtmosphericIonizerStatus.CHARGING : AtmosphericIonizerStatus.WAITING_AE;
         }
      } else {
         return AtmosphericIonizerStatus.TARGET_ALREADY_ACTIVE;
      }
   }

   public boolean commitLockedCondensate() {
      if (!(this.f_58857_ instanceof ServerLevel serverLevel) || this.lockedType == null || !this.hasLockedCondensateInput() || !this.isInstalledInWorld()) {
         return false;
      }

      if (WeatherControlHelper.supportsWeather(serverLevel) && !this.lockedType.isActive(serverLevel)) {
         ItemStack extracted = this.inventory.extractItem(0, 1, false);
         if (extracted.m_41619_()) {
            return false;
         } else {
            WeatherCondensateItem.Type committedType = this.lockedType;
            if (!committedType.apply(serverLevel, serverLevel.f_46441_)) {
               LOG.debug("[ae2lt/ionizer] commit aborted: apply() failed for type={} at {}", committedType, this.f_58858_);
               this.inventory.insertItem(0, extracted, false);
               return false;
            } else {
               LOG.debug("[ae2lt/ionizer] commit OK: type={} at {}", committedType, this.f_58858_);
               if (committedType == WeatherCondensateItem.Type.THUNDERSTORM) {
                  this.summonRitualLightning(serverLevel);
               }

               this.lockedType = null;
               this.consumedEnergy = 0L;
               this.processingTicksSpent = 0;
               this.setWorking(false);
               this.saveChanges();
               this.markForUpdate();
               this.logic.onStateChanged();
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(AtmosphericIonizerMenu.TYPE, player, locator);
   }

   public boolean isWorking() {
      return this.working;
   }

   public boolean isInstalledInWorld() {
      if (this.f_58857_ != null && !this.m_58901_()) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         return state.m_60713_((Block)ModBlocks.ATMOSPHERIC_IONIZER.get())
            && state.m_61138_(AtmosphericIonizerBlock.HALF)
            && state.m_61143_(AtmosphericIonizerBlock.HALF) == DoubleBlockHalf.LOWER
            && this.f_58857_.m_7702_(this.f_58858_) == this;
      } else {
         return false;
      }
   }

   public void setWorking(boolean working) {
      boolean changed = this.working != working;
      this.working = working;
      if (this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         if (state.m_60713_((Block)ModBlocks.ATMOSPHERIC_IONIZER.get())
            && this.f_58857_.m_7702_(this.f_58858_) == this
            && state.m_61138_(AtmosphericIonizerBlock.WORKING)
            && (Boolean)state.m_61143_(AtmosphericIonizerBlock.WORKING) != working) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(AtmosphericIonizerBlock.WORKING, working), 3);
         } else if (changed) {
            this.markForUpdate();
         }
      }
   }

   public void cancelProcessingForRemoval() {
      this.lockedType = null;
      this.consumedEnergy = 0L;
      this.processingTicksSpent = 0;
      this.working = false;
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.setWorking(this.lockedType != null);
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

   private void summonRitualLightning(ServerLevel serverLevel) {
      LightningBolt bolt = (LightningBolt)EntityType.f_20465_.m_20615_(serverLevel);
      if (bolt == null) {
         LOG.warn("[ae2lt/ionizer] summonRitualLightning: EntityType.LIGHTNING_BOLT.create returned null at {}", this.f_58858_);
      } else {
         BlockPos strikePos = this.f_58858_.m_6630_(2);
         bolt.m_20219_(Vec3.m_82539_(strikePos));
         bolt.m_20874_(false);
         ResearchRitualService.markRitualLightning(bolt, this.f_58858_);
         serverLevel.m_7967_(bolt);
         LOG.info("[ae2lt/ionizer] thunderstorm nucleation -> spawn lightning at {} (ionizer={})", strikePos, this.f_58858_);
      }
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      this.inventory.saveToTag(data, "Inventory");
      data.m_128356_("ConsumedEnergy", this.consumedEnergy);
      data.m_128405_("ProcessingTicks", this.processingTicksSpent);
      if (this.lockedType != null) {
         data.m_128359_("LockedType", this.lockedType.m_7912_());
      } else {
         data.m_128473_("LockedType");
      }

      this.frequencyBinding.save(data);
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      this.inventory.loadFromTag(data, "Inventory");
      this.lockedType = data.m_128441_("LockedType") ? WeatherCondensateItem.Type.fromName(data.m_128461_("LockedType")) : null;
      this.consumedEnergy = Math.max(0L, data.m_128454_("ConsumedEnergy"));
      this.processingTicksSpent = Math.max(0, data.m_128451_("ProcessingTicks"));
      this.frequencyBinding.load(data);
      if (this.lockedType == null) {
         this.consumedEnergy = 0L;
         this.processingTicksSpent = 0;
      } else {
         this.consumedEnergy = Math.min(this.consumedEnergy, this.lockedType.totalEnergy());
         this.processingTicksSpent = Math.min(this.processingTicksSpent, 100);
      }

      this.working = this.lockedType != null;
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);
      ItemStack condensate = this.getInstalledCondensate();
      if (!condensate.m_41619_()) {
         drops.add(condensate.m_41777_());
      }
   }

   public void m_6211_() {
      super.m_6211_();
      this.inventory.clear();
   }

   public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
      super.exportSettings(mode, output, player);
      FrequencyBindingHelper.exportMemorySettings(mode, output, this.getFrequencyId());
   }

   public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
      super.importSettings(mode, input, player);
      FrequencyBindingHelper.importMemorySettings(mode, input, this::setFrequency);
   }

   protected Item getItemFromBlockEntity() {
      return ((AtmosphericIonizerBlock)ModBlocks.ATMOSPHERIC_IONIZER.get()).m_5456_();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.of(Direction.DOWN);
   }

   private boolean isWeatherAlreadyActive(WeatherCondensateItem.Type type) {
      if (this.f_58857_ instanceof ServerLevel serverLevel && type.isActive(serverLevel)) {
         return true;
      }

      return false;
   }

   private long requiredEnergyForTick(WeatherCondensateItem.Type type, int ticksSpent, long consumed) {
      if (type != null && ticksSpent < 100) {
         long remainingEnergy = Math.max(0L, type.totalEnergy() - consumed);
         int remainingTicks = Math.max(1, 100 - ticksSpent);
         return (remainingEnergy + (long)remainingTicks - 1L) / (long)remainingTicks;
      } else {
         return 0L;
      }
   }

   private void onInventoryChanged() {
      this.saveChanges();
      this.markForUpdate();
      this.logic.onStateChanged();
   }

   private boolean extractAEPower(long amount, Actionable mode) {
      if (amount <= 0L) {
         return true;
      } else {
         IGrid grid = this.getMainNode().getGrid();
         if (grid == null) {
            return false;
         } else {
            double extracted = grid.getEnergyService().extractAEPower((double)amount, mode, PowerMultiplier.CONFIG);
            return extracted >= (double)amount - 0.01;
         }
      }
   }

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      return cap == ForgeCapabilities.ITEM_HANDLER ? LazyOptional.of(this::getAutomationInventory).cast() : super.getCapability(cap, side);
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.SMART;
   }
}
