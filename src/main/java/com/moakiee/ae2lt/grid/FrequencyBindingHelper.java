package com.moakiee.ae2lt.grid;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener.State;
import appeng.api.networking.pathing.ChannelMode;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.util.SettingsFrom;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingAccess;
import com.moakiee.ae2lt.blockentity.OverloadedControllerBlockEntity;
import com.moakiee.ae2lt.grid.wirelesslink.MultiblockLinkReadiness;
import com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkOps;
import com.moakiee.ae2lt.logic.MemoryCardConfigSupport;
import com.moakiee.ae2lt.me.GridNodeAccess;
import com.moakiee.thunderbolt.core.channel.HighCapacityChannelSupport;
import java.util.function.IntConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FrequencyBindingHelper implements WirelessFrequencyManager.TransmitterListener, FrequencyBindingAccess {
   public static final String TAG_FREQUENCY_ID = "FrequencyId";
   public static final String TAG_MEMORY_FREQUENCY = "Frequency";
   private static final Logger LOG = LoggerFactory.getLogger("ae2lt-wireless");
   private static final int INITIAL_RETRY_COOLDOWN_TICKS = 20;
   private static final int MAX_RETRY_COOLDOWN_TICKS = 200;
   private static final int CONNECTION_AUDIT_INTERVAL_TICKS = 20;
   private final com.moakiee.ae2lt.api.frequency.FrequencyBindingHost host;
   private int frequencyId = -1;
   @Nullable
   private IGridConnection virtualConnection;
   private boolean needsConnectionUpdate;
   private int retryCooldownTicks;
   private int nextRetryCooldownTicks = 20;
   private int connectionAuditTicks = 20;
   private int subscribedFrequencyId = -1;

   public FrequencyBindingHelper(com.moakiee.ae2lt.api.frequency.FrequencyBindingHost host) {
      this.host = host;
   }

   @Override
   public int getFrequencyId() {
      return this.frequencyId;
   }

   @Override
   public void setFrequency(int newFreqId) {
      if (newFreqId != this.frequencyId) {
         this.detach();
         this.frequencyId = newFreqId;
         this.attach();
         this.host.saveFrequencyBindingChanges();
         this.host.markFrequencyBindingForUpdate();
      }
   }

   @Override
   public void clearFrequency() {
      this.detach();
      this.frequencyId = -1;
      this.host.saveFrequencyBindingChanges();
      this.host.markFrequencyBindingForUpdate();
   }

   @Override
   public void onTransmitterChanged(int freqId, boolean available) {
      if (freqId == this.frequencyId) {
         if (!available) {
            WirelessFrequencyManager manager = WirelessFrequencyManager.get();
            if (manager != null && !manager.isFrequencyValid(freqId)) {
               this.detach();
               this.frequencyId = -1;
               this.host.saveFrequencyBindingChanges();
               this.host.markFrequencyBindingForUpdate();
               return;
            }
         }

         this.requestConnectionUpdate();
      }
   }

   @Override
   public void onMainNodeStateChanged(State reason) {
      if (reason == State.GRID_BOOT) {
         this.requestConnectionUpdate();
      }
   }

   @Override
   public void serverTick() {
      AENetworkBlockEntity be = this.host.getFrequencyBindingBlockEntity();
      if (this.frequencyId > 0 && be.m_58904_() != null && !be.m_58904_().m_5776_()) {
         this.auditConnectionIfDue();
         if (this.retryCooldownTicks > 0) {
            this.retryCooldownTicks--;
         } else if (this.needsConnectionUpdate) {
            if (be.getMainNode().getNode() == null) {
               this.scheduleRetry();
            } else {
               this.needsConnectionUpdate = false;
               boolean wasConnected = this.hasLiveVirtualConnection();
               if (this.virtualConnection != null) {
                  this.revalidateConnection();
               }

               if (this.virtualConnection == null) {
                  this.tryEstablishConnection();
               }

               boolean connected = this.hasLiveVirtualConnection();
               if (connected) {
                  this.resetRetryBackoff();
               }

               if (connected != wasConnected) {
                  this.host.markFrequencyBindingForUpdate();
               }
            }
         }
      } else {
         this.needsConnectionUpdate = false;
      }
   }

   @Override
   public void onReady() {
      if (this.frequencyId > 0) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager != null && !manager.isFrequencyValid(this.frequencyId)) {
            this.detach();
            this.frequencyId = -1;
            this.host.saveFrequencyBindingChanges();
            return;
         }
      }

      this.attach();
   }

   @Override
   public void setRemoved() {
      this.detach();
   }

   @Override
   public void clearRemoved() {
      this.attach();
   }

   public void onChunkUnloaded() {
      this.unsubscribeListener();
      this.clearConnectionUpdate();
      this.virtualConnection = null;
   }

   @Override
   public void save(CompoundTag tag) {
      tag.m_128405_("FrequencyId", this.frequencyId);
   }

   @Override
   public void load(CompoundTag tag) {
      this.frequencyId = tag.m_128441_("FrequencyId") ? tag.m_128451_("FrequencyId") : -1;
   }

   public static void writeMemoryFrequency(CompoundTag tag, int frequencyId) {
      if (frequencyId > 0) {
         tag.m_128405_("Frequency", frequencyId);
      }
   }

   public static boolean importMemoryFrequency(CompoundTag tag, IntConsumer setter) {
      if (!tag.m_128441_("Frequency")) {
         return false;
      } else {
         setter.accept(tag.m_128451_("Frequency"));
         return true;
      }
   }

   public static void exportMemorySettings(SettingsFrom mode, CompoundTag output, int frequencyId) {
      MemoryCardConfigSupport.exportMemoryCardSettings(mode, output, tag -> writeMemoryFrequency(tag, frequencyId));
   }

   public static void importMemorySettings(SettingsFrom mode, CompoundTag input, IntConsumer setter) {
      MemoryCardConfigSupport.importMemoryCardSettings(mode, input, tag -> importMemoryFrequency(tag, setter));
   }

   @Override
   public int getGridUsedChannels() {
      IGrid grid = GridNodeAccess.getGridIfPresent(this.host.getFrequencyBindingBlockEntity().getMainNode().getNode());
      return grid == null ? 0 : HighCapacityChannelSupport.countUsedChannels(grid);
   }

   @Override
   public int getGridMaxChannels() {
      IGrid grid = GridNodeAccess.getGridIfPresent(this.host.getFrequencyBindingBlockEntity().getMainNode().getNode());
      if (grid == null) {
         return 0;
      } else {
         ChannelMode channelMode = grid.getPathingService().getChannelMode();
         if (channelMode == ChannelMode.INFINITE) {
            return -1;
         } else {
            int overloadedCount = 0;
            int vanillaCount = 0;

            for (IGridNode node : HighCapacityChannelSupport.getAllControllerNodes(grid)) {
               if (node.getOwner() instanceof OverloadedControllerBlockEntity) {
                  overloadedCount++;
               } else {
                  vanillaCount++;
               }
            }

            int factor = Math.max(1, channelMode.getCableCapacityFactor());
            long cap = (long)overloadedCount * (long)HighCapacityChannelSupport.channelsPerController() * (long)factor
               + (long)vanillaCount * 32L * (long)factor;
            return (int)Math.min(2147483647L, cap);
         }
      }
   }

   @Override
   public boolean isConnected() {
      return this.hasEffectiveFrequencyConnection();
   }

   private boolean hasEffectiveFrequencyConnection() {
      if (this.hasLiveVirtualConnection()) {
         return true;
      } else {
         AENetworkBlockEntity be = this.host.getFrequencyBindingBlockEntity();
         if (this.frequencyId > 0 && be.m_58904_() instanceof ServerLevel serverLevel) {
            IGridNode var7 = be.getMainNode().getNode();
            WirelessFrequencyManager manager = WirelessFrequencyManager.get();
            IGridNode remoteNode = manager == null ? null : manager.resolveNode(this.frequencyId, serverLevel.m_7654_());
            if (var7 != null && remoteNode != null) {
               boolean alreadyInFrequencyGrid = isAlreadyInFrequencyGrid(var7, remoteNode);
               return hasEffectiveConnection(false, alreadyInFrequencyGrid, alreadyInFrequencyGrid && var7.meetsChannelRequirements());
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   static boolean hasEffectiveConnection(boolean liveVirtualConnection, boolean alreadyInFrequencyGrid, boolean meetsChannelRequirements) {
      return liveVirtualConnection || alreadyInFrequencyGrid && meetsChannelRequirements;
   }

   private boolean hasLiveVirtualConnection() {
      if (this.virtualConnection == null) {
         return false;
      } else {
         IGridNode myNode = this.host.getFrequencyBindingBlockEntity().getMainNode().getNode();
         if (myNode == null) {
            return false;
         } else {
            for (IGridConnection conn : myNode.getConnections()) {
               if (conn == this.virtualConnection && !conn.isInWorld()) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private void detach() {
      this.clearConnectionUpdate();
      this.unsubscribeListener();
      this.destroyVirtualConnection();
      this.unregisterDeviceIfBound();
   }

   private void attach() {
      this.subscribeListener();
      this.registerDevice();
      if (this.frequencyId > 0) {
         this.requestConnectionUpdate();
      }
   }

   private void requestConnectionUpdate() {
      this.needsConnectionUpdate = true;
      this.retryCooldownTicks = 0;
      this.resetRetryBackoff();
      this.resetConnectionAudit();
   }

   private void clearConnectionUpdate() {
      this.needsConnectionUpdate = false;
      this.retryCooldownTicks = 0;
      this.resetRetryBackoff();
      this.resetConnectionAudit();
   }

   private void resetConnectionAudit() {
      this.connectionAuditTicks = 20;
   }

   private void auditConnectionIfDue() {
      if (--this.connectionAuditTicks <= 0) {
         this.resetConnectionAudit();
         if (this.virtualConnection != null) {
            this.revalidateConnection();
         }

         if (!this.hasEffectiveFrequencyConnection() && !this.needsConnectionUpdate) {
            this.scheduleRetry();
         }
      }
   }

   private void resetRetryBackoff() {
      this.nextRetryCooldownTicks = 20;
   }

   private void scheduleRetry() {
      this.needsConnectionUpdate = true;
      this.retryCooldownTicks = this.nextRetryCooldownTicks;
      this.nextRetryCooldownTicks = Math.min(this.nextRetryCooldownTicks * 2, 200);
   }

   private void unregisterDeviceIfBound() {
      if (this.frequencyId > 0) {
         AENetworkBlockEntity be = this.host.getFrequencyBindingBlockEntity();
         if (be.m_58904_() != null) {
            WirelessFrequencyManager manager = WirelessFrequencyManager.get();
            if (manager != null) {
               manager.unregisterDevice(this.frequencyId, be.m_58904_().m_46472_(), be.m_58899_());
            }
         }
      }
   }

   private void subscribeListener() {
      if (this.frequencyId > 0) {
         if (this.subscribedFrequencyId != this.frequencyId) {
            if (this.subscribedFrequencyId > 0) {
               this.unsubscribeListener();
            }

            WirelessFrequencyManager manager = WirelessFrequencyManager.get();
            if (manager != null) {
               manager.addListener(this.frequencyId, this);
               this.subscribedFrequencyId = this.frequencyId;
            }
         }
      }
   }

   private void unsubscribeListener() {
      if (this.subscribedFrequencyId > 0) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager != null) {
            manager.removeListener(this.subscribedFrequencyId, this);
         }

         this.subscribedFrequencyId = -1;
      }
   }

   private void registerDevice() {
      if (this.frequencyId > 0) {
         AENetworkBlockEntity be = this.host.getFrequencyBindingBlockEntity();
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         if (manager != null && be.m_58904_() != null) {
            manager.registerDevice(
               this.frequencyId,
               new WirelessFrequencyManager.DeviceEntry(be.m_58904_().m_46472_(), be.m_58899_(), false, false, this.host.getFrequencyBindingDeviceName())
            );
         }
      }
   }

   private void tryEstablishConnection() {
      AENetworkBlockEntity be = this.host.getFrequencyBindingBlockEntity();
      if (this.frequencyId > 0 && be.m_58904_() != null && !be.m_58904_().m_5776_()) {
         if (this.virtualConnection == null) {
            IGridNode myNode = be.getMainNode().getNode();
            if (myNode == null) {
               this.scheduleRetry();
            } else if (!MultiblockLinkReadiness.canKeepVirtualConnection(myNode)) {
               MultiblockLinkReadiness.refreshAfterVirtualConnectionRemoved(myNode);
               this.scheduleRetry();
            } else {
               WirelessFrequencyManager manager = WirelessFrequencyManager.get();
               if (manager != null) {
                  WirelessFrequencyManager.TransmitterEntry entry = manager.findTransmitter(this.frequencyId);
                  if (entry == null) {
                     this.scheduleRetry();
                  } else if (entry.advanced() || be.m_58904_().m_46472_().equals(entry.dimension())) {
                     MinecraftServer server = ((ServerLevel)be.m_58904_()).m_7654_();
                     IGridNode remoteNode = manager.resolveNode(this.frequencyId, server);
                     if (remoteNode == null) {
                        this.scheduleRetry();
                     } else if (!alreadyHasFrequencyChannel(myNode, remoteNode)) {
                        if (wouldMergeControllerNetworks(GridNodeAccess.getGridIfPresent(myNode), GridNodeAccess.getGridIfPresent(remoteNode))) {
                           LOG.warn("Virtual connection blocked to avoid controller-network merge: device@{} -> freq={}", be.m_58899_(), this.frequencyId);
                        } else {
                           try {
                              this.virtualConnection = WirelessLinkOps.createVirtualConnection(myNode, remoteNode);
                              LOG.debug("Virtual connection established: device@{} -> freq={}", be.m_58899_(), this.frequencyId);
                           } catch (IllegalStateException var8) {
                              LOG.warn("Virtual connection FAILED: device@{} -> freq={}: {}", new Object[]{be.m_58899_(), this.frequencyId, var8.getMessage()});
                              this.scheduleRetry();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void destroyVirtualConnection() {
      if (this.virtualConnection != null) {
         IGridNode myNode = this.host.getFrequencyBindingBlockEntity().getMainNode().getNode();
         WirelessLinkOps.destroy(this.virtualConnection, myNode);
         if (myNode != null) {
            MultiblockLinkReadiness.refreshAfterVirtualConnectionRemoved(myNode);
         }

         this.virtualConnection = null;
      }
   }

   private void revalidateConnection() {
      AENetworkBlockEntity be = this.host.getFrequencyBindingBlockEntity();
      if (this.frequencyId > 0 && be.m_58904_() != null && !be.m_58904_().m_5776_()) {
         if (this.virtualConnection != null) {
            IGridNode myNode = be.getMainNode().getNode();
            if (myNode != null) {
               if (!MultiblockLinkReadiness.canKeepVirtualConnection(myNode)) {
                  this.destroyVirtualConnection();
                  this.scheduleRetry();
               } else {
                  boolean connectionAlive = false;
                  IGridNode connectedTarget = null;

                  for (IGridConnection conn : myNode.getConnections()) {
                     if (conn == this.virtualConnection && !conn.isInWorld()) {
                        connectionAlive = true;
                        connectedTarget = conn.getOtherSide(myNode);
                        break;
                     }
                  }

                  if (!connectionAlive) {
                     this.virtualConnection = null;
                  } else {
                     WirelessFrequencyManager manager = WirelessFrequencyManager.get();
                     if (manager != null) {
                        MinecraftServer server = ((ServerLevel)be.m_58904_()).m_7654_();
                        IGridNode currentTarget = manager.resolveNode(this.frequencyId, server);
                        if (currentTarget == null || connectedTarget != currentTarget) {
                           this.destroyVirtualConnection();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isAlreadyInFrequencyGrid(IGridNode targetNode, IGridNode transmitterNode) {
      IGrid targetGrid = GridNodeAccess.getGridIfPresent(targetNode);
      IGrid transmitterGrid = GridNodeAccess.getGridIfPresent(transmitterNode);
      return targetGrid != null && transmitterGrid != null && targetGrid == transmitterGrid;
   }

   private static boolean alreadyHasFrequencyChannel(IGridNode targetNode, IGridNode transmitterNode) {
      return isAlreadyInFrequencyGrid(targetNode, transmitterNode) && targetNode.meetsChannelRequirements();
   }

   private static boolean wouldMergeControllerNetworks(@Nullable IGrid targetGrid, @Nullable IGrid frequencyGrid) {
      return targetGrid != null && targetGrid != frequencyGrid ? !HighCapacityChannelSupport.getAllControllerNodes(targetGrid).isEmpty() : false;
   }
}
