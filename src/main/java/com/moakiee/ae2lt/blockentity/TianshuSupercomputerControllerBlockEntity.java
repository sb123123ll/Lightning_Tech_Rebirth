package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.IPriorityHost;
import appeng.me.service.CraftingService;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import com.google.common.collect.ImmutableSet;
import com.moakiee.ae2lt.block.TianshuSupercomputerControllerBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputerGlassBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputerPortBlock;
import com.moakiee.ae2lt.block.TianshuSupercomputerStructureBlock;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPool;
import com.moakiee.ae2lt.crafting.timewheel.TimeWheelCraftingCpuPoolHost;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.logic.persistence.CompletePhysicalStorageSet;
import com.moakiee.ae2lt.logic.persistence.ControllerMachineIdentity;
import com.moakiee.ae2lt.logic.persistence.ControllerMachineStateSavedData;
import com.moakiee.ae2lt.logic.tianshu.CpuInternalCoreCalculator;
import com.moakiee.ae2lt.logic.tianshu.CpuInternalCoreProfile;
import com.moakiee.ae2lt.logic.tianshu.CpuMainCoreTier;
import com.moakiee.ae2lt.logic.tianshu.TianshuAutoBuildPlan;
import com.moakiee.ae2lt.logic.tianshu.TianshuCraftingCpuHost;
import com.moakiee.ae2lt.logic.tianshu.TianshuFunctionProfile;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockComponent;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanAttempt;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanIssue;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanResult;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanner;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternDecoder;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternPayload;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternRepository;
import com.moakiee.ae2lt.logic.tianshu.maintenance.TianshuInventoryMaintenanceHost;
import com.moakiee.ae2lt.logic.tianshu.maintenance.TianshuInventoryMaintenanceService;
import com.moakiee.ae2lt.menu.TianshuSupercomputerControllerMenu;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import com.moakiee.thunderbolt.api.crafting.ConfigurableCraftingAlgorithmProvider;
import com.moakiee.thunderbolt.api.crafting.DefaultCraftingAlgorithmProviderState;
import com.moakiee.thunderbolt.core.crafting.loop.ReusableSeedPattern;
import com.moakiee.thunderbolt.core.crafting.planner.ThunderboltV2PlanningEngine;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import com.moakiee.thunderbolt.core.crafting.support.CraftingProviderChangeTracker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TianshuSupercomputerControllerBlockEntity
   extends BlockEntity
   implements TimeWheelCraftingCpuPoolHost,
   TianshuInventoryMaintenanceHost,
   TianshuCraftingCpuHost,
   IPriorityHost {
   private static final long NO_SCAN = Long.MIN_VALUE;
   private static final int AUTO_BUILD_INTERVAL_TICKS = 1;
   private static final int CHUNK_RECHECK_INTERVAL_TICKS = 20;
   private static final String TAG_FORMED = "Formed";
   private static final String TAG_PORT_POS = "PortPos";
   private static final String TAG_MIN_POS = "MinPos";
   private static final String TAG_MAX_POS = "MaxPos";
   private static final String TAG_MEMBER_COUNT = "MemberCount";
   private static final String TAG_MAIN_CORE = "MainCore";
   private static final String TAG_STORAGE_UNITS = "CapacityCores";
   private static final String TAG_PARALLEL_UNITS = "ParallelCores";
   private static final String TAG_AMPLIFIER_UNITS = "AmplifierCores";
   private static final String TAG_CLOSED_LOOP_STORAGES = "ClosedLoopStorages";
   private static final String TAG_SEED_STORAGES = "SeedStorages";
   private static final String TAG_MACHINE_ID = "MachineId";
   private static final String TAG_FAST_PLANNING = "FastPlanning";
   private static final String TAG_CPU_PRIORITY = "CpuPriority";
   private static final String TAG_ALGORITHM_PROVIDER = "CraftingAlgorithmProvider";
   private static final String TAG_CPU_POOL = "CpuPool";
   private static final String TAG_MAINTENANCE = "InventoryMaintenance";
   private boolean formed;
   private BlockPos portPos;
   private BlockPos minPos;
   private BlockPos maxPos;
   private int memberCount;
   private CpuInternalCoreProfile coreProfile = CpuInternalCoreProfile.empty();
   private TianshuFunctionProfile functionProfile = TianshuFunctionProfile.empty();
   private final TimeWheelCraftingCpuPool cpuPool = new TimeWheelCraftingCpuPool(this, 0L, 0, 1L, false);
   private final ClosedLoopPatternRepository closedLoopPatterns = new ClosedLoopPatternRepository(() -> this.functionProfile.closedLoopPatternCapacity());
   private final IdentityHashMap<ClosedLoopPatternPayload, ClosedLoopPatternDecoder.DecodedPayload> decodedClosedLoopPatterns = new IdentityHashMap<>();
   private final TianshuInventoryMaintenanceService maintenance = new TianshuInventoryMaintenanceService(this);
   private List<BlockPos> patternStoragePositions = List.of();
   private List<BlockPos> seedStoragePositions = List.of();
   private UUID machineId = UUID.randomUUID();
   private boolean identityInitialized;
   private boolean persistentStateOwner;
   private UUID loadedRuntimeId;
   private boolean structureAvailable;
   private boolean waitingForChunks;
   private long scheduledScanTick = Long.MIN_VALUE;
   private long nextChunkCheckTick;
   private List<TianshuMultiblockScanIssue> lastIssues = List.of();
   private boolean fastPlanningEnabled = true;
   private int cpuPriority;
   private List<TianshuAutoBuildPlan.Placement> autoBuildPlacements = List.of();
   private UUID autoBuildPlayerId;
   private Direction autoBuildFacing = Direction.NORTH;
   private int autoBuildPlacementIndex;
   private int autoBuildPlacedBlocks;
   private long nextAutoBuildTick;
   private boolean runtimeStateDirty;
   private long pendingStorage = -1L;
   private int pendingParallel = -1;
   private long pendingMaxCopiesPerTick = -1L;
   private boolean pendingUnboundedBatch;
   private Set<AEItemKey> publishedClosedLoopPatternDefinitions = Set.of();
   private final CraftingProviderChangeTracker closedLoopDependencyChanges = new CraftingProviderChangeTracker();
   private final DefaultCraftingAlgorithmProviderState algorithmProvider = new DefaultCraftingAlgorithmProviderState(
      ThunderboltV2PlanningEngine.ID, 0, this::algorithmProviderChanged
   );

   public TianshuSupercomputerControllerBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.TIANSHU_SUPERCOMPUTER_CONTROLLER.get(), pos, state);
   }

   @Nullable
   @Override
   public Level getCpuLevel() {
      return this.f_58857_;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TianshuSupercomputerControllerBlockEntity controller) {
      if (!controller.persistentStateOwner) {
         if (controller.formed) {
            controller.deform();
         }

         controller.syncWorkingState();
      } else {
         if ((controller.formed || controller.waitingForChunks) && level.m_46467_() >= controller.nextChunkCheckTick) {
            controller.nextChunkCheckTick = level.m_46467_() + 20L;
            controller.checkChunkAvailability();
         }

         if (controller.isAutoBuilding()) {
            controller.tickAutoBuild();
         } else if (controller.scheduledScanTick != Long.MIN_VALUE && level.m_46467_() >= controller.scheduledScanTick) {
            controller.scheduledScanTick = Long.MIN_VALUE;
            controller.scanNow();
         }

         if (controller.isFormed() && controller.portPos != null && level.m_7702_(controller.portPos) instanceof TianshuSupercomputerPortBlockEntity port) {
            controller.applyPendingProfile();
            controller.maintenance.tick();
            controller.refreshClosedLoopProviderForDependencyChanges(port);
         }

         controller.syncWorkingState();
      }
   }

   @Override
   public boolean isFormed() {
      return this.formed && this.structureAvailable;
   }

   public BlockPos getPortPos() {
      return this.portPos;
   }

   public boolean ownsPort(BlockPos candidate) {
      return this.formed && candidate != null && candidate.equals(this.portPos);
   }

   public boolean isPortActive(BlockPos candidate) {
      return this.structureAvailable && this.ownsPort(candidate);
   }

   public String issueText() {
      return this.lastIssues.isEmpty() ? this.profileText() : this.lastIssues.stream().map(Object::toString).collect(Collectors.joining(", "));
   }

   private String profileText() {
      if (this.coreProfile.mainCore() == null) {
         return "0";
      } else {
         String storage = this.coreProfile.storageBytes() == Long.MAX_VALUE ? "∞" : Long.toString(this.coreProfile.storageBytes());
         return this.coreProfile.mainCore() + ", " + storage + " bytes, " + this.coreProfile.parallelism() + " parallel";
      }
   }

   public CpuInternalCoreProfile getCoreProfile() {
      return this.coreProfile;
   }

   @Override
   public TianshuFunctionProfile getFunctionProfile() {
      return this.functionProfile;
   }

   public UUID getMachineId() {
      return this.machineId;
   }

   @Override
   public UUID getTianshuId() {
      return this.machineId;
   }

   public boolean isPersistentStateOwner() {
      return this.persistentStateOwner;
   }

   public boolean isFastPlanningEnabled() {
      return this.fastPlanningEnabled;
   }

   public void toggleFastPlanning() {
      this.fastPlanningEnabled = !this.fastPlanningEnabled;
      this.cpuPool.setFastPlanningEnabled(this.fastPlanningEnabled);
      this.m_6596_();
   }

   public ConfigurableCraftingAlgorithmProvider getCraftingAlgorithmProvider() {
      return this.algorithmProvider;
   }

   public int getCraftingAlgorithmPriority() {
      return this.algorithmProvider.getPriority();
   }

   public int getPriority() {
      return this.cpuPriority;
   }

   public void setPriority(int priority) {
      if (this.cpuPriority != priority) {
         this.cpuPriority = priority;
         this.m_6596_();
      }
   }

   @Override
   public int getCpuPriority() {
      return this.cpuPriority;
   }

   public void returnToMainMenu(Player player, ISubMenu subMenu) {
      MenuOpener.returnTo(TianshuSupercomputerControllerMenu.TYPE, player, subMenu.getLocator());
   }

   public ItemStack getMainMenuIcon() {
      return new ItemStack((ItemLike)ModBlocks.TIANSHU_SUPERCOMPUTER_CONTROLLER.get());
   }

   private void algorithmProviderChanged() {
      this.m_6596_();
   }

   public void initializeIdentityFromItem(ItemStack stack) {
      UUID itemId = ControllerMachineIdentity.read(stack);
      this.identityInitialized = true;
      if (itemId != null) {
         this.changeMachineId(itemId);
      }

      this.claimPersistentState();
      this.m_6596_();
   }

   public int getPrimaryIssueOrdinal() {
      return this.lastIssues.isEmpty() ? -1 : this.lastIssues.get(0).ordinal();
   }

   public int memberCount() {
      return this.memberCount;
   }

   public void scheduleStructureCheck() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         long targetTick = this.f_58857_.m_46467_() + 1L;
         if (this.scheduledScanTick == Long.MIN_VALUE || targetTick < this.scheduledScanTick) {
            this.scheduledScanTick = targetTick;
         }
      }
   }

   public void scanNow() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         if (!this.persistentStateOwner) {
            this.deform();
         } else {
            Direction orientation = (Direction)this.m_58900_().m_61143_(TianshuSupercomputerControllerBlock.FACING);
            TianshuMultiblockScanAttempt attempt = TianshuMultiblockScanner.scan(this.f_58857_, this.f_58858_, orientation);
            this.lastIssues = attempt.issues();
            if (attempt.chunksUnavailable()) {
               this.suspendForUnloadedChunks();
            } else if (attempt.formed()) {
               this.form(attempt.result());
            } else {
               this.deform();
            }
         }
      }
   }

   public void autoBuild(ServerPlayer player) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         if (this.isAutoBuilding()) {
            player.m_5661_(Component.m_237115_("ae2lt.tianshu.build_in_progress").m_130940_(ChatFormatting.YELLOW), true);
         } else if (this.ensureStructureChunksLoaded()) {
            TianshuAutoBuildPlan plan = this.createAutoBuildPlan();
            if (!plan.blocked().isEmpty()) {
               player.m_5661_(
                  Component.m_237110_("ae2lt.tianshu.build_blocked", new Object[]{plan.blocked().size(), this.describeBlockedPositions(plan.blocked())})
                     .m_130940_(ChatFormatting.RED),
                  false
               );
            } else {
               Map<Item, Integer> requirements = this.autoBuildRequirements(plan);
               if (!player.m_150110_().f_35937_) {
                  Map<Item, Integer> missing = this.findMissingRequirements(player, requirements);
                  if (!missing.isEmpty()) {
                     player.m_5661_(
                        Component.m_237110_("ae2lt.tianshu.build_missing", new Object[]{this.describeMissing(missing)}).m_130940_(ChatFormatting.RED), false
                     );
                     return;
                  }
               }

               if (plan.placements().isEmpty()) {
                  this.finishAutoBuild(player, 0);
               } else {
                  this.autoBuildPlacements = plan.placements();
                  this.autoBuildPlayerId = player.m_20148_();
                  this.autoBuildFacing = (Direction)this.m_58900_().m_61143_(TianshuSupercomputerControllerBlock.FACING);
                  this.autoBuildPlacementIndex = 0;
                  this.autoBuildPlacedBlocks = 0;
                  this.nextAutoBuildTick = this.f_58857_.m_46467_() + 1L;
                  this.scheduledScanTick = Long.MIN_VALUE;
                  this.m_6596_();
                  player.m_5661_(
                     Component.m_237110_("ae2lt.tianshu.build_started", new Object[]{this.autoBuildPlacements.size()}).m_130940_(ChatFormatting.GREEN), true
                  );
               }
            }
         }
      }
   }

   private boolean isAutoBuilding() {
      return this.autoBuildPlayerId != null && this.autoBuildPlacementIndex < this.autoBuildPlacements.size();
   }

   private void tickAutoBuild() {
      if (this.f_58857_ != null && this.f_58857_.m_46467_() >= this.nextAutoBuildTick) {
         MinecraftServer server = this.f_58857_.m_7654_();
         ServerPlayer player = server != null ? server.m_6846_().m_11259_(this.autoBuildPlayerId) : null;
         if (player == null) {
            this.clearAutoBuildSession();
            this.scanNow();
         } else {
            TianshuAutoBuildPlan.Placement placement = this.autoBuildPlacements.get(this.autoBuildPlacementIndex);
            BlockPos pos = TianshuMultiblockScanner.worldPos(this.f_58858_, placement.localPos(), this.autoBuildFacing);
            if (!this.f_58857_.m_46749_(pos)) {
               this.nextAutoBuildTick = this.f_58857_.m_46467_() + 20L;
            } else {
               TianshuMultiblockComponent current = TianshuMultiblockScanner.componentAt(this.f_58857_, pos);
               if (this.matchesAutoBuildTarget(current, placement.target())) {
                  this.advanceAutoBuild(player, false);
               } else if (current != TianshuMultiblockComponent.AIR) {
                  this.abortAutoBuildPlacement(player, pos);
               } else {
                  BlockState state = this.stateForAutoBuild(placement.target());
                  Item consumedItem = state.m_60734_().m_5456_();
                  if (player.m_150110_().f_35937_ || consumedItem != Items.f_41852_ && this.countItem(player, consumedItem) > 0) {
                     if (!this.f_58857_.m_7731_(pos, state, 3)) {
                        this.abortAutoBuildPlacement(player, pos);
                     } else {
                        if (!player.m_150110_().f_35937_) {
                           this.consumeItem(player, consumedItem, 1);
                        }

                        this.playAutoBuildPlaceSound(player, pos, state);
                        this.advanceAutoBuild(player, true);
                     }
                  } else {
                     this.abortAutoBuildMissingItem(player, consumedItem.m_41466_());
                  }
               }
            }
         }
      }
   }

   private boolean matchesAutoBuildTarget(TianshuMultiblockComponent component, TianshuAutoBuildPlan.Target target) {
      return switch (target) {
         case CASING -> component == TianshuMultiblockComponent.CASING;
         case COOLING -> component.fillsCoolingPosition();
         case GLASS -> component == TianshuMultiblockComponent.GLASS;
         case PORT -> component == TianshuMultiblockComponent.PORT;
      };
   }

   private void playAutoBuildPlaceSound(ServerPlayer player, BlockPos pos, BlockState state) {
      SoundType soundType = state.getSoundType(this.f_58857_, pos, player);
      float volume = (soundType.m_56773_() + 1.0F) / 4.0F;
      float pitch = soundType.m_56774_() * (0.82F + this.f_58857_.f_46441_.m_188501_() * 0.12F);
      this.f_58857_.m_5594_(null, pos, soundType.m_56777_(), SoundSource.BLOCKS, volume, pitch);
   }

   private void advanceAutoBuild(ServerPlayer player, boolean placed) {
      if (placed) {
         this.autoBuildPlacedBlocks++;
      }

      this.autoBuildPlacementIndex++;
      if (this.autoBuildPlacementIndex >= this.autoBuildPlacements.size()) {
         int placedBlocks = this.autoBuildPlacedBlocks;
         this.clearAutoBuildSession();
         this.finishAutoBuild(player, placedBlocks);
      } else {
         this.nextAutoBuildTick = this.f_58857_.m_46467_() + 1L;
      }
   }

   private void abortAutoBuildPlacement(ServerPlayer player, BlockPos pos) {
      this.clearAutoBuildSession();
      this.scanNow();
      player.m_5661_(Component.m_237110_("ae2lt.tianshu.build_place_failed", new Object[]{this.describePosition(pos)}).m_130940_(ChatFormatting.RED), false);
   }

   private void abortAutoBuildMissingItem(ServerPlayer player, Component itemName) {
      this.clearAutoBuildSession();
      this.scanNow();
      player.m_5661_(Component.m_237110_("ae2lt.tianshu.build_interrupted_missing", new Object[]{itemName}).m_130940_(ChatFormatting.RED), false);
   }

   private void clearAutoBuildSession() {
      this.autoBuildPlacements = List.of();
      this.autoBuildPlayerId = null;
      this.autoBuildPlacementIndex = 0;
      this.autoBuildPlacedBlocks = 0;
      this.nextAutoBuildTick = 0L;
      this.scheduledScanTick = Long.MIN_VALUE;
      this.m_6596_();
   }

   private void finishAutoBuild(ServerPlayer player, int placedBlocks) {
      this.scanNow();
      if (!this.lastIssues.contains(TianshuMultiblockScanIssue.CHUNKS_UNLOADED)) {
         Component message;
         if (this.isFormed()) {
            message = placedBlocks == 0
               ? Component.m_237115_("ae2lt.tianshu.build_already_complete")
               : Component.m_237110_("ae2lt.tianshu.build_complete", new Object[]{placedBlocks});
         } else {
            message = placedBlocks == 0
               ? Component.m_237115_("ae2lt.tianshu.build_nothing_to_place")
               : Component.m_237110_("ae2lt.tianshu.build_shell_complete", new Object[]{placedBlocks});
         }

         player.m_5661_(message.m_6881_().m_130940_(ChatFormatting.GREEN), true);
      }
   }

   private TianshuAutoBuildPlan createAutoBuildPlan() {
      Direction facing = (Direction)this.m_58900_().m_61143_(TianshuSupercomputerControllerBlock.FACING);
      return TianshuAutoBuildPlan.create(
         local -> TianshuMultiblockScanner.componentAt(this.f_58857_, TianshuMultiblockScanner.worldPos(this.f_58858_, local, facing))
      );
   }

   private Map<Item, Integer> autoBuildRequirements(TianshuAutoBuildPlan plan) {
      LinkedHashMap<Item, Integer> result = new LinkedHashMap<>();

      for (TianshuAutoBuildPlan.Placement placement : plan.placements()) {
         Item item = this.stateForAutoBuild(placement.target()).m_60734_().m_5456_();
         if (item != Items.f_41852_) {
            result.merge(item, Integer.valueOf(1), Integer::sum);
         }
      }

      return result;
   }

   private BlockState stateForAutoBuild(TianshuAutoBuildPlan.Target target) {
      return switch (target) {
         case CASING -> ((TianshuSupercomputerStructureBlock)ModBlocks.TIANSHU_SUPERCOMPUTER_CASING.get()).m_49966_();
         case COOLING -> ((TianshuSupercomputerStructureBlock)ModBlocks.PHASE_CHANGE_COOLING_UNIT.get()).m_49966_();
         case GLASS -> ((TianshuSupercomputerGlassBlock)ModBlocks.TIANSHU_SUPERCOMPUTER_GLASS.get()).m_49966_();
         case PORT -> ((TianshuSupercomputerPortBlock)ModBlocks.TIANSHU_SUPERCOMPUTER_PORT.get()).m_49966_();
      };
   }

   private Map<Item, Integer> findMissingRequirements(Player player, Map<Item, Integer> requirements) {
      LinkedHashMap<Item, Integer> missing = new LinkedHashMap<>();

      for (Entry<Item, Integer> entry : requirements.entrySet()) {
         int available = this.countItem(player, entry.getKey());
         if (available < entry.getValue()) {
            missing.put(entry.getKey(), Integer.valueOf(entry.getValue() - available));
         }
      }

      return missing;
   }

   private int countItem(Player player, Item item) {
      int count = 0;
      Inventory inventory = player.m_150109_();

      for (int i = 0; i < inventory.m_6643_(); i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (stack.m_150930_(item)) {
            count += stack.m_41613_();
         }
      }

      return count;
   }

   private void consumeItem(Player player, Item item, int amount) {
      int remaining = amount;
      Inventory inventory = player.m_150109_();

      for (int i = 0; i < inventory.m_6643_() && remaining > 0; i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (stack.m_150930_(item)) {
            int consumed = Math.min(remaining, stack.m_41613_());
            stack.m_41774_(consumed);
            if (stack.m_41619_()) {
               inventory.m_6836_(i, ItemStack.f_41583_);
            }

            remaining -= consumed;
         }
      }
   }

   private Component describeBlockedPositions(List<BlockPos> localPositions) {
      MutableComponent result = Component.m_237119_();
      Direction facing = (Direction)this.m_58900_().m_61143_(TianshuSupercomputerControllerBlock.FACING);
      int visible = Math.min(localPositions.size(), 4);

      for (int i = 0; i < visible; i++) {
         if (i > 0) {
            result.m_130946_(", ");
         }

         result.m_7220_(this.describePosition(TianshuMultiblockScanner.worldPos(this.f_58858_, localPositions.get(i), facing)));
      }

      if (localPositions.size() > visible) {
         result.m_130946_(", ...");
      }

      return result;
   }

   private Component describeMissing(Map<Item, Integer> missing) {
      MutableComponent result = Component.m_237119_();
      int index = 0;

      for (Entry<Item, Integer> entry : missing.entrySet()) {
         if (index > 0) {
            result.m_130946_(", ");
         }

         result.m_7220_(entry.getKey().m_41466_()).m_130946_(" x").m_130946_(Integer.toString(entry.getValue()));
         index++;
      }

      return result;
   }

   private Component describePosition(BlockPos pos) {
      return Component.m_237113_("[" + pos.m_123341_() + ", " + pos.m_123342_() + ", " + pos.m_123343_() + "]");
   }

   private void form(TianshuMultiblockScanResult result) {
      if (!this.identityInitialized
         && this.f_58857_.m_7702_(result.portPos()) instanceof TianshuSupercomputerPortBlockEntity port
         && port.getLegacyTianshuId() != null) {
         this.identityInitialized = true;
         this.changeMachineId(port.getLegacyTianshuId());
      }

      if (!this.persistentStateOwner) {
         this.deform();
      } else {
         this.structureAvailable = true;
         this.waitingForChunks = false;
         this.nextChunkCheckTick = this.f_58857_.m_46467_() + 20L;
         if (this.formed
            && result.portPos().equals(this.portPos)
            && result.minPos().equals(this.minPos)
            && result.maxPos().equals(this.maxPos)
            && result.coreProfile().equals(this.coreProfile)
            && result.functionProfile().equals(this.functionProfile)
            && result.patternStoragePositions().equals(this.patternStoragePositions)
            && result.seedStoragePositions().equals(this.seedStoragePositions)) {
            for (BlockPos pos : result.members()) {
               this.setMemberFormed(pos, true);
            }

            this.bindFunctionalMembers(result);
            this.syncControllerState();
         } else {
            this.persistRuntimeStateIfChanged();
            this.clearStructureBindings();
            this.formed = true;
            this.portPos = result.portPos();
            this.minPos = result.minPos();
            this.maxPos = result.maxPos();
            this.memberCount = result.members().size();
            this.coreProfile = result.coreProfile();
            this.functionProfile = result.functionProfile();
            this.patternStoragePositions = result.patternStoragePositions();
            this.seedStoragePositions = result.seedStoragePositions();

            for (BlockPos pos : result.members()) {
               this.setMemberFormed(pos, true);
            }

            this.bindFunctionalMembers(result);
            this.syncControllerState();
         }
      }
   }

   private void bindFunctionalMembers(TianshuMultiblockScanResult result) {
      if (this.f_58857_.m_7702_(result.portPos()) instanceof TianshuSupercomputerPortBlockEntity port) {
         this.closedLoopDependencyChanges.reset();
         this.prepareRuntime(port);

         for (BlockPos patternStoragePos : result.patternStoragePositions()) {
            if (this.f_58857_.m_7702_(patternStoragePos) instanceof TianshuPatternStorageBlockEntity storage) {
               storage.bindToPort(this.portPos);
            }
         }

         for (BlockPos seedStoragePos : result.seedStoragePositions()) {
            if (this.f_58857_.m_7702_(seedStoragePos) instanceof TianshuSeedStorageBlockEntity drive) {
               drive.bindToPort(this.portPos);
            }
         }

         this.loadPatternsFromWarehouses(port);
         this.maintenance.functionCapacityChanged();
         this.cpuPool.resolvePendingLoad();
         port.bindToController(this.f_58858_, this.machineId, this.cpuPool, this.coreProfile.mainCore());

         for (BlockPos patternStoragePosx : result.patternStoragePositions()) {
            if (this.f_58857_.m_7702_(patternStoragePosx) instanceof TianshuPatternStorageBlockEntity storage) {
               storage.bindToPort(this.portPos);
            }
         }
      }
   }

   private void deform() {
      this.persistRuntimeStateIfChanged();
      this.clearStructureBindings();
      this.formed = false;
      this.structureAvailable = false;
      this.waitingForChunks = false;
      this.nextChunkCheckTick = 0L;
      this.portPos = null;
      this.minPos = null;
      this.maxPos = null;
      this.memberCount = 0;
      this.coreProfile = CpuInternalCoreProfile.empty();
      this.functionProfile = TianshuFunctionProfile.empty();
      this.patternStoragePositions = List.of();
      this.seedStoragePositions = List.of();
      this.syncControllerState();
   }

   public void clearStructureBindings() {
      if (this.f_58857_ != null && this.minPos != null && this.maxPos != null) {
         for (BlockPos mutable : BlockPos.m_121940_(this.minPos, this.maxPos)) {
            BlockPos pos = mutable.m_7949_();
            if (this.f_58857_.m_46749_(pos)) {
               this.setMemberFormed(pos, false);
               BlockEntity var7 = this.f_58857_.m_7702_(pos);
               if (var7 instanceof TianshuSupercomputerPortBlockEntity) {
                  TianshuSupercomputerPortBlockEntity port = (TianshuSupercomputerPortBlockEntity)var7;
                  if (this.f_58858_.equals(port.getControllerPos())) {
                     port.bindToController(null);
                     continue;
                  }
               }

               if (this.f_58857_.m_7702_(pos) instanceof TianshuPatternStorageBlockEntity storage) {
                  storage.bindToPort(null);
               } else if (this.f_58857_.m_7702_(pos) instanceof TianshuSeedStorageBlockEntity drive) {
                  drive.bindToPort(null);
               }
            }
         }
      }
   }

   private void setMemberFormed(BlockPos pos, boolean value) {
      if (this.f_58857_ != null && this.f_58857_.m_46749_(pos)) {
         BlockState state = this.f_58857_.m_8055_(pos);
         if (state.m_61138_(TianshuSupercomputerStructureBlock.FORMED) && (Boolean)state.m_61143_(TianshuSupercomputerStructureBlock.FORMED) != value) {
            this.f_58857_.m_7731_(pos, (BlockState)state.m_61124_(TianshuSupercomputerStructureBlock.FORMED, value), 2);
         } else if (state.m_61138_(TianshuSupercomputerPortBlock.FORMED) && (Boolean)state.m_61143_(TianshuSupercomputerPortBlock.FORMED) != value) {
            this.f_58857_.m_7731_(pos, (BlockState)state.m_61124_(TianshuSupercomputerPortBlock.FORMED, value), 2);
         }
      }
   }

   private void syncControllerState() {
      BlockState state = this.m_58900_();
      boolean activeFormed = this.isFormed();
      boolean activeWorking = activeFormed && this.hasActiveCpuTasks();
      if ((Boolean)state.m_61143_(TianshuSupercomputerControllerBlock.FORMED) != activeFormed
         || (Boolean)state.m_61143_(TianshuSupercomputerControllerBlock.WORKING) != activeWorking) {
         this.f_58857_
            .m_7731_(
               this.f_58858_,
               (BlockState)((BlockState)state.m_61124_(TianshuSupercomputerControllerBlock.FORMED, activeFormed))
                  .m_61124_(TianshuSupercomputerControllerBlock.WORKING, activeWorking),
               2
            );
      }

      this.m_6596_();
   }

   private void syncWorkingState() {
      BlockState state = this.m_58900_();
      boolean activeWorking = this.isFormed() && this.hasActiveCpuTasks();
      if ((Boolean)state.m_61143_(TianshuSupercomputerControllerBlock.WORKING) != activeWorking) {
         this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_(TianshuSupercomputerControllerBlock.WORKING, activeWorking), 2);
      }
   }

   private boolean hasActiveCpuTasks() {
      return !this.cpuPool.getActiveCpus().isEmpty();
   }

   private void prepareRuntime(TianshuSupercomputerPortBlockEntity port) {
      if (!this.machineId.equals(this.loadedRuntimeId)) {
         this.maintenance.shutdownCalculations();
         this.clearTransientRuntime();
         this.reconfigureCpuPool(this.coreProfile);
         this.loadRuntimeState(port);
      } else if (this.cpuPool.getTotalStorage() != this.coreProfile.storageBytes()
         || this.cpuPool.getCoProcessors() != this.coreProfile.coProcessors()
         || this.cpuPool.getMaxCopiesPerTick() != this.coreProfile.maxCopiesPerTick()
         || this.cpuPool.hasUnboundedBatch() != this.coreProfile.unboundedBatch()) {
         this.pendingStorage = this.coreProfile.storageBytes();
         this.pendingParallel = this.coreProfile.coProcessors();
         this.pendingMaxCopiesPerTick = this.coreProfile.maxCopiesPerTick();
         this.pendingUnboundedBatch = this.coreProfile.unboundedBatch();
         this.applyPendingProfile();
      }

      this.cpuPool.setFastPlanningEnabled(this.fastPlanningEnabled);
   }

   private void applyPendingProfile() {
      if (this.pendingStorage >= 0L && !this.cpuPool.hasPersistentState()) {
         this.cpuPool.reconfigure(this.pendingStorage, this.pendingParallel, this.pendingMaxCopiesPerTick, this.pendingUnboundedBatch);
         this.pendingStorage = -1L;
         this.pendingParallel = -1;
         this.pendingMaxCopiesPerTick = -1L;
         this.pendingUnboundedBatch = false;
      }
   }

   private void reconfigureCpuPool(CpuInternalCoreProfile profile) {
      this.cpuPool.reconfigure(profile.storageBytes(), profile.coProcessors(), profile.maxCopiesPerTick(), profile.unboundedBatch());
   }

   public ClosedLoopPatternRepository getClosedLoopPatternRepository() {
      return this.isFormed() && this.patternStorages() != null ? this.closedLoopPatterns : null;
   }

   public TianshuInventoryMaintenanceService getInventoryMaintenance() {
      return this.maintenance;
   }

   @Override
   public TimeWheelCraftingCpuPool getTimeWheelCraftingCpuPool() {
      return this.cpuPool;
   }

   @Override
   public boolean isCpuActive() {
      TianshuSupercomputerPortBlockEntity port = this.getLinkedPort();
      return this.persistentStateOwner && this.formed && port != null && port.isLinkActive();
   }

   @Override
   public IGrid getGrid() {
      TianshuSupercomputerPortBlockEntity port = this.getLinkedPort();
      return port != null ? port.getGrid() : null;
   }

   @Override
   public IActionSource getActionSource() {
      TianshuSupercomputerPortBlockEntity port = this.getPortLinkEndpoint();
      return port != null ? port.getActionSource() : IActionSource.empty();
   }

   @Override
   public IGridNode getActionableNode() {
      TianshuSupercomputerPortBlockEntity port = this.getPortLinkEndpoint();
      return port != null ? port.getMainNode().getNode() : null;
   }

   @Override
   public void markCpuDirty() {
      this.markRuntimeStateDirty();
   }

   @Override
   public Component getCpuDisplayName() {
      return Component.m_237115_("ae2lt.tianshu.cpu_name");
   }

   @Override
   public void maintenanceStateChanged() {
      this.markRuntimeStateDirty();
   }

   public ImmutableSet<ICraftingLink> getRequestedJobs() {
      return this.maintenance.getRequestedJobs();
   }

   public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
      return this.maintenance.insertCraftedItems(link, what, amount, mode);
   }

   public void jobStateChange(ICraftingLink link) {
      this.maintenance.jobStateChange(link);
   }

   @Override
   public long extractReusableSeed(AEKey key, long amount, Actionable mode) {
      if (this.isFormed() && this.functionProfile.supportsClosedLoopSeeds()) {
         List<TianshuSeedStorageBlockEntity> drives = this.seedDrives();
         if (drives == null) {
            return 0L;
         } else {
            long remaining = Math.max(0L, amount);
            long extracted = 0L;

            for (TianshuSeedStorageBlockEntity drive : drives) {
               long moved = drive.extract(key, remaining, mode, this.getActionSource());
               extracted = saturatingAdd(extracted, moved);
               remaining -= moved;
               if (remaining <= 0L) {
                  break;
               }
            }

            if (extracted > 0L && mode == Actionable.MODULATE) {
               this.seedStorageChanged();
            }

            return extracted;
         }
      } else {
         return 0L;
      }
   }

   @Override
   public KeyCounter extractReusableSeedVariants(AEKey planned, long amount, Predicate<AEKey> acceptsVariant, Actionable mode) {
      KeyCounter result = new KeyCounter();
      if (this.isFormed() && this.functionProfile.supportsClosedLoopSeeds() && planned != null && amount > 0L && acceptsVariant != null) {
         KeyCounter available = this.reusableSeedSnapshot();
         ArrayList<AEKey> candidates = new ArrayList<>();
         if (available.get(planned) > 0L && acceptsVariant.test(planned)) {
            candidates.add(planned);
         }

         for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> entry : available) {
            if (!((AEKey)entry.getKey()).equals(planned) && entry.getLongValue() > 0L && acceptsVariant.test((AEKey)entry.getKey())) {
               candidates.add((AEKey)entry.getKey());
            }
         }

         candidates.sort(Comparator.<AEKey, String>comparing(key -> key.getId().toString()).thenComparing(Object::toString));
         if (candidates.remove(planned)) {
            candidates.add(0, planned);
         }

         long remaining = amount;

         for (AEKey actual : candidates) {
            long extracted = this.extractReusableSeed(actual, remaining, mode);
            if (extracted > 0L) {
               result.add(actual, extracted);
               remaining -= extracted;
            }

            if (remaining <= 0L) {
               break;
            }
         }

         return result;
      } else {
         return result;
      }
   }

   @Override
   public long insertReusableSeed(AEKey key, long amount, Actionable mode) {
      if (this.isFormed() && this.functionProfile.supportsClosedLoopSeeds()) {
         List<TianshuSeedStorageBlockEntity> drives = this.seedDrives();
         if (drives == null) {
            return 0L;
         } else {
            long remaining = Math.max(0L, amount);
            long inserted = 0L;

            for (TianshuSeedStorageBlockEntity drive : drives) {
               long moved = drive.insert(key, remaining, mode, this.getActionSource());
               inserted = saturatingAdd(inserted, moved);
               remaining -= moved;
               if (remaining <= 0L) {
                  break;
               }
            }

            if (inserted > 0L && mode == Actionable.MODULATE) {
               this.seedStorageChanged();
            }

            return inserted;
         }
      } else {
         return 0L;
      }
   }

   public long reusableSeedAmount(AEKey key) {
      if (this.isFormed() && this.functionProfile.supportsClosedLoopSeeds()) {
         List<TianshuSeedStorageBlockEntity> drives = this.seedDrives();
         if (drives == null) {
            return 0L;
         } else {
            long amount = 0L;

            for (TianshuSeedStorageBlockEntity drive : drives) {
               amount = saturatingAdd(amount, drive.amount(key, this.getActionSource()));
            }

            return amount;
         }
      } else {
         return 0L;
      }
   }

   public void seedStorageChanged() {
      TianshuSupercomputerPortBlockEntity port = this.getLinkedPort();
      if (port != null) {
         port.refreshCraftingProvider();
      }
   }

   public void closedLoopPatternsChanged() {
      this.decodedClosedLoopPatterns.clear();
      if (this.persistPatternsToWarehouses()) {
         TianshuSupercomputerPortBlockEntity port = this.getLinkedPort();
         if (port != null) {
            port.refreshCraftingProvider();
         }
      }
   }

   public void patternWarehouseChanged() {
      if (this.reloadPatternsFromWarehouses()) {
         TianshuSupercomputerPortBlockEntity port = this.getLinkedPort();
         if (port != null) {
            port.refreshCraftingProvider();
         }
      }
   }

   public List<IPatternDetails> getAvailablePatterns() {
      TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatterns available = this.collectAvailablePatterns();
      this.publishedClosedLoopPatternDefinitions = available.patternDefinitions();
      return available.patterns();
   }

   private TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatterns collectAvailablePatterns() {
      if (!this.closedLoopPatternsReadyForPublication()) {
         return TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatterns.EMPTY;
      } else {
         ArrayList<IPatternDetails> result = new ArrayList<>();
         LinkedHashSet<AEItemKey> patternDefinitions = new LinkedHashSet<>();
         ClosedLoopPublicationSupport.SeedSnapshotMemoizer availableSeeds = new ClosedLoopPublicationSupport.SeedSnapshotMemoizer(this::availableSeedsFor);

         for (ClosedLoopPatternPayload payload : this.closedLoopPatterns.activePatterns()) {
            TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatternCandidate candidate = this.availableClosedLoopPatternCandidate(payload);
            if (candidate != null) {
               IPatternDetails details = this.createAvailableClosedLoopPatternDetails(candidate, availableSeeds);
               if (details != null) {
                  result.add(details);
                  patternDefinitions.add(details.getDefinition());
               }
            }
         }

         return new TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatterns(List.copyOf(result), Set.copyOf(patternDefinitions));
      }
   }

   private Set<AEItemKey> collectAvailablePatternDefinitionsForDependencyChanges() {
      if (!this.closedLoopPatternsReadyForPublication()) {
         return Set.of();
      } else {
         LinkedHashSet<AEItemKey> result = new LinkedHashSet<>();
         ClosedLoopPublicationSupport.SeedSnapshotMemoizer availableSeeds = new ClosedLoopPublicationSupport.SeedSnapshotMemoizer(this::availableSeedsFor);

         for (ClosedLoopPatternPayload payload : this.closedLoopPatterns.activePatterns()) {
            TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatternCandidate candidate = this.availableClosedLoopPatternCandidate(payload);
            if (candidate != null) {
               AEItemKey definition = ClosedLoopPublicationSupport.reusePublishedOrValidate(
                  candidate.definition(), this.publishedClosedLoopPatternDefinitions, () -> {
                     IPatternDetails details = this.createAvailableClosedLoopPatternDetails(candidate, availableSeeds);
                     return details != null ? details.getDefinition() : null;
                  }
               );
               if (definition != null) {
                  result.add(definition);
               }
            }
         }

         return Set.copyOf(result);
      }
   }

   private boolean closedLoopPatternsReadyForPublication() {
      return this.isFormed()
         && this.f_58857_ != null
         && this.functionProfile.supportsClosedLoopPatterns()
         && this.functionProfile.closedLoopPatternCapacity() > 0
         && this.patternStorages() != null
         && (!this.functionProfile.supportsClosedLoopSeeds() || this.seedDrives() != null);
   }

   private TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatternCandidate availableClosedLoopPatternCandidate(ClosedLoopPatternPayload payload) {
      if (!payload.enabled()) {
         return null;
      } else {
         ClosedLoopPatternDecoder.DecodedPayload decoded = this.decodedClosedLoopPatterns
            .computeIfAbsent(payload, candidate -> ClosedLoopPatternDecoder.decodePayload(candidate, this.f_58857_));
         if (decoded.valid() && this.membersAreAvailable(decoded.members())) {
            ClosedLoopPatternItem item = (ClosedLoopPatternItem)ModItems.CLOSED_LOOP_PATTERN.get();
            AEItemKey definition = AEItemKey.of(item.createStack(payload, this.f_58857_.m_9598_()));
            return definition != null ? new TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatternCandidate(definition, decoded) : null;
         } else {
            return null;
         }
      }
   }

   private IPatternDetails createAvailableClosedLoopPatternDetails(
      TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatternCandidate candidate, Function<ReusableSeedPattern, Map<AEKey, Long>> availableSeeds
   ) {
      try {
         return candidate.decoded().createDetails(candidate.definition(), this.f_58857_, this.machineId, availableSeeds);
      } catch (RuntimeException var4) {
         return null;
      }
   }

   private void refreshClosedLoopProviderForDependencyChanges(TianshuSupercomputerPortBlockEntity port) {
      IGrid grid = this.getGrid();
      if (grid != null && this.closedLoopDependencyChanges.shouldRecheck(grid.getCraftingService())) {
         Set<AEItemKey> availableDefinitions = this.collectAvailablePatternDefinitionsForDependencyChanges();
         if (!availableDefinitions.equals(this.publishedClosedLoopPatternDefinitions)) {
            port.refreshCraftingProvider();
         }
      }
   }

   private Map<AEKey, Long> availableSeedsFor(ReusableSeedPattern ignoredPattern) {
      KeyCounter available = this.reusableSeedSnapshot();
      LinkedHashMap<AEKey, Long> variants = new LinkedHashMap<>();

      for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> candidate : available) {
         if (candidate.getLongValue() > 0L) {
            variants.put((AEKey)candidate.getKey(), Long.valueOf(candidate.getLongValue()));
         }
      }

      return Map.copyOf(variants);
   }

   private boolean membersAreAvailable(List<IPatternDetails> members) {
      IGrid grid = this.getGrid();
      if (grid == null) {
         return false;
      } else {
         CraftingService crafting = (CraftingService)grid.getCraftingService();

         for (IPatternDetails details : members) {
            IPatternDetails providerPattern = CraftingPatternDelegates.forProviderLookup(details);
            if (!crafting.getProviders(providerPattern).iterator().hasNext()) {
               return false;
            }
         }

         return true;
      }
   }

   public void persistRuntimeStateIfChanged() {
      if (this.persistentStateOwner && this.machineId.equals(this.loadedRuntimeId) && this.f_58857_ instanceof ServerLevel serverLevel) {
         CompoundTag var3 = this.createRuntimeStateSnapshot(serverLevel);
         ControllerMachineStateSavedData.get(serverLevel).setOwnedState(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId, var3);
         this.runtimeStateDirty = false;
      }
   }

   private CompoundTag createRuntimeStateSnapshot(ServerLevel serverLevel) {
      CompoundTag state = new CompoundTag();
      CompoundTag maintenanceTag = new CompoundTag();
      this.maintenance.writeTo(maintenanceTag, serverLevel.m_9598_());
      state.m_128365_("InventoryMaintenance", maintenanceTag);
      if (this.cpuPool.hasPersistentState()) {
         CompoundTag poolTag = new CompoundTag();
         this.cpuPool.writeToNBT(poolTag, serverLevel.m_9598_());
         if (!poolTag.m_128456_()) {
            state.m_128365_("CpuPool", poolTag);
         }
      }

      return state;
   }

   private void markRuntimeStateDirty() {
      if (!this.runtimeStateDirty) {
         if (this.persistentStateOwner && this.machineId.equals(this.loadedRuntimeId) && this.f_58857_ instanceof ServerLevel serverLevel) {
            this.runtimeStateDirty = true;
            ControllerMachineStateSavedData.get(serverLevel)
               .deferStateSnapshot(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId, this::takeDeferredRuntimeStateSnapshot);
         }
      }
   }

   private void flushRuntimeStateIfDirty() {
      if (this.runtimeStateDirty) {
         this.persistRuntimeStateIfChanged();
      }
   }

   private CompoundTag takeDeferredRuntimeStateSnapshot() {
      if (this.runtimeStateDirty
         && this.persistentStateOwner
         && this.machineId.equals(this.loadedRuntimeId)
         && this.f_58857_ instanceof ServerLevel serverLevel) {
         CompoundTag var3 = this.createRuntimeStateSnapshot(serverLevel);
         this.runtimeStateDirty = false;
         return var3;
      } else {
         return null;
      }
   }

   private void discardPendingRuntimeState() {
      if (this.runtimeStateDirty && this.f_58857_ instanceof ServerLevel serverLevel) {
         ControllerMachineStateSavedData.get(serverLevel).cancelDeferredStateSnapshot(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId);
      }

      this.runtimeStateDirty = false;
   }

   public void prepareForControllerRemoval() {
      if (this.persistentStateOwner && this.f_58857_ instanceof ServerLevel) {
         this.cpuPool.tryReleaseContents();
         this.persistRuntimeStateIfChanged();
      }
   }

   private void loadRuntimeState(TianshuSupercomputerPortBlockEntity port) {
      if (this.f_58857_ instanceof ServerLevel serverLevel) {
         ControllerMachineStateSavedData data = ControllerMachineStateSavedData.get(serverLevel);
         boolean stored = data.hasState(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId);
         CompoundTag legacy = port.copyLegacyRuntimeState();
         CompoundTag state = stored
            ? data.getState(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId)
            : (legacy != null ? legacy : new CompoundTag());
         this.maintenance.readFrom(state.m_128469_("InventoryMaintenance"), serverLevel.m_9598_());
         this.cpuPool.readFromNBT(state.m_128469_("CpuPool"), serverLevel.m_9598_());
         this.loadedRuntimeId = this.machineId;
         if (!stored) {
            this.markRuntimeStateDirty();
         }

         if (legacy != null) {
            port.consumeLegacyRuntimeState();
         }
      }
   }

   private void clearTransientRuntime() {
      this.discardPendingRuntimeState();
      this.loadedRuntimeId = null;
      if (this.f_58857_ != null) {
         this.maintenance.readFrom(new CompoundTag(), this.f_58857_.m_9598_());
         this.cpuPool.readFromNBT(new CompoundTag(), this.f_58857_.m_9598_());
         this.closedLoopPatterns.clear();
         this.decodedClosedLoopPatterns.clear();
         this.pendingStorage = -1L;
         this.pendingParallel = -1;
         this.pendingMaxCopiesPerTick = -1L;
         this.pendingUnboundedBatch = false;
      }
   }

   private void suspendRuntime() {
      this.maintenance.shutdownCalculations();
      this.clearTransientRuntime();
   }

   private void loadPatternsFromWarehouses(TianshuSupercomputerPortBlockEntity port) {
      if (this.reloadPatternsFromWarehouses()) {
         CompoundTag legacyState = port.copyLegacyPatternState();
         if (legacyState != null && this.f_58857_ != null) {
            ClosedLoopPatternRepository legacy = new ClosedLoopPatternRepository(() -> Integer.MAX_VALUE);
            legacy.readFrom(legacyState);
            ClosedLoopPatternItem item = (ClosedLoopPatternItem)ModItems.CLOSED_LOOP_PATTERN.get();

            for (ClosedLoopPatternPayload payload : legacy.patterns()) {
               ClosedLoopPatternRepository.PutResult result = this.closedLoopPatterns.add(payload);
               if (result == ClosedLoopPatternRepository.PutResult.FULL || result == ClosedLoopPatternRepository.PutResult.UNAVAILABLE) {
                  NativeStackDropHelper.popResource(this.f_58857_, port.m_58899_(), item.createStack(payload, this.f_58857_.m_9598_()));
               }
            }

            if (this.persistPatternsToWarehouses()) {
               port.consumeLegacyPatternState();
            }
         }
      }
   }

   private boolean reloadPatternsFromWarehouses() {
      List<TianshuPatternStorageBlockEntity> storages = this.patternStorages();
      if (storages == null) {
         return false;
      } else {
         ArrayList<ClosedLoopPatternPayload> merged = new ArrayList<>();

         for (TianshuPatternStorageBlockEntity storage : storages) {
            merged.addAll(storage.patterns());
         }

         this.closedLoopPatterns.replaceAll(merged);
         this.decodedClosedLoopPatterns.clear();
         return true;
      }
   }

   private boolean persistPatternsToWarehouses() {
      List<TianshuPatternStorageBlockEntity> storages = this.patternStorages();
      if (storages == null) {
         return false;
      } else {
         List<ClosedLoopPatternPayload> patterns = this.closedLoopPatterns.patterns();
         int offset = 0;

         for (TianshuPatternStorageBlockEntity storage : storages) {
            int end = Math.min(patterns.size(), offset + 36);
            storage.replacePatterns(offset < end ? patterns.subList(offset, end) : List.of());
            offset = end;
         }

         return true;
      }
   }

   private KeyCounter reusableSeedSnapshot() {
      KeyCounter result = new KeyCounter();
      List<TianshuSeedStorageBlockEntity> drives = this.seedDrives();
      if (drives != null) {
         for (TianshuSeedStorageBlockEntity drive : drives) {
            drive.getAvailableStacks(result);
         }
      }

      return result;
   }

   private List<TianshuSeedStorageBlockEntity> seedDrives() {
      if (this.f_58857_ == null) {
         return null;
      } else {
         Optional<List<TianshuSeedStorageBlockEntity>> result = CompletePhysicalStorageSet.resolve(
            this.seedStoragePositions,
            pos -> {
               if (!this.f_58857_.m_46749_(pos)) {
                  return null;
               } else {
                  if (this.f_58857_.m_7702_(pos) instanceof TianshuSeedStorageBlockEntity drive
                     && !drive.m_58901_()
                     && this.portPos != null
                     && this.portPos.equals(drive.getPortPos())) {
                     return drive;
                  }

                  return null;
               }
            }
         );
         if (result.isEmpty()) {
            this.suspendForUnavailablePhysicalStorage();
         }

         return result.orElse(null);
      }
   }

   private List<TianshuPatternStorageBlockEntity> patternStorages() {
      if (this.f_58857_ == null) {
         return null;
      } else {
         Optional<List<TianshuPatternStorageBlockEntity>> result = CompletePhysicalStorageSet.resolve(
            this.patternStoragePositions,
            pos -> {
               if (!this.f_58857_.m_46749_(pos)) {
                  return null;
               } else {
                  if (this.f_58857_.m_7702_(pos) instanceof TianshuPatternStorageBlockEntity storage
                     && !storage.m_58901_()
                     && this.portPos != null
                     && this.portPos.equals(storage.getPortPos())) {
                     return storage;
                  }

                  return null;
               }
            }
         );
         if (result.isEmpty()) {
            this.suspendForUnavailablePhysicalStorage();
         }

         return result.orElse(null);
      }
   }

   private void suspendForUnavailablePhysicalStorage() {
      this.lastIssues = List.of(TianshuMultiblockScanIssue.CHUNKS_UNLOADED);
      this.suspendForUnloadedChunks();
      if (this.f_58857_ != null) {
         this.nextChunkCheckTick = this.f_58857_.m_46467_();
      }
   }

   private TianshuSupercomputerPortBlockEntity getLinkedPort() {
      TianshuSupercomputerPortBlockEntity port = this.getPortLinkEndpoint();
      return port != null && port.isLinkedTo(this.f_58858_, this.machineId) ? port : null;
   }

   private TianshuSupercomputerPortBlockEntity getPortLinkEndpoint() {
      if (this.formed && this.portPos != null && this.f_58857_ != null && this.f_58857_.m_46749_(this.portPos)) {
         return this.f_58857_.m_7702_(this.portPos) instanceof TianshuSupercomputerPortBlockEntity port ? port : null;
      } else {
         return null;
      }
   }

   private static long saturatingAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left >= Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }

   private void checkChunkAvailability() {
      if (this.f_58857_ != null && (this.formed || this.waitingForChunks)) {
         Direction facing = (Direction)this.m_58900_().m_61143_(TianshuSupercomputerControllerBlock.FACING);
         if (!TianshuMultiblockScanner.areRequiredChunksLoaded(this.f_58857_, this.f_58858_, facing)) {
            this.lastIssues = List.of(TianshuMultiblockScanIssue.CHUNKS_UNLOADED);
            this.suspendForUnloadedChunks();
         } else if (!this.structureAvailable || this.waitingForChunks) {
            this.scheduleStructureCheck();
         }
      }
   }

   private void suspendForUnloadedChunks() {
      boolean changed = !this.waitingForChunks || this.structureAvailable;
      this.waitingForChunks = true;
      this.structureAvailable = false;
      if (changed
         && this.f_58857_ != null
         && this.portPos != null
         && this.f_58857_.m_46749_(this.portPos)
         && this.f_58857_.m_7702_(this.portPos) instanceof TianshuSupercomputerPortBlockEntity port) {
         port.suspendFromController(this.f_58858_);
      }

      if (changed && this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.syncControllerState();
      }
   }

   private boolean ensureStructureChunksLoaded() {
      Direction facing = (Direction)this.m_58900_().m_61143_(TianshuSupercomputerControllerBlock.FACING);
      if (TianshuMultiblockScanner.areRequiredChunksLoaded(this.f_58857_, this.f_58858_, facing)) {
         return true;
      } else {
         this.lastIssues = List.of(TianshuMultiblockScanIssue.CHUNKS_UNLOADED);
         this.suspendForUnloadedChunks();
         return false;
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.flushRuntimeStateIfDirty();
      tag.m_128379_("Formed", this.formed);
      if (this.portPos != null) {
         tag.m_128356_("PortPos", this.portPos.m_121878_());
      }

      if (this.minPos != null) {
         tag.m_128356_("MinPos", this.minPos.m_121878_());
      }

      if (this.maxPos != null) {
         tag.m_128356_("MaxPos", this.maxPos.m_121878_());
      }

      tag.m_128405_("MemberCount", this.memberCount);
      tag.m_128379_("FastPlanning", this.fastPlanningEnabled);
      tag.m_128405_("CpuPriority", this.cpuPriority);
      CompoundTag algorithmProviderTag = new CompoundTag();
      this.algorithmProvider.writeToNBT(algorithmProviderTag);
      tag.m_128365_("CraftingAlgorithmProvider", algorithmProviderTag);
      if (this.coreProfile.mainCore() != null) {
         tag.m_128359_("MainCore", this.coreProfile.mainCore().name());
         tag.m_128405_("CapacityCores", this.coreProfile.storageUnitCount());
         tag.m_128405_("ParallelCores", this.coreProfile.parallelUnitCount());
         tag.m_128405_("AmplifierCores", this.coreProfile.amplifierUnitCount());
      }

      tag.m_128405_("ClosedLoopStorages", this.functionProfile.closedLoopPatternStorageCount());
      tag.m_128405_("SeedStorages", this.functionProfile.closedLoopSeedStorageCount());
      tag.m_128362_("MachineId", this.machineId);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.formed = tag.m_128471_("Formed");
      this.structureAvailable = false;
      this.waitingForChunks = false;
      this.nextChunkCheckTick = 0L;
      this.portPos = tag.m_128425_("PortPos", 4) ? BlockPos.m_122022_(tag.m_128454_("PortPos")) : null;
      this.minPos = tag.m_128425_("MinPos", 4) ? BlockPos.m_122022_(tag.m_128454_("MinPos")) : null;
      this.maxPos = tag.m_128425_("MaxPos", 4) ? BlockPos.m_122022_(tag.m_128454_("MaxPos")) : null;
      this.memberCount = tag.m_128451_("MemberCount");
      this.fastPlanningEnabled = !tag.m_128425_("FastPlanning", 1) || tag.m_128471_("FastPlanning");
      this.cpuPriority = tag.m_128451_("CpuPriority");
      this.cpuPool.setFastPlanningEnabled(this.fastPlanningEnabled);
      if (tag.m_128425_("CraftingAlgorithmProvider", 10)) {
         this.algorithmProvider.readFromNBT(tag.m_128469_("CraftingAlgorithmProvider"));
      }

      if (tag.m_128425_("MainCore", 8)) {
         try {
            CpuMainCoreTier tier = CpuMainCoreTier.valueOf(tag.m_128461_("MainCore"));
            this.coreProfile = CpuInternalCoreCalculator.calculate(
               tier, tag.m_128451_("CapacityCores"), tag.m_128451_("ParallelCores"), tag.m_128451_("AmplifierCores")
            );
         } catch (IllegalArgumentException var3) {
            this.coreProfile = CpuInternalCoreProfile.empty();
         }
      }

      this.functionProfile = new TianshuFunctionProfile(Math.max(0, tag.m_128451_("ClosedLoopStorages")), Math.max(0, tag.m_128451_("SeedStorages")));
      this.identityInitialized = tag.m_128403_("MachineId");
      if (this.identityInitialized) {
         this.machineId = tag.m_128342_("MachineId");
      }
   }

   public void onLoad() {
      super.onLoad();
      this.claimPersistentState();
      this.nextChunkCheckTick = this.f_58857_ != null ? this.f_58857_.m_46467_() : 0L;
      this.scheduleStructureCheck();
   }

   public void onChunkUnloaded() {
      this.persistRuntimeStateIfChanged();
      this.suspendRuntime();
      this.releasePersistentState();
      super.onChunkUnloaded();
   }

   public void m_7651_() {
      this.persistRuntimeStateIfChanged();
      this.suspendRuntime();
      this.releasePersistentState();
      super.m_7651_();
   }

   private void changeMachineId(UUID newId) {
      if (newId != null && !newId.equals(this.machineId)) {
         this.persistRuntimeStateIfChanged();
         this.suspendRuntime();
         this.releasePersistentState();
         this.machineId = newId;
         this.claimPersistentState();
      }
   }

   private void claimPersistentState() {
      if (this.f_58857_ instanceof ServerLevel serverLevel) {
         this.persistentStateOwner = ControllerMachineStateSavedData.get(serverLevel)
            .claim(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId, serverLevel, this.f_58858_);
      }
   }

   private void releasePersistentState() {
      if (this.persistentStateOwner && this.f_58857_ instanceof ServerLevel serverLevel) {
         ControllerMachineStateSavedData.get(serverLevel)
            .release(ControllerMachineStateSavedData.MachineType.TIANSHU, this.machineId, serverLevel, this.f_58858_);
      }

      this.persistentStateOwner = false;
   }

   private static record AvailableClosedLoopPatternCandidate(AEItemKey definition, ClosedLoopPatternDecoder.DecodedPayload decoded) {
   }

   private static record AvailableClosedLoopPatterns(List<IPatternDetails> patterns, Set<AEItemKey> patternDefinitions) {
      private static final TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatterns EMPTY = new TianshuSupercomputerControllerBlockEntity.AvailableClosedLoopPatterns(
         List.of(), Set.of()
      );
   }
}
