package com.moakiee.ae2lt.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.moakiee.ae2lt.AE2LightningTech;
import com.moakiee.ae2lt.block.MatrixCasingBlock;
import com.moakiee.ae2lt.block.MatrixControllerBlock;
import com.moakiee.ae2lt.block.MatrixFormedBlock;
import com.moakiee.ae2lt.block.MatrixGlassBlock;
import com.moakiee.ae2lt.block.MatrixMultiblockComponentBlock;
import com.moakiee.ae2lt.block.MatrixMultiblockDirectionalBlock;
import com.moakiee.ae2lt.block.MatrixPatternStorageBlock;
import com.moakiee.ae2lt.block.MatrixPortBlock;
import com.moakiee.ae2lt.crafting.matrix.core.CraftingCoreHost;
import com.moakiee.ae2lt.crafting.matrix.core.MolecularCopyAssembler;
import com.moakiee.ae2lt.logic.craft.MatrixAutoBuildPlan;
import com.moakiee.ae2lt.logic.craft.MatrixCraftCore;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingCluster;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingEnergy;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingMath;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingProfile;
import com.moakiee.ae2lt.logic.craft.MatrixCraftingUnit;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockComponent;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockMember;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockScanAttempt;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockScanIssue;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockScanResult;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockScanner;
import com.moakiee.ae2lt.logic.craft.MatrixMultiblockTemplate;
import com.moakiee.ae2lt.logic.craft.MatrixPatternCore;
import com.moakiee.ae2lt.logic.persistence.CompletePhysicalStorageSet;
import com.moakiee.ae2lt.logic.persistence.ControllerMachineIdentity;
import com.moakiee.ae2lt.logic.persistence.ControllerMachineStateSavedData;
import com.moakiee.ae2lt.network.MatrixControllerActionPacket;
import com.moakiee.ae2lt.registry.ModBlockEntities;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.moakiee.ae2lt.registry.ModItems;
import com.moakiee.ae2lt.util.NativeStackDropHelper;
import com.moakiee.thunderbolt.api.crafting.batch.BatchDispatchMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MatrixControllerBlockEntity extends BlockEntity implements CraftingCoreHost, MatrixCraftingEnergy {
   private static final double POWER_EPSILON = 0.01;
   private static final long NO_SCHEDULED_SCAN = Long.MIN_VALUE;
   private static final int AUTO_BUILD_INTERVAL_TICKS = 1;
   private static final int CHUNK_RECHECK_INTERVAL_TICKS = 20;
   private static final String TAG_FORMED = "Formed";
   private static final String TAG_ORIENTATION = "Orientation";
   private static final String TAG_PORT_POS = "PortPos";
   private static final String TAG_MIN_POS = "MinPos";
   private static final String TAG_MAX_POS = "MaxPos";
   private static final String TAG_MEMBER_COUNT = "MemberCount";
   private static final String TAG_PATTERN_STORAGE_COUNT = "PatternStorageCount";
   private static final String TAG_CRAFTING_UNIT_COUNT = "CraftingUnitCount";
   private static final String TAG_MACHINE_ID = "MachineId";
   private boolean formed;
   private Direction orientation = Direction.NORTH;
   private BlockPos portPos;
   private BlockPos minPos;
   private BlockPos maxPos;
   private int memberCount;
   private int patternStorageCount;
   private int craftingUnitCount;
   private MatrixMultiblockScanIssue primaryScanIssue;
   private UUID machineId = UUID.randomUUID();
   private boolean persistentStateOwner;
   private final MatrixPatternCore patternCore = new MatrixPatternCore() {
      @Override
      public List<IPatternDetails> getAvailablePatterns() {
         return MatrixControllerBlockEntity.this.collectAvailablePatterns();
      }

      @Override
      public boolean hasPattern(IPatternDetails details) {
         return MatrixControllerBlockEntity.this.hasAvailablePattern(details);
      }
   };
   private final MatrixCraftCore craftCore = new MatrixCraftCore() {
      @Override
      public List<MatrixCraftingUnit> craftingUnits() {
         return MatrixControllerBlockEntity.this.findCraftingUnits();
      }
   };
   private final MatrixCraftingCluster cluster = new MatrixCraftingCluster(
      this::isFormed,
      List.of(this.patternCore),
      List.of(this.craftCore),
      this,
      new MolecularCopyAssembler(this::m_58904_),
      AE2LightningTech.craftingCoreRegistry(),
      this
   );
   private UUID loadedRuntimeId;
   private List<BlockPos> patternStoragePositions = List.of();
   private List<MatrixPatternStorageBlockEntity> cachedPatternStorages = List.of();
   private List<MatrixControllerBlockEntity.CraftingUnitCacheEntry> craftingUnitCacheEntries = List.of();
   private List<MatrixCraftingUnit> cachedCraftingUnits = List.of();
   private boolean structureCacheValid;
   private boolean structureCacheValidationRequired = true;
   private boolean lastStructureCacheValidationResult;
   private long lastStructureCacheValidationTick = Long.MIN_VALUE;
   private boolean structureAvailable;
   private boolean waitingForChunks;
   private long scheduledScanTick = Long.MIN_VALUE;
   private long nextChunkCheckTick;
   private List<MatrixAutoBuildPlan.Placement> autoBuildPlacements = List.of();
   private UUID autoBuildPlayerId;
   private Direction autoBuildFacing = Direction.NORTH;
   private int autoBuildPlacementIndex;
   private int autoBuildPlacedBlocks;
   private long nextAutoBuildTick;
   private long lastObservedClusterThreads = -1L;

   public MatrixControllerBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.MATRIX_CONTROLLER.get(), pos, blockState);
      this.orientation = this.orientationFromState(blockState);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, MatrixControllerBlockEntity be) {
      if (!level.f_46443_) {
         if (!be.persistentStateOwner) {
            if (be.formed) {
               be.deform();
            }
         } else {
            if ((be.formed || be.waitingForChunks) && level.m_46467_() >= be.nextChunkCheckTick) {
               be.nextChunkCheckTick = level.m_46467_() + 20L;
               be.checkChunkAvailability();
            }

            if (be.formed && be.structureAvailable && !be.structureCacheValid) {
               be.scheduleStructureCheck();
            }

            if (be.isAutoBuilding()) {
               be.tickAutoBuild();
            } else if (be.scheduledScanTick != Long.MIN_VALUE && level.m_46467_() >= be.scheduledScanTick) {
               be.scheduledScanTick = Long.MIN_VALUE;
               be.refreshStructure();
            }

            if (be.isFormed()) {
               be.cluster.tickLimiter();
            }

            be.persistRuntimeStateIfChanged();
            long clusterThreads = be.cluster.threadsInFlight();
            if (clusterThreads > 0L || clusterThreads != be.lastObservedClusterThreads) {
               be.m_6596_();
            }

            be.lastObservedClusterThreads = clusterThreads;
            be.syncRenderState();
         }
      }
   }

   public boolean isFormed() {
      return this.formed && this.structureAvailable;
   }

   public Direction getOrientation() {
      return this.orientationFromState(this.m_58900_());
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

   public int getMemberCount() {
      return this.memberCount;
   }

   public int getPatternStorageCount() {
      return this.patternStorageCount;
   }

   public int getCraftingUnitCount() {
      return this.craftingUnitCount;
   }

   public int getPrimaryIssueOrdinal() {
      return this.primaryScanIssue == null ? -1 : this.primaryScanIssue.ordinal();
   }

   public UUID getMachineId() {
      return this.machineId;
   }

   public boolean isPersistentStateOwner() {
      return this.persistentStateOwner;
   }

   public void initializeIdentityFromItem(ItemStack stack) {
      UUID itemId = ControllerMachineIdentity.read(stack);
      if (itemId != null && !itemId.equals(this.machineId)) {
         this.persistRuntimeStateIfChanged();
         this.suspendRuntime();
         this.releasePersistentState();
         this.machineId = itemId;
      }

      this.claimPersistentState();
      this.m_6596_();
   }

   public int getPatternSlotCount() {
      int total = 0;

      for (MatrixPatternStorageBlockEntity storage : this.findPatternStorages()) {
         total += storage.capacity();
      }

      return total;
   }

   public MatrixCraftingProfile getCraftingProfile() {
      return this.cluster.craftingProfile();
   }

   public MatrixCraftingMath.Snapshot getLimiterSnapshot() {
      return this.cluster.previewSnapshot();
   }

   public int getAvailableProviderCalls() {
      return this.cluster.availableProviderCalls();
   }

   public boolean isCraftingBusy() {
      return this.cluster.isBusy();
   }

   public void performAction(MatrixControllerActionPacket.Action action, ServerPlayer player) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && this.persistentStateOwner) {
         switch (action) {
            case AUTO_BUILD:
               this.autoBuild(player);
               break;
            case UPGRADE_PATTERN_STORAGE:
               this.upgradePatternStorage(player);
         }
      }
   }

   public void scheduleStructureCheck() {
      this.scheduleStructureCheck(1L);
   }

   private void scheduleStructureCheck(long delayTicks) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.structureCacheValidationRequired = true;
         long targetTick = this.f_58857_.m_46467_() + Math.max(1L, delayTicks);
         if (this.scheduledScanTick == Long.MIN_VALUE || targetTick < this.scheduledScanTick) {
            this.scheduledScanTick = targetTick;
         }

         this.m_6596_();
      }
   }

   public void clearStructureBindings() {
      this.setBoundsConnectedTextureFormed(false);
      this.clearBindingsInStoredBounds();
   }

   public void scanAndForm(ServerPlayer player) {
      if (this.persistentStateOwner) {
         MatrixMultiblockScanAttempt attempt = this.scanCurrent();
         if (attempt.chunksUnavailable()) {
            this.suspendForUnloadedChunks();
         } else if (!attempt.formed()) {
            this.deform();
            player.m_5661_(Component.m_237110_("ae2lt.matrix.scan_failed", new Object[]{this.describeIssues(attempt)}).m_130940_(ChatFormatting.RED), true);
         } else {
            this.form(attempt.result());
            player.m_5661_(
               Component.m_237110_("ae2lt.matrix.formed", new Object[]{this.memberCount, this.patternStorageCount, this.craftingUnitCount})
                  .m_130940_(ChatFormatting.GREEN),
               true
            );
         }
      }
   }

   public void autoBuild(ServerPlayer player) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         if (this.isAutoBuilding()) {
            player.m_5661_(Component.m_237115_("ae2lt.matrix.build_in_progress").m_130940_(ChatFormatting.YELLOW), true);
         } else if (this.ensureStructureChunksLoaded()) {
            int patternStorageBudget = player.m_150110_().f_35937_ ? Integer.MAX_VALUE : this.countPatternStorageItems(player);
            MatrixAutoBuildPlan plan = this.createAutoBuildPlan(patternStorageBudget);
            if (!plan.blocked().isEmpty()) {
               player.m_5661_(
                  Component.m_237110_("ae2lt.matrix.build_blocked", new Object[]{plan.blocked().size(), this.describeBlockedPositions(plan.blocked())})
                     .m_130940_(ChatFormatting.RED),
                  false
               );
            } else {
               Map<Item, Integer> requirements = this.autoBuildRequirementsForMissingBlocks(plan);
               if (!player.m_150110_().f_35937_) {
                  Map<Item, Integer> missing = this.findMissingRequirements(player, requirements);
                  if (!missing.isEmpty() || plan.missingPatternStorages() > 0) {
                     player.m_5661_(
                        Component.m_237110_("ae2lt.matrix.build_missing", new Object[]{this.describeMissing(missing, plan.missingPatternStorages())})
                           .m_130940_(ChatFormatting.RED),
                        false
                     );
                     return;
                  }
               }

               if (plan.placements().isEmpty()) {
                  this.finishAutoBuild(player, 0);
               } else {
                  this.autoBuildPlacements = plan.placements();
                  this.autoBuildPlayerId = player.m_20148_();
                  this.autoBuildFacing = this.getOrientation();
                  this.autoBuildPlacementIndex = 0;
                  this.autoBuildPlacedBlocks = 0;
                  this.nextAutoBuildTick = this.f_58857_.m_46467_() + 1L;
                  this.scheduledScanTick = Long.MIN_VALUE;
                  this.m_6596_();
                  player.m_5661_(
                     Component.m_237110_("ae2lt.matrix.build_started", new Object[]{this.autoBuildPlacements.size()}).m_130940_(ChatFormatting.GREEN), true
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
            this.refreshStructure();
         } else {
            MatrixAutoBuildPlan.Placement placement = this.autoBuildPlacements.get(this.autoBuildPlacementIndex);
            BlockPos pos = MatrixMultiblockScanner.worldPos(this.f_58858_, placement.localPos(), this.autoBuildFacing);
            if (!this.f_58857_.m_46749_(pos)) {
               this.nextAutoBuildTick = this.f_58857_.m_46467_() + 20L;
            } else {
               MatrixMultiblockComponent current = MatrixMultiblockScanner.componentAt(this.f_58857_, pos);
               if (this.matchesAutoBuildTarget(current, placement.target())) {
                  this.advanceAutoBuild(player, false);
               } else if (current != MatrixMultiblockComponent.AIR) {
                  this.abortAutoBuildPlacement(player, pos);
               } else {
                  Item consumedItem = null;
                  BlockState state;
                  if (placement.target() == MatrixAutoBuildPlan.Target.PATTERN_STORAGE && !player.m_150110_().f_35937_) {
                     consumedItem = this.findPatternStorageItem(player);
                     state = this.stateForPatternStorageItem(consumedItem);
                     if (state == null) {
                        this.abortAutoBuildMissingItem(player, Component.m_237115_("ae2lt.matrix.pattern_storage_any"));
                        return;
                     }
                  } else {
                     state = this.stateForAutoBuild(placement.target());
                  }

                  if (state != null && !state.m_60795_()) {
                     if (!player.m_150110_().f_35937_ && consumedItem == null) {
                        consumedItem = state.m_60734_().m_5456_();
                        if (consumedItem == Items.f_41852_ || this.countItem(player, consumedItem) <= 0) {
                           this.abortAutoBuildMissingItem(player, consumedItem.m_41466_());
                           return;
                        }
                     }

                     boolean placed = pos.equals(this.f_58858_)
                        ? this.f_58857_.m_7731_(pos, (BlockState)this.m_58900_().m_61124_(MatrixMultiblockDirectionalBlock.FACING, this.autoBuildFacing), 3)
                        : this.f_58857_.m_7731_(pos, state, 3);
                     if (!placed) {
                        this.abortAutoBuildPlacement(player, pos);
                     } else {
                        if (consumedItem != null) {
                           this.consumeItem(player, consumedItem, 1);
                        }

                        this.playAutoBuildPlaceSound(player, pos, state);
                        this.advanceAutoBuild(player, true);
                     }
                  } else {
                     this.abortAutoBuildPlacement(player, pos);
                  }
               }
            }
         }
      }
   }

   private boolean matchesAutoBuildTarget(MatrixMultiblockComponent component, MatrixAutoBuildPlan.Target target) {
      return switch (target) {
         case CASING -> component == MatrixMultiblockComponent.MATRIX_CASING;
         case CONSTRAINT_FRAME -> component == MatrixMultiblockComponent.MATRIX_CONSTRAINT_FRAME;
         case GLASS -> component == MatrixMultiblockComponent.MATRIX_GLASS;
         case PORT -> component == MatrixMultiblockComponent.MATRIX_PORT;
         case PATTERN_STORAGE -> component.isPatternStorage();
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
      this.autoBuildPlacementFailed(player, pos);
   }

   private void abortAutoBuildMissingItem(ServerPlayer player, Component itemName) {
      this.clearAutoBuildSession();
      this.refreshStructure();
      player.m_5661_(Component.m_237110_("ae2lt.matrix.build_interrupted_missing", new Object[]{itemName}).m_130940_(ChatFormatting.RED), false);
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

   private void autoBuildPlacementFailed(ServerPlayer player, BlockPos pos) {
      this.refreshStructure();
      player.m_5661_(Component.m_237110_("ae2lt.matrix.build_place_failed", new Object[]{this.describePosition(pos)}).m_130940_(ChatFormatting.RED), false);
   }

   private void finishAutoBuild(ServerPlayer player, int placedBlocks) {
      MatrixMultiblockScanAttempt attempt = this.scanCurrent();
      if (attempt.chunksUnavailable()) {
         this.suspendForUnloadedChunks();
      } else if (attempt.formed()) {
         this.form(attempt.result());
         Component message = placedBlocks == 0
            ? Component.m_237115_("ae2lt.matrix.build_already_complete")
            : Component.m_237110_("ae2lt.matrix.build_complete", new Object[]{placedBlocks});
         player.m_5661_(message.m_6881_().m_130940_(ChatFormatting.GREEN), true);
      } else {
         this.deform();
         Component message = placedBlocks == 0
            ? Component.m_237115_("ae2lt.matrix.build_nothing_to_place")
            : Component.m_237110_("ae2lt.matrix.build_placed", new Object[]{placedBlocks});
         player.m_5661_(message.m_6881_().m_130940_(ChatFormatting.GREEN), true);
      }
   }

   public void upgradePatternStorage(ServerPlayer player) {
      MatrixMultiblockScanAttempt attempt = this.scanCurrent();
      if (attempt.chunksUnavailable()) {
         this.suspendForUnloadedChunks();
      } else if (!attempt.formed()) {
         player.m_5661_(Component.m_237110_("ae2lt.matrix.scan_failed", new Object[]{this.describeIssues(attempt)}).m_130940_(ChatFormatting.RED), true);
      } else {
         ArrayList<MatrixPatternStorageBlockEntity> t1Storages = new ArrayList<>();

         for (MatrixMultiblockMember member : attempt.result().patternMembers()) {
            if (member.component() == MatrixMultiblockComponent.PATTERN_STORAGE_T1
               && this.f_58857_.m_7702_(member.worldPos()) instanceof MatrixPatternStorageBlockEntity storage) {
               t1Storages.add(storage);
            }
         }

         if (t1Storages.isEmpty()) {
            player.m_5661_(Component.m_237115_("ae2lt.matrix.upgrade_none").m_130940_(ChatFormatting.YELLOW), true);
         } else {
            int available = this.countUpgradeItems(player);
            if (available <= 0 && !player.m_150110_().f_35937_) {
               player.m_5661_(Component.m_237115_("ae2lt.matrix.upgrade_missing").m_130940_(ChatFormatting.RED), true);
            } else {
               int toUpgrade = player.m_150110_().f_35937_ ? t1Storages.size() : Math.min(available, t1Storages.size());

               for (int i = 0; i < toUpgrade; i++) {
                  this.upgradeStorageInPlace(t1Storages.get(i));
               }

               if (!player.m_150110_().f_35937_) {
                  this.consumeUpgradeItems(player, toUpgrade);
               }

               this.scanAndForm(player);
               player.m_5661_(Component.m_237110_("ae2lt.matrix.upgraded", new Object[]{toUpgrade, t1Storages.size()}).m_130940_(ChatFormatting.GREEN), true);
            }
         }
      }
   }

   public List<MatrixPatternStorageBlockEntity> findPatternStorages() {
      if (this.f_58857_ != null && this.isFormed()) {
         this.ensureStructureCache();
         return !this.validateStructureCacheForCurrentTick() ? List.of() : this.cachedPatternStorages;
      } else {
         return List.of();
      }
   }

   public List<MatrixCraftingUnit> findCraftingUnits() {
      if (this.f_58857_ != null && this.isFormed()) {
         this.ensureStructureCache();
         return !this.validateStructureCacheForCurrentTick() ? List.of() : this.cachedCraftingUnits;
      } else {
         return List.of();
      }
   }

   public List<IPatternDetails> getAvailablePatterns() {
      return this.cluster.getAvailablePatterns();
   }

   public boolean isMatrixBusy() {
      return this.cluster.isBusy();
   }

   public long getBatchCapacity(IPatternDetails details) {
      return this.cluster.getBatchCapacity(details);
   }

   public BatchDispatchMode getBatchDispatchMode() {
      return this.cluster.batchDispatchMode();
   }

   public long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
      long remaining = this.cluster.pushBatch(details, oneCopyTemplate, maxCraft);
      if (remaining != maxCraft) {
         this.persistRuntimeStateIfChanged();
      }

      return remaining;
   }

   public boolean pushPattern(IPatternDetails details, KeyCounter[] oneCopyTemplate) {
      boolean accepted = this.cluster.pushSingle(details, oneCopyTemplate);
      if (accepted) {
         this.persistRuntimeStateIfChanged();
      }

      return accepted;
   }

   public boolean isWorking() {
      return this.formed && this.cluster.threadsInFlight() > 0L;
   }

   @Override
   public long getGameTime() {
      return this.f_58857_ != null ? this.f_58857_.m_46467_() : 0L;
   }

   @Override
   public boolean isConnected() {
      MatrixPortBlockEntity port = this.getLinkedPort();
      return port != null && port.isLinkConnected();
   }

   @Override
   public long affordableOperations(long requestedOperations) {
      if (requestedOperations <= 0L) {
         return 0L;
      } else {
         MatrixPortBlockEntity port = this.getLinkedPort();
         IGrid grid = port != null && port.isLinkConnected() ? port.getGrid() : null;
         if (grid == null) {
            return 0L;
         } else {
            double requestedPower = (double)requestedOperations;
            double available = grid.getEnergyService().extractAEPower(requestedPower, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            if (available == Double.POSITIVE_INFINITY || available >= requestedPower - 0.01) {
               return requestedOperations;
            } else {
               return !Double.isFinite(available) ? 0L : Math.min(requestedOperations, Math.max(0L, (long)Math.floor(available)));
            }
         }
      }
   }

   @Override
   public void consumeOperations(long acceptedOperations) {
      if (acceptedOperations > 0L) {
         MatrixPortBlockEntity port = this.getLinkedPort();
         IGrid grid = port != null && port.isLinkConnected() ? port.getGrid() : null;
         if (grid != null) {
            grid.getEnergyService().extractAEPower((double)acceptedOperations, Actionable.MODULATE, PowerMultiplier.CONFIG);
         }
      }
   }

   @Override
   public long insertToNetwork(AEKey key, long amount) {
      MatrixPortBlockEntity port = this.getLinkedPort();
      return port != null ? port.insertToNetworkLink(key, amount) : 0L;
   }

   @Override
   public void spawnToWorld(AEKey key, long amount) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && key != null && amount > 0L) {
         ArrayList<ItemStack> drops = new ArrayList<>();
         key.addDrops(amount, drops, this.f_58857_, this.f_58858_);

         for (ItemStack drop : drops) {
            if (!drop.m_41619_()) {
               NativeStackDropHelper.popResource(this.f_58857_, this.f_58858_, drop);
            }
         }
      }
   }

   public void persistRuntimeStateIfChanged() {
      if (this.persistentStateOwner && this.machineId.equals(this.loadedRuntimeId) && this.f_58857_ instanceof ServerLevel serverLevel) {
         CompoundTag var3 = new CompoundTag();
         this.cluster.writeEngineTo(var3, serverLevel.m_9598_());
         ControllerMachineStateSavedData.get(serverLevel).setState(ControllerMachineStateSavedData.MachineType.MATRIX, this.machineId, var3);
      }
   }

   public void prepareForControllerRemoval() {
      if (this.persistentStateOwner && this.f_58857_ instanceof ServerLevel) {
         this.cluster.tryReleaseOutputs();
         this.persistRuntimeStateIfChanged();
      }
   }

   private void ensureRuntimeStateLoaded(MatrixPortBlockEntity port) {
      if (!this.machineId.equals(this.loadedRuntimeId) && this.f_58857_ instanceof ServerLevel serverLevel) {
         this.cluster.suspendRuntime();
         ControllerMachineStateSavedData data = ControllerMachineStateSavedData.get(serverLevel);
         boolean stored = data.hasState(ControllerMachineStateSavedData.MachineType.MATRIX, this.machineId);
         CompoundTag legacy = port.copyLegacyClusterState();
         CompoundTag state = stored
            ? data.getState(ControllerMachineStateSavedData.MachineType.MATRIX, this.machineId)
            : (legacy != null ? legacy : new CompoundTag());
         this.cluster.readEngineFrom(state, serverLevel.m_9598_());
         this.loadedRuntimeId = this.machineId;
         if (!stored) {
            this.persistRuntimeStateIfChanged();
         }

         if (legacy != null) {
            port.consumeLegacyClusterState();
         }
      }
   }

   private void suspendRuntime() {
      this.cluster.suspendRuntime();
      this.loadedRuntimeId = null;
   }

   private List<IPatternDetails> collectAvailablePatterns() {
      if (this.isFormed() && this.f_58857_ != null) {
         ArrayList<IPatternDetails> result = new ArrayList<>();

         for (MatrixPatternStorageBlockEntity storage : this.findPatternStorages()) {
            result.addAll(storage.getAvailablePatterns());
         }

         return List.copyOf(result);
      } else {
         return List.of();
      }
   }

   private boolean hasAvailablePattern(IPatternDetails details) {
      if (details != null && this.isFormed() && this.f_58857_ != null) {
         for (MatrixPatternStorageBlockEntity storage : this.findPatternStorages()) {
            if (storage.hasPattern(details)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private MatrixMultiblockScanAttempt scanCurrent() {
      MatrixMultiblockScanAttempt attempt = MatrixMultiblockScanner.scan(this.f_58857_, this.f_58858_, this.getOrientation());
      this.primaryScanIssue = attempt.issues().isEmpty() ? null : attempt.issues().get(0);
      return attempt;
   }

   private MatrixPortBlockEntity getLinkedPort() {
      if (this.f_58857_ != null && this.isFormed() && this.portPos != null && this.f_58857_.m_46749_(this.portPos)) {
         if (this.f_58857_.m_7702_(this.portPos) instanceof MatrixPortBlockEntity port && port.isLinkedTo(this.f_58858_, this.machineId)) {
            return port;
         }

         return null;
      } else {
         return null;
      }
   }

   private void form(MatrixMultiblockScanResult result) {
      this.persistRuntimeStateIfChanged();
      List<BlockPos> newPatternStoragePositions = result.patternMembers().stream().map(member -> member.worldPos().m_7949_()).toList();
      boolean bindingLayoutChanged = !this.formed
         || this.orientation != result.orientation()
         || !result.portPos().equals(this.portPos)
         || !result.minPos().equals(this.minPos)
         || !result.maxPos().equals(this.maxPos)
         || !newPatternStoragePositions.equals(this.patternStoragePositions);
      if (bindingLayoutChanged) {
         this.setBoundsConnectedTextureFormed(false);
         this.clearBindingsInStoredBounds();
      }

      this.formed = true;
      this.primaryScanIssue = null;
      this.structureAvailable = true;
      this.waitingForChunks = false;
      this.nextChunkCheckTick = this.f_58857_.m_46467_() + 20L;
      this.orientation = result.orientation();
      this.portPos = result.portPos();
      this.minPos = result.minPos();
      this.maxPos = result.maxPos();
      this.memberCount = result.members().size();
      this.patternStorageCount = result.patternMembers().size();
      this.craftingUnitCount = result.craftingMembers().size();
      this.patternStoragePositions = newPatternStoragePositions;
      this.cachedPatternStorages = this.resolvePatternStorages(result);
      this.craftingUnitCacheEntries = this.createCraftingUnitCacheEntries(result);
      this.cachedCraftingUnits = this.craftingUnitCacheEntries.stream().map(MatrixControllerBlockEntity.CraftingUnitCacheEntry::unit).toList();
      this.structureCacheValid = true;
      this.rememberStructureCacheValidation(true);
      if (this.f_58857_.m_7702_(result.portPos()) instanceof MatrixPortBlockEntity port) {
         this.ensureRuntimeStateLoaded(port);
      }

      this.bindMembers(result);
      this.setMembersFormed(result, true);
      this.setChangedAndUpdate();
   }

   private void deform() {
      this.persistRuntimeStateIfChanged();
      this.clearBindingsInStoredBounds();
      this.setBoundsConnectedTextureFormed(false);
      this.formed = false;
      this.structureAvailable = false;
      this.waitingForChunks = false;
      this.nextChunkCheckTick = 0L;
      this.portPos = null;
      this.minPos = null;
      this.maxPos = null;
      this.memberCount = 0;
      this.patternStorageCount = 0;
      this.craftingUnitCount = 0;
      this.patternStoragePositions = List.of();
      this.cachedPatternStorages = List.of();
      this.craftingUnitCacheEntries = List.of();
      this.cachedCraftingUnits = List.of();
      this.structureCacheValid = false;
      this.rememberStructureCacheValidation(false);
      this.setChangedAndUpdate();
   }

   private List<MatrixPatternStorageBlockEntity> resolvePatternStorages(MatrixMultiblockScanResult result) {
      ArrayList<MatrixPatternStorageBlockEntity> storages = new ArrayList<>();

      for (MatrixMultiblockMember member : result.patternMembers()) {
         if (this.f_58857_.m_46749_(member.worldPos()) && this.f_58857_.m_7702_(member.worldPos()) instanceof MatrixPatternStorageBlockEntity storage) {
            storages.add(storage);
         }
      }

      return List.copyOf(storages);
   }

   private List<MatrixControllerBlockEntity.CraftingUnitCacheEntry> createCraftingUnitCacheEntries(MatrixMultiblockScanResult result) {
      ArrayList<MatrixControllerBlockEntity.CraftingUnitCacheEntry> entries = new ArrayList<>();

      for (MatrixMultiblockMember member : result.craftingMembers()) {
         MatrixCraftingUnit unit = member.component().toCraftingUnit(distanceToCraftingCenter(member.localPos()));
         if (unit != null) {
            entries.add(new MatrixControllerBlockEntity.CraftingUnitCacheEntry(member.worldPos().m_7949_(), member.component(), unit));
         }
      }

      return List.copyOf(entries);
   }

   private boolean validateStructureCacheForCurrentTick() {
      if (this.f_58857_ != null && this.isFormed() && this.structureCacheValid) {
         long currentTick = this.f_58857_.m_46467_();
         if (!this.structureCacheValidationRequired && this.lastStructureCacheValidationTick == currentTick) {
            return this.lastStructureCacheValidationResult;
         } else {
            boolean valid = this.validateStructureCache();
            this.rememberStructureCacheValidation(valid);
            return valid;
         }
      } else {
         this.rememberStructureCacheValidation(false);
         return false;
      }
   }

   private boolean validateStructureCache() {
      if (this.f_58857_ != null && this.isFormed() && this.structureCacheValid) {
         Optional<List<MatrixPatternStorageBlockEntity>> storages = CompletePhysicalStorageSet.resolve(
            this.patternStoragePositions,
            pos -> {
               if (!this.f_58857_.m_46749_(pos)) {
                  return null;
               } else {
                  if (this.f_58857_.m_7702_(pos) instanceof MatrixPatternStorageBlockEntity storage
                     && !storage.m_58901_()
                     && this.f_58858_.equals(storage.getControllerPos())) {
                     return storage;
                  }

                  return null;
               }
            }
         );
         Optional<List<MatrixCraftingUnit>> units = CompletePhysicalStorageSet.resolve(this.craftingUnitCacheEntries, entry -> {
            if (!this.f_58857_.m_46749_(entry.pos())) {
               return null;
            } else {
               return MatrixMultiblockScanner.componentAt(this.f_58857_, entry.pos()) == entry.component() ? entry.unit() : null;
            }
         });
         if (!storages.isEmpty() && !units.isEmpty()) {
            this.cachedPatternStorages = storages.orElseThrow();
            this.cachedCraftingUnits = units.orElseThrow();
            return true;
         } else {
            this.suspendForUnloadedChunks();
            this.nextChunkCheckTick = this.f_58857_.m_46467_();
            return false;
         }
      } else {
         return false;
      }
   }

   private void rememberStructureCacheValidation(boolean valid) {
      this.lastStructureCacheValidationTick = this.f_58857_ != null ? this.f_58857_.m_46467_() : Long.MIN_VALUE;
      this.lastStructureCacheValidationResult = valid;
      this.structureCacheValidationRequired = false;
   }

   private static int distanceToCraftingCenter(BlockPos localPos) {
      BlockPos center = MatrixMultiblockTemplate.CRAFTING_CENTER_LOCAL;
      return Math.abs(localPos.m_123341_() - center.m_123341_())
         + Math.abs(localPos.m_123342_() - center.m_123342_())
         + Math.abs(localPos.m_123343_() - center.m_123343_());
   }

   private void refreshStructure() {
      MatrixMultiblockScanAttempt attempt = this.scanCurrent();
      if (attempt.chunksUnavailable()) {
         this.suspendForUnloadedChunks();
      } else if (attempt.formed()) {
         this.form(attempt.result());
      } else if (this.formed) {
         this.deform();
      }
   }

   private void ensureStructureCache() {
      if (this.isFormed() && !this.structureCacheValid && this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.refreshStructure();
      }
   }

   private void checkChunkAvailability() {
      if (this.f_58857_ != null && (this.formed || this.waitingForChunks)) {
         if (!MatrixMultiblockScanner.areRequiredChunksLoaded(this.f_58857_, this.f_58858_, this.getOrientation())) {
            this.suspendForUnloadedChunks();
         } else if (!this.structureAvailable || this.waitingForChunks) {
            this.scheduleStructureCheck();
         }
      }
   }

   private void suspendForUnloadedChunks() {
      boolean changed = !this.waitingForChunks
         || this.structureAvailable
         || this.structureCacheValid
         || !this.cachedPatternStorages.isEmpty()
         || !this.cachedCraftingUnits.isEmpty();
      this.waitingForChunks = true;
      this.structureAvailable = false;
      this.structureCacheValid = false;
      this.rememberStructureCacheValidation(false);
      this.cachedPatternStorages = List.of();
      this.craftingUnitCacheEntries = List.of();
      this.cachedCraftingUnits = List.of();
      if (changed
         && this.f_58857_ != null
         && this.portPos != null
         && this.f_58857_.m_46749_(this.portPos)
         && this.f_58857_.m_7702_(this.portPos) instanceof MatrixPortBlockEntity port) {
         port.suspendFromController(this.f_58858_);
      }

      if (changed) {
         this.setChangedAndUpdate();
      }
   }

   private boolean ensureStructureChunksLoaded() {
      if (MatrixMultiblockScanner.areRequiredChunksLoaded(this.f_58857_, this.f_58858_, this.getOrientation())) {
         return true;
      } else {
         this.suspendForUnloadedChunks();
         return false;
      }
   }

   private void bindMembers(MatrixMultiblockScanResult result) {
      MatrixPortBlockEntity linkedPort = null;
      if (this.f_58857_.m_46749_(result.portPos()) && this.f_58857_.m_7702_(result.portPos()) instanceof MatrixPortBlockEntity port) {
         linkedPort = port;
      }

      for (MatrixMultiblockMember member : result.patternMembers()) {
         if (this.f_58857_.m_46749_(member.worldPos()) && this.f_58857_.m_7702_(member.worldPos()) instanceof MatrixPatternStorageBlockEntity storage) {
            storage.setControllerPos(this.f_58858_);
         }
      }

      if (linkedPort != null) {
         linkedPort.bindToController(this.f_58858_, this.machineId);
      }

      for (MatrixMultiblockMember memberx : result.patternMembers()) {
         if (this.f_58857_.m_46749_(memberx.worldPos()) && this.f_58857_.m_7702_(memberx.worldPos()) instanceof MatrixPatternStorageBlockEntity storage) {
            storage.bindToController(this.f_58858_, linkedPort);
         }
      }
   }

   private void clearBindingsInStoredBounds() {
      if (this.f_58857_ != null && this.minPos != null && this.maxPos != null) {
         for (int x = this.minPos.m_123341_(); x <= this.maxPos.m_123341_(); x++) {
            for (int y = this.minPos.m_123342_(); y <= this.maxPos.m_123342_(); y++) {
               for (int z = this.minPos.m_123343_(); z <= this.maxPos.m_123343_(); z++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  if (this.f_58857_.m_46749_(pos)) {
                     BlockEntity be = this.f_58857_.m_7702_(pos);
                     if (be instanceof MatrixPortBlockEntity) {
                        MatrixPortBlockEntity port = (MatrixPortBlockEntity)be;
                        if (this.f_58858_.equals(port.getControllerPos())) {
                           port.bindToController(null);
                           continue;
                        }
                     }

                     if (be instanceof MatrixPatternStorageBlockEntity) {
                        MatrixPatternStorageBlockEntity storage = (MatrixPatternStorageBlockEntity)be;
                        if (this.f_58858_.equals(storage.getControllerPos())) {
                           storage.setControllerPos(null);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void setMembersFormed(MatrixMultiblockScanResult result, boolean formedValue) {
      for (MatrixMultiblockMember member : result.members()) {
         this.setConnectedTextureFormed(member.worldPos(), formedValue);
      }
   }

   private void setBoundsConnectedTextureFormed(boolean formedValue) {
      if (this.f_58857_ != null && this.minPos != null && this.maxPos != null) {
         for (int x = this.minPos.m_123341_(); x <= this.maxPos.m_123341_(); x++) {
            for (int y = this.minPos.m_123342_(); y <= this.maxPos.m_123342_(); y++) {
               for (int z = this.minPos.m_123343_(); z <= this.maxPos.m_123343_(); z++) {
                  this.setConnectedTextureFormed(new BlockPos(x, y, z), formedValue);
               }
            }
         }
      }
   }

   private void setConnectedTextureFormed(BlockPos pos, boolean formedValue) {
      if (this.f_58857_ != null && this.f_58857_.m_46749_(pos)) {
         BlockState state = this.f_58857_.m_8055_(pos);
         if (state.m_60734_() instanceof MatrixMultiblockComponentBlock componentBlock
            && componentBlock.matrixComponent(state) != MatrixMultiblockComponent.MATRIX_CONTROLLER
            && state.m_61138_(MatrixFormedBlock.FORMED)
            && (Boolean)state.m_61143_(MatrixFormedBlock.FORMED) != formedValue) {
            this.f_58857_.m_7731_(pos, (BlockState)state.m_61124_(MatrixFormedBlock.FORMED, formedValue), 2);
         }
      }
   }

   private void syncRenderState() {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         BlockState state = this.m_58900_();
         if (state.m_61138_(MatrixControllerBlock.FORMED) && state.m_61138_(MatrixControllerBlock.WORKING)) {
            boolean activeFormed = this.isFormed();
            boolean working = this.isWorking();
            if ((Boolean)state.m_61143_(MatrixControllerBlock.FORMED) != activeFormed || (Boolean)state.m_61143_(MatrixControllerBlock.WORKING) != working) {
               this.f_58857_
                  .m_7731_(
                     this.f_58858_,
                     (BlockState)((BlockState)state.m_61124_(MatrixControllerBlock.FORMED, activeFormed)).m_61124_(MatrixControllerBlock.WORKING, working),
                     2
                  );
            }
         }
      }
   }

   private MatrixAutoBuildPlan createAutoBuildPlan(int patternStorageBudget) {
      Direction facing = this.getOrientation();
      return MatrixAutoBuildPlan.create(
         local -> MatrixMultiblockScanner.componentAt(this.f_58857_, MatrixMultiblockScanner.worldPos(this.f_58858_, local, facing)), patternStorageBudget
      );
   }

   private Map<Item, Integer> autoBuildRequirementsForMissingBlocks(MatrixAutoBuildPlan plan) {
      LinkedHashMap<Item, Integer> result = new LinkedHashMap<>();

      for (MatrixAutoBuildPlan.Placement placement : plan.placements()) {
         if (placement.target() != MatrixAutoBuildPlan.Target.PATTERN_STORAGE) {
            BlockState state = this.stateForAutoBuild(placement.target());
            if (state != null && !state.m_60795_()) {
               Item item = state.m_60734_().m_5456_();
               if (item != Items.f_41852_) {
                  result.merge(item, Integer.valueOf(1), Integer::sum);
               }
            }
         }
      }

      return result;
   }

   private BlockState stateForAutoBuild(MatrixAutoBuildPlan.Target target) {
      return switch (target) {
         case CASING -> ((MatrixCasingBlock)ModBlocks.MATTER_WARPING_MATRIX_CASING.get()).m_49966_();
         case CONSTRAINT_FRAME -> ((MatrixFormedBlock)ModBlocks.MATTER_WARPING_MATRIX_CONSTRAINT_FRAME.get()).m_49966_();
         case GLASS -> ((MatrixGlassBlock)ModBlocks.MATTER_WARPING_MATRIX_GLASS.get()).m_49966_();
         case PORT -> ((MatrixPortBlock)ModBlocks.MATTER_WARPING_MATRIX_PORT.get()).m_49966_();
         case PATTERN_STORAGE -> ((MatrixPatternStorageBlock)ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T1.get()).m_49966_();
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

   private int countPatternStorageItems(Player player) {
      int count = 0;
      Inventory inventory = player.m_150109_();

      for (int i = 0; i < inventory.m_6643_(); i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (this.isPatternStorageItem(stack.m_41720_())) {
            count += stack.m_41613_();
         }
      }

      return count;
   }

   private Item findPatternStorageItem(Player player) {
      Inventory inventory = player.m_150109_();

      for (int i = 0; i < inventory.m_6643_(); i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (!stack.m_41619_() && this.isPatternStorageItem(stack.m_41720_())) {
            return stack.m_41720_();
         }
      }

      return null;
   }

   private BlockState stateForPatternStorageItem(Item item) {
      if (item instanceof BlockItem blockItem && blockItem.m_40614_() instanceof MatrixPatternStorageBlock) {
         return blockItem.m_40614_().m_49966_();
      }

      return null;
   }

   private boolean isPatternStorageItem(Item item) {
      if (item instanceof BlockItem blockItem && blockItem.m_40614_() instanceof MatrixPatternStorageBlock) {
         return true;
      }

      return false;
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

   private Component describeMissing(Map<Item, Integer> missing, int missingPatternStorages) {
      MutableComponent result = Component.m_237119_();
      int totalEntries = missing.size() + (missingPatternStorages > 0 ? 1 : 0);
      int visibleEntries = 0;
      if (missingPatternStorages > 0) {
         this.appendMissingEntry(result, Component.m_237115_("ae2lt.matrix.pattern_storage_any"), missingPatternStorages, visibleEntries++);
      }

      for (Entry<Item, Integer> entry : missing.entrySet()) {
         this.appendMissingEntry(result, entry.getKey().m_41466_(), entry.getValue(), visibleEntries++);
         if (visibleEntries >= 4 && totalEntries > visibleEntries) {
            result.m_130946_(", ...");
            break;
         }
      }

      return result;
   }

   private void appendMissingEntry(MutableComponent result, Component name, int count, int index) {
      if (index > 0) {
         result.m_130946_(", ");
      }

      result.m_7220_(name).m_130946_(" x").m_130946_(Integer.toString(count));
   }

   private void upgradeStorageInPlace(MatrixPatternStorageBlockEntity oldStorage) {
      BlockPos pos = oldStorage.m_58899_();
      List<ItemStack> contents = oldStorage.copyContents();
      this.f_58857_.m_7731_(pos, ((MatrixPatternStorageBlock)ModBlocks.MATTER_WARPING_MATRIX_PATTERN_STORAGE_T2.get()).m_49966_(), 3);
      if (this.f_58857_.m_7702_(pos) instanceof MatrixPatternStorageBlockEntity newStorage) {
         newStorage.loadContents(contents);
         newStorage.setControllerPos(this.f_58858_);
      }
   }

   private int countUpgradeItems(Player player) {
      Item upgrade = (Item)ModItems.MATTER_WARPING_MATRIX_PATTERN_STORAGE_UPGRADE.get();
      int count = 0;
      Inventory inventory = player.m_150109_();

      for (int i = 0; i < inventory.m_6643_(); i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (stack.m_150930_(upgrade)) {
            count += stack.m_41613_();
         }
      }

      return count;
   }

   private void consumeUpgradeItems(Player player, int amount) {
      Item upgrade = (Item)ModItems.MATTER_WARPING_MATRIX_PATTERN_STORAGE_UPGRADE.get();
      int remaining = amount;
      Inventory inventory = player.m_150109_();

      for (int i = 0; i < inventory.m_6643_() && remaining > 0; i++) {
         ItemStack stack = inventory.m_8020_(i);
         if (stack.m_150930_(upgrade)) {
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
      Direction facing = this.getOrientation();
      int visible = Math.min(localPositions.size(), 4);

      for (int i = 0; i < visible; i++) {
         if (i > 0) {
            result.m_130946_(", ");
         }

         result.m_7220_(this.describePosition(MatrixMultiblockScanner.worldPos(this.f_58858_, localPositions.get(i), facing)));
      }

      if (localPositions.size() > visible) {
         result.m_130946_(", ...");
      }

      return result;
   }

   private Component describePosition(BlockPos pos) {
      return Component.m_237113_("[" + pos.m_123341_() + ", " + pos.m_123342_() + ", " + pos.m_123343_() + "]");
   }

   private String describeIssues(MatrixMultiblockScanAttempt attempt) {
      return attempt.issues().isEmpty() ? "unknown" : attempt.issues().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("unknown");
   }

   private Direction orientationFromState(BlockState state) {
      if (state.m_61138_(MatrixMultiblockDirectionalBlock.FACING)) {
         Direction facing = (Direction)state.m_61143_(MatrixMultiblockDirectionalBlock.FACING);
         if (facing.m_122434_() != Axis.Y) {
            return facing;
         }
      }

      return Direction.NORTH;
   }

   private void setChangedAndUpdate() {
      this.m_6596_();
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 2);
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128379_("Formed", this.formed);
      tag.m_128405_("Orientation", this.orientation.m_122411_());
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
      tag.m_128405_("PatternStorageCount", this.patternStorageCount);
      tag.m_128405_("CraftingUnitCount", this.craftingUnitCount);
      tag.m_128362_("MachineId", this.machineId);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.formed = tag.m_128471_("Formed");
      this.structureCacheValid = false;
      this.structureCacheValidationRequired = true;
      this.lastStructureCacheValidationResult = false;
      this.lastStructureCacheValidationTick = Long.MIN_VALUE;
      this.structureAvailable = false;
      this.waitingForChunks = false;
      this.nextChunkCheckTick = 0L;
      this.patternStoragePositions = List.of();
      this.cachedPatternStorages = List.of();
      this.craftingUnitCacheEntries = List.of();
      this.cachedCraftingUnits = List.of();
      this.orientation = Direction.m_122376_(tag.m_128451_("Orientation"));
      if (this.orientation.m_122434_() == Axis.Y) {
         this.orientation = Direction.NORTH;
      }

      this.portPos = tag.m_128425_("PortPos", 4) ? BlockPos.m_122022_(tag.m_128454_("PortPos")) : null;
      this.minPos = tag.m_128425_("MinPos", 4) ? BlockPos.m_122022_(tag.m_128454_("MinPos")) : null;
      this.maxPos = tag.m_128425_("MaxPos", 4) ? BlockPos.m_122022_(tag.m_128454_("MaxPos")) : null;
      this.memberCount = tag.m_128451_("MemberCount");
      this.patternStorageCount = tag.m_128451_("PatternStorageCount");
      this.craftingUnitCount = tag.m_128451_("CraftingUnitCount");
      if (tag.m_128403_("MachineId")) {
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

   private void claimPersistentState() {
      if (this.f_58857_ instanceof ServerLevel serverLevel) {
         this.persistentStateOwner = ControllerMachineStateSavedData.get(serverLevel)
            .claim(ControllerMachineStateSavedData.MachineType.MATRIX, this.machineId, serverLevel, this.f_58858_);
      }
   }

   private void releasePersistentState() {
      if (this.persistentStateOwner && this.f_58857_ instanceof ServerLevel serverLevel) {
         ControllerMachineStateSavedData.get(serverLevel)
            .release(ControllerMachineStateSavedData.MachineType.MATRIX, this.machineId, serverLevel, this.f_58858_);
      }

      this.persistentStateOwner = false;
   }

   private static record CraftingUnitCacheEntry(BlockPos pos, MatrixMultiblockComponent component, MatrixCraftingUnit unit) {
   }
}
