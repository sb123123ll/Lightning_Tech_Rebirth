package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.MachineSource;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.locator.MenuLocators;
import com.google.common.collect.ImmutableSet;
import com.moakiee.ae2lt.block.TianshuSupercomputerPortBlock;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPool;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPoolProvider;
import com.moakiee.ae2lt.logic.tianshu.CpuMainCoreTier;
import com.moakiee.ae2lt.logic.tianshu.TianshuFunctionProfile;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternRepository;
import com.moakiee.ae2lt.logic.tianshu.maintenance.TianshuInventoryMaintenanceService;
import com.moakiee.ae2lt.menu.TianshuSupercomputerControllerMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.thunderbolt.api.crafting.CraftingAlgorithmProvider;
import com.moakiee.thunderbolt.api.crafting.CraftingAlgorithmSelection;
import com.moakiee.thunderbolt.api.crafting.cpu.ExtendedCraftingCpuClusterProvider;
import com.moakiee.thunderbolt.core.crafting.algorithm.menu.CraftingAlgorithmProviderMenu;
import com.moakiee.thunderbolt.core.crafting.algorithm.menu.CraftingAlgorithmProviderMenuHost;
import com.moakiee.thunderbolt.core.crafting.planner.ThunderboltV2PlanningEngine;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TianshuSupercomputerPortBlockEntity
   extends AENetworkBlockEntity
   implements TimeWheelCraftingCpuPoolProvider,
   ICraftingProvider,
   ICraftingRequester,
   CraftingAlgorithmProviderMenuHost {
   private static final double LINK_IDLE_POWER = 8.0;
   private static final String TAG_CONTROLLER_POS = "ControllerPos";
   private static final String TAG_FORMED = "Formed";
   private static final String TAG_CPU_POOL = "CpuPool";
   private static final String TAG_CLOSED_LOOP_PATTERNS = "ClosedLoopPatterns";
   private static final String TAG_TIANSHU_ID = "TianshuId";
   private static final String TAG_MAINTENANCE = "InventoryMaintenance";
   private final IActionSource actionSource = new MachineSource(this.getMainNode()::getNode);
   private static final int BINDING_CHECK_INTERVAL_TICKS = 20;
   private BlockPos controllerPos;
   private UUID boundMachineId;
   private TimeWheelCraftingCpuPool linkedCpuPool;
   private boolean formed;
   private UUID legacyTianshuId;
   private CompoundTag legacyRuntimeState;
   private CompoundTag legacyPatternState;
   private long nextBindingCheckTick;

   public TianshuSupercomputerPortBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.TIANSHU_SUPERCOMPUTER_PORT.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TianshuSupercomputerPortBlockEntity port) {
      if (!level.f_46443_ && level.m_46467_() >= port.nextBindingCheckTick) {
         port.nextBindingCheckTick = level.m_46467_() + 20L;
         port.validateControllerBinding();
      }
   }

   protected IManagedGridNode createMainNode() {
      return super.createMainNode()
         .setTagName("tianshu_supercomputer_port")
         .setVisualRepresentation((ItemLike)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get())
         .setIdlePowerUsage(8.0)
         .setFlags(new GridFlags[]{GridFlags.REQUIRE_CHANNEL})
         .addService(ExtendedCraftingCpuClusterProvider.class, this)
         .addService(CraftingAlgorithmProvider.class, this)
         .addService(TimeWheelCraftingCpuPoolProvider.class, this)
         .addService(ICraftingProvider.class, this)
         .addService(ICraftingRequester.class, this);
   }

   public ResourceLocation getProvidedAlgorithm() {
      return ThunderboltV2PlanningEngine.ID;
   }

   public ResourceLocation getSelectedAlgorithm() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getCraftingAlgorithmProvider().getSelectedAlgorithm() : ThunderboltV2PlanningEngine.ID;
   }

   public int getPriority() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getCraftingAlgorithmPriority() : 0;
   }

   public void setSelection(CraftingAlgorithmSelection selection) {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.getCraftingAlgorithmProvider().setSelection(selection);
      }
   }

   public Component getCraftingAlgorithmMenuTitle() {
      return Component.m_237115_("ae2lt.tianshu.gui.algorithm_selection");
   }

   public void returnToMainMenu(Player player, ISubMenu subMenu) {
      if (subMenu instanceof PriorityMenu) {
         MenuOpener.returnTo(CraftingAlgorithmProviderMenu.TYPE, player, subMenu.getLocator());
      } else {
         TianshuSupercomputerControllerBlockEntity controller = this.getController();
         if (controller != null) {
            MenuOpener.returnTo(TianshuSupercomputerControllerMenu.TYPE, player, MenuLocators.forBlockEntity(controller));
         }
      }
   }

   public ItemStack getMainMenuIcon() {
      return new ItemStack((ItemLike)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get());
   }

   public AECableType getCableConnectionType(Direction dir) {
      return this.formed ? AECableType.DENSE_SMART : AECableType.NONE;
   }

   public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
      return (Set<Direction>)(this.formed ? EnumSet.allOf(Direction.class) : Collections.emptySet());
   }

   public void bindToController(@Nullable BlockPos controllerPos) {
      if (controllerPos != null) {
         throw new IllegalArgumentException("A Tianshu link requires its controller UUID and CPU pool");
      } else {
         boolean bindingChanged = this.formed || this.controllerPos != null || this.boundMachineId != null || this.linkedCpuPool != null;
         boolean formedChanged = this.formed;
         this.controllerPos = null;
         this.boundMachineId = null;
         this.linkedCpuPool = null;
         this.formed = false;
         this.getMainNode().setIdlePowerUsage(8.0);
         if (formedChanged) {
            this.onGridConnectableSidesChanged();
         }

         this.updateLinkState(bindingChanged);
      }
   }

   public void bindToController(BlockPos controllerPos, UUID machineId, TimeWheelCraftingCpuPool cpuPool, CpuMainCoreTier mainCore) {
      if (controllerPos != null && machineId != null && cpuPool != null && mainCore != null) {
         boolean bindingChanged = !this.formed
            || !controllerPos.equals(this.controllerPos)
            || !machineId.equals(this.boundMachineId)
            || this.linkedCpuPool != cpuPool;
         boolean formedChanged = !this.formed;
         this.controllerPos = controllerPos.m_7949_();
         this.boundMachineId = machineId;
         this.linkedCpuPool = cpuPool;
         this.formed = true;
         this.getMainNode().setIdlePowerUsage(mainCore.idlePowerUsage());
         this.legacyTianshuId = null;
         if (formedChanged) {
            this.onGridConnectableSidesChanged();
         }

         this.updateLinkState(bindingChanged);
      } else {
         this.bindToController(null);
      }
   }

   public void suspendFromController(BlockPos expectedControllerPos) {
      if (this.formed && expectedControllerPos != null && expectedControllerPos.equals(this.controllerPos)) {
         this.formed = false;
         this.getMainNode().setIdlePowerUsage(8.0);
         this.onGridConnectableSidesChanged();
         this.updateLinkState(true);
      }
   }

   private void updateLinkState(boolean bindingChanged) {
      boolean blockStateChanged = false;
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         BlockState state = this.m_58900_();
         if (state.m_61138_(TianshuSupercomputerPortBlock.FORMED) && (Boolean)state.m_61143_(TianshuSupercomputerPortBlock.FORMED) != this.formed) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(TianshuSupercomputerPortBlock.FORMED, this.formed), 3);
            blockStateChanged = true;
         }

         if (bindingChanged || blockStateChanged) {
            this.f_58857_.m_46672_(this.f_58858_, state.m_60734_());
         }

         this.refreshCraftingProvider();
      }

      this.saveChanges();
      this.markForUpdate();
   }

   public void refreshCraftingProvider() {
      IGrid grid = this.getMainNode().getGrid();
      if (grid != null) {
         grid.getCraftingService().refreshNodeCraftingProvider(this.getMainNode().getNode());
      }
   }

   public BlockPos getControllerPos() {
      return this.controllerPos;
   }

   public boolean isLinkedTo(BlockPos controllerPos, UUID machineId) {
      return this.formed && controllerPos != null && machineId != null && controllerPos.equals(this.controllerPos) && machineId.equals(this.boundMachineId);
   }

   public boolean isFormed() {
      return this.getController() != null;
   }

   public boolean isLinkActive() {
      return this.getController() != null && this.getMainNode().isActive() && this.getMainNode().getGrid() != null;
   }

   public TianshuFunctionProfile getFunctionProfile() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getFunctionProfile() : TianshuFunctionProfile.empty();
   }

   @Nullable
   public ClosedLoopPatternRepository getClosedLoopPatternRepository() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getClosedLoopPatternRepository() : null;
   }

   public UUID getTianshuId() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getTianshuId() : (this.boundMachineId != null ? this.boundMachineId : new UUID(0L, 0L));
   }

   public UUID getLegacyTianshuId() {
      return this.legacyTianshuId;
   }

   @Nullable
   public TianshuInventoryMaintenanceService getInventoryMaintenance() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getInventoryMaintenance() : null;
   }

   public void tickTianshuFunctions() {
      TianshuInventoryMaintenanceService maintenance = this.getInventoryMaintenance();
      if (maintenance != null) {
         maintenance.tick();
      }
   }

   public void maintenanceStateChanged() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.maintenanceStateChanged();
      }
   }

   public ImmutableSet<ICraftingLink> getRequestedJobs() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getRequestedJobs() : ImmutableSet.of();
   }

   public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.insertCraftedItems(link, what, amount, mode) : 0L;
   }

   public void jobStateChange(ICraftingLink link) {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.jobStateChange(link);
      }
   }

   public long insertReusableSeed(AEKey key, long amount, Actionable mode) {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.insertReusableSeed(key, amount, mode) : 0L;
   }

   public long reusableSeedAmount(AEKey key) {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.reusableSeedAmount(key) : 0L;
   }

   public void seedDrivesChanged() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.seedStorageChanged();
      }
   }

   public void closedLoopPatternsChanged() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.closedLoopPatternsChanged();
      }
   }

   public List<IPatternDetails> getAvailablePatterns() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller != null ? controller.getAvailablePatterns() : List.of();
   }

   public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
      return false;
   }

   public boolean isBusy() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      return controller == null || !controller.isCpuActive();
   }

   @Nullable
   @Override
   public TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool() {
      return this.isNetworkActive() && this.getController() != null ? this.linkedCpuPool : null;
   }

   public IGrid getGrid() {
      return this.formed ? this.getMainNode().getGrid() : null;
   }

   public IActionSource getActionSource() {
      return this.actionSource;
   }

   public boolean isNetworkActive() {
      return this.formed && this.getMainNode().isActive() && this.getMainNode().getGrid() != null;
   }

   @Nullable
   public TianshuSupercomputerControllerBlockEntity getController() {
      if (this.formed && this.controllerPos != null && this.boundMachineId != null && this.f_58857_ != null && this.f_58857_.m_46749_(this.controllerPos)) {
         if (this.f_58857_.m_7702_(this.controllerPos) instanceof TianshuSupercomputerControllerBlockEntity controller
            && controller.isPersistentStateOwner()
            && this.boundMachineId.equals(controller.getMachineId())
            && controller.isPortActive(this.f_58858_)) {
            return controller;
         }

         return null;
      } else {
         return null;
      }
   }

   public void persistRuntimeStateIfChanged() {
      TianshuSupercomputerControllerBlockEntity controller = this.getController();
      if (controller != null) {
         controller.persistRuntimeStateIfChanged();
      }
   }

   @Nullable
   public CompoundTag copyLegacyRuntimeState() {
      return this.legacyRuntimeState != null ? this.legacyRuntimeState.m_6426_() : null;
   }

   public void consumeLegacyRuntimeState() {
      this.legacyRuntimeState = null;
      this.saveChanges();
   }

   @Nullable
   public CompoundTag copyLegacyPatternState() {
      return this.legacyPatternState != null ? this.legacyPatternState.m_6426_() : null;
   }

   public void consumeLegacyPatternState() {
      this.legacyPatternState = null;
      this.saveChanges();
   }

   public void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      if (this.controllerPos != null) {
         tag.m_128356_("ControllerPos", this.controllerPos.m_121878_());
      }

      tag.m_128379_("Formed", this.formed);
      if (this.legacyTianshuId != null) {
         tag.m_128362_("TianshuId", this.legacyTianshuId);
      }

      if (this.legacyRuntimeState != null) {
         if (this.legacyRuntimeState.m_128425_("InventoryMaintenance", 10)) {
            tag.m_128365_("InventoryMaintenance", this.legacyRuntimeState.m_128469_("InventoryMaintenance").m_6426_());
         }

         if (this.legacyRuntimeState.m_128425_("CpuPool", 10)) {
            tag.m_128365_("CpuPool", this.legacyRuntimeState.m_128469_("CpuPool").m_6426_());
         }
      }

      if (this.legacyPatternState != null) {
         tag.m_128365_("ClosedLoopPatterns", this.legacyPatternState.m_6426_());
      }
   }

   public void loadTag(CompoundTag tag) {
      super.loadTag(tag);
      this.controllerPos = tag.m_128425_("ControllerPos", 4) ? BlockPos.m_122022_(tag.m_128454_("ControllerPos")) : null;
      this.formed = false;
      this.boundMachineId = null;
      this.linkedCpuPool = null;
      this.legacyTianshuId = tag.m_128403_("TianshuId") ? tag.m_128342_("TianshuId") : null;
      this.legacyPatternState = tag.m_128425_("ClosedLoopPatterns", 10) ? tag.m_128469_("ClosedLoopPatterns").m_6426_() : null;
      CompoundTag runtime = new CompoundTag();
      if (tag.m_128425_("InventoryMaintenance", 10)) {
         runtime.m_128365_("InventoryMaintenance", tag.m_128469_("InventoryMaintenance").m_6426_());
      }

      if (tag.m_128425_("CpuPool", 10)) {
         runtime.m_128365_("CpuPool", tag.m_128469_("CpuPool").m_6426_());
      }

      this.legacyRuntimeState = runtime.m_128456_() ? null : runtime;
   }

   public void onLoad() {
      super.onLoad();
      this.nextBindingCheckTick = this.f_58857_ != null ? this.f_58857_.m_46467_() : 0L;
   }

   protected Item getItemFromBlockEntity() {
      return ((TianshuSupercomputerPortBlock)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get()).m_5456_();
   }

   private void validateControllerBinding() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && this.controllerPos != null) {
         if (!this.f_58857_.m_46749_(this.controllerPos)) {
            this.suspendFromController(this.controllerPos);
         } else {
            if (this.f_58857_.m_7702_(this.controllerPos) instanceof TianshuSupercomputerControllerBlockEntity controller) {
               if (controller.isPortActive(this.f_58858_)) {
                  if (!this.formed) {
                     controller.scheduleStructureCheck();
                  }
               } else if (controller.ownsPort(this.f_58858_)) {
                  this.suspendFromController(this.controllerPos);
                  controller.scheduleStructureCheck();
               } else {
                  this.bindToController(null);
               }
            } else if (!this.f_58857_.m_8055_(this.controllerPos).m_60713_((Block)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get())) {
               this.bindToController(null);
            }
         }
      }
   }
}
