package com.moakiee.ae2lt.logic;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.helpers.MachineSource;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.logic.energy.PowerCostUtil;
import com.moakiee.ae2lt.logic.energy.WirelessEnergyAPI;
import com.moakiee.ae2lt.logic.energy.WirelessEnergyDistributor;
import com.moakiee.ae2lt.mixin.PatternProviderLogicAccessor;
import com.moakiee.thunderbolt.api.crafting.batch.IBatchCraftingProvider;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class OverloadedPatternProviderLogic extends PatternProviderLogic implements IBatchCraftingProvider {
   private final OverloadedReturnInventoryController returnInventory;
   private final OverloadedPatternProviderBlockEntity overloadedHost;
   private final OverloadedEjectController ejectController;
   private final IManagedGridNode gridNode;
   private final IActionSource wirelessSource;
   private final int totalCapacity;
   private final OverloadedProviderStorageController storage;
   private final WirelessEnergyDistributor wirelessDistributor;
   private List<WirelessEnergyAPI.Target> validTargetsCache = List.of();
   private int validTargetsVersion;
   private final OverloadedReturnPolicy returnPolicy = new OverloadedReturnPolicy();
   private static final int OVERFLOW_FLUSH_BUDGET = 64;
   private final ProviderWirelessDispatch wirelessDispatch = new ProviderWirelessDispatch();
   private final WirelessOverflowQueue wirelessOverflow = this.wirelessDispatch.overflow();
   private final WirelessOverflowPersistence wirelessOverflowPersistence = new WirelessOverflowPersistence();
   private final AdaptiveBatchStatePersistence adaptiveBatchStatePersistence = new AdaptiveBatchStatePersistence();
   private static final int SINGLE_PUSH_TARGET_ATTEMPTS = 2;
   @Nullable
   private ProviderTarget pendingLocalDirectionalOverflowTarget;
   @Nullable
   private OverloadedPatternProviderLogic.PendingLocalDirectionalOverflow pendingLocalDirectionalOverflowLoad;
   private final ProviderNormalDispatch normalDispatch = new ProviderNormalDispatch();
   private final OverloadedProviderPatternCatalog patternCatalog = new OverloadedProviderPatternCatalog();
   private final OverloadedAutoReturnController autoReturn;
   private static final int GRID_TICK_MIN = 1;
   private static final int GRID_TICK_MAX = 20;
   private static final int VALIDATE_INTERVAL = 20;
   private List<OverloadedPatternProviderBlockEntity.WirelessConnection> validConnectionsCache = List.of();
   private Set<OverloadedPatternProviderBlockEntity.WirelessConnection> validConnectionSet = Set.of();
   private long validConnectionsCacheTick = -1L;
   private boolean connectionsDirty = true;
   private long lastEnergyTickGameTime = -1L;
   private boolean cachedInductionCardInstalled;
   private boolean inductionCardCacheDirty = true;
   private static final Item APPFLUX_INDUCTION_CARD = AppFluxHelper.getInductionCard();
   @Nullable
   private static final AEKey CACHED_APPFLUX_FE_KEY = AppFluxHelper.FE_KEY;
   private static final long CACHED_APPFLUX_TRANSFER_RATE = AppFluxHelper.TRANSFER_RATE;
   private static final String TAG_LOCAL_DIRECTIONAL_OVERFLOW = "ae2lt:local_directional_overflow";
   private static final String TAG_LOCAL_TARGET_DIRECTION = "target_direction";
   private static final String TAG_LOCAL_OVERFLOW_ENTRIES = "entries";

   public OverloadedPatternProviderLogic(IManagedGridNode mainNode, OverloadedPatternProviderBlockEntity host, int patternInventorySize) {
      super(mainNode, host, Math.min(patternInventorySize, 36));
      mainNode.addService(IGridTickable.class, new OverloadedPatternProviderLogic.Ticker());
      this.overloadedHost = host;
      this.ejectController = new OverloadedEjectController(host);
      this.gridNode = mainNode;
      this.wirelessSource = new MachineSource(mainNode::getNode);
      this.totalCapacity = patternInventorySize;
      this.wirelessDistributor = new WirelessEnergyDistributor(new OverloadedPatternProviderLogic.DistributorHost());
      PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)this;
      this.storage = new OverloadedProviderStorageController(host, accessor, this.totalCapacity);
      IAEItemFilter patternFilter = new IAEItemFilter() {
         public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return PatternDetailsHelper.isEncodedPattern(stack);
         }
      };
      if (this.totalCapacity > 36) {
         AppEngInternalInventory largeInv = new AppEngInternalInventory(this, this.totalCapacity);
         largeInv.setFilter(patternFilter);
         accessor.setPatternInventory(largeInv);
      } else {
         accessor.getPatternInventory().setFilter(patternFilter);
      }

      Runnable returnListener = () -> {
         this.gridNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
         this.overloadedHost.saveChanges();
      };
      ReturnSlotFilter returnFilter = (slot, key) -> {
         if (!this.overloadedHost.isFilteredImport()) {
            return true;
         } else {
            AllowedOutputFilter filter = this.getOrBuildOutputFilter();
            return !filter.isEmpty() && filter.matches(key);
         }
      };
      this.returnInventory = new OverloadedReturnInventoryController(this.totalCapacity, returnListener, returnFilter);
      accessor.setReturnInv(this.returnInventory.full());
      this.autoReturn = new OverloadedAutoReturnController(new OverloadedPatternProviderLogic.AutoReturnEnvironment());
   }

   protected OverloadedPatternProviderBlockEntity getOverloadedHost() {
      return this.overloadedHost;
   }

   protected IManagedGridNode getGridNode() {
      return this.gridNode;
   }

   protected IActionSource getActionSource() {
      return this.wirelessSource;
   }

   public PatternProviderReturnInventory getReturnInv() {
      return this.returnInventory.pageView();
   }

   public PatternProviderReturnInventory getInternalReturnInv() {
      return this.returnInventory.full();
   }

   public long maxAffordableExternalReturn(AEKey what, long amount) {
      return PowerCostUtil.maxAffordable(this.gridNode.getGrid(), what, amount);
   }

   public void consumeExternalReturnPower(AEKey what, long amount) {
      PowerCostUtil.consume(this.gridNode.getGrid(), what, amount);
   }

   public void resetCraftingLock() {
      super.resetCraftingLock();
      this.returnPolicy.clearUnlockRule();
   }

   public int getCurrentPage() {
      return this.returnInventory.currentPage();
   }

   public int getTotalPages() {
      return this.returnInventory.totalPages();
   }

   public void setCurrentPage(int page) {
      this.returnInventory.setCurrentPage(page);
   }

   public void syncReturnPageViewFromFull() {
      this.returnInventory.copyFullToPage();
   }

   public void updatePatterns() {
      this.wirelessDispatch.patternsChanged();
      this.normalDispatch.patternsChanged();
      PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)this;
      List<IPatternDetails> patterns = accessor.getPatterns();
      Set<AEKey> patternInputs = accessor.getPatternInputs();
      AppEngInternalInventory inventory = accessor.getPatternInventory();
      Level level = this.overloadedHost.m_58904_();
      this.patternCatalog.rebuild(inventory, level, patterns, patternInputs);
      this.finishPendingAdaptiveBatchStateLoad();
      this.returnPolicy.patternsChanged();
      this.refreshEjectRegistrations();
      ICraftingProvider.requestUpdate(accessor.getMainNode());
      this.alertGridTick();
   }

   public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
      if (!this.wirelessOverflow.isEmpty()) {
         this.flushWirelessSends();
      }

      if (this.overloadedHost.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL) {
         if (this.hasLocalDirectionalOverflow()) {
            this.flushLocalDirectionalOverflow();
            if (this.hasLocalDirectionalOverflow()) {
               return false;
            }
         }

         if (!AdvancedAECompat.isDirectional(patternDetails)
            && this.overloadedHost.getBlockingMode() == OverloadedPatternProviderBlockEntity.BlockingMode.SAME_PATTERN) {
            return this.pushNormalBatch(patternDetails, inputHolder, 1L, PatternInputAcceptance.VANILLA_SINGLE_COPY) == 0L;
         } else {
            double cost = PowerCostUtil.totalCost(inputHolder);
            IGrid grid = this.gridNode.getGrid();
            if (!PowerCostUtil.canAfford(grid, cost)) {
               return false;
            } else {
               boolean result;
               if (AdvancedAECompat.isDirectional(patternDetails)) {
                  result = this.pushPatternDirectionally(patternDetails, inputHolder);
               } else {
                  if (this.overloadedHost.m_58904_() instanceof ServerLevel serverLevel) {
                     this.prepareNormalTargetsForDispatch(serverLevel, patternDetails);
                  }

                  result = super.pushPattern(patternDetails, inputHolder);
                  if (result) {
                     this.syncPendingUnlockRule(patternDetails);
                  }
               }

               if (result) {
                  PowerCostUtil.consumeRaw(grid, cost);
                  this.alertGridTick();
               }

               return result;
            }
         }
      } else {
         return this.wirelessPushPattern(patternDetails, inputHolder);
      }
   }

   private void prepareNormalTargetsForDispatch(ServerLevel level, IPatternDetails pattern) {
      if (this.overloadedHost.getReturnMode() == OverloadedPatternProviderBlockEntity.ReturnMode.AUTO) {
         IPatternDetails patternHandle = this.patternCatalog.resolve(pattern);
         if (patternHandle != null) {
            for (Direction direction : this.activeNormalTargetDirections()) {
               ProviderTarget target = this.normalDispatch.target(level, this.overloadedHost.m_58899_(), direction);
               if (target.canAccept(level, pattern, this.wirelessSource) && !this.isTargetBlocked(target, level, patternHandle)) {
                  this.autoReturn.beforeDispatch(level, target);
               }
            }
         }
      }
   }

   public long getBatchCapacity(IPatternDetails details) {
      if (this.isBusy()) {
         return 0L;
      } else {
         return this.canUseAdaptiveBatch(details) ? Long.MAX_VALUE : 1L;
      }
   }

   public long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
      if (maxCraft <= 0L) {
         return 0L;
      } else if (oneCopyTemplate == null) {
         return maxCraft;
      } else if (!this.canUseAdaptiveBatch(details)) {
         return this.pushPattern(details, oneCopyTemplate) ? maxCraft - 1L : maxCraft;
      } else {
         if (!this.wirelessOverflow.isEmpty()) {
            this.flushWirelessSends();
         }

         return this.overloadedHost.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS
            ? this.pushWirelessBatch(details, oneCopyTemplate, maxCraft)
            : this.pushNormalBatch(details, oneCopyTemplate, maxCraft, PatternInputAcceptance.COMPLETE_BATCH);
      }
   }

   private boolean canUseAdaptiveBatch(IPatternDetails details) {
      return this.overloadedHost.isAdaptiveBatchEnabled()
         && details != null
         && details.supportsPushInputsToExternalInventory()
         && !AdvancedAECompat.isDirectional(details);
   }

   private long pushNormalBatch(IPatternDetails pattern, KeyCounter[] oneCopyTemplate, long maxCraft, PatternInputAcceptance inputAcceptance) {
      PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)this;
      IPatternDetails patternHandle = this.patternCatalog.resolve(pattern);
      if (!this.hasLocalDirectionalOverflow()
         && accessor.getSendList().isEmpty()
         && this.gridNode.isActive()
         && patternHandle != null
         && this.getCraftingLockedReason() == LockCraftingMode.NONE) {
         if (this.overloadedHost.m_58904_() instanceof ServerLevel serverLevel) {
            List<Direction> targetList = this.activeNormalTargetDirections();
            if (targetList.isEmpty()) {
               return maxCraft;
            } else {
               LinkedHashMap<ProviderTarget, OverloadedPatternProviderLogic.BatchTargetContext> contexts = new LinkedHashMap<>();

               for (Direction pushDirection : this.normalDispatch.dispatchOrder(targetList)) {
                  OverloadedPatternProviderLogic.BatchTargetContext context = this.resolveNormalBatchTarget(serverLevel, pushDirection, pattern);
                  if (context != null) {
                     contexts.put(context.target(), context);
                  }
               }

               if (contexts.isEmpty()) {
                  return maxCraft;
               } else {
                  double oneCopyCost = PowerCostUtil.totalCost(oneCopyTemplate);
                  return this.normalDispatch
                     .dispatchBatch(
                        contexts.keySet(),
                        maxCraft,
                        (target, share) -> {
                           OverloadedPatternProviderLogic.BatchTargetContext contextx = contexts.get(target);
                           if (contextx == null) {
                              return new ProviderNormalDispatch.BatchAttemptResult(0L, false, false);
                           } else if (this.isBatchTargetBlocked(contextx, patternHandle)) {
                              return new ProviderNormalDispatch.BatchAttemptResult(0L, false, false);
                           } else {
                              this.autoReturn.beforeDispatch(contextx.level(), contextx.target());
                              ProviderTarget.BatchDispatchResult ramp = this.dispatchNormalBatchRamp(
                                 contextx, pattern, patternHandle, oneCopyTemplate, share, oneCopyCost, inputAcceptance
                              );
                              if (ramp.ownedCopies() <= 0L) {
                                 return new ProviderNormalDispatch.BatchAttemptResult(0L, ramp.globalAbort(), false);
                              } else {
                                 accessor.setSendDirection(contextx.target().boundFace().m_122424_());
                                 accessor.invokeSendStacksOut();
                                 this.alertGridTick();
                                 return new ProviderNormalDispatch.BatchAttemptResult(ramp.ownedCopies(), ramp.globalAbort(), !accessor.getSendList().isEmpty());
                              }
                           }
                        }
                     );
               }
            }
         } else {
            return maxCraft;
         }
      } else {
         return maxCraft;
      }
   }

   @Nullable
   private OverloadedPatternProviderLogic.BatchTargetContext resolveNormalBatchTarget(ServerLevel level, Direction pushDirection, IPatternDetails pattern) {
      ProviderTarget target = this.normalDispatch.target(level, this.overloadedHost.m_58899_(), pushDirection);
      return !target.canAccept(level, pattern, this.wirelessSource) ? null : new OverloadedPatternProviderLogic.BatchTargetContext(level, target);
   }

   private long pushWirelessBatch(IPatternDetails pattern, KeyCounter[] oneCopyTemplate, long maxCraft) {
      this.wirelessOverflow.refreshBackpressure();
      IPatternDetails patternHandle = this.patternCatalog.resolve(pattern);
      if (!this.wirelessOverflow.isBackpressured()
         && this.gridNode.isActive()
         && patternHandle != null
         && this.getCraftingLockedReason() == LockCraftingMode.NONE) {
         if (this.overloadedHost.m_58904_() instanceof ServerLevel providerLevel) {
            MinecraftServer server = providerLevel.m_7654_();
            long gameTick = providerLevel.m_46467_();
            List<OverloadedPatternProviderBlockEntity.WirelessConnection> valid = this.getOrRefreshValidConnections(providerLevel, gameTick);
            if (valid.isEmpty()) {
               return maxCraft;
            } else {
               OverloadedPatternProviderBlockEntity.WirelessDispatchMode dispatchMode = this.overloadedHost.getWirelessDispatchMode();
               boolean fastMode = this.overloadedHost.getWirelessSpeedMode() == OverloadedPatternProviderBlockEntity.WirelessSpeedMode.FAST;
               this.wirelessDispatch.prepare(valid, gameTick, fastMode, dispatchMode);
               return this.pushWirelessBatchTargets(pattern, patternHandle, oneCopyTemplate, maxCraft, server, gameTick, fastMode, dispatchMode);
            }
         } else {
            return maxCraft;
         }
      } else {
         return maxCraft;
      }
   }

   private long pushWirelessBatchTargets(
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] oneCopyTemplate,
      long maxCraft,
      MinecraftServer server,
      long gameTick,
      boolean fastMode,
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode dispatchMode
   ) {
      double oneCopyCost = PowerCostUtil.totalCost(oneCopyTemplate);
      return this.wirelessDispatch
         .dispatchBatch(
            dispatchMode,
            patternHandle,
            maxCraft,
            gameTick,
            fastMode,
            (connection, share, exploratoryAttempt, preserveBatchHistoryOnRejection) -> {
               OverloadedPatternProviderLogic.BatchTargetDispatchResult result = this.tryPushBatchToConnection(
                  pattern, patternHandle, oneCopyTemplate, share, oneCopyCost, preserveBatchHistoryOnRejection, connection, server
               );
               return new ProviderWirelessDispatch.BatchAttemptResult(
                  result.ownedCopies(),
                  result.attemptedCopies(),
                  result.acceptedFullChunk(),
                  result.requestLimited(),
                  result.baselineStatus(),
                  result.outcome()
               );
            },
            connection -> isConnectionAlive(connection, server),
            connection -> this.connectionsDirty = true
         );
   }

   private OverloadedPatternProviderLogic.BatchTargetDispatchResult tryPushBatchToConnection(
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] oneCopyTemplate,
      long maxCraft,
      double oneCopyCost,
      boolean preserveBatchHistoryOnRejection,
      OverloadedPatternProviderBlockEntity.WirelessConnection conn,
      MinecraftServer server
   ) {
      if (this.wirelessOverflow.contains(conn)) {
         return OverloadedPatternProviderLogic.BatchTargetDispatchResult.rejected(WirelessPushOutcome.SOFT_FAIL);
      } else {
         ServerLevel targetLevel = server.m_129880_(conn.dimension());
         if (targetLevel == null || !targetLevel.m_46749_(conn.pos())) {
            return OverloadedPatternProviderLogic.BatchTargetDispatchResult.rejected(WirelessPushOutcome.HARD_FAIL);
         } else if (!conn.canAccept(targetLevel, pattern, this.wirelessSource)) {
            return OverloadedPatternProviderLogic.BatchTargetDispatchResult.rejected(WirelessPushOutcome.HARD_FAIL);
         } else {
            OverloadedPatternProviderLogic.BatchTargetContext context = new OverloadedPatternProviderLogic.BatchTargetContext(targetLevel, conn);
            if (this.isBatchTargetBlocked(context, patternHandle)) {
               return OverloadedPatternProviderLogic.BatchTargetDispatchResult.rejected(WirelessPushOutcome.SOFT_FAIL);
            } else {
               this.autoReturn.beforeDispatch(targetLevel, conn);
               ProviderTarget.BatchStepResult step = this.dispatchWirelessBatchStep(
                  context, pattern, patternHandle, oneCopyTemplate, maxCraft, oneCopyCost, preserveBatchHistoryOnRejection
               );
               if (step.ownedCopies() <= 0L) {
                  return new OverloadedPatternProviderLogic.BatchTargetDispatchResult(
                     0L,
                     step.attemptedCopies(),
                     false,
                     step.requestLimited(),
                     step.baselineStatus(),
                     step.globalAbort() ? WirelessPushOutcome.GLOBAL_ABORT : WirelessPushOutcome.SOFT_FAIL
                  );
               } else {
                  this.alertGridTick();
                  return new OverloadedPatternProviderLogic.BatchTargetDispatchResult(
                     step.ownedCopies(),
                     step.attemptedCopies(),
                     step.acceptedFullChunk(),
                     step.requestLimited(),
                     step.baselineStatus(),
                     step.globalAbort() ? WirelessPushOutcome.GLOBAL_ABORT : WirelessPushOutcome.SUCCESS
                  );
               }
            }
         }
      }
   }

   private boolean isBatchTargetBlocked(OverloadedPatternProviderLogic.BatchTargetContext context, IPatternDetails pattern) {
      return this.isTargetBlocked(context.target(), context.level(), pattern);
   }

   private boolean isTargetBlocked(ProviderTarget target, ServerLevel level, IPatternDetails pattern) {
      boolean craftingLocked = this.getCraftingLockedReason() != LockCraftingMode.NONE;
      boolean blockingEnabled = this.isBlocking();
      return target.isBlocked(
         level,
         this.wirelessSource,
         pattern,
         craftingLocked,
         blockingEnabled,
         this.overloadedHost.getBlockingMode() == OverloadedPatternProviderBlockEntity.BlockingMode.SAME_PATTERN,
         ((PatternProviderLogicAccessor)this).getPatternInputs()
      );
   }

   private ProviderTarget.BatchDispatchResult dispatchNormalBatchRamp(
      OverloadedPatternProviderLogic.BatchTargetContext context,
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] oneCopyTemplate,
      long maxCraft,
      double oneCopyCost,
      PatternInputAcceptance inputAcceptance
   ) {
      boolean batchSupported = context.target().supportsBatch(context.level(), pattern);
      long targetMaxCraft = Math.min(maxCraft, context.target().batchCopyLimit(context.level()));
      ProviderTarget.BatchDispatchResult result = context.target()
         .pushPattern(
            patternHandle,
            targetMaxCraft,
            batchSupported,
            () -> this.isBatchTargetBlocked(context, patternHandle),
            copies -> this.pushBatchChunk(context, pattern, patternHandle, oneCopyTemplate, copies, oneCopyCost, inputAcceptance)
         );
      this.saveAdaptiveBatchStateIfDirty(context.target());
      return result;
   }

   private ProviderTarget.BatchStepResult dispatchWirelessBatchStep(
      OverloadedPatternProviderLogic.BatchTargetContext context,
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] oneCopyTemplate,
      long maxCraft,
      double oneCopyCost,
      boolean preserveBatchHistoryOnRejection
   ) {
      boolean batchSupported = context.target().supportsBatch(context.level(), pattern);
      ProviderTarget.BatchStepResult result = context.target()
         .pushPatternStep(
            patternHandle,
            maxCraft,
            context.level().m_46467_(),
            batchSupported,
            preserveBatchHistoryOnRejection,
            () -> this.isBatchTargetBlocked(context, patternHandle),
            copies -> this.pushBatchChunk(context, pattern, patternHandle, oneCopyTemplate, copies, oneCopyCost, PatternInputAcceptance.COMPLETE_BATCH)
         );
      this.saveAdaptiveBatchStateIfDirty(context.target());
      return result;
   }

   private void saveAdaptiveBatchStateIfDirty(ProviderTarget target) {
      if (target.consumeAdaptiveBatchHistoryDirty()) {
         this.saveChanges();
      }
   }

   private ProviderTarget.BatchChunk pushBatchChunk(
      OverloadedPatternProviderLogic.BatchTargetContext context,
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] oneCopyTemplate,
      int copies,
      double oneCopyCost,
      PatternInputAcceptance inputAcceptance
   ) {
      if (copies <= 0) {
         return ProviderTarget.BatchChunk.REJECTED;
      } else {
         double requestedCost = oneCopyCost * (double)copies;
         IGrid grid = this.gridNode.getGrid();
         if (Double.isFinite(requestedCost) && PowerCostUtil.canAfford(grid, requestedCost)) {
            PushResult result = context.target()
               .pushCopies(
                  context.level(),
                  pattern,
                  oneCopyTemplate,
                  copies,
                  inputAcceptance,
                  ((PatternProviderLogicAccessor)this).getPatternInputs(),
                  this.wirelessSource
               );
            long ownedCopies = (long)Math.min(copies, Math.max(0, result.acceptedCopies()));
            if (ownedCopies <= 0L) {
               return ProviderTarget.BatchChunk.REJECTED;
            } else {
               PowerCostUtil.consumeRaw(grid, oneCopyCost * (double)ownedCopies);
               if (!result.overflow().isEmpty()) {
                  if (context.target() instanceof OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
                     this.bucketOverflow(connection, pattern, result.overflow(), true);
                  } else {
                     PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)this;

                     for (GenericStack overflow : result.overflow()) {
                        accessor.invokeAddToSendList(overflow.what(), overflow.amount());
                     }
                  }
               }

               this.recordSuccessfulBatchChunk(context, pattern, patternHandle);
               return new ProviderTarget.BatchChunk(ownedCopies, ownedCopies == (long)copies && result.overflow().isEmpty(), false);
            }
         } else {
            return ProviderTarget.BatchChunk.GLOBAL_ABORT;
         }
      }
   }

   private void recordSuccessfulBatchChunk(OverloadedPatternProviderLogic.BatchTargetContext context, IPatternDetails pattern, IPatternDetails patternHandle) {
      ((PatternProviderLogicAccessor)this).invokeOnPushPatternSuccess(pattern);
      this.syncPendingUnlockRule(pattern);
      context.target().markPatternDispatched(context.level(), patternHandle);
   }

   private boolean wirelessPushPattern(IPatternDetails pattern, KeyCounter[] inputs) {
      this.wirelessOverflow.refreshBackpressure();
      IPatternDetails patternHandle = this.patternCatalog.resolve(pattern);
      if (this.wirelessOverflow.isBackpressured()) {
         return false;
      } else if (!this.gridNode.isActive()) {
         return false;
      } else if (patternHandle == null) {
         return false;
      } else if (this.getCraftingLockedReason() != LockCraftingMode.NONE) {
         return false;
      } else if (this.overloadedHost.m_58904_() instanceof ServerLevel sl) {
         MinecraftServer server = sl.m_7654_();
         OverloadedPatternProviderBlockEntity.WirelessDispatchMode dispatchMode = this.overloadedHost.getWirelessDispatchMode();
         List<OverloadedPatternProviderBlockEntity.WirelessConnection> valid = this.getOrRefreshValidConnections(sl, sl.m_46467_());
         if (valid.isEmpty()) {
            return false;
         } else {
            double cost = PowerCostUtil.totalCost(inputs);
            IGrid grid = this.gridNode.getGrid();
            if (!PowerCostUtil.canAfford(grid, cost)) {
               return false;
            } else {
               long gameTick = sl.m_46467_();
               boolean fastMode = this.overloadedHost.getWirelessSpeedMode() == OverloadedPatternProviderBlockEntity.WirelessSpeedMode.FAST;
               this.wirelessDispatch.prepare(valid, gameTick, fastMode, dispatchMode);
               return this.wirelessDispatch
                  .dispatchSingleCopy(
                     dispatchMode,
                     patternHandle,
                     gameTick,
                     fastMode,
                     2,
                     connection -> this.tryPushToConnection(pattern, patternHandle, inputs, connection, server),
                     connection -> isConnectionAlive(connection, server),
                     connection -> this.connectionsDirty = true
                  );
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isConnectionAlive(OverloadedPatternProviderBlockEntity.WirelessConnection conn, MinecraftServer server) {
      ServerLevel level = server.m_129880_(conn.dimension());
      return level != null && conn.isAlive(level);
   }

   private WirelessPushOutcome tryPushToConnection(
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] inputs,
      OverloadedPatternProviderBlockEntity.WirelessConnection conn,
      MinecraftServer server
   ) {
      if (this.wirelessOverflow.contains(conn)) {
         return WirelessPushOutcome.SOFT_FAIL;
      } else if (AdvancedAECompat.isDirectional(pattern)) {
         return this.tryPushToConnectionDirectionally(pattern, patternHandle, inputs, conn, server);
      } else {
         ServerLevel targetLevel = server.m_129880_(conn.dimension());
         if (targetLevel == null) {
            return WirelessPushOutcome.HARD_FAIL;
         } else if (!conn.canAccept(targetLevel, pattern, this.wirelessSource)) {
            return WirelessPushOutcome.HARD_FAIL;
         } else {
            double cost = PowerCostUtil.totalCost(inputs);
            IGrid grid = this.gridNode.getGrid();
            if (!PowerCostUtil.canAfford(grid, cost)) {
               return WirelessPushOutcome.GLOBAL_ABORT;
            } else if (this.isTargetBlocked(conn, targetLevel, patternHandle)) {
               return WirelessPushOutcome.SOFT_FAIL;
            } else {
               this.autoReturn.beforeDispatch(targetLevel, conn);
               PushResult result = conn.pushCopies(
                  targetLevel,
                  pattern,
                  inputs,
                  1,
                  PatternInputAcceptance.VANILLA_SINGLE_COPY,
                  ((PatternProviderLogicAccessor)this).getPatternInputs(),
                  this.wirelessSource
               );
               if (result.acceptedCopies() == 0) {
                  return WirelessPushOutcome.SOFT_FAIL;
               } else {
                  PowerCostUtil.consumeRaw(grid, cost);
                  if (!result.overflow().isEmpty()) {
                     this.bucketOverflow(conn, pattern, result.overflow(), false);
                  }

                  ((PatternProviderLogicAccessor)this).invokeOnPushPatternSuccess(pattern);
                  this.syncPendingUnlockRule(pattern);
                  conn.markPatternDispatched(targetLevel, patternHandle);
                  this.alertGridTick();
                  return WirelessPushOutcome.SUCCESS;
               }
            }
         }
      }
   }

   private void bucketOverflow(
      OverloadedPatternProviderBlockEntity.WirelessConnection conn, IPatternDetails pattern, List<GenericStack> overflow, boolean forceFallback
   ) {
      if (!overflow.isEmpty()) {
         this.wirelessOverflow.store(conn, pattern, overflow, forceFallback, this.currentGameTick());
         this.wirelessDispatch.markDirty();
         this.alertGridTick();
         this.saveChanges();
      }
   }

   private void bucketRoutedOverflow(
      OverloadedPatternProviderBlockEntity.WirelessConnection conn, IPatternDetails pattern, List<RoutedPatternOverflow.Entry> overflow
   ) {
      if (!overflow.isEmpty()) {
         this.wirelessOverflow.storeRouted(conn, pattern, overflow, this.currentGameTick());
         this.wirelessDispatch.markDirty();
         this.alertGridTick();
         this.saveChanges();
      }
   }

   private boolean pushPatternDirectionally(IPatternDetails pattern, KeyCounter[] inputs) {
      PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)this;
      IPatternDetails patternHandle = this.patternCatalog.resolve(pattern);
      if (this.hasLocalDirectionalOverflow()) {
         return false;
      } else if (!accessor.getSendList().isEmpty()) {
         return false;
      } else if (!this.gridNode.isActive()) {
         return false;
      } else if (patternHandle == null) {
         return false;
      } else if (this.getCraftingLockedReason() != LockCraftingMode.NONE) {
         return false;
      } else if (!pattern.supportsPushInputsToExternalInventory()) {
         return false;
      } else if (!(this.overloadedHost.m_58904_() instanceof ServerLevel sl)) {
         return false;
      } else {
         List<Direction> targets = this.activeNormalTargetDirections();
         if (targets.isEmpty()) {
            return false;
         } else {
            EjectModeRegistry.setBypass(true);

            try {
               for (Direction pushDir : targets) {
                  ProviderTarget target = this.normalDispatch.target(sl, this.overloadedHost.m_58899_(), pushDir);
                  Direction defaultFace = target.boundFace();
                  BlockEntity be = target.resolveBlockEntity(sl);
                  if (be != null) {
                     Map<Direction, PatternProviderTarget> faceToTarget = this.buildDirectionalTargets(
                        sl, target, defaultFace, pattern, inputs, this.wirelessSource
                     );
                     if (faceToTarget != null && !this.isTargetBlocked(target, sl, patternHandle)) {
                        this.autoReturn.beforeDispatch(sl, target);
                        if (simulateDirectionalAcceptance(faceToTarget, defaultFace, pattern, inputs)) {
                           List<RoutedPatternOverflow.Entry> overflow = commitDirectionalPush(pattern, inputs, faceToTarget, defaultFace);
                           if (!overflow.isEmpty()) {
                              target.setDirectionalOverflow(RoutedPatternOverflow.routed(overflow));
                              this.pendingLocalDirectionalOverflowTarget = target;
                              if (!this.flushLocalDirectionalOverflow()) {
                                 this.saveChanges();
                              }
                           }

                           accessor.invokeOnPushPatternSuccess(pattern);
                           this.syncPendingUnlockRule(pattern);
                           target.markPatternDispatched(sl, patternHandle);
                           return true;
                        }
                     }
                  }
               }

               return false;
            } finally {
               EjectModeRegistry.setBypass(false);
            }
         }
      }
   }

   private WirelessPushOutcome tryPushToConnectionDirectionally(
      IPatternDetails pattern,
      IPatternDetails patternHandle,
      KeyCounter[] inputs,
      OverloadedPatternProviderBlockEntity.WirelessConnection conn,
      MinecraftServer server
   ) {
      if (this.wirelessOverflow.contains(conn)) {
         return WirelessPushOutcome.SOFT_FAIL;
      } else {
         ServerLevel targetLevel = server.m_129880_(conn.dimension());
         if (targetLevel == null) {
            return WirelessPushOutcome.HARD_FAIL;
         } else if (!targetLevel.m_46749_(conn.pos())) {
            return WirelessPushOutcome.HARD_FAIL;
         } else if (!pattern.supportsPushInputsToExternalInventory()) {
            return WirelessPushOutcome.SOFT_FAIL;
         } else {
            BlockEntity be = targetLevel.m_7702_(conn.pos());
            if (be == null) {
               return WirelessPushOutcome.HARD_FAIL;
            } else {
               double cost = PowerCostUtil.totalCost(inputs);
               IGrid grid = this.gridNode.getGrid();
               if (!PowerCostUtil.canAfford(grid, cost)) {
                  return WirelessPushOutcome.GLOBAL_ABORT;
               } else {
                  Direction defaultFace = conn.boundFace();
                  EjectModeRegistry.setBypass(true);

                  label94: {
                     WirelessPushOutcome overflow;
                     try {
                        Map<Direction, PatternProviderTarget> faceToTarget = this.buildDirectionalTargets(
                           targetLevel, conn, defaultFace, pattern, inputs, this.wirelessSource
                        );
                        if (faceToTarget == null) {
                           return WirelessPushOutcome.SOFT_FAIL;
                        }

                        if (this.isTargetBlocked(conn, targetLevel, patternHandle)) {
                           return WirelessPushOutcome.SOFT_FAIL;
                        }

                        this.autoReturn.beforeDispatch(targetLevel, conn);
                        if (simulateDirectionalAcceptance(faceToTarget, defaultFace, pattern, inputs)) {
                           List<RoutedPatternOverflow.Entry> overflowx = commitDirectionalPush(pattern, inputs, faceToTarget, defaultFace);
                           PowerCostUtil.consumeRaw(grid, cost);
                           if (!overflowx.isEmpty()) {
                              this.bucketRoutedOverflow(conn, pattern, overflowx);
                           }
                           break label94;
                        }

                        overflow = WirelessPushOutcome.SOFT_FAIL;
                     } finally {
                        EjectModeRegistry.setBypass(false);
                     }

                     return overflow;
                  }

                  ((PatternProviderLogicAccessor)this).invokeOnPushPatternSuccess(pattern);
                  this.syncPendingUnlockRule(pattern);
                  conn.markPatternDispatched(targetLevel, patternHandle);
                  this.alertGridTick();
                  return WirelessPushOutcome.SUCCESS;
               }
            }
         }
      }
   }

   @Nullable
   private Map<Direction, PatternProviderTarget> buildDirectionalTargets(
      ServerLevel level, ProviderTarget providerTarget, Direction defaultFace, IPatternDetails pattern, KeyCounter[] inputs, IActionSource source
   ) {
      HashMap<Direction, PatternProviderTarget> map = new HashMap<>();
      KeyCounter[] var8 = inputs;
      int var9 = inputs.length;

      for (int var10 = 0; var10 < var9; var10++) {
         for (Entry<AEKey> entry : var8[var10]) {
            Direction dir = AdvancedAECompat.getDirectionForKey(pattern, (AEKey)entry.getKey());
            Direction face = dir != null ? dir : defaultFace;
            map.computeIfAbsent(face, f -> providerTarget.resolveStorageTarget(level, f, source));
         }
      }

      return !map.isEmpty() && !map.containsValue(null) ? map : null;
   }

   private static boolean simulateDirectionalAcceptance(
      Map<Direction, PatternProviderTarget> faceToTarget, Direction defaultFace, IPatternDetails pattern, KeyCounter[] inputs
   ) {
      KeyCounter[] var4 = inputs;
      int var5 = inputs.length;

      for (int var6 = 0; var6 < var5; var6++) {
         for (Entry<AEKey> entry : var4[var6]) {
            Direction dir = AdvancedAECompat.getDirectionForKey(pattern, (AEKey)entry.getKey());
            Direction face = dir != null ? dir : defaultFace;
            PatternProviderTarget target = faceToTarget.get(face);
            if (target == null) {
               return false;
            }

            if (target.insert((AEKey)entry.getKey(), entry.getLongValue(), Actionable.SIMULATE) == 0L) {
               return false;
            }
         }
      }

      return true;
   }

   private static List<RoutedPatternOverflow.Entry> commitDirectionalPush(
      IPatternDetails pattern, KeyCounter[] inputs, Map<Direction, PatternProviderTarget> faceToTarget, Direction defaultFace
   ) {
      ArrayList<RoutedPatternOverflow.Entry> overflow = new ArrayList<>();
      pattern.pushInputsToExternalInventory(inputs, (what, amount) -> {
         Direction dir = AdvancedAECompat.getDirectionForKey(pattern, what);
         Direction face = dir != null ? dir : defaultFace;
         PatternProviderTarget target = faceToTarget.get(face);
         if (target != null) {
            long inserted = target.insert(what, amount, Actionable.MODULATE);
            if (inserted < amount) {
               overflow.add(new RoutedPatternOverflow.Entry(face, new GenericStack(what, amount - inserted)));
            }
         } else {
            overflow.add(new RoutedPatternOverflow.Entry(face, new GenericStack(what, amount)));
         }
      });
      return overflow;
   }

   private boolean flushLocalDirectionalOverflow() {
      this.finishPendingLocalDirectionalOverflowLoad();
      ProviderTarget target = this.pendingLocalDirectionalOverflowTarget;
      if (target == null) {
         return false;
      } else {
         RoutedPatternOverflow overflow = target.directionalOverflow();
         if (overflow == null) {
            this.pendingLocalDirectionalOverflowTarget = null;
            return false;
         } else if (this.overloadedHost.m_58904_() instanceof ServerLevel serverLevel) {
            BlockEntity targetBlockEntity = target.resolveBlockEntity(serverLevel);
            if (targetBlockEntity == null) {
               return false;
            } else {
               this.autoReturn.beforeDispatch(serverLevel, target);
               EjectModeRegistry.setBypass(true);

               boolean progressed;
               try {
                  progressed = overflow.flush(target.boundFace(), (face, what, amount) -> {
                     PatternProviderTarget storageTarget = target.resolveStorageTarget(serverLevel, face, this.wirelessSource);
                     return storageTarget == null ? 0L : storageTarget.insert(what, amount, Actionable.MODULATE);
                  });
               } finally {
                  EjectModeRegistry.setBypass(false);
               }

               if (overflow.isEmpty()) {
                  target.clearDirectionalOverflow();
                  this.pendingLocalDirectionalOverflowTarget = null;
               }

               if (progressed) {
                  this.saveChanges();
               }

               return progressed;
            }
         } else {
            return false;
         }
      }
   }

   private void flushWirelessSends() {
      if (this.overloadedHost.m_58904_() instanceof ServerLevel sl) {
         long gameTick = sl.m_46467_();
         if (this.wirelessOverflow.beginFlush(gameTick)) {
            MinecraftServer server = sl.m_7654_();
            int attempts = 0;
            boolean overflowStateChanged = false;

            while (attempts < 64) {
               OverloadedPatternProviderBlockEntity.WirelessConnection conn = this.wirelessOverflow.pollDue(gameTick);
               if (conn == null) {
                  break;
               }

               WirelessOverflowQueue.Bucket bucket = this.wirelessOverflow.get(conn);
               if (bucket != null) {
                  attempts++;
                  ServerLevel targetLevel = server.m_129880_(conn.dimension());
                  if (targetLevel == null || !targetLevel.m_46749_(conn.pos())) {
                     this.wirelessOverflow.rescheduleBlocked(conn, bucket, gameTick);
                  } else if (conn.resolveAdapter(targetLevel) == null) {
                     this.wirelessOverflow.rescheduleBlocked(conn, bucket, gameTick);
                  } else {
                     this.autoReturn.beforeDispatch(targetLevel, conn);
                     BlockEntity be = conn.resolveBlockEntity(targetLevel);
                     WirelessOverflowQueue.OverflowAttemptResult result = bucket.compactMode
                        ? this.flushCompactBucket(bucket, conn, targetLevel)
                        : this.flushFallbackBucket(bucket, conn, targetLevel, be);
                     if (result.removeBucket()) {
                        this.wirelessOverflow.remove(conn);
                        this.wirelessDispatch.resumeTarget(conn, gameTick);
                        this.wirelessDispatch.markDirty();
                     } else if (result.reschedule()) {
                        this.wirelessOverflow.reschedule(conn, bucket, gameTick, result);
                     }

                     overflowStateChanged |= result.persistentStateChanged();
                  }
               }
            }

            if (overflowStateChanged) {
               this.saveChanges();
            }
         }
      }
   }

   public void tickOverflowRetries() {
      this.flushWirelessSends();
   }

   private WirelessOverflowQueue.OverflowAttemptResult flushCompactBucket(
      WirelessOverflowQueue.Bucket bucket, OverloadedPatternProviderBlockEntity.WirelessConnection conn, ServerLevel targetLevel
   ) {
      IPatternDetails pattern = this.wirelessOverflow.pattern(Short.toUnsignedInt(bucket.patternId));
      if (pattern == null) {
         return WirelessOverflowQueue.OverflowAttemptResult.CLEARED;
      } else {
         IInput[] inputs = pattern.getInputs();
         boolean progressed = false;

         while (bucket.stuckIndex < inputs.length) {
            IInput input = inputs[bucket.stuckIndex];
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length != 1) {
               return WirelessOverflowQueue.OverflowAttemptResult.CLEARED;
            }

            ArrayList<GenericStack> single = new ArrayList<>(1);
            single.add(new GenericStack(possible[0].what(), bucket.remaining));
            conn.flushOverflow(targetLevel, single, this.wirelessSource);
            long left = single.isEmpty() ? 0L : single.get(0).amount();
            long inserted = bucket.remaining - left;
            if (inserted == 0L) {
               return progressed ? WirelessOverflowQueue.OverflowAttemptResult.PROGRESSED : WirelessOverflowQueue.OverflowAttemptResult.BLOCKED;
            }

            progressed = true;
            if (left > 0L) {
               bucket.remaining = left;
               return WirelessOverflowQueue.OverflowAttemptResult.PROGRESSED;
            }

            bucket.stuckIndex++;
            if (bucket.stuckIndex < inputs.length) {
               bucket.remaining = WirelessOverflowPatternTable.inputAmount(inputs[bucket.stuckIndex]);
            }
         }

         return WirelessOverflowQueue.OverflowAttemptResult.CLEARED;
      }
   }

   private WirelessOverflowQueue.OverflowAttemptResult flushFallbackBucket(
      WirelessOverflowQueue.Bucket bucket,
      OverloadedPatternProviderBlockEntity.WirelessConnection conn,
      ServerLevel targetLevel,
      @Nullable BlockEntity targetBlockEntity
   ) {
      if (!bucket.fallback.hasExplicitFaces()) {
         boolean progressed = bucket.fallback.flushUnrouted(stacks -> conn.flushOverflow(targetLevel, stacks, this.wirelessSource));
         if (bucket.fallback.isEmpty()) {
            return WirelessOverflowQueue.OverflowAttemptResult.CLEARED;
         } else {
            return progressed ? WirelessOverflowQueue.OverflowAttemptResult.PROGRESSED : WirelessOverflowQueue.OverflowAttemptResult.BLOCKED;
         }
      } else if (targetBlockEntity == null) {
         return WirelessOverflowQueue.OverflowAttemptResult.BLOCKED;
      } else {
         EjectModeRegistry.setBypass(true);

         boolean progressed;
         try {
            progressed = bucket.fallback.flush(conn.boundFace(), (face, what, amount) -> {
               PatternProviderTarget target = conn.resolveStorageTarget(targetLevel, face, this.wirelessSource);
               return target == null ? 0L : target.insert(what, amount, Actionable.MODULATE);
            });
         } finally {
            EjectModeRegistry.setBypass(false);
         }

         if (bucket.fallback.isEmpty()) {
            return WirelessOverflowQueue.OverflowAttemptResult.CLEARED;
         } else {
            return progressed ? WirelessOverflowQueue.OverflowAttemptResult.PROGRESSED : WirelessOverflowQueue.OverflowAttemptResult.BLOCKED;
         }
      }
   }

   private long currentGameTick() {
      return this.overloadedHost.m_58904_() instanceof ServerLevel serverLevel ? serverLevel.m_46467_() : 0L;
   }

   public void tickAutoReturn() {
      if (this.hasAnyTickWork()) {
         this.tickWirelessInductionEnergy();
         this.autoReturn.tick();
      }
   }

   public boolean hasAnyTickWork() {
      if (this.hasLocalDirectionalOverflow()) {
         return true;
      } else if (!this.wirelessOverflow.isEmpty()) {
         return true;
      } else if (this.storage.hasPendingRestore()) {
         return true;
      } else if (this.overloadedHost.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS
         && this.gridNode.isActive()
         && this.isInductionCardInstalled()
         && CACHED_APPFLUX_FE_KEY != null) {
         return true;
      } else {
         return this.overloadedHost.getReturnMode() == OverloadedPatternProviderBlockEntity.ReturnMode.AUTO && !this.getOrBuildOutputFilter().isEmpty()
            ? true
            : !this.returnInventory.full().isEmpty();
      }
   }

   protected AllowedOutputFilter getOrBuildOutputFilter() {
      return this.returnPolicy.outputFilter(this.getAvailablePatterns());
   }

   public boolean handleOverloadUnlockOnReturnedStack(GenericStack returnedStack) {
      if (this.getCraftingLockedReason() != LockCraftingMode.LOCK_UNTIL_RESULT) {
         this.returnPolicy.clearUnlockRule();
         return false;
      } else {
         GenericStack unlockStack = this.getUnlockStack();
         if (unlockStack == null) {
            this.resetCraftingLock();
            return true;
         } else {
            ReturnedCraftingUnlock.Result result = ReturnedCraftingUnlock.resolveMatchedAmount(
               this.returnPolicy.matchesUnlock(unlockStack, returnedStack), unlockStack.amount(), returnedStack.amount()
            );
            if (!result.matched()) {
               return false;
            } else {
               if (result.shouldResetLock()) {
                  this.resetCraftingLock();
               } else {
                  ((PatternProviderLogicAccessor)this).setUnlockStack(new GenericStack(unlockStack.what(), result.remainingAmount()));
                  this.saveChanges();
               }

               return true;
            }
         }
      }
   }

   protected void syncPendingUnlockRule(IPatternDetails pattern) {
      this.returnPolicy.synchronizeUnlockRule(pattern, this.getCraftingLockedReason() == LockCraftingMode.LOCK_UNTIL_RESULT);
   }

   public void refreshEjectRegistrations() {
      this.ejectController.refresh();
   }

   protected void tickWirelessInductionEnergy() {
      if (this.overloadedHost.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS) {
         if (this.gridNode.isActive() && this.isInductionCardInstalled()) {
            if (this.overloadedHost.m_58904_() instanceof ServerLevel sl) {
               if (CACHED_APPFLUX_FE_KEY != null) {
                  if (CACHED_APPFLUX_TRANSFER_RATE > 0L) {
                     long gameTick = sl.m_46467_();
                     if (gameTick != this.lastEnergyTickGameTime) {
                        this.lastEnergyTickGameTime = gameTick;
                        this.getOrRefreshValidConnections(sl, gameTick);
                        this.wirelessDistributor.tickNormal(sl);
                     }
                  }
               }
            }
         }
      }
   }

   private List<OverloadedPatternProviderBlockEntity.WirelessConnection> getOrRefreshValidConnections(ServerLevel providerLevel, long gameTick) {
      if (!this.connectionsDirty && gameTick - this.validConnectionsCacheTick < 20L) {
         return this.validConnectionsCache;
      } else {
         MinecraftServer server = providerLevel.m_7654_();
         ArrayList<OverloadedPatternProviderBlockEntity.WirelessConnection> valid = new ArrayList<>();

         for (OverloadedPatternProviderBlockEntity.WirelessConnection conn : this.overloadedHost.getConnections()) {
            if (conn.dimension().equals(providerLevel.m_46472_())
               && WirelessConnectionRange.isConnectorLinkInRange(providerLevel.m_46472_(), this.overloadedHost.m_58899_(), conn.dimension(), conn.pos())) {
               ServerLevel targetLevel = server.m_129880_(conn.dimension());
               if (targetLevel != null && targetLevel.m_46749_(conn.pos()) && targetLevel.m_7702_(conn.pos()) != null) {
                  valid.add(this.wirelessOverflow.adopt(conn));
               }
            }
         }

         List<OverloadedPatternProviderBlockEntity.WirelessConnection> refreshedConnections = List.copyOf(valid);
         boolean connectionsChanged = !refreshedConnections.equals(this.validConnectionsCache);
         if (connectionsChanged) {
            this.validConnectionsCache = refreshedConnections;
            this.validConnectionSet = Set.copyOf(refreshedConnections);
            HashSet<OverloadedPatternProviderBlockEntity.WirelessConnection> retainedStates = new HashSet<>(this.validConnectionSet);
            retainedStates.addAll(this.wirelessOverflow.connections());
            this.wirelessDispatch.retainStates(retainedStates);
            this.rebuildValidTargets();
         }

         this.validConnectionsCacheTick = gameTick;
         this.connectionsDirty = false;
         return this.validConnectionsCache;
      }
   }

   protected List<OverloadedPatternProviderBlockEntity.WirelessConnection> getValidConnections(ServerLevel providerLevel, long gameTick) {
      return this.getOrRefreshValidConnections(providerLevel, gameTick);
   }

   private void rebuildValidTargets() {
      ArrayList<WirelessEnergyAPI.Target> targets = new ArrayList<>(this.validConnectionsCache.size());

      for (OverloadedPatternProviderBlockEntity.WirelessConnection conn : this.validConnectionsCache) {
         targets.add(new WirelessEnergyAPI.Target(conn.dimension(), conn.pos(), conn.boundFace()));
      }

      this.validTargetsCache = List.copyOf(targets);
      this.validTargetsVersion++;
   }

   @Nullable
   private ServerLevel resolveTargetLevel(ServerLevel providerLevel, OverloadedPatternProviderBlockEntity.WirelessConnection conn) {
      if (!conn.dimension().equals(providerLevel.m_46472_())) {
         return null;
      } else if (!WirelessConnectionRange.isConnectorLinkInRange(providerLevel.m_46472_(), this.overloadedHost.m_58899_(), conn.dimension(), conn.pos())) {
         return null;
      } else {
         ServerLevel targetLevel = providerLevel.m_7654_().m_129880_(conn.dimension());
         return targetLevel != null && targetLevel.m_46749_(conn.pos()) ? targetLevel : null;
      }
   }

   public void onHostStateChanged() {
      this.invalidateValidConnectionsCache();
      this.inductionCardCacheDirty = true;
      this.refreshEjectRegistrations();
      this.alertGridTick();
   }

   public void flushWirelessEnergyBuffer() {
      this.wirelessDistributor.flushBufferToNetwork();
   }

   public boolean prepareInvalidConnectionRemoval(OverloadedPatternProviderBlockEntity.WirelessConnection conn) {
      WirelessOverflowQueue.Bucket bucket = this.wirelessOverflow.get(conn);
      if (bucket == null) {
         return true;
      } else if (!this.drainBucketToNetwork(bucket)) {
         return false;
      } else {
         this.wirelessOverflow.remove(conn);
         this.connectionsDirty = true;
         this.wirelessDispatch.markDirty();
         this.alertGridTick();
         this.saveChanges();
         return true;
      }
   }

   private boolean drainBucketToNetwork(WirelessOverflowQueue.Bucket bucket) {
      if (bucket.compactMode) {
         return this.drainCompactBucketToNetwork(bucket);
      } else {
         bucket.fallback.flush(Direction.DOWN, (face, what, amount) -> {
            long remaining = this.insertStackToNetwork(what, amount);
            return amount - remaining;
         });
         return bucket.fallback.isEmpty();
      }
   }

   private boolean drainCompactBucketToNetwork(WirelessOverflowQueue.Bucket bucket) {
      IPatternDetails pattern = this.wirelessOverflow.pattern(Short.toUnsignedInt(bucket.patternId));
      if (pattern == null) {
         return true;
      } else {
         IInput[] inputs = pattern.getInputs();

         while (bucket.stuckIndex < inputs.length) {
            GenericStack[] possible = inputs[bucket.stuckIndex].getPossibleInputs();
            if (possible.length != 1) {
               return true;
            }

            long remaining = this.insertStackToNetwork(possible[0].what(), bucket.remaining);
            if (remaining > 0L) {
               bucket.remaining = remaining;
               return false;
            }

            bucket.stuckIndex++;
            if (bucket.stuckIndex < inputs.length) {
               bucket.remaining = WirelessOverflowPatternTable.inputAmount(inputs[bucket.stuckIndex]);
            }
         }

         return true;
      }
   }

   private long insertStackToNetwork(AEKey what, long amount) {
      IGrid grid = this.gridNode.getGrid();
      if (grid != null && amount > 0L) {
         MEStorage storage = grid.getStorageService().getInventory();
         long remaining = amount;

         while (remaining > 0L) {
            long affordable = PowerCostUtil.maxAffordable(grid, what, remaining);
            if (affordable <= 0L) {
               break;
            }

            long inserted = storage.insert(what, affordable, Actionable.MODULATE, this.wirelessSource);
            if (inserted <= 0L) {
               break;
            }

            PowerCostUtil.consume(grid, what, inserted);
            remaining -= inserted;
         }

         return remaining;
      } else {
         return amount;
      }
   }

   public void onPersistentStateChanged() {
      this.inductionCardCacheDirty = true;
      this.alertGridTick();
   }

   public void onNeighborChanged() {
      this.alertGridTick();
   }

   private boolean hasCombinedGridTickWork() {
      PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)this;
      return accessor.invokeHasWorkToDo() || this.hasAnyTickWork();
   }

   private boolean hasActiveOverloadedTickWork(long gameTick) {
      if (this.wirelessOverflow.nextDueTick() <= gameTick) {
         return true;
      } else {
         return this.shouldTickWirelessEnergyNow(gameTick) ? true : this.shouldPollAutoReturnNow(gameTick);
      }
   }

   private boolean shouldTickWirelessEnergyNow(long gameTick) {
      if (this.overloadedHost.getProviderMode() != OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS) {
         return false;
      } else {
         return this.gridNode.isActive() && this.isInductionCardInstalled() ? CACHED_APPFLUX_FE_KEY != null && CACHED_APPFLUX_TRANSFER_RATE > 0L : false;
      }
   }

   private boolean shouldPollAutoReturnNow(long gameTick) {
      if (this.overloadedHost.getReturnMode() != OverloadedPatternProviderBlockEntity.ReturnMode.AUTO || !this.gridNode.isActive()) {
         return false;
      } else if (this.getOrBuildOutputFilter().isEmpty()) {
         return false;
      } else {
         if (this.overloadedHost.m_58904_() instanceof ServerLevel serverLevel && this.autoReturn.nextPollTick(serverLevel) <= gameTick + 1L) {
            return true;
         }

         return false;
      }
   }

   protected void alertGridTick() {
      this.gridNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }

   private List<Direction> activeNormalTargetDirections() {
      return List.copyOf(((PatternProviderLogicAccessor)this).invokeGetActiveSides());
   }

   private void prepareParentSendListForDispatch(PatternProviderLogicAccessor accessor) {
      if (this.overloadedHost.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL
         && !accessor.getSendList().isEmpty()
         && this.overloadedHost.m_58904_() instanceof ServerLevel level) {
         Direction direction = accessor.getSendDirection();
         if (direction != null) {
            ProviderTarget target = this.normalDispatch.target(level, this.overloadedHost.m_58899_(), direction);
            this.autoReturn.beforeDispatch(level, target);
         }
      }
   }

   private void invalidateValidConnectionsCache() {
      this.normalDispatch.clearRuntimeState();
      HashSet<OverloadedPatternProviderBlockEntity.WirelessConnection> wirelessTargets = new HashSet<>();
      wirelessTargets.addAll(this.validConnectionsCache);
      wirelessTargets.addAll(this.overloadedHost.getConnections());
      wirelessTargets.addAll(this.wirelessOverflow.connections());
      wirelessTargets.forEach(ProviderTarget::clearRuntimeState);
      this.connectionsDirty = true;
      this.validConnectionsCache = List.of();
      this.validConnectionSet = Set.of();
      this.validConnectionsCacheTick = -1L;
      this.validTargetsCache = List.of();
      this.validTargetsVersion++;
      this.wirelessDispatch.clear();
      this.autoReturn.clearSchedule();
      this.wirelessDistributor.clearTickState(true);
   }

   private boolean isInductionCardInstalled() {
      if (this.inductionCardCacheDirty) {
         this.cachedInductionCardInstalled = this.computeInductionCardInstalled();
         this.inductionCardCacheDirty = false;
      }

      return this.cachedInductionCardInstalled;
   }

   private boolean computeInductionCardInstalled() {
      Item card = getAppliedFluxInductionCard();
      if (card == null) {
         return false;
      } else {
         return this instanceof IUpgradeableObject upgradeableLogic ? upgradeableLogic.getUpgrades().isInstalled(card) : false;
      }
   }

   public void removeSavedData() {
      this.storage.removeSavedData();
   }

   public void onBlockEntityReady() {
      if (this.storage.loadOnReady()) {
         this.saveChanges();
      }

      this.finishPendingLocalDirectionalOverflowLoad();
      this.finishPendingWirelessOverflowLoad();
   }

   public int getTotalCapacity() {
      return this.totalCapacity;
   }

   @Nullable
   private static Item getAppliedFluxInductionCard() {
      return APPFLUX_INDUCTION_CARD;
   }

   public boolean isBusy() {
      return this.overloadedHost.getProviderMode() == OverloadedPatternProviderBlockEntity.ProviderMode.WIRELESS
         ? false
         : this.hasLocalDirectionalOverflow() || super.isBusy();
   }

   public void addDrops(List<ItemStack> drops) {
      super.addDrops(drops);

      for (WirelessOverflowQueue.Bucket bucket : this.wirelessOverflow.buckets()) {
         this.addBucketDrops(bucket, drops);
      }

      RoutedPatternOverflow localOverflow = this.pendingLocalDirectionalOverflowTarget != null
         ? this.pendingLocalDirectionalOverflowTarget.directionalOverflow()
         : (this.pendingLocalDirectionalOverflowLoad == null ? null : this.pendingLocalDirectionalOverflowLoad.overflow());
      if (localOverflow != null) {
         for (RoutedPatternOverflow.Entry entry : localOverflow.snapshot()) {
            GenericStack stack = entry.stack();
            stack.what().addDrops(stack.amount(), drops, this.overloadedHost.m_58904_(), this.overloadedHost.m_58899_());
         }
      }

      this.storage.addDrops(drops);
   }

   public void clearContent() {
      super.clearContent();
      this.storage.clear();
      if (this.pendingLocalDirectionalOverflowTarget != null) {
         this.pendingLocalDirectionalOverflowTarget.clearDirectionalOverflow();
         this.pendingLocalDirectionalOverflowTarget = null;
      }

      this.pendingLocalDirectionalOverflowLoad = null;
      this.clearWirelessOverflowState();
      this.normalDispatch.clear();
      this.wirelessDispatch.clear();
      this.autoReturn.clear();
      this.returnPolicy.patternsChanged();
      this.adaptiveBatchStatePersistence.clear();
      this.invalidateValidConnectionsCache();
      this.inductionCardCacheDirty = true;
      this.lastEnergyTickGameTime = -1L;
      this.ejectController.clear();
   }

   public void writeToNBT(CompoundTag tag) {
      super.writeToNBT(tag);
      this.storage.writeToNBT(tag);
      this.returnPolicy.writeToNBT(tag);
      RoutedPatternOverflow localOverflow = this.pendingLocalDirectionalOverflowTarget != null
         ? this.pendingLocalDirectionalOverflowTarget.directionalOverflow()
         : (this.pendingLocalDirectionalOverflowLoad == null ? null : this.pendingLocalDirectionalOverflowLoad.overflow());
      Direction localPushDirection = this.pendingLocalDirectionalOverflowTarget != null
         ? this.pendingLocalDirectionalOverflowTarget.boundFace().m_122424_()
         : (this.pendingLocalDirectionalOverflowLoad == null ? null : this.pendingLocalDirectionalOverflowLoad.pushDirection());
      if (localOverflow != null && localPushDirection != null) {
         CompoundTag localTag = new CompoundTag();
         localTag.m_128344_("target_direction", (byte)localPushDirection.m_122411_());
         localTag.m_128365_("entries", WirelessOverflowPersistence.writeRoutedOverflow(localOverflow));
         tag.m_128365_("ae2lt:local_directional_overflow", localTag);
      }

      this.wirelessOverflowPersistence.write(tag, this.wirelessOverflow);
      this.adaptiveBatchStatePersistence
         .write(tag, ((PatternProviderLogicAccessor)this).getPatternInventory(), this.patternCatalog, this.normalDispatch, this.overloadedHost.getConnections());
   }

   public void readFromNBT(CompoundTag tag) {
      super.readFromNBT(tag);
      this.storage.readFromNBT(tag, this.returnInventory.full());
      this.returnPolicy.readFromNBT(tag);
      this.pendingLocalDirectionalOverflowTarget = null;
      this.pendingLocalDirectionalOverflowLoad = null;
      if (tag.m_128425_("ae2lt:local_directional_overflow", 10)) {
         CompoundTag localTag = tag.m_128469_("ae2lt:local_directional_overflow");
         int directionId = localTag.m_128445_("target_direction");
         List<RoutedPatternOverflow.Entry> entries = WirelessOverflowPersistence.readRoutedOverflow(localTag.m_128437_("entries", 10));
         if (directionId >= 0 && directionId < Direction.values().length && !entries.isEmpty()) {
            this.pendingLocalDirectionalOverflowLoad = new OverloadedPatternProviderLogic.PendingLocalDirectionalOverflow(
               Direction.m_122376_(directionId), RoutedPatternOverflow.routed(entries)
            );
            this.finishPendingLocalDirectionalOverflowLoad();
         }
      }

      this.clearWirelessOverflowState();
      this.wirelessOverflowPersistence.read(tag);
      this.finishPendingWirelessOverflowLoad();
      this.autoReturn.clear();
      this.invalidateValidConnectionsCache();
      this.adaptiveBatchStatePersistence.read(tag, this.totalCapacity, 1024);
      this.inductionCardCacheDirty = true;
      this.lastEnergyTickGameTime = -1L;
      this.refreshEjectRegistrations();
   }

   private void addBucketDrops(WirelessOverflowQueue.Bucket bucket, List<ItemStack> drops) {
      if (bucket.compactMode) {
         IPatternDetails pattern = this.wirelessOverflow.pattern(Short.toUnsignedInt(bucket.patternId));
         if (pattern != null) {
            IInput[] inputs = pattern.getInputs();

            for (int i = bucket.stuckIndex; i < inputs.length; i++) {
               GenericStack[] possible = inputs[i].getPossibleInputs();
               if (possible.length == 1) {
                  long amount = i == bucket.stuckIndex ? bucket.remaining : WirelessOverflowPatternTable.inputAmount(inputs[i]);
                  if (amount > 0L) {
                     possible[0].what().addDrops(amount, drops, this.overloadedHost.m_58904_(), this.overloadedHost.m_58899_());
                  }
               }
            }
         }
      } else {
         for (RoutedPatternOverflow.Entry entry : bucket.fallback.snapshot()) {
            GenericStack stack = entry.stack();
            stack.what().addDrops(stack.amount(), drops, this.overloadedHost.m_58904_(), this.overloadedHost.m_58899_());
         }
      }
   }

   private void finishPendingWirelessOverflowLoad() {
      boolean loaded = this.wirelessOverflowPersistence
         .finishLoad(
            this.overloadedHost.m_58904_(),
            this.currentGameTick(),
            this.wirelessOverflow,
            this::resolveRestoredOverflowConnection,
            this.wirelessDispatch::pauseTarget
         );
      if (loaded) {
         this.connectionsDirty = true;
         this.wirelessDispatch.markDirty();
      }
   }

   private void finishPendingLocalDirectionalOverflowLoad() {
      OverloadedPatternProviderLogic.PendingLocalDirectionalOverflow pending = this.pendingLocalDirectionalOverflowLoad;
      Level level = this.overloadedHost.m_58904_();
      if (pending != null && level != null) {
         ProviderTarget target = new ProviderTarget(
            level.m_46472_(), this.overloadedHost.m_58899_().m_121945_(pending.pushDirection()), pending.pushDirection().m_122424_()
         );
         target.setDirectionalOverflow(pending.overflow());
         this.normalDispatch.restore(pending.pushDirection(), target);
         this.pendingLocalDirectionalOverflowTarget = target;
         this.pendingLocalDirectionalOverflowLoad = null;
      }
   }

   private void finishPendingAdaptiveBatchStateLoad() {
      if (this.overloadedHost.m_58904_() instanceof ServerLevel serverLevel) {
         this.adaptiveBatchStatePersistence
            .finishLoad(
               serverLevel,
               this.overloadedHost.m_58899_(),
               ((PatternProviderLogicAccessor)this).getPatternInventory(),
               this.patternCatalog,
               this.normalDispatch,
               this.activeNormalTargetDirections(),
               this.overloadedHost.getConnections()
            );
      }
   }

   private boolean hasLocalDirectionalOverflow() {
      return this.pendingLocalDirectionalOverflowTarget != null || this.pendingLocalDirectionalOverflowLoad != null;
   }

   private OverloadedPatternProviderBlockEntity.WirelessConnection resolveRestoredOverflowConnection(
      OverloadedPatternProviderBlockEntity.WirelessConnection restored
   ) {
      for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : this.overloadedHost.getConnections()) {
         if (connection.equals(restored)) {
            return connection;
         }
      }

      return this.wirelessOverflow.adopt(restored);
   }

   private void clearWirelessOverflowState() {
      this.wirelessOverflow.clear();
      this.wirelessOverflowPersistence.clear();
      this.wirelessDispatch.patternsChanged();
   }

   private final class AutoReturnEnvironment implements OverloadedAutoReturnController.Environment {
      @Override
      public OverloadedPatternProviderBlockEntity provider() {
         return OverloadedPatternProviderLogic.this.overloadedHost;
      }

      @Override
      public IManagedGridNode gridNode() {
         return OverloadedPatternProviderLogic.this.gridNode;
      }

      @Override
      public IActionSource actionSource() {
         return OverloadedPatternProviderLogic.this.wirelessSource;
      }

      @Override
      public AllowedOutputFilter outputFilter() {
         return OverloadedPatternProviderLogic.this.getOrBuildOutputFilter();
      }

      @Override
      public PatternProviderReturnInventory returnInventory() {
         return OverloadedPatternProviderLogic.this.returnInventory.full();
      }

      @Override
      public List<OverloadedPatternProviderBlockEntity.WirelessConnection> validConnections(ServerLevel providerLevel, long gameTick) {
         return OverloadedPatternProviderLogic.this.getOrRefreshValidConnections(providerLevel, gameTick);
      }

      @Nullable
      @Override
      public ServerLevel resolveTargetLevel(ServerLevel providerLevel, OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
         return OverloadedPatternProviderLogic.this.resolveTargetLevel(providerLevel, connection);
      }

      @Override
      public ProviderTarget normalTarget(ServerLevel level, Direction pushDirection) {
         return OverloadedPatternProviderLogic.this.normalDispatch.target(level, OverloadedPatternProviderLogic.this.overloadedHost.m_58899_(), pushDirection);
      }

      @Override
      public List<Direction> normalTargetDirections() {
         return OverloadedPatternProviderLogic.this.activeNormalTargetDirections();
      }

      @Override
      public void onReturnedStack(GenericStack returnedStack) {
         OverloadedPatternProviderLogic.this.handleOverloadUnlockOnReturnedStack(returnedStack);
      }
   }

   private static record BatchTargetContext(ServerLevel level, ProviderTarget target) {
   }

   private static record BatchTargetDispatchResult(
      long ownedCopies,
      int attemptedCopies,
      boolean acceptedFullChunk,
      boolean requestLimited,
      ProviderTarget.BaselineStatus baselineStatus,
      WirelessPushOutcome outcome
   ) {
      private static OverloadedPatternProviderLogic.BatchTargetDispatchResult rejected(WirelessPushOutcome outcome) {
         return new OverloadedPatternProviderLogic.BatchTargetDispatchResult(0L, 0, false, false, ProviderTarget.BaselineStatus.NONE, outcome);
      }
   }

   private final class DistributorHost implements WirelessEnergyDistributor.Host {
      @Override
      public IManagedGridNode getMainNode() {
         return OverloadedPatternProviderLogic.this.gridNode;
      }

      @Override
      public IActionSource actionSource() {
         return OverloadedPatternProviderLogic.this.wirelessSource;
      }

      @Override
      public boolean isHostRemoved() {
         return OverloadedPatternProviderLogic.this.overloadedHost.m_58901_();
      }

      @Override
      public List<WirelessEnergyAPI.Target> getValidTargets() {
         return OverloadedPatternProviderLogic.this.validTargetsCache;
      }

      @Override
      public int getValidTargetsVersion() {
         return OverloadedPatternProviderLogic.this.validTargetsVersion;
      }
   }

   private static record PendingLocalDirectionalOverflow(Direction pushDirection, RoutedPatternOverflow overflow) {
   }

   private class Ticker implements IGridTickable {
      public TickingRequest getTickingRequest(IGridNode node) {
         return new TickingRequest(1, 20, !OverloadedPatternProviderLogic.this.hasCombinedGridTickWork(), true);
      }

      public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
         if (!OverloadedPatternProviderLogic.this.gridNode.isActive()) {
            return TickRateModulation.SLEEP;
         } else {
            PatternProviderLogicAccessor accessor = (PatternProviderLogicAccessor)OverloadedPatternProviderLogic.this;
            OverloadedPatternProviderLogic.this.prepareParentSendListForDispatch(accessor);
            boolean parentDidWork = accessor.invokeDoWork();
            boolean localDirectionalDidWork = OverloadedPatternProviderLogic.this.flushLocalDirectionalOverflow();
            OverloadedPatternProviderLogic.this.flushWirelessSends();
            OverloadedPatternProviderLogic.this.storage
               .drainPendingRestore(OverloadedPatternProviderLogic.this::insertStackToNetwork, OverloadedPatternProviderLogic.this::saveChanges);
            OverloadedPatternProviderLogic.this.tickAutoReturn();
            long gameTick = OverloadedPatternProviderLogic.this.overloadedHost.m_58904_() instanceof ServerLevel sl ? sl.m_46467_() : Long.MAX_VALUE;
            if (OverloadedPatternProviderLogic.this.hasActiveOverloadedTickWork(gameTick)) {
               return TickRateModulation.URGENT;
            } else {
               boolean parentHasWork = accessor.invokeHasWorkToDo();
               if (parentHasWork) {
                  return parentDidWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
               } else if (OverloadedPatternProviderLogic.this.hasLocalDirectionalOverflow()) {
                  return localDirectionalDidWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
               } else {
                  return OverloadedPatternProviderLogic.this.hasAnyTickWork() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
               }
            }
         }
      }
   }
}
