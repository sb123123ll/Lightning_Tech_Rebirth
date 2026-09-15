package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.AE2LTCapabilities;
import com.moakiee.ae2lt.api.event.LightningCollectedEvent;
import com.moakiee.ae2lt.block.LightningCollectorBlock;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencyBindingHost;
import com.moakiee.ae2lt.item.ElectroChimeCrystalItem;
import com.moakiee.ae2lt.machine.lightningcollector.LightningCollectorInventory;
import com.moakiee.ae2lt.me.GridLightningEnergyHandler;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.menu.LightningCollectorMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import com.mojang.logging.LogUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class LightningCollectorBlockEntity extends AENetworkBlockEntity implements IActionHost, FrequencyBindingHost {
   private static final Logger LOG = LogUtils.getLogger();
   private static final String TAG_INVENTORY = "Inventory";
   private static final String TAG_COOLDOWN = "CooldownTicks";
   private static final String TAG_WORKING_TICKS = "WorkingTicks";
   private static final int WORKING_DURATION_TICKS = 20;
   private static final long NATURAL_CULTIVATION_DEBOUNCE_TICKS = 20L;
   private static boolean warnedInvalidHighVoltageBaseRange;
   private static boolean warnedInvalidExtremeVoltageBaseRange;
   private final LightningCollectorInventory inventory = new LightningCollectorInventory(this::onInventoryChanged);
   private final FrequencyBindingHelper frequencyBinding = new FrequencyBindingHelper(this);
   private int cooldownTicks;
   private int workingTicks;
   private long lastCaptureGameTime = Long.MIN_VALUE;
   private long lastNaturalCultivationGameTime = Long.MIN_VALUE;

   public LightningCollectorBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.LIGHTNING_COLLECTOR.get(), pos, blockState);
      this.getMainNode().setIdlePowerUsage(0.0);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, LightningCollectorBlockEntity blockEntity) {
      if (level instanceof ServerLevel) {
         blockEntity.frequencyBinding.serverTick();
         if (blockEntity.cooldownTicks > 0) {
            blockEntity.cooldownTicks--;
            if (blockEntity.cooldownTicks == 0) {
               blockEntity.saveChanges();
               blockEntity.markForUpdate();
            }
         }

         if (blockEntity.workingTicks > 0) {
            blockEntity.workingTicks--;
            if (blockEntity.workingTicks == 0) {
               blockEntity.updateWorkingBlockState(false);
               blockEntity.saveChanges();
            }
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

   public IItemHandlerModifiable getAutomationInventory() {
      return this.inventory;
   }

   public LightningCollectorInventory getInventory() {
      return this.inventory;
   }

   public int getCooldownTicks() {
      return this.cooldownTicks;
   }

   public ItemStack getInstalledCrystal() {
      return this.inventory.getStackInSlot(0);
   }

   public int getCatalysisValue() {
      ItemStack crystal = this.getInstalledCrystal();
      return crystal.m_41619_() ? 0 : ElectroChimeCrystalItem.getCatalysisValue(crystal);
   }

   public LightningCollectorBlockEntity.OutputPreview getPreview(LightningKey.Tier tier) {
      ItemStack crystal = this.getInstalledCrystal();
      boolean extremeHighVoltage = tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE;
      if (crystal.m_41619_()) {
         int baseMin = extremeHighVoltage ? AE2LTCommonConfig.lightningCollectorEhvBaseMin() : AE2LTCommonConfig.lightningCollectorHvBaseMin();
         int baseMax = extremeHighVoltage ? AE2LTCommonConfig.lightningCollectorEhvBaseMax() : AE2LTCommonConfig.lightningCollectorHvBaseMax();
         if (baseMin > baseMax) {
            warnInvalidBaseRange(extremeHighVoltage, baseMin, baseMax);
         }

         return new LightningCollectorBlockEntity.OutputPreview(Math.min(baseMin, baseMax), Math.max(baseMin, baseMax));
      } else if (crystal.m_150930_((Item)ModItems.PERFECT_ELECTRO_CHIME_CRYSTAL.get())) {
         int output = extremeHighVoltage ? AE2LTCommonConfig.lightningCollectorPerfectEhvOutput() : AE2LTCommonConfig.lightningCollectorPerfectHvOutput();
         return new LightningCollectorBlockEntity.OutputPreview(output, output);
      } else {
         double progress = ElectroChimeCrystalItem.getCatalysisPercent(crystal);
         double center = Mth.m_14139_(
            progress,
            extremeHighVoltage ? (double)AE2LTCommonConfig.lightningCollectorEhvCrystalStart() : (double)AE2LTCommonConfig.lightningCollectorHvCrystalStart(),
            extremeHighVoltage ? (double)AE2LTCommonConfig.lightningCollectorEhvCrystalEnd() : (double)AE2LTCommonConfig.lightningCollectorHvCrystalEnd()
         );
         int spread = Math.max(1, Mth.m_14107_(center * AE2LTCommonConfig.lightningCollectorSpreadRatio()));
         int min = Math.max(1, Mth.m_14107_(center) - spread);
         int max = Math.max(min, Mth.m_14165_(center) + spread);
         return new LightningCollectorBlockEntity.OutputPreview(min, max);
      }
   }

   public boolean canCaptureLightning() {
      return this.cooldownTicks <= 0 && !this.hasCapturedThisTick();
   }

   public boolean captureLightning(boolean naturalWeatherLightning) {
      if (this.f_58857_ instanceof ServerLevel serverLevel && this.cooldownTicks <= 0 && !this.hasCapturedThisTick(serverLevel)) {
         IGrid grid = this.getMainNode().getGrid();
         if (grid == null) {
            return false;
         }

         LightningKey.Tier tier = naturalWeatherLightning ? LightningKey.Tier.EXTREME_HIGH_VOLTAGE : LightningKey.Tier.HIGH_VOLTAGE;
         LightningCollectorBlockEntity.OutputPreview preview = this.getPreview(tier);
         int rolledOutput = preview.roll(serverLevel.f_46441_);
         if (rolledOutput <= 0) {
            return false;
         }

         LightningCollectedEvent collectedEvent = new LightningCollectedEvent(
            serverLevel, this.f_58858_, LightningKey.toApiTier(tier), (long)rolledOutput, naturalWeatherLightning
         );
         MinecraftForge.EVENT_BUS.post(collectedEvent);
         if (collectedEvent.isCanceled()) {
            return false;
         }

         long amountToInsert = collectedEvent.getAmount();
         long inserted = amountToInsert > 0L
            ? grid.getStorageService().getInventory().insert(LightningKey.of(tier), amountToInsert, Actionable.MODULATE, IActionSource.ofMachine(this))
            : 0L;
         boolean captured = inserted > 0L;
         if (!captured) {
            return false;
         }

         if (tier == LightningKey.Tier.EXTREME_HIGH_VOLTAGE && this.canCultivateFromNaturalStrike(serverLevel) && this.cultivateCrystal(serverLevel.f_46441_)) {
            this.lastNaturalCultivationGameTime = serverLevel.m_46467_();
         }

         this.lastCaptureGameTime = serverLevel.m_46467_();
         this.cooldownTicks = AE2LTCommonConfig.lightningCollectorCooldownTicks();
         this.workingTicks = 20;
         this.updateWorkingBlockState(true);
         this.saveChanges();
         this.markForUpdate();
         return true;
      }

      return false;
   }

   public void openMenu(Player player, MenuLocator locator) {
      MenuOpener.open(LightningCollectorMenu.TYPE, player, locator);
   }

   public void m_183515_(CompoundTag data) {
      super.m_183515_(data);
      this.inventory.saveToTag(data, "Inventory");
      data.m_128405_("CooldownTicks", this.cooldownTicks);
      data.m_128405_("WorkingTicks", this.workingTicks);
      this.frequencyBinding.save(data);
   }

   public void loadTag(CompoundTag data) {
      super.loadTag(data);
      this.inventory.loadFromTag(data, "Inventory");
      this.cooldownTicks = Math.max(0, data.m_128451_("CooldownTicks"));
      this.workingTicks = Math.max(0, data.m_128451_("WorkingTicks"));
      this.frequencyBinding.load(data);
   }

   protected void writeToStream(FriendlyByteBuf data) {
      super.writeToStream(data);
      data.m_130055_(this.getInstalledCrystal());
      data.m_130130_(this.cooldownTicks);
   }

   protected boolean readFromStream(FriendlyByteBuf data) {
      boolean changed = super.readFromStream(data);
      ItemStack oldCrystal = this.getInstalledCrystal();
      ItemStack newCrystal = data.m_130267_();
      if (!ItemStack.m_41728_(oldCrystal, newCrystal)) {
         this.inventory.setClientRenderStack(newCrystal);
         changed = true;
      }

      int newCooldown = data.m_130242_();
      if (newCooldown != this.cooldownTicks) {
         this.cooldownTicks = newCooldown;
         changed = true;
      }

      return changed;
   }

   public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
      super.addAdditionalDrops(level, pos, drops);
      ItemStack crystal = this.getInstalledCrystal();
      if (!crystal.m_41619_()) {
         drops.add(crystal.m_41777_());
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
      return ((LightningCollectorBlock)ModBlocks.LIGHTNING_COLLECTOR.get()).m_5456_();
   }

   public IGridNode getActionableNode() {
      return this.getMainNode().getNode();
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return EnumSet.allOf(Direction.class);
   }

   public AECableType getCableConnectionType(Direction dir) {
      return AECableType.SMART;
   }

   private boolean canCultivateFromNaturalStrike(ServerLevel serverLevel) {
      long gameTime = serverLevel.m_46467_();
      return this.lastNaturalCultivationGameTime == Long.MIN_VALUE || gameTime - this.lastNaturalCultivationGameTime >= 20L;
   }

   private boolean hasCapturedThisTick() {
      if (this.f_58857_ instanceof ServerLevel serverLevel && this.hasCapturedThisTick(serverLevel)) {
         return true;
      }

      return false;
   }

   private boolean hasCapturedThisTick(ServerLevel serverLevel) {
      return this.lastCaptureGameTime == serverLevel.m_46467_();
   }

   private boolean cultivateCrystal(RandomSource random) {
      ItemStack crystal = this.getInstalledCrystal();
      if (!crystal.m_41619_() && !crystal.m_150930_((Item)ModItems.PERFECT_ELECTRO_CHIME_CRYSTAL.get())) {
         int feed = ElectroChimeCrystalItem.rollCatalysisFeed(random);
         int catalysis = ElectroChimeCrystalItem.addCatalysis(crystal, feed);
         if (catalysis >= ElectroChimeCrystalItem.getMaxCatalysis()) {
            this.inventory.setStackInSlot(0, new ItemStack((ItemLike)ModItems.PERFECT_ELECTRO_CHIME_CRYSTAL.get()));
         }

         return true;
      } else {
         return false;
      }
   }

   private static void warnInvalidBaseRange(boolean extremeHighVoltage, int min, int max) {
      if (extremeHighVoltage) {
         if (warnedInvalidExtremeVoltageBaseRange) {
            return;
         }

         warnedInvalidExtremeVoltageBaseRange = true;
      } else {
         if (warnedInvalidHighVoltageBaseRange) {
            return;
         }

         warnedInvalidHighVoltageBaseRange = true;
      }

      LOG.warn(
         "Invalid lightning collector {} base output range: min={} max={}. Swapping values as a fallback.",
         new Object[]{extremeHighVoltage ? "extremeHighVoltage" : "highVoltage", min, max}
      );
   }

   private void onInventoryChanged() {
      this.saveChanges();
      this.markForUpdate();
   }

   private void updateWorkingBlockState(boolean working) {
      if (this.f_58857_ != null) {
         BlockState state = this.f_58857_.m_8055_(this.f_58858_);
         if (state.m_60713_((Block)ModBlocks.LIGHTNING_COLLECTOR.get())
            && this.f_58857_.m_7702_(this.f_58858_) == this
            && state.m_61138_(LightningCollectorBlock.WORKING)
            && (Boolean)state.m_61143_(LightningCollectorBlock.WORKING) != working) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(LightningCollectorBlock.WORKING, working), 3);
         }
      }
   }

   public void onReady() {
      super.onReady();
      this.frequencyBinding.onReady();
      this.updateWorkingBlockState(this.workingTicks > 0);
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

   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      if (cap == ForgeCapabilities.ITEM_HANDLER) {
         return LazyOptional.of(this::getAutomationInventory).cast();
      } else {
         return cap == AE2LTCapabilities.LIGHTNING_ENERGY_BLOCK
            ? LazyOptional.of(() -> new GridLightningEnergyHandler(this)).cast()
            : super.getCapability(cap, side);
      }
   }

   public static record OutputPreview(int min, int max) {
      public int roll(RandomSource random) {
         return this.max <= this.min ? this.min : random.m_188503_(this.max - this.min + 1) + this.min;
      }
   }
}
