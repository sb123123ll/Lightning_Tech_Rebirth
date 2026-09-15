package com.moakiee.ae2lt.logic.energy;

import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class WirelessEnergyDistributor {
   private static final int ENERGY_DELAY_MEAN = 5;
   private static final int ENERGY_DELAY_MAX = 20;
   private static final int ENERGY_DELAY_MIN = 1;
   private static final int ENERGY_WHEEL_SLOTS = 32;
   private static final int ENERGY_WHEEL_SLOTS_MASK = 31;
   private static final int ENERGY_WHEEL_INITIAL_CAPACITY = 32;
   private static final int INITIAL_CACHE_CAPACITY = 64;
   private final WirelessEnergyDistributor.Host host;
   private final Map<WirelessEnergyAPI.Target, WirelessEnergyDistributor.BlockEnergyTargetCache> targetCachePool = new HashMap<>();
   private final WirelessEnergyDistributor.IntWheelSlot[] normalEnergyWheel = new WirelessEnergyDistributor.IntWheelSlot[32];
   private WirelessEnergyDistributor.IntWheelSlot spareNormalWheelSlot;
   private final List<WirelessEnergyAPI.Target> cachedValidTargets;
   private final Set<WirelessEnergyAPI.Target> cachedValidTargetSet;
   private int hostVersion;
   private int validTargetsVersion;
   private int normalWheelTargetsVersion;
   private boolean normalWheelDirty;
   private int normalWheelPointer;
   private WirelessEnergyDistributor.BlockEnergyTargetCache[] connectionTargetCaches;
   private long[] normalBatchDemand;
   private TargetAccess[] normalBatchEnergyTargets;
   @Nullable
   private IStorageService delegateStorageService;
   @Nullable
   private BufferedMEStorage bufferedStorage;
   @Nullable
   private BufferedStorageService storageProxy;
   private WirelessEnergyDistributor.Status lastStatus;
   private long lastTransferAmount;

   public WirelessEnergyDistributor(WirelessEnergyDistributor.Host host) {
      for (int i = 0; i < 32; i++) {
         this.normalEnergyWheel[i] = new WirelessEnergyDistributor.IntWheelSlot();
      }

      this.spareNormalWheelSlot = new WirelessEnergyDistributor.IntWheelSlot();
      this.cachedValidTargets = new ArrayList<>();
      this.cachedValidTargetSet = new HashSet<>();
      this.hostVersion = Integer.MIN_VALUE;
      this.normalWheelTargetsVersion = -1;
      this.normalWheelDirty = true;
      this.connectionTargetCaches = new WirelessEnergyDistributor.BlockEnergyTargetCache[0];
      this.normalBatchDemand = new long[0];
      this.normalBatchEnergyTargets = new TargetAccess[0];
      this.lastStatus = WirelessEnergyDistributor.Status.IDLE;
      this.host = host;
   }

   public boolean tickNormal(ServerLevel serverLevel) {
      if (!AppFluxBridge.canUseEnergyHandler()) {
         this.enterIdleState(WirelessEnergyDistributor.Status.APPFLUX_UNAVAILABLE, true);
         return false;
      } else {
         IManagedGridNode mainNode = this.host.getMainNode();
         if (mainNode.isActive() && mainNode.getGrid() != null) {
            BufferedStorageService proxy = this.ensureStorageProxy();
            BufferedMEStorage buffer = this.bufferedStorage;
            if (proxy != null && buffer != null) {
               buffer.advanceHistory();
               buffer.setCostMultiplier(1);
               this.refreshTargets();
               if (this.cachedValidTargets.isEmpty()) {
                  this.enterIdleState(
                     this.host.getValidTargets().isEmpty()
                        ? WirelessEnergyDistributor.Status.NO_CONNECTIONS
                        : WirelessEnergyDistributor.Status.NO_VALID_TARGETS,
                     false
                  );
                  return false;
               } else {
                  this.setStatus(WirelessEnergyDistributor.Status.IDLE);
                  this.lastTransferAmount = 0L;

                  boolean didWork;
                  try {
                     didWork = this.runNormalTick(serverLevel, buffer);
                  } finally {
                     AEKey feKey = AppFluxBridge.FE_KEY;
                     if (feKey != null) {
                        buffer.endTick(feKey, this.host.actionSource());
                     }
                  }

                  return didWork;
               }
            } else {
               this.setStatus(WirelessEnergyDistributor.Status.NO_GRID);
               return false;
            }
         } else {
            this.setStatus(WirelessEnergyDistributor.Status.NO_GRID);
            return false;
         }
      }
   }

   public void refreshTargets() {
      int version = this.host.getValidTargetsVersion();
      if (this.hostVersion != version) {
         this.hostVersion = version;
         List<WirelessEnergyAPI.Target> fresh = this.host.getValidTargets();
         if (!this.cachedValidTargets.equals(fresh)) {
            this.cachedValidTargets.clear();
            this.cachedValidTargets.addAll(fresh);
            this.cachedValidTargetSet.clear();
            this.cachedValidTargetSet.addAll(fresh);
            this.validTargetsVersion++;
            this.rebuildConnectionState();
         }
      }
   }

   public void onStateChanged() {
      if (this.bufferedStorage != null) {
         this.flushBufferToNetwork();
      }

      this.cachedValidTargets.clear();
      this.cachedValidTargetSet.clear();
      this.hostVersion = Integer.MIN_VALUE;
      this.clearTargetCaches();
      this.resetNormalWheel();
      if (!AppFluxBridge.canUseEnergyHandler()) {
         this.setStatus(WirelessEnergyDistributor.Status.APPFLUX_UNAVAILABLE);
      } else if (this.host.getValidTargets().isEmpty()) {
         this.setStatus(WirelessEnergyDistributor.Status.NO_CONNECTIONS);
      } else {
         this.setStatus(WirelessEnergyDistributor.Status.IDLE);
      }

      this.host.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }

   public void flushBufferToNetwork() {
      AEKey feKey = AppFluxBridge.FE_KEY;
      if (this.bufferedStorage != null) {
         if (feKey != null) {
            this.bufferedStorage.flushAll(feKey, this.host.actionSource());
         }

         this.bufferedStorage.clearBuffer();
      }
   }

   public void persistCellCache() {
      AEKey feKey = AppFluxBridge.FE_KEY;
      if (this.bufferedStorage != null && feKey != null) {
         this.bufferedStorage.flushAll(feKey, this.host.actionSource());
      }
   }

   public void clearTickState(boolean flushBuffer) {
      this.resetNormalWheel();
      if (flushBuffer) {
         this.flushBufferToNetwork();
      }
   }

   @Nullable
   public BufferedMEStorage prepareTick() {
      BufferedStorageService proxy = this.ensureStorageProxy();
      BufferedMEStorage buffer = this.bufferedStorage;
      if (proxy != null && buffer != null) {
         buffer.advanceHistory();
         this.refreshTargets();
         return buffer;
      } else {
         return null;
      }
   }

   public WirelessEnergyDistributor.Status getStatus() {
      return this.lastStatus;
   }

   public long getLastTransferAmount() {
      return this.lastTransferAmount;
   }

   public long getBufferedEnergy() {
      return this.bufferedStorage != null ? this.bufferedStorage.getBufferedEnergy() : 0L;
   }

   @Nullable
   public BufferedMEStorage getBufferedStorage() {
      return this.bufferedStorage;
   }

   @Nullable
   public BufferedStorageService getStorageProxy() {
      return this.storageProxy;
   }

   public int getValidTargetsVersion() {
      return this.validTargetsVersion;
   }

   public int getValidTargetCount() {
      return this.cachedValidTargets.size();
   }

   public WirelessEnergyAPI.Target getValidTarget(int index) {
      return this.cachedValidTargets.get(index);
   }

   @Nullable
   public TargetAccess resolveTargetAtIndex(int index, ServerLevel serverLevel) {
      if (index >= 0 && index < this.cachedValidTargets.size()) {
         WirelessEnergyDistributor.BlockEnergyTargetCache cache = this.connectionTargetCaches[index];
         return cache != null ? cache.resolve(serverLevel) : null;
      } else {
         return null;
      }
   }

   public void setCostMultiplier(int multiplier) {
      if (this.bufferedStorage != null) {
         this.bufferedStorage.setCostMultiplier(multiplier);
      }
   }

   public void setStatus(WirelessEnergyDistributor.Status status) {
      this.lastStatus = status;
      if (status != WirelessEnergyDistributor.Status.ACTIVE) {
         this.lastTransferAmount = 0L;
      }
   }

   public void recordActiveTransfer(long amount) {
      this.lastStatus = WirelessEnergyDistributor.Status.ACTIVE;
      this.lastTransferAmount += amount;
   }

   private boolean runNormalTick(ServerLevel serverLevel, BufferedMEStorage buffer) {
      if (this.normalWheelDirty || this.normalWheelTargetsVersion != this.validTargetsVersion) {
         this.rebuildNormalWheel();
      }

      this.normalWheelPointer = this.normalWheelPointer + 1 & 31;
      WirelessEnergyDistributor.IntWheelSlot due = this.pollNormalWheel();
      int dueSize = due.size;
      if (dueSize == 0) {
         return false;
      } else {
         int[] dueData = due.data;
         WirelessEnergyDistributor.BlockEnergyTargetCache[] caches = this.connectionTargetCaches;
         AEKey feKey = AppFluxBridge.FE_KEY;
         if (feKey != null && this.storageProxy != null) {
            this.ensureNormalBatchCapacity(dueSize);
            long[] demands = this.normalBatchDemand;
            TargetAccess[] resolvedTargets = this.normalBatchEnergyTargets;
            long totalDemand = 0L;
            long perCallLimit = Math.max(0L, AppFluxBridge.TRANSFER_RATE);

            for (int i = 0; i < dueSize; i++) {
               WirelessEnergyDistributor.BlockEnergyTargetCache cache = caches[dueData[i]];
               TargetAccess energyTarget = cache.resolve(serverLevel);
               resolvedTargets[i] = energyTarget;
               long demand = 0L;
               if (energyTarget != null) {
                  long sim = AppFluxBridge.simulateTarget(energyTarget, perCallLimit);
                  demand = sim > perCallLimit ? perCallLimit : sim;
               }

               demands[i] = demand;
               totalDemand += demand;
            }

            if (totalDemand <= 0L) {
               for (int i = 0; i < dueSize; i++) {
                  this.adjustAndScheduleNormalEntry(dueData[i], 0L);
                  resolvedTargets[i] = null;
               }

               due.size = 0;
               this.updateIdleFailureStatus(false, true);
               return false;
            } else {
               long pulled = buffer.beginMemoryBatch(feKey, totalDemand, this.host.actionSource());
               if (pulled <= 0L) {
                  boolean var39;
                  try {
                     this.requeueNormalEntriesSoon(due, 0);
                     this.updateIdleFailureStatus(false, false);
                     var39 = false;
                  } finally {
                     buffer.endBatch(feKey, this.host.actionSource());
                     Arrays.fill(resolvedTargets, 0, dueSize, null);
                     due.size = 0;
                  }

                  return var39;
               } else {
                  boolean didWork = false;
                  long remainingBudget = pulled;
                  IActionSource src = this.host.actionSource();

                  try {
                     for (int i = 0; i < dueSize; i++) {
                        int targetIndex = dueData[i];
                        long demand = demands[i];
                        if (demand <= 0L) {
                           this.adjustAndScheduleNormalEntry(targetIndex, 0L);
                        } else {
                           if (remainingBudget <= 0L) {
                              this.requeueNormalEntriesSoon(due, i);
                              break;
                           }

                           long requested = demand < remainingBudget ? demand : remainingBudget;
                           long pushed = AppFluxBridge.sendToTargetKnownDemand(resolvedTargets[i], buffer, src, requested);
                           if (pushed > 0L) {
                              this.lastStatus = WirelessEnergyDistributor.Status.ACTIVE;
                              this.lastTransferAmount += pushed;
                              didWork = true;
                              remainingBudget -= pushed;
                           }

                           this.adjustAndScheduleNormalEntry(targetIndex, pushed);
                        }
                     }
                  } finally {
                     buffer.endBatch(feKey, src);
                     Arrays.fill(resolvedTargets, 0, dueSize, null);
                     due.size = 0;
                  }

                  if (!didWork && this.lastStatus == WirelessEnergyDistributor.Status.IDLE) {
                     this.setStatus(WirelessEnergyDistributor.Status.TARGET_BLOCKED);
                  }

                  return didWork;
               }
            }
         } else {
            for (int ix = 0; ix < dueSize; ix++) {
               this.scheduleNormalEntry(dueData[ix]);
            }

            due.size = 0;
            return false;
         }
      }
   }

   private void enterIdleState(WirelessEnergyDistributor.Status status, boolean flushBuffer) {
      this.resetNormalWheel();
      if (flushBuffer) {
         this.flushBufferToNetwork();
      }

      this.setStatus(status);
   }

   private void rebuildConnectionState() {
      int n = this.cachedValidTargets.size();
      this.targetCachePool.keySet().retainAll(this.cachedValidTargetSet);
      if (this.connectionTargetCaches.length < n) {
         this.connectionTargetCaches = new WirelessEnergyDistributor.BlockEnergyTargetCache[Math.max(n, 64)];
      }

      for (int i = 0; i < n; i++) {
         this.connectionTargetCaches[i] = this.targetCachePool
            .computeIfAbsent(this.cachedValidTargets.get(i), x$0 -> new WirelessEnergyDistributor.BlockEnergyTargetCache(x$0));
      }

      for (int i = n; i < this.connectionTargetCaches.length; i++) {
         this.connectionTargetCaches[i] = null;
      }

      this.normalWheelDirty = true;
   }

   private void clearTargetCaches() {
      this.targetCachePool.clear();
      if (this.connectionTargetCaches.length > 0) {
         Arrays.fill(this.connectionTargetCaches, null);
      }
   }

   private WirelessEnergyDistributor.IntWheelSlot pollNormalWheel() {
      WirelessEnergyDistributor.IntWheelSlot slot = this.normalEnergyWheel[this.normalWheelPointer];
      if (slot.size == 0) {
         return slot;
      } else {
         WirelessEnergyDistributor.IntWheelSlot spare = this.spareNormalWheelSlot;
         spare.size = 0;
         this.normalEnergyWheel[this.normalWheelPointer] = spare;
         this.spareNormalWheelSlot = slot;
         return slot;
      }
   }

   private void scheduleNormalEntry(int targetIndex) {
      int delay = this.connectionTargetCaches[targetIndex].scheduleDelay;
      int slot = this.normalWheelPointer + delay & 31;
      this.normalEnergyWheel[slot].add(targetIndex);
   }

   private void requeueNormalEntriesSoon(WirelessEnergyDistributor.IntWheelSlot due, int startIndex) {
      if (startIndex < due.size) {
         int slotIdx = this.normalWheelPointer + 1 & 31;
         WirelessEnergyDistributor.IntWheelSlot targetSlot = this.normalEnergyWheel[slotIdx];
         int[] dueData = due.data;
         int dueSize = due.size;

         for (int i = startIndex; i < dueSize; i++) {
            targetSlot.add(dueData[i]);
         }
      }
   }

   private void rebuildNormalWheel() {
      for (WirelessEnergyDistributor.IntWheelSlot slot : this.normalEnergyWheel) {
         slot.clear();
      }

      this.spareNormalWheelSlot.clear();
      this.normalWheelPointer = 0;
      int size = this.cachedValidTargets.size();

      for (int i = 0; i < size; i++) {
         int initialSlot = this.normalWheelPointer + 1 + i & 31;
         this.normalEnergyWheel[initialSlot].add(i);
      }

      this.normalWheelTargetsVersion = this.validTargetsVersion;
      this.normalWheelDirty = false;
   }

   private void resetNormalWheel() {
      for (WirelessEnergyDistributor.IntWheelSlot slot : this.normalEnergyWheel) {
         slot.clear();
      }

      this.normalBatchDemand = new long[0];
      this.normalBatchEnergyTargets = new TargetAccess[0];
      this.spareNormalWheelSlot.clear();
      this.normalWheelPointer = 0;
      this.normalWheelTargetsVersion = -1;
      this.normalWheelDirty = true;
   }

   private void ensureNormalBatchCapacity(int size) {
      if (this.normalBatchDemand.length < size) {
         this.normalBatchDemand = new long[size];
         this.normalBatchEnergyTargets = new TargetAccess[size];
      }
   }

   private void adjustAndScheduleNormalEntry(int targetIndex, long pushed) {
      WirelessEnergyDistributor.BlockEnergyTargetCache cache = this.connectionTargetCaches[targetIndex];
      int delay = cache.scheduleDelay;
      if (pushed > 0L) {
         delay = delay > 5 ? delay / 2 : delay - 1;
      } else {
         delay++;
      }

      if (delay < 1) {
         delay = 1;
      } else if (delay > 20) {
         delay = 20;
      }

      cache.scheduleDelay = delay;
      int slot = this.normalWheelPointer + delay & 31;
      WirelessEnergyDistributor.IntWheelSlot ws = this.normalEnergyWheel[slot];
      int[] data = ws.data;
      int size = ws.size;
      if (size >= data.length) {
         data = Arrays.copyOf(data, data.length << 1);
         ws.data = data;
      }

      data[size] = targetIndex;
      ws.size = size + 1;
   }

   @Nullable
   private BufferedStorageService ensureStorageProxy() {
      IGrid grid = this.host.getMainNode().getGrid();
      if (grid == null) {
         this.flushBufferToNetwork();
         this.delegateStorageService = null;
         this.bufferedStorage = null;
         this.storageProxy = null;
         return null;
      } else {
         IStorageService storageService = grid.getStorageService();
         if (this.storageProxy == null || this.bufferedStorage == null || this.delegateStorageService != storageService) {
            if (this.bufferedStorage != null) {
               this.flushBufferToNetwork();
            }

            this.bufferedStorage = new BufferedMEStorage(storageService.getInventory(), this.host.getCellStorageSupplier(), this.host.getCellPersistCallback());
            this.storageProxy = new BufferedStorageService(storageService, this.bufferedStorage);
            this.delegateStorageService = storageService;
         }

         return this.storageProxy;
      }
   }

   private void updateIdleFailureStatus(boolean didWork, boolean targetSideFailure) {
      if (!didWork && this.lastStatus == WirelessEnergyDistributor.Status.IDLE) {
         this.setStatus(targetSideFailure ? WirelessEnergyDistributor.Status.TARGET_BLOCKED : WirelessEnergyDistributor.Status.NO_NETWORK_FE);
      }
   }

   public final class BlockEnergyTargetCache {
      private final WirelessEnergyAPI.Target target;
      int scheduleDelay = 5;
      @Nullable
      private Object capCache;
      @Nullable
      private ServerLevel cachedLevel;

      private BlockEnergyTargetCache(WirelessEnergyAPI.Target target) {
         this.target = target;
      }

      @Nullable
      TargetAccess resolve(ServerLevel providerLevel) {
         ServerLevel targetLevel = WirelessEnergyAPI.resolveLevel(providerLevel.m_7654_(), this.target);
         if (targetLevel == null) {
            this.cachedLevel = null;
            this.capCache = null;
            return null;
         } else {
            if (this.capCache == null || this.cachedLevel != targetLevel) {
               this.capCache = WirelessEnergyAPI.resolveCapCache(providerLevel, this.target, () -> WirelessEnergyDistributor.this.host.getMainNode().getGrid());
               this.cachedLevel = targetLevel;
            }

            return this.capCache == null ? null : WirelessEnergyAPI.resolveEnergyTarget(this.capCache, this.target.face());
         }
      }
   }

   public interface Host {
      IManagedGridNode getMainNode();

      IActionSource actionSource();

      boolean isHostRemoved();

      List<WirelessEnergyAPI.Target> getValidTargets();

      int getValidTargetsVersion();

      @Nullable
      default Supplier<MEStorage> getCellStorageSupplier() {
         return null;
      }

      @Nullable
      default Runnable getCellPersistCallback() {
         return null;
      }
   }

   private static final class IntWheelSlot {
      int[] data = new int[32];
      int size;

      void add(int i) {
         if (this.size >= this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length << 1);
         }

         this.data[this.size++] = i;
      }

      void clear() {
         this.size = 0;
      }
   }

   public static enum Status {
      IDLE,
      APPFLUX_UNAVAILABLE,
      NO_GRID,
      NO_CONNECTIONS,
      NO_VALID_TARGETS,
      NO_NETWORK_FE,
      TARGET_UNSUPPORTED,
      TARGET_BLOCKED,
      ACTIVE;
   }
}
