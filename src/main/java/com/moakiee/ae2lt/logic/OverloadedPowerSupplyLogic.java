package com.moakiee.ae2lt.logic;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.logic.energy.AppFluxBridge;
import com.moakiee.ae2lt.logic.energy.BufferedMEStorage;
import com.moakiee.ae2lt.logic.energy.TargetAccess;
import com.moakiee.ae2lt.logic.energy.WirelessEnergyAPI;
import com.moakiee.ae2lt.logic.energy.WirelessEnergyDistributor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;

public class OverloadedPowerSupplyLogic implements IGridTickable {
   private static final int TICK_MIN = 1;
   private static final int TICK_MAX = 20;
   private static final int OVERLOAD_MAX_CONNECTIONS = 64;
   private static final int OVERLOAD_MAX_CALLS = 64;
   private static final int TICKET_DURATION = 20;
   private static final int SENTINEL_BUCKETS = 5;
   private static final int REVALIDATION_INTERVAL = 100;
   private final OverloadedPowerSupplyBlockEntity host;
   private final IActionSource actionSource;
   private final WirelessEnergyDistributor distributor;
   private List<OverloadedPowerSupplyBlockEntity.WirelessConnection> cachedConnections = List.of();
   private List<WirelessEnergyAPI.Target> cachedConnectionTargets = List.of();
   private final List<WirelessEnergyAPI.Target> cachedValidTargets = new ArrayList<>();
   private final ArrayList<WirelessEnergyAPI.Target> nextValidTargets = new ArrayList<>();
   private final ArrayList<OverloadedPowerSupplyBlockEntity.WirelessConnection> invalidConnections = new ArrayList<>();
   private List<WirelessEnergyAPI.Target> exposedValidTargets = List.of();
   private long validTargetsCacheTick = Long.MIN_VALUE;
   private int validTargetsVersion;
   private final int revalidationOffset = (System.identityHashCode(this) & 2147483647) % 100;
   private int cachedConnectionVersion = -1;
   private long[] connectionTicketExpiry = new long[0];
   private WirelessEnergyAPI.Target[] overloadBatchTargets = new WirelessEnergyAPI.Target[64];
   private TargetAccess[] overloadBatchEnergyTargets = new TargetAccess[64];
   private boolean[] overloadBatchTicketed = new boolean[64];
   private int overloadBatchSize;
   private int sentinelIndex;
   private OverloadedPowerSupplyLogic.Status lastStatus = OverloadedPowerSupplyLogic.Status.IDLE;
   private long lastTransferAmount;

   public OverloadedPowerSupplyLogic(OverloadedPowerSupplyBlockEntity host) {
      this.host = host;
      this.actionSource = IActionSource.ofMachine(host);
      this.distributor = new WirelessEnergyDistributor(new OverloadedPowerSupplyLogic.DistributorHost());
   }

   public TickingRequest getTickingRequest(IGridNode node) {
      return new TickingRequest(1, 20, false, true);
   }

   public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
      if (this.host.m_58904_() instanceof ServerLevel serverLevel) {
         boolean var5 = this.tick(serverLevel);
         this.host
            .updateVisualState(
               this.lastStatus == OverloadedPowerSupplyLogic.Status.ACTIVE, this.host.getMode() == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD
            );
         if (this.isOverloadActive() && this.getActiveTicketCount() > 0) {
            return TickRateModulation.URGENT;
         } else if (this.host.getConnections().isEmpty()) {
            return TickRateModulation.IDLE;
         } else if (this.host.getMode() == OverloadedPowerSupplyBlockEntity.PowerMode.NORMAL) {
            return TickRateModulation.URGENT;
         } else {
            return var5 ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
         }
      } else {
         return TickRateModulation.SLEEP;
      }
   }

   private boolean isOverloadActive() {
      return this.host.getMode() == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD && this.host.getBufferCapacity() > 0L;
   }

   public void onStateChanged() {
      this.distributor.flushBufferToNetwork();
      if (!this.isOverloadActive() && this.connectionTicketExpiry.length > 0) {
         Arrays.fill(this.connectionTicketExpiry, 0L);
      }

      this.cachedConnections = List.of();
      this.cachedConnectionTargets = List.of();
      this.cachedValidTargets.clear();
      this.exposedValidTargets = List.of();
      this.validTargetsCacheTick = Long.MIN_VALUE;
      this.cachedConnectionVersion = -1;
      this.sentinelIndex = 0;
      this.distributor.clearTickState(false);
      if (!AppFluxBridge.canUseEnergyHandler()) {
         this.setStatus(OverloadedPowerSupplyLogic.Status.APPFLUX_UNAVAILABLE);
      } else if (this.host.getConnections().isEmpty()) {
         this.setStatus(OverloadedPowerSupplyLogic.Status.NO_CONNECTIONS);
      } else {
         this.setStatus(OverloadedPowerSupplyLogic.Status.IDLE);
      }

      this.host.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
   }

   public void flushBufferToNetwork() {
      this.distributor.flushBufferToNetwork();
   }

   public void persistCellCache() {
      this.distributor.persistCellCache();
   }

   public long getBufferedEnergy() {
      return this.distributor.getBufferedEnergy();
   }

   public int getActiveTicketCount() {
      long[] expiry = this.connectionTicketExpiry;
      int n = Math.min(expiry.length, this.cachedValidTargets.size());
      if (n == 0) {
         return 0;
      } else if (this.host.m_58904_() instanceof ServerLevel serverLevel) {
         long var9 = serverLevel.m_46467_();
         int count = 0;

         for (int i = 0; i < n; i++) {
            if (expiry[i] >= var9) {
               count++;
            }
         }

         return count;
      } else {
         int count = 0;

         for (int ix = 0; ix < n; ix++) {
            if (expiry[ix] > 0L) {
               count++;
            }
         }

         return count;
      }
   }

   public OverloadedPowerSupplyLogic.Status getLastStatus() {
      return this.lastStatus;
   }

   public long getLastTransferAmount() {
      return this.lastTransferAmount;
   }

   private boolean tick(ServerLevel serverLevel) {
      if (!AppFluxBridge.canUseEnergyHandler()) {
         this.enterIdleState(OverloadedPowerSupplyLogic.Status.APPFLUX_UNAVAILABLE, true);
         return false;
      } else {
         IManagedGridNode mainNode = this.host.getMainNode();
         if (mainNode.isActive() && mainNode.getGrid() != null) {
            boolean wantsOverload = this.host.getMode() == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD;
            boolean hasCell = this.host.getBufferCapacity() > 0L;
            boolean overloadActive = wantsOverload && hasCell;
            if (wantsOverload && !hasCell) {
               this.enterIdleState(OverloadedPowerSupplyLogic.Status.NO_CELL, false);
               return false;
            } else {
               List<WirelessEnergyAPI.Target> validTargets = this.getValidTargets(serverLevel);
               if (validTargets.isEmpty()) {
                  this.enterIdleState(
                     this.host.getConnections().isEmpty()
                        ? OverloadedPowerSupplyLogic.Status.NO_CONNECTIONS
                        : OverloadedPowerSupplyLogic.Status.NO_VALID_TARGETS,
                     overloadActive
                  );
                  return false;
               } else {
                  return !overloadActive ? this.tickNormalDelegate(serverLevel) : this.tickOverloadFlow(serverLevel);
               }
            }
         } else {
            this.setStatus(OverloadedPowerSupplyLogic.Status.NO_GRID);
            return false;
         }
      }
   }

   private boolean tickNormalDelegate(ServerLevel serverLevel) {
      boolean didWork = this.distributor.tickNormal(serverLevel);
      this.lastStatus = mapStatus(this.distributor.getStatus());
      this.lastTransferAmount = this.distributor.getLastTransferAmount();
      return didWork;
   }

   private boolean tickOverloadFlow(ServerLevel serverLevel) {
      BufferedMEStorage buffer = this.distributor.prepareTick();
      if (buffer == null) {
         this.setStatus(OverloadedPowerSupplyLogic.Status.NO_GRID);
         return false;
      } else {
         buffer.setCostMultiplier(2);
         this.setStatus(OverloadedPowerSupplyLogic.Status.IDLE);
         this.lastTransferAmount = 0L;

         boolean didWork;
         try {
            didWork = this.tickOverloadStaged(serverLevel);
         } finally {
            AEKey feKey = AppFluxBridge.FE_KEY;
            if (feKey != null) {
               buffer.endTick(feKey, this.actionSource);
            }
         }

         return didWork;
      }
   }

   private void enterIdleState(OverloadedPowerSupplyLogic.Status status, boolean flushBuffer) {
      if (this.connectionTicketExpiry.length > 0) {
         Arrays.fill(this.connectionTicketExpiry, 0L);
      }

      this.distributor.clearTickState(flushBuffer);
      this.setStatus(status);
   }

   private boolean tickOverloadStaged(ServerLevel serverLevel) {
      long gameTime = serverLevel.m_46467_();

      boolean expiry;
      try {
         int targetCount = this.prepareOverloadTargets(serverLevel);
         if (targetCount != 0) {
            long[] expiryx = this.connectionTicketExpiry;
            boolean[] ticketed = this.overloadBatchTicketed;
            TargetAccess[] energyTargets = this.overloadBatchEnergyTargets;
            int idleCount = 0;

            for (int i = 0; i < targetCount; i++) {
               TargetAccess energyTarget = energyTargets[i];
               if (energyTarget == null) {
                  ticketed[i] = false;
               } else {
                  boolean hasTicket = expiryx[i] >= gameTime;
                  ticketed[i] = hasTicket;
                  if (!hasTicket) {
                     idleCount++;
                  }
               }
            }

            boolean didWork = false;
            int scans = idleCount == 0 ? 0 : Math.max(1, (idleCount + 5 - 1) / 5);
            if (idleCount <= 0) {
               this.sentinelIndex = 0;
            } else {
               int index = this.sentinelIndex % targetCount;
               int visited = 0;
               int found = 0;

               while (visited < targetCount && found < scans) {
                  int targetIndex = index;
                  index = (index + 1) % targetCount;
                  visited++;
                  TargetAccess energyTarget = energyTargets[targetIndex];
                  if (!ticketed[targetIndex] && energyTarget != null) {
                     found++;
                     long pushed = this.pushPrepared(energyTarget, 1);
                     if (pushed > 0L) {
                        expiryx[targetIndex] = gameTime + 20L;
                        ticketed[targetIndex] = true;
                        didWork = true;
                     }
                  }
               }

               this.sentinelIndex = index;
            }

            for (int ix = 0; ix < targetCount; ix++) {
               if (ticketed[ix]) {
                  long pushed = this.pushPrepared(energyTargets[ix], 64);
                  if (pushed > 0L) {
                     expiryx[ix] = gameTime + 20L;
                     didWork = true;
                  }
               }
            }

            this.updateIdleFailureStatus(didWork, true);
            return didWork;
         }

         this.setStatus(OverloadedPowerSupplyLogic.Status.NO_VALID_TARGETS);
         expiry = false;
      } finally {
         this.clearOverloadBatch();
      }

      return expiry;
   }

   private int prepareOverloadTargets(ServerLevel serverLevel) {
      int targetCount = Math.min(this.distributor.getValidTargetCount(), 64);
      this.ensureOverloadBatchCapacity(targetCount);
      long[] expiry = this.connectionTicketExpiry;
      WirelessEnergyAPI.Target[] batchTargets = this.overloadBatchTargets;
      TargetAccess[] batchEnergyTargets = this.overloadBatchEnergyTargets;

      for (int i = 0; i < targetCount; i++) {
         WirelessEnergyAPI.Target target = this.distributor.getValidTarget(i);
         TargetAccess energyTarget = this.distributor.resolveTargetAtIndex(i, serverLevel);
         batchTargets[i] = target;
         batchEnergyTargets[i] = energyTarget;
         if (energyTarget == null && i < expiry.length) {
            expiry[i] = 0L;
         }
      }

      this.overloadBatchSize = targetCount;
      return targetCount;
   }

   private void ensureOverloadBatchCapacity(int size) {
      if (this.overloadBatchTargets.length < size) {
         this.overloadBatchTargets = new WirelessEnergyAPI.Target[size];
      }

      if (this.overloadBatchEnergyTargets.length < size) {
         this.overloadBatchEnergyTargets = new TargetAccess[size];
      }

      if (this.overloadBatchTicketed.length < size) {
         this.overloadBatchTicketed = new boolean[size];
      }
   }

   private void clearOverloadBatch() {
      for (int i = 0; i < this.overloadBatchSize; i++) {
         this.overloadBatchTargets[i] = null;
         this.overloadBatchEnergyTargets[i] = null;
         this.overloadBatchTicketed[i] = false;
      }

      this.overloadBatchSize = 0;
   }

   private long pushPrepared(TargetAccess energyTarget, int maxCalls) {
      BufferedMEStorage buffer = this.distributor.getBufferedStorage();
      if (buffer == null) {
         return 0L;
      } else {
         long pushed = WirelessEnergyAPI.sendToTargetRepeatedOptimistic(
            energyTarget, buffer, this.actionSource, Math.max(0L, AppFluxBridge.TRANSFER_RATE), maxCalls
         );
         if (pushed > 0L) {
            this.setActive(pushed);
            return pushed;
         } else {
            if (pushed < 0L) {
               this.setStatus(OverloadedPowerSupplyLogic.Status.TARGET_UNSUPPORTED);
            }

            return 0L;
         }
      }
   }

   private List<WirelessEnergyAPI.Target> getValidTargets(ServerLevel serverLevel) {
      int version = this.host.getConnectionVersion();
      if (this.cachedConnectionVersion != version) {
         List<OverloadedPowerSupplyBlockEntity.WirelessConnection> connections = this.host.getConnections();
         ArrayList<WirelessEnergyAPI.Target> rebuilt = new ArrayList<>(connections.size());

         for (OverloadedPowerSupplyBlockEntity.WirelessConnection conn : connections) {
            rebuilt.add(new WirelessEnergyAPI.Target(conn.dimension(), conn.pos(), conn.boundFace()));
         }

         this.cachedConnections = List.copyOf(connections);
         this.cachedConnectionTargets = List.copyOf(rebuilt);
         this.cachedValidTargets.clear();
         this.exposedValidTargets = List.of();
         this.validTargetsCacheTick = Long.MIN_VALUE;
         this.cachedConnectionVersion = version;
      }

      long gameTime = serverLevel.m_46467_();
      if (this.validTargetsCacheTick == Long.MIN_VALUE || (gameTime + (long)this.revalidationOffset) % 100L == 0L) {
         this.nextValidTargets.clear();
         this.invalidConnections.clear();

         for (int i = 0; i < this.cachedConnectionTargets.size(); i++) {
            WirelessEnergyAPI.Target target = this.cachedConnectionTargets.get(i);
            switch (WirelessConnectionValidator.validate(serverLevel, this.host.m_58899_(), target.dimension(), target.pos())) {
               case REMOVE:
                  this.invalidConnections.add(this.cachedConnections.get(i));
                  break;
               case VALID:
                  this.nextValidTargets.add(target);
               case UNLOADED:
            }
         }

         if (!this.invalidConnections.isEmpty()) {
            this.host.removeConnections(this.invalidConnections);
            this.invalidConnections.clear();
            return this.getValidTargets(serverLevel);
         }

         if (!this.cachedValidTargets.equals(this.nextValidTargets)) {
            this.cachedValidTargets.clear();
            this.cachedValidTargets.addAll(this.nextValidTargets);
            this.exposedValidTargets = List.copyOf(this.cachedValidTargets);
            this.validTargetsVersion++;
            this.onValidTargetsChanged();
         }

         this.validTargetsCacheTick = gameTime;
      }

      return this.cachedValidTargets;
   }

   private void onValidTargetsChanged() {
      int n = this.cachedValidTargets.size();
      if (this.connectionTicketExpiry.length < n) {
         this.connectionTicketExpiry = new long[Math.max(n, 64)];
      } else if (this.connectionTicketExpiry.length > 0) {
         Arrays.fill(this.connectionTicketExpiry, 0L);
      }
   }

   private void updateIdleFailureStatus(boolean didWork, boolean hadEnergyBudget) {
      if (!didWork && this.lastStatus == OverloadedPowerSupplyLogic.Status.IDLE) {
         this.setStatus(hadEnergyBudget ? OverloadedPowerSupplyLogic.Status.TARGET_BLOCKED : OverloadedPowerSupplyLogic.Status.NO_NETWORK_FE);
      }
   }

   private void setStatus(OverloadedPowerSupplyLogic.Status status) {
      this.lastStatus = status;
      if (status != OverloadedPowerSupplyLogic.Status.ACTIVE) {
         this.lastTransferAmount = 0L;
      }
   }

   private void setActive(long amount) {
      this.lastStatus = OverloadedPowerSupplyLogic.Status.ACTIVE;
      this.lastTransferAmount += amount;
   }

   private static OverloadedPowerSupplyLogic.Status mapStatus(WirelessEnergyDistributor.Status dist) {
      return switch (dist) {
         case IDLE -> OverloadedPowerSupplyLogic.Status.IDLE;
         case APPFLUX_UNAVAILABLE -> OverloadedPowerSupplyLogic.Status.APPFLUX_UNAVAILABLE;
         case NO_GRID -> OverloadedPowerSupplyLogic.Status.NO_GRID;
         case NO_CONNECTIONS -> OverloadedPowerSupplyLogic.Status.NO_CONNECTIONS;
         case NO_VALID_TARGETS -> OverloadedPowerSupplyLogic.Status.NO_VALID_TARGETS;
         case NO_NETWORK_FE -> OverloadedPowerSupplyLogic.Status.NO_NETWORK_FE;
         case TARGET_UNSUPPORTED -> OverloadedPowerSupplyLogic.Status.TARGET_UNSUPPORTED;
         case TARGET_BLOCKED -> OverloadedPowerSupplyLogic.Status.TARGET_BLOCKED;
         case ACTIVE -> OverloadedPowerSupplyLogic.Status.ACTIVE;
      };
   }

   private final class DistributorHost implements WirelessEnergyDistributor.Host {
      @Override
      public IManagedGridNode getMainNode() {
         return OverloadedPowerSupplyLogic.this.host.getMainNode();
      }

      @Override
      public IActionSource actionSource() {
         return OverloadedPowerSupplyLogic.this.actionSource;
      }

      @Override
      public boolean isHostRemoved() {
         return OverloadedPowerSupplyLogic.this.host.m_58901_();
      }

      @Override
      public List<WirelessEnergyAPI.Target> getValidTargets() {
         return OverloadedPowerSupplyLogic.this.exposedValidTargets;
      }

      @Override
      public int getValidTargetsVersion() {
         return OverloadedPowerSupplyLogic.this.validTargetsVersion;
      }

      @Override
      public Supplier<MEStorage> getCellStorageSupplier() {
         return OverloadedPowerSupplyLogic.this.host::getInstalledCellStorage;
      }

      @Override
      public Runnable getCellPersistCallback() {
         return OverloadedPowerSupplyLogic.this.host::persistCellStorage;
      }
   }

   public static enum Status {
      IDLE,
      APPFLUX_UNAVAILABLE,
      NO_CELL,
      NO_GRID,
      NO_CONNECTIONS,
      NO_VALID_TARGETS,
      NO_NETWORK_FE,
      TARGET_UNSUPPORTED,
      TARGET_BLOCKED,
      ACTIVE;
   }
}
