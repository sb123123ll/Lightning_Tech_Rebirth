package com.moakiee.ae2lt.grid.wirelesslink;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.pathing.ChannelMode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.SelectedPart;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.parts.AEBasePart;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.blockentity.OverloadedControllerBlockEntity;
import com.moakiee.ae2lt.blockentity.WirelessOverloadedControllerBlockEntity;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.grid.FrequencyAccessLevel;
import com.moakiee.ae2lt.grid.FrequencyDisplayName;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardData;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.me.GridNodeAccess;
import com.moakiee.thunderbolt.api.channel.ChannelSourceRegistry;
import com.moakiee.thunderbolt.core.channel.HighCapacityChannelSupport;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class WirelessLinkRegistry extends SavedData {
   private static final Logger LOG = LogUtils.getLogger();
   private static final String DATA_NAME = "ae2lt_wireless_links";
   private static final int RESTORE_BATCH_SIZE = 64;
   private static final int RESTORE_INTERVAL_TICKS = 20;
   private static final int TOPOLOGY_RECONCILE_DELAY_TICKS = 2;
   private static final int CHANNEL_EXPANSION_BATCH_SIZE = 16;
   private static final Comparator<WirelessLink> LINK_PREFERENCE = Comparator.comparingLong(WirelessLink::createdTime)
      .thenComparing(link -> link.linkId().toString());
   private final WirelessLinkIndex links = new WirelessLinkIndex();
   private final Map<UUID, WirelessLinkRegistry.RuntimeEntrances> runtimeConnections = new HashMap<>();
   private final Map<IGridNode, LinkedHashSet<UUID>> runtimeLinksByAnchor = new IdentityHashMap<>();
   private final LinkedHashSet<UUID> pendingChannelExpansion = new LinkedHashSet<>();
   private final List<WirelessLinkRegistry.PendingAutoConnect> pendingAutoConnect = new ArrayList<>();
   private final Map<WirelessLinkRegistry.TopologyChangeKey, WirelessLinkRegistry.PendingClusterReconcile> pendingClusterReconciles = new LinkedHashMap<>();
   private int restoreCooldown;
   private long nextCleanupGameTime;
   @Nullable
   private static WirelessLinkRegistry instance;

   public WirelessLinkRegistry() {
   }

   private WirelessLinkRegistry(CompoundTag root) {
      this.read(root);
   }

   public static void onServerStart(MinecraftServer server) {
      instance = (WirelessLinkRegistry)server.m_129783_().m_8895_().m_164861_(WirelessLinkRegistry::new, WirelessLinkRegistry::new, "ae2lt_wireless_links");
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();

      for (WirelessLink link : List.copyOf(instance.links.values())) {
         if (manager != null && manager.isFrequencyValid(link.frequencyId())) {
            instance.registerDevice(link);
         } else {
            instance.removeLink(link);
         }
      }
   }

   public static void onServerStop() {
      if (instance != null) {
         instance.runtimeConnections.clear();
         instance.runtimeLinksByAnchor.clear();
         instance.pendingChannelExpansion.clear();
         instance.pendingAutoConnect.clear();
         instance.pendingClusterReconciles.clear();
      }

      WirelessLinkOps.clearWirelessBridgeTracking();
      instance = null;
   }

   public static WirelessLinkRegistry get(MinecraftServer server) {
      if (instance == null) {
         onServerStart(server);
      }

      return instance;
   }

   @Nullable
   public static WirelessLinkRegistry get() {
      return instance;
   }

   public int removeFrequencyLinks(int frequencyId) {
      if (frequencyId <= 0) {
         return 0;
      } else {
         List<WirelessLink> frequencyLinks = List.copyOf(this.links.findAllForFrequency(frequencyId));
         LinkedHashSet<UUID> removedLinkIds = new LinkedHashSet<>();

         for (WirelessLink link : frequencyLinks) {
            removedLinkIds.add(link.linkId());
         }

         this.discardPendingFrequencyInheritance(frequencyId, removedLinkIds);
         return this.removeLinks(frequencyLinks);
      }
   }

   private void discardPendingFrequencyInheritance(int frequencyId, Set<UUID> removedLinkIds) {
      for (WirelessLinkRegistry.PendingClusterReconcile pending : this.pendingClusterReconciles.values()) {
         LinkedHashSet<UUID> removedInheritedSourceIds = new LinkedHashSet<>();
         pending.inheritedSeeds.removeIf(seed -> {
            WirelessLinkRegistry.LinkInheritance inheritance = seed.inheritance();
            if (inheritance != null && inheritance.frequencyId() == frequencyId) {
               removedInheritedSourceIds.add(inheritance.sourceLinkId());
               return true;
            } else {
               return false;
            }
         });
         pending.sourceLinkIds.removeAll(removedLinkIds);
         pending.sourceLinkIds.removeAll(removedInheritedSourceIds);
      }
   }

   public void queueAutoConnect(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction side, int delayTicks) {
      this.queueAutoConnect(player, dimension, pos, side, "", "", delayTicks);
   }

   public void queuePartAutoConnect(
      ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, Direction side, String expectedPartId, String expectedPartSideName, int delayTicks
   ) {
      this.queueAutoConnect(player, dimension, pos, side, expectedPartId, expectedPartSideName, delayTicks);
   }

   private void queueAutoConnect(
      ServerPlayer player,
      ResourceKey<Level> dimension,
      BlockPos pos,
      @Nullable Direction side,
      String expectedPartId,
      String expectedPartSideName,
      int delayTicks
   ) {
      if (!(player instanceof FakePlayer) && !OverloadedFrequencyCardItem.findAutoConnectCard(player).isEmpty()) {
         this.pendingAutoConnect
            .add(
               new WirelessLinkRegistry.PendingAutoConnect(
                  player.m_20148_(),
                  dimension.m_135782_().toString(),
                  pos.m_121878_(),
                  side == null ? "" : side.m_122433_(),
                  expectedPartId,
                  expectedPartSideName,
                  Math.max(1, delayTicks)
               )
            );
      }
   }

   public void queueClusterTopologyChange(ServerLevel level, BlockPos changedPos) {
      WirelessLinkRegistry.PendingClusterReconcile pending = this.pendingClusterChange(level.m_46472_(), changedPos);
      pending.inspectChangedPosition = true;
      pending.postpone();
   }

   public void prepareClusterTopologyChange(ServerLevel level, BlockPos changedPos) {
      List<WirelessLinkRegistry.LocatedTarget> changedTargets = this.resolveAllTargetsAt(level, changedPos);
      if (!changedTargets.isEmpty()) {
         Set<IGridNode> alreadyHandled = PhysicalGridCluster.newIdentityNodeSet();
         WirelessLinkRegistry.PendingClusterReconcile pending = null;

         for (WirelessLinkRegistry.LocatedTarget changedTarget : changedTargets) {
            if (!alreadyHandled.contains(changedTarget.target().node())) {
               Set<IGridNode> cluster = PhysicalGridCluster.collect(changedTarget.target().node());
               alreadyHandled.addAll(cluster);
               List<WirelessLink> clusterLinks = this.findLinksInCluster(cluster, level.m_7654_());
               List<Integer> frequencies = clusterLinks.stream().map(WirelessLink::frequencyId).distinct().limit(2L).toList();
               WirelessLinkRegistry.LinkInheritance inheritance = frequencies.size() == 1
                  ? clusterLinks.stream().min(LINK_PREFERENCE).map(WirelessLinkRegistry.LinkInheritance::from).orElse(null)
                  : null;
               if (!clusterLinks.isEmpty()) {
                  if (pending == null) {
                     pending = this.pendingClusterChange(level.m_46472_(), changedPos);
                     pending.inspectChangedPosition = true;
                  }

                  for (WirelessLink link : clusterLinks) {
                     pending.sourceLinkIds.add(link.linkId());
                  }

                  List<IGridNode> changedNodes = changedTargets.stream().map(candidate -> candidate.target().node()).filter(cluster::contains).toList();

                  for (IGridNode neighbourNode : PhysicalGridCluster.directNeighbours(changedNodes)) {
                     WirelessLinkRegistry.LocatedTarget neighbour = this.locateNode(neighbourNode);
                     if (neighbour != null) {
                        pending.inheritedSeeds.add(new WirelessLinkRegistry.InheritedClusterSeed(neighbour.locator(), inheritance));
                     }
                  }

                  if (inheritance != null) {
                     WirelessLink source = this.links.get(inheritance.sourceLinkId());
                     if (source != null && source.posLong() != changedPos.m_121878_()) {
                        pending.inheritedSeeds.add(new WirelessLinkRegistry.InheritedClusterSeed(locatorOf(source), inheritance));
                     }
                  }
               }
            }
         }

         if (pending != null) {
            pending.postpone();
         }
      }
   }

   private WirelessLinkRegistry.PendingClusterReconcile pendingClusterChange(ResourceKey<Level> dimension, BlockPos changedPos) {
      WirelessLinkRegistry.TopologyChangeKey key = new WirelessLinkRegistry.TopologyChangeKey(dimension.m_135782_().toString(), changedPos.m_121878_());
      return this.pendingClusterReconciles.computeIfAbsent(key, ignored -> new WirelessLinkRegistry.PendingClusterReconcile(key.dimensionId(), key.posLong()));
   }

   public void tick(MinecraftServer server) {
      this.processPendingClusterReconciles(server);
      this.processPendingAutoConnect(server);
      this.processPendingChannelExpansion(server);
      if (++this.restoreCooldown >= 20) {
         this.restoreCooldown = 0;
         boolean cleanupPass = this.shouldRunCleanup(server);
         if (cleanupPass) {
            this.nextCleanupGameTime = server.m_129783_().m_46467_() + (long)AE2LTCommonConfig.frequencyCardCleanupIntervalSeconds() * 20L;
         }

         this.processLinks(server, cleanupPass);
      }
   }

   public void onBlockChanged(ServerLevel level, BlockPos changedPos) {
      this.prepareClusterTopologyChange(level, changedPos);
      List<WirelessLink> candidates = this.links.findAllInDimension(level.m_46472_().m_135782_().toString());
      if (!candidates.isEmpty()) {
         long changedPosLong = changedPos.m_121878_();
         long now = currentGameTime(level.m_7654_());
         boolean changed = false;

         for (WirelessLink link : candidates) {
            if (this.links.contains(link.linkId())) {
               if (link.posLong() == changedPosLong) {
                  this.removeLink(link);
                  changed = true;
               } else if (this.runtimeConnections.containsKey(link.linkId())) {
                  WirelessLinkRegistry.PersistedTarget target = this.resolvePersistedTarget(link, level.m_7654_());
                  if (target.target() != null) {
                     IGridNode targetNode = target.target().node();
                     if (MultiblockLinkReadiness.isKnownMultiblockAffectedByChange(targetNode, changedPos)) {
                        this.destroyRuntimeConnection(link, targetNode);
                        if (this.links.contains(link.linkId())) {
                           this.links.put(link.withState(WirelessLinkState.TARGET_NOT_READY, now));
                        }

                        changed = true;
                     }
                  }
               }
            }
         }

         if (changed) {
            this.m_77762_();
         }
      }
   }

   public WirelessLinkRegistry.ActionFeedback handleManualUse(
      ServerPlayer player, int frequencyId, ServerLevel level, BlockPos pos, Direction face, Vec3 hitVec
   ) {
      Optional<WirelessLinkRegistry.ActionFeedback> nativeFeedback = this.handleNativeFrequencyHost(player, frequencyId, level, pos);
      if (nativeFeedback.isPresent()) {
         return nativeFeedback.get();
      } else {
         WirelessLinkRegistry.TargetResolution resolution = this.resolveTarget(level, pos, face, hitVec);
         return resolution.failureKey() != null
            ? WirelessLinkRegistry.ActionFeedback.red(resolution.failureKey())
            : this.connectOrDisconnectTarget(player, frequencyId, level, pos, resolution.target(), false);
      }
   }

   public boolean isPotentialLinkTarget(ServerLevel level, BlockPos pos) {
      BlockEntity be = level.m_7702_(pos);
      if (be instanceof OverloadedControllerBlockEntity || be instanceof WirelessOverloadedControllerBlockEntity || be instanceof ControllerBlockEntity) {
         return true;
      } else if (be instanceof FrequencyBindingHost) {
         return true;
      } else {
         return be instanceof IPartHost ? true : GridHelper.getNodeHost(level, pos) != null;
      }
   }

   public WirelessLinkRegistry.TargetLinkInspection inspectTarget(ServerLevel level, BlockPos pos, @Nullable Direction face, @Nullable Vec3 hitVec) {
      WirelessLinkRegistry.TargetResolution resolution = this.resolveTarget(level, pos, face, hitVec);
      if (resolution.target() == null) {
         return new WirelessLinkRegistry.TargetLinkInspection(false, List.of());
      } else {
         WirelessLinkRegistry.LinkTarget target = resolution.target();
         ArrayList<WirelessLinkRegistry.InspectedTargetLink> inspectedLinks = new ArrayList<>();
         LinkedHashSet<UUID> runtimeIds = this.runtimeLinksByAnchor.get(target.node());
         if (runtimeIds != null) {
            for (UUID linkId : runtimeIds) {
               WirelessLink link = this.links.get(linkId);
               if (link != null) {
                  inspectedLinks.add(new WirelessLinkRegistry.InspectedTargetLink(link.frequencyId(), link.state()));
               }
            }
         }

         String dimensionId = level.m_46472_().m_135782_().toString();
         long posLong = pos.m_121878_();

         for (WirelessLink link : this.links.findAllAt(dimensionId, posLong)) {
            if ((runtimeIds == null || !runtimeIds.contains(link.linkId())) && matchesPersistedTarget(link, target)) {
               inspectedLinks.add(new WirelessLinkRegistry.InspectedTargetLink(link.frequencyId(), link.state()));
            }
         }

         return new WirelessLinkRegistry.TargetLinkInspection(runtimeIds != null && !runtimeIds.isEmpty(), inspectedLinks);
      }
   }

   private static boolean matchesPersistedTarget(WirelessLink link, WirelessLinkRegistry.LinkTarget target) {
      if (link.mode() != target.mode() || !link.blockId().equals(target.blockId()) || !link.blockEntityTypeId().equals(target.blockEntityTypeId())) {
         return false;
      } else {
         return target.mode() == WirelessLinkMode.DEVICE
            ? true
            : link.sideName().equals(target.sideName()) && link.partId().equals(target.partId()) && link.partClassName().equals(target.partClassName());
      }
   }

   private Optional<WirelessLinkRegistry.ActionFeedback> handleNativeFrequencyHost(ServerPlayer player, int frequencyId, ServerLevel level, BlockPos pos) {
      BlockEntity be = level.m_7702_(pos);
      if (be instanceof FrequencyBindingHost host && !(be instanceof WirelessOverloadedControllerBlockEntity)) {
         int currentFrequency = host.getFrequencyId();
         if (currentFrequency == frequencyId) {
            host.clearFrequency();
            return Optional.of(WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.disconnected"));
         }

         if (currentFrequency > 0) {
            return Optional.of(WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.other_frequency"));
         }

         NativeHostSafety safety = this.evaluateNativeHostSafety(host, frequencyId, level.m_7654_());
         if (safety == NativeHostSafety.PENDING) {
            host.setFrequency(frequencyId);
            return Optional.of(this.feedbackForNativeHostSafety(safety, false, frequencyId));
         }

         if (safety != NativeHostSafety.READY) {
            return Optional.of(this.feedbackForNativeHostSafety(safety, false, frequencyId));
         }

         host.setFrequency(frequencyId);
         return Optional.of(WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.connected", frequencyDisplayName(frequencyId)));
      }

      return Optional.empty();
   }

   private WirelessLinkRegistry.ActionFeedback connectOrDisconnectTarget(
      @Nullable ServerPlayer player, int frequencyId, ServerLevel level, BlockPos pos, WirelessLinkRegistry.LinkTarget target, boolean automatic
   ) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      WirelessFrequency frequency = manager == null ? null : manager.getFrequency(frequencyId);
      if (frequency == null) {
         return WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.frequency_unavailable");
      } else {
         FrequencyAccessLevel actorAccess = player == null ? FrequencyAccessLevel.BLOCKED : frequency.getPlayerAccess(player);
         Set<IGridNode> physicalCluster = PhysicalGridCluster.collect(target.node());
         List<WirelessLink> clusterLinks = this.findLinksInCluster(physicalCluster, level.m_7654_());
         List<WirelessLink> sameFrequencyLinks = clusterLinks.stream().filter(link -> link.frequencyId() == frequencyId).toList();
         boolean hasOtherFrequency = clusterLinks.stream().anyMatch(link -> link.frequencyId() != frequencyId);
         if (hasOtherFrequency) {
            if (!automatic && !sameFrequencyLinks.isEmpty()) {
               if (player != null && !sameFrequencyLinks.stream().anyMatch(link -> !link.canBeRemovedBy(player.m_20148_(), actorAccess.isManager()))) {
                  for (WirelessLink link : sameFrequencyLinks) {
                     if (this.links.contains(link.linkId())) {
                        this.removeLink(link);
                     }
                  }

                  WirelessLinkRegistry.ClusterComponent component = new WirelessLinkRegistry.ClusterComponent(physicalCluster);
                  component.anchorCandidates.add(new WirelessLinkRegistry.LocatedTarget(level.m_46472_().m_135782_().toString(), pos.m_121878_(), target));
                  this.reconcileClusterComponent(component, level.m_7654_());
                  return WirelessLinkRegistry.ActionFeedback.green(
                     "ae2lt.frequency_card.disconnected_conflicting_frequency", FrequencyDisplayName.of(frequencyId, frequency.getName())
                  );
               } else {
                  return WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.no_frequency_permission");
               }
            } else {
               return WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.cluster_frequency_conflict");
            }
         } else if (!sameFrequencyLinks.isEmpty()) {
            if (automatic) {
               return WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.auto_silent_skip");
            } else if (player != null && !sameFrequencyLinks.stream().anyMatch(linkx -> !linkx.canBeRemovedBy(player.m_20148_(), actorAccess.isManager()))) {
               for (WirelessLink linkx : sameFrequencyLinks) {
                  if (this.links.contains(linkx.linkId())) {
                     this.removeLink(linkx);
                  }
               }

               return WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.disconnected");
            } else {
               return WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.no_frequency_permission");
            }
         } else if (!manager.isAdvancedTransmitter(frequencyId)) {
            return WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.requires_advanced_transmitter");
         } else {
            IGridNode transmitterNode = manager.resolveNode(frequencyId, level.m_7654_());
            if (transmitterNode != null && alreadyHasFrequencyChannel(target.node(), transmitterNode)) {
               return automatic
                  ? WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.auto_silent_skip")
                  : WirelessLinkRegistry.ActionFeedback.yellow("ae2lt.frequency_card.already_in_frequency");
            } else if (transmitterNode != null
               && wouldMergeControllerNetworks(GridNodeAccess.getGridIfPresent(target.node()), GridNodeAccess.getGridIfPresent(transmitterNode))) {
               return automatic
                  ? WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.auto_silent_skip")
                  : WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.controller_conflict");
            } else {
               UUID owner = player == null ? new UUID(0L, 0L) : player.m_20148_();
               WirelessLink updated = this.createAndEstablishLink(
                  frequencyId, owner, new WirelessLinkRegistry.LocatedTarget(level.m_46472_().m_135782_().toString(), pos.m_121878_(), target), level.m_7654_()
               );
               return updated.state() == WirelessLinkState.CONNECTED
                  ? WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.connected", FrequencyDisplayName.of(frequencyId, frequency.getName()))
                  : WirelessLinkRegistry.ActionFeedback.yellow("ae2lt.frequency_card.pending");
            }
         }
      }
   }

   private void processPendingClusterReconciles(MinecraftServer server) {
      if (!this.pendingClusterReconciles.isEmpty()) {
         ArrayList<WirelessLinkRegistry.PendingClusterReconcile> ready = new ArrayList<>();
         Iterator<Entry<WirelessLinkRegistry.TopologyChangeKey, WirelessLinkRegistry.PendingClusterReconcile>> iterator = this.pendingClusterReconciles
            .entrySet()
            .iterator();

         while (iterator.hasNext()) {
            WirelessLinkRegistry.PendingClusterReconcile pending = iterator.next().getValue();
            if (--pending.delayTicks <= 0) {
               ready.add(pending);
               iterator.remove();
            }
         }

         if (!ready.isEmpty()) {
            this.reconcileClusterTopologies(server, ready);
         }
      }
   }

   private void reconcileClusterTopologies(MinecraftServer server, List<WirelessLinkRegistry.PendingClusterReconcile> pendingChanges) {
      ArrayList<WirelessLinkRegistry.ClusterComponent> components = new ArrayList<>();
      IdentityHashMap<IGridNode, WirelessLinkRegistry.ClusterComponent> componentByNode = new IdentityHashMap<>();
      LinkedHashSet<UUID> sourceIds = new LinkedHashSet<>();

      for (WirelessLinkRegistry.PendingClusterReconcile pending : pendingChanges) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(pending.dimensionId));
         ServerLevel level = server.m_129880_(dim);
         if (level != null) {
            if (pending.inspectChangedPosition) {
               for (WirelessLinkRegistry.LocatedTarget target : this.resolveAllTargetsAt(level, BlockPos.m_122022_(pending.changedPosLong))) {
                  this.addTargetToComponent(target, components, componentByNode);
               }
            }

            sourceIds.addAll(pending.sourceLinkIds);

            for (WirelessLinkRegistry.InheritedClusterSeed seed : pending.inheritedSeeds) {
               WirelessLinkRegistry.LocatedTarget target = this.resolveLocator(seed.locator(), server);
               if (target != null) {
                  WirelessLinkRegistry.ClusterComponent component = this.addTargetToComponent(target, components, componentByNode);
                  if (seed.inheritance() != null) {
                     component.inheritedLinks.add(seed.inheritance());
                  }
               }
            }
         }
      }

      for (UUID sourceId : sourceIds) {
         WirelessLink source = this.links.get(sourceId);
         if (source != null
            && this.runtimeConnections.containsKey(sourceId)
            && this.runtimeEntrancesSpanChangedComponents(source, server, components, componentByNode)) {
            this.destroyRuntimeConnection(source, this.resolveRuntimeTargetNode(source));
         }
      }

      for (WirelessLinkRegistry.ClusterComponent component : components) {
         this.reconcileClusterComponent(component, server);
      }

      for (UUID sourceIdx : sourceIds) {
         WirelessLink source = this.links.get(sourceIdx);
         if (source != null) {
            WirelessLink updated = this.establishOrUpdate(source, server, false);
            if (this.links.contains(updated.linkId())) {
               this.links.put(updated);
               this.m_77762_();
            }
         }
      }
   }

   private boolean runtimeEntrancesSpanChangedComponents(
      WirelessLink source,
      MinecraftServer server,
      List<WirelessLinkRegistry.ClusterComponent> components,
      IdentityHashMap<IGridNode, WirelessLinkRegistry.ClusterComponent> componentByNode
   ) {
      WirelessLinkRegistry.PersistedTarget persisted = this.resolvePersistedTarget(source, server);
      if (persisted.target() == null) {
         return true;
      } else {
         WirelessLinkRegistry.ClusterComponent sourceComponent = componentByNode.get(persisted.target().node());
         if (sourceComponent == null) {
            return true;
         } else {
            for (WirelessLinkRegistry.ClusterComponent component : components) {
               if (component != sourceComponent
                  && component.inheritedLinks.stream().anyMatch(inheritance -> inheritance.sourceLinkId().equals(source.linkId()))) {
                  return true;
               }
            }

            WirelessLinkRegistry.RuntimeEntrances runtime = this.runtimeConnections.get(source.linkId());
            if (runtime != null) {
               for (IGridNode anchor : runtime.anchors()) {
                  WirelessLinkRegistry.ClusterComponent componentx = componentByNode.get(anchor);
                  if (componentx != null && componentx != sourceComponent) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private void processPendingChannelExpansion(MinecraftServer server) {
      if (!this.pendingChannelExpansion.isEmpty()) {
         ArrayList<UUID> ready = new ArrayList<>(Math.min(16, this.pendingChannelExpansion.size()));
         Iterator<UUID> iterator = this.pendingChannelExpansion.iterator();

         while (iterator.hasNext() && ready.size() < 16) {
            ready.add(iterator.next());
            iterator.remove();
         }

         for (UUID linkId : ready) {
            WirelessLink link = this.links.get(linkId);
            if (link != null && this.expandRuntimeEntrances(link, server)) {
               this.pendingChannelExpansion.add(linkId);
            }
         }
      }
   }

   private boolean expandRuntimeEntrances(WirelessLink link, MinecraftServer server) {
      WirelessLinkRegistry.PersistedTarget target = this.resolvePersistedTarget(link, server);
      if (target.target() == null) {
         return false;
      } else {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         IGridNode transmitterNode = manager == null ? null : manager.resolveNode(link.frequencyId(), server);
         if (transmitterNode == null) {
            return false;
         } else {
            IGridNode primaryAnchor = target.target().node();
            WirelessLinkRegistry.RuntimeEntrances runtime = this.runtimeConnections.get(link.linkId());
            if (runtime != null && WirelessLinkOps.isConnectedTo(runtime.get(primaryAnchor), primaryAnchor, transmitterNode)) {
               long now = currentGameTime(server);
               if (!runtime.canCheckChannels(now)) {
                  return true;
               } else {
                  Set<IGridNode> cluster = PhysicalGridCluster.collect(primaryAnchor);
                  boolean pruned = this.pruneRuntimeEntrances(link.linkId(), runtime, cluster, transmitterNode);
                  if (!this.runtimeConnections.containsKey(link.linkId())) {
                     return false;
                  } else if (pruned) {
                     runtime.deferChannelCheck(now + 1L);
                     return true;
                  } else {
                     IGrid grid = GridNodeAccess.getGridIfPresent(transmitterNode);
                     if (grid == null || grid.getPathingService().isNetworkBooting()) {
                        return true;
                     } else if (!this.hasAvailableChannelSupply(grid)) {
                        return false;
                     } else {
                        Set<IGridNode> excluded = PhysicalGridCluster.newIdentityNodeSet();
                        excluded.addAll(runtime.anchors());

                        IGridNode candidate;
                        while (
                           (candidate = WirelessClusterEntrancePlanner.findSupplementalEntrance(cluster, excluded)) != null
                              && !MultiblockLinkReadiness.canKeepVirtualConnection(candidate)
                        ) {
                           excluded.add(candidate);
                        }

                        if (candidate == null) {
                           return false;
                        } else {
                           try {
                              IGridConnection connection = WirelessLinkOps.createVirtualConnection(candidate, transmitterNode);
                              runtime.put(candidate, connection);
                              runtime.deferChannelCheck(now + 1L);
                              this.registerRuntimeAnchor(link.linkId(), candidate);
                              LOG.debug(
                                 "Added supplemental overloaded-frequency entrance for cluster link {} (entrances={})", link.linkId(), runtime.anchors().size()
                              );
                              return true;
                           } catch (IllegalStateException var16) {
                              return false;
                           }
                        }
                     }
                  }
               }
            } else {
               return false;
            }
         }
      }
   }

   private boolean pruneRuntimeEntrances(UUID linkId, WirelessLinkRegistry.RuntimeEntrances runtime, Set<IGridNode> physicalCluster, IGridNode transmitterNode) {
      boolean pruned = false;

      for (Entry<IGridNode, IGridConnection> entry : new ArrayList<>(runtime.entries())) {
         IGridNode anchor = entry.getKey();
         IGridConnection connection = entry.getValue();
         if (!physicalCluster.contains(anchor) || !WirelessLinkOps.isConnectedTo(connection, anchor, transmitterNode)) {
            runtime.remove(anchor);
            this.unregisterRuntimeAnchor(linkId, anchor);
            WirelessLinkOps.destroy(connection, anchor);
            MultiblockLinkReadiness.refreshAfterVirtualConnectionRemoved(anchor);
            pruned = true;
         }
      }

      if (runtime.isEmpty()) {
         this.runtimeConnections.remove(linkId);
      }

      return pruned;
   }

   private boolean hasAvailableChannelSupply(IGrid grid) {
      ChannelMode mode = grid.getPathingService().getChannelMode();
      if (mode == ChannelMode.INFINITE) {
         return false;
      } else {
         long capacity = 0L;
         int factor = Math.max(1, mode.getCableCapacityFactor());

         for (IGridNode node : HighCapacityChannelSupport.getAllControllerNodes(grid)) {
            if (ChannelSourceRegistry.isChannelSource(node.getOwner())) {
               capacity += (long)HighCapacityChannelSupport.channelsPerController() * (long)factor;
            } else {
               for (IGridConnection connection : node.getConnections()) {
                  IGridNode other = connection.getOtherSide(node);
                  if (!(other.getOwner() instanceof ControllerBlockEntity)) {
                     capacity += 32L * (long)factor;
                  }
               }
            }

            if (capacity >= 2147483647L) {
               capacity = 2147483647L;
               break;
            }
         }

         return (long)HighCapacityChannelSupport.countUsedChannels(grid) < capacity;
      }
   }

   private WirelessLinkRegistry.ClusterComponent addTargetToComponent(
      WirelessLinkRegistry.LocatedTarget target,
      List<WirelessLinkRegistry.ClusterComponent> components,
      IdentityHashMap<IGridNode, WirelessLinkRegistry.ClusterComponent> componentByNode
   ) {
      IGridNode node = target.target().node();
      WirelessLinkRegistry.ClusterComponent component = componentByNode.get(node);
      if (component == null) {
         Set<IGridNode> nodes = PhysicalGridCluster.collect(node);
         component = new WirelessLinkRegistry.ClusterComponent(nodes);
         components.add(component);

         for (IGridNode member : nodes) {
            componentByNode.put(member, component);
         }
      }

      boolean alreadyCandidate = component.anchorCandidates.stream().anyMatch(candidate -> candidate.target().node() == node);
      if (!alreadyCandidate) {
         component.anchorCandidates.add(target);
      }

      return component;
   }

   private void reconcileClusterComponent(WirelessLinkRegistry.ClusterComponent component, MinecraftServer server) {
      List<WirelessLink> existing = this.findLinksInCluster(component.nodes, server);
      if (!existing.isEmpty() || !component.inheritedLinks.isEmpty()) {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         ArrayList<WirelessLink> validExisting = new ArrayList<>(existing.size());

         for (WirelessLink link : existing) {
            if (manager != null && manager.isFrequencyValid(link.frequencyId())) {
               validExisting.add(link);
            } else if (this.links.contains(link.linkId())) {
               this.removeLink(link);
            }
         }

         ArrayList<WirelessLinkRegistry.LinkInheritance> allInheritance = new ArrayList<>();

         for (WirelessLinkRegistry.LinkInheritance inheritance : component.inheritedLinks) {
            if (manager != null && manager.isFrequencyValid(inheritance.frequencyId())) {
               allInheritance.add(inheritance);
            }
         }

         for (WirelessLink linkx : validExisting) {
            allInheritance.add(WirelessLinkRegistry.LinkInheritance.from(linkx));
         }

         long distinctFrequencies = allInheritance.stream().map(WirelessLinkRegistry.LinkInheritance::frequencyId).distinct().count();
         if (distinctFrequencies > 1L) {
            LOG.warn(
               "Physical ME clusters carrying different overloaded frequencies were merged; suspending all frequency entrances until the physical cluster is split or one frequency is explicitly disconnected"
            );
            long now = currentGameTime(server);

            for (WirelessLink linkx : validExisting) {
               this.destroyRuntimeConnection(linkx, this.resolveRuntimeTargetNode(linkx));
               if (this.links.contains(linkx.linkId())) {
                  this.links.put(linkx.withState(WirelessLinkState.CLUSTER_FREQUENCY_CONFLICT, now));
               }
            }

            if (!validExisting.isEmpty()) {
               this.m_77762_();
            }
         } else {
            WirelessLinkRegistry.LinkInheritance winner = allInheritance.stream()
               .min(
                  Comparator.comparingLong(WirelessLinkRegistry.LinkInheritance::createdTime)
                     .thenComparing(inheritancex -> inheritancex.sourceLinkId().toString())
               )
               .orElse(null);
            if (winner != null) {
               WirelessLink keep = validExisting.stream().filter(linkxx -> linkxx.frequencyId() == winner.frequencyId()).min(LINK_PREFERENCE).orElse(null);

               for (WirelessLink linkxx : validExisting) {
                  if ((keep == null || !linkxx.linkId().equals(keep.linkId())) && this.links.contains(linkxx.linkId())) {
                     this.removeLink(linkxx);
                  }
               }

               if (keep == null) {
                  WirelessLinkRegistry.LocatedTarget anchor = component.anchorCandidates
                     .stream()
                     .filter(candidate -> component.nodes.contains(candidate.target().node()))
                     .findFirst()
                     .orElse(null);
                  if (anchor != null) {
                     this.createAndEstablishLink(winner.frequencyId(), winner.ownerUuid(), anchor, server);
                  }
               } else {
                  WirelessLink current = this.links.get(keep.linkId());
                  if (current != null) {
                     this.registerDevice(current);
                     if (current.state() == WirelessLinkState.CLUSTER_FREQUENCY_CONFLICT) {
                        current = current.withState(WirelessLinkState.DISCONNECTED, currentGameTime(server));
                     }

                     WirelessLink updated = this.establishOrUpdate(current, server, false);
                     if (this.links.contains(updated.linkId())) {
                        this.links.put(updated);
                        this.m_77762_();
                     }
                  }
               }
            }
         }
      }
   }

   private void processPendingAutoConnect(MinecraftServer server) {
      if (!this.pendingAutoConnect.isEmpty()) {
         ArrayList<WirelessLinkRegistry.PendingAutoConnect> ready = new ArrayList<>();

         for (int i = this.pendingAutoConnect.size() - 1; i >= 0; i--) {
            WirelessLinkRegistry.PendingAutoConnect pending = this.pendingAutoConnect.get(i).tickDown();
            if (pending.delayTicks() <= 0) {
               ready.add(pending);
               this.pendingAutoConnect.remove(i);
            } else {
               this.pendingAutoConnect.set(i, pending);
            }
         }

         for (WirelessLinkRegistry.PendingAutoConnect pending : ready) {
            this.processOnePendingAutoConnect(server, pending);
         }
      }
   }

   private void processOnePendingAutoConnect(MinecraftServer server, WirelessLinkRegistry.PendingAutoConnect pending) {
      ServerPlayer player = server.m_6846_().m_11259_(pending.playerId());
      if (player != null) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(pending.dimensionId()));
         ServerLevel level = server.m_129880_(dim);
         if (level != null) {
            if (matchesExpectedPlacedPart(level, BlockPos.m_122022_(pending.posLong()), pending)) {
               ItemStack stack = OverloadedFrequencyCardItem.findAutoConnectCard(player).orElse(ItemStack.f_41583_);
               if (stack.m_41619_()) {
                  if (OverloadedFrequencyCardItem.hasMultipleAutoConnectCandidates(player)) {
                     player.m_5661_(Component.m_237115_("ae2lt.frequency_card.auto_ambiguous").m_130940_(ChatFormatting.RED), true);
                  }
               } else {
                  OverloadedFrequencyCardData data = OverloadedFrequencyCardItem.getData(stack);
                  if (data.isBound()) {
                     WirelessFrequencyManager manager = WirelessFrequencyManager.get();
                     WirelessFrequency frequency = manager == null ? null : manager.getFrequency(data.frequencyId());
                     if (frequency != null && frequency.canPlayerAccess(player, "")) {
                        Optional<WirelessLinkRegistry.ActionFeedback> nativeFeedback = this.autoConnectNativeFrequencyHost(
                           level, BlockPos.m_122022_(pending.posLong()), data.frequencyId()
                        );
                        if (nativeFeedback.isPresent()) {
                           WirelessLinkRegistry.ActionFeedback feedback = nativeFeedback.get();
                           if (!"ae2lt.frequency_card.auto_silent_skip".equals(feedback.translationKey()) && feedback.style() != ChatFormatting.GREEN) {
                              player.m_5661_(Component.m_237110_(feedback.translationKey(), feedback.args()).m_130940_(feedback.style()), true);
                           }
                        } else {
                           Direction side = parseDirection(pending.sideName());
                           WirelessLinkRegistry.TargetResolution resolution = this.resolveTarget(level, BlockPos.m_122022_(pending.posLong()), side, null);
                           if (resolution.target() != null) {
                              WirelessLinkRegistry.ActionFeedback feedback = this.connectOrDisconnectTarget(
                                 player, data.frequencyId(), level, BlockPos.m_122022_(pending.posLong()), resolution.target(), true
                              );
                              if (!"ae2lt.frequency_card.auto_silent_skip".equals(feedback.translationKey()) && feedback.style() != ChatFormatting.GREEN) {
                                 player.m_5661_(Component.m_237110_(feedback.translationKey(), feedback.args()).m_130940_(feedback.style()), true);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Optional<WirelessLinkRegistry.ActionFeedback> autoConnectNativeFrequencyHost(ServerLevel level, BlockPos pos, int frequencyId) {
      BlockEntity be = level.m_7702_(pos);
      if (be instanceof FrequencyBindingHost host && !(be instanceof WirelessOverloadedControllerBlockEntity)) {
         int currentFrequency = host.getFrequencyId();
         if (currentFrequency <= 0) {
            NativeHostSafety safety = this.evaluateNativeHostSafety(host, frequencyId, level.m_7654_());
            if (safety == NativeHostSafety.PENDING) {
               host.setFrequency(frequencyId);
               return Optional.of(this.feedbackForNativeHostSafety(safety, true, frequencyId));
            }

            if (safety != NativeHostSafety.READY) {
               return Optional.of(this.feedbackForNativeHostSafety(safety, true, frequencyId));
            }

            host.setFrequency(frequencyId);
            return Optional.of(WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.connected", frequencyDisplayName(frequencyId)));
         }

         return Optional.of(
            currentFrequency == frequencyId
               ? WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.auto_silent_skip")
               : WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.other_frequency")
         );
      }

      return Optional.empty();
   }

   private WirelessLinkRegistry.ActionFeedback feedbackForNativeHostSafety(NativeHostSafety safety, boolean automatic, int frequencyId) {
      return switch (safety) {
         case READY -> WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.connected", frequencyDisplayName(frequencyId));
         case PENDING -> WirelessLinkRegistry.ActionFeedback.yellow("ae2lt.frequency_card.pending");
         case ALREADY_IN_FREQUENCY -> automatic
         ? WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.auto_silent_skip")
         : WirelessLinkRegistry.ActionFeedback.yellow("ae2lt.frequency_card.already_in_frequency");
         case CONTROLLER_CONFLICT -> automatic
         ? WirelessLinkRegistry.ActionFeedback.green("ae2lt.frequency_card.auto_silent_skip")
         : WirelessLinkRegistry.ActionFeedback.red("ae2lt.frequency_card.controller_conflict");
      };
   }

   private static String frequencyDisplayName(int frequencyId) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      WirelessFrequency frequency = manager == null ? null : manager.getFrequency(frequencyId);
      return FrequencyDisplayName.of(frequencyId, frequency == null ? null : frequency.getName());
   }

   private static boolean matchesExpectedPlacedPart(ServerLevel level, BlockPos pos, WirelessLinkRegistry.PendingAutoConnect pending) {
      if (!pending.expectsPlacedPart()) {
         return true;
      } else if (!(level.m_7702_(pos) instanceof IPartHost host)) {
         return false;
      } else {
         IPart part = host.getPart(parseDirection(pending.expectedPartSideName()));
         return part != null && pending.expectedPartId().equals(partId(part));
      }
   }

   private NativeHostSafety evaluateNativeHostSafety(FrequencyBindingHost host, int frequencyId, MinecraftServer server) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      IGridNode targetNode = host.getFrequencyBindingBlockEntity().getMainNode().getNode();
      IGridNode transmitterNode = manager == null ? null : manager.resolveNode(frequencyId, server);
      boolean nodesReady = targetNode != null && transmitterNode != null;
      return NativeHostSafety.classify(
         targetNode != null,
         transmitterNode != null,
         nodesReady && alreadyHasFrequencyChannel(targetNode, transmitterNode),
         nodesReady && wouldMergeControllerNetworks(GridNodeAccess.getGridIfPresent(targetNode), GridNodeAccess.getGridIfPresent(transmitterNode))
      );
   }

   private void processLinks(MinecraftServer server, boolean cleanupPass) {
      if (!this.links.isEmpty()) {
         int batch = cleanupPass ? Math.max(1, AE2LTCommonConfig.frequencyCardCleanupBatchSize()) : 64;

         for (WirelessLink link : this.links.nextBatch(batch)) {
            if (this.links.contains(link.linkId())) {
               WirelessLink updated = this.establishOrUpdate(link, server, cleanupPass);
               if (this.links.contains(updated.linkId())) {
                  this.links.put(updated);
               }
            }
         }
      }
   }

   private WirelessLink createAndEstablishLink(int frequencyId, UUID ownerUuid, WirelessLinkRegistry.LocatedTarget anchor, MinecraftServer server) {
      long now = currentGameTime(server);
      WirelessLinkRegistry.LinkTarget target = anchor.target();
      WirelessLink link = target.mode() == WirelessLinkMode.PART
         ? WirelessLink.createPart(
            UUID.randomUUID(),
            frequencyId,
            anchor.dimensionId(),
            anchor.posLong(),
            target.sideName(),
            target.blockId(),
            target.blockEntityTypeId(),
            target.partId(),
            target.partClassName(),
            ownerUuid,
            now
         )
         : WirelessLink.createDevice(
            UUID.randomUUID(), frequencyId, anchor.dimensionId(), anchor.posLong(), target.blockId(), target.blockEntityTypeId(), ownerUuid, now
         );
      this.links.put(link);
      this.registerDevice(link);
      this.m_77762_();
      WirelessLink updated = this.establishOrUpdate(link, server, false);
      if (this.links.contains(updated.linkId())) {
         this.links.put(updated);
         this.m_77762_();
      }

      return updated;
   }

   private WirelessLink establishOrUpdate(WirelessLink link, MinecraftServer server, boolean cleanupPass) {
      WirelessLinkRegistry.PersistedTarget target = this.resolvePersistedTarget(link, server);
      if (target.state() != null) {
         return this.markState(link, target.state(), server, cleanupPass);
      } else if (link.state() == WirelessLinkState.CLUSTER_FREQUENCY_CONFLICT) {
         return link;
      } else {
         WirelessFrequencyManager manager = WirelessFrequencyManager.get();
         WirelessFrequency frequency = manager == null ? null : manager.getFrequency(link.frequencyId());
         if (frequency == null) {
            return this.markState(link, WirelessLinkState.FREQUENCY_INVALID, server, cleanupPass);
         } else if (!manager.isAdvancedTransmitter(link.frequencyId())) {
            if (this.runtimeConnections.containsKey(link.linkId())) {
               this.destroyRuntimeConnection(link, target.target().node());
            }

            return this.markState(link, WirelessLinkState.PENDING_TRANSMITTER, server, cleanupPass);
         } else if (!link.ownerCanUseFrequency(frequency.getPlayerAccess(link.ownerUuid()).canUse())) {
            this.destroyRuntimeConnection(link, target.target().node());
            return this.markState(link, WirelessLinkState.PERMISSION_DENIED, server, cleanupPass);
         } else {
            IGridNode transmitterNode = manager.resolveNode(link.frequencyId(), server);
            if (transmitterNode == null) {
               return this.markState(link, WirelessLinkState.PENDING_TRANSMITTER, server, cleanupPass);
            } else {
               IGridNode targetNode = target.target().node();
               WirelessLinkRegistry.RuntimeEntrances runtime = this.runtimeConnections.get(link.linkId());
               if (runtime != null && WirelessLinkOps.isConnectedTo(runtime.get(targetNode), targetNode, transmitterNode)) {
                  this.registerRuntimeAnchor(link.linkId(), targetNode);
                  if (!MultiblockLinkReadiness.canKeepVirtualConnection(targetNode)) {
                     this.destroyRuntimeConnection(link, targetNode);
                     return this.markState(link, WirelessLinkState.TARGET_NOT_READY, server, cleanupPass);
                  } else {
                     this.pendingChannelExpansion.add(link.linkId());
                     return link.withState(WirelessLinkState.CONNECTED, currentGameTime(server)).clearInvalidTracking(currentGameTime(server));
                  }
               } else {
                  if (runtime != null) {
                     this.destroyRuntimeConnection(link, targetNode);
                  } else {
                     this.unregisterRuntimeAnchor(link.linkId(), targetNode);
                  }

                  if (!MultiblockLinkReadiness.canKeepVirtualConnection(targetNode)) {
                     this.destroyRuntimeConnection(link, targetNode);
                     return this.markState(link, WirelessLinkState.TARGET_NOT_READY, server, cleanupPass);
                  } else if (alreadyHasFrequencyChannel(targetNode, transmitterNode)) {
                     return this.markState(link, WirelessLinkState.REDUNDANT_LINK, server, cleanupPass);
                  } else if (wouldMergeControllerNetworks(GridNodeAccess.getGridIfPresent(targetNode), GridNodeAccess.getGridIfPresent(transmitterNode))) {
                     return this.markState(link, WirelessLinkState.DISCONNECTED, server, cleanupPass);
                  } else {
                     try {
                        IGridConnection connection = WirelessLinkOps.createVirtualConnection(targetNode, transmitterNode);
                        WirelessLinkRegistry.RuntimeEntrances entrances = new WirelessLinkRegistry.RuntimeEntrances();
                        entrances.put(targetNode, connection);
                        entrances.deferChannelCheck(currentGameTime(server) + 1L);
                        this.runtimeConnections.put(link.linkId(), entrances);
                        this.registerRuntimeAnchor(link.linkId(), targetNode);
                        this.pendingChannelExpansion.add(link.linkId());
                        return link.withState(WirelessLinkState.CONNECTED, currentGameTime(server)).clearInvalidTracking(currentGameTime(server));
                     } catch (IllegalStateException var12) {
                        return this.markState(link, WirelessLinkState.PENDING_TRANSMITTER, server, cleanupPass);
                     }
                  }
               }
            }
         }
      }
   }

   private WirelessLinkRegistry.PersistedTarget resolvePersistedTarget(WirelessLink link, MinecraftServer server) {
      ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(link.dimensionId()));
      ServerLevel level = server.m_129880_(dim);
      if (level == null) {
         return WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.PENDING_TARGET_CHUNK);
      } else {
         BlockPos pos = BlockPos.m_122022_(link.posLong());
         if (level.m_7726_().m_7131_(pos.m_123341_() >> 4, pos.m_123343_() >> 4) == null) {
            return WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.PENDING_TARGET_CHUNK);
         } else {
            BlockEntity be = level.m_7702_(pos);
            if (be == null) {
               return WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.TARGET_MISSING);
            } else {
               String currentBlockId = BuiltInRegistries.f_256975_.m_7981_(level.m_8055_(pos).m_60734_()).toString();
               String currentBeType = BuiltInRegistries.f_257049_.m_7981_(be.m_58903_()).toString();
               if (currentBlockId.equals(link.blockId()) && currentBeType.equals(link.blockEntityTypeId())) {
                  if (link.mode() != WirelessLinkMode.PART) {
                     WirelessLinkRegistry.TargetResolution resolution = this.resolveDeviceTarget(level, pos, null);
                     return resolution.target() == null
                        ? WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.TARGET_NOT_NETWORK_DEVICE)
                        : WirelessLinkRegistry.PersistedTarget.target(resolution.target());
                  } else if (be instanceof IPartHost host) {
                     Direction side = parseDirection(link.sideName());
                     IPart part = host.getPart(side);
                     if (part == null) {
                        return WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.PART_MISSING);
                     } else {
                        String partId = partId(part);
                        if (partId.equals(link.partId()) && part.getClass().getName().equals(link.partClassName())) {
                           IGridNode node = part.getGridNode();
                           return node == null
                              ? WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.PART_NOT_NETWORK_DEVICE)
                              : WirelessLinkRegistry.PersistedTarget.target(
                                 new WirelessLinkRegistry.LinkTarget(
                                    WirelessLinkMode.PART, node, link.sideName(), currentBlockId, currentBeType, partId, part.getClass().getName()
                                 )
                              );
                        } else {
                           return WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.PART_TYPE_CHANGED);
                        }
                     }
                  } else {
                     return WirelessLinkRegistry.PersistedTarget.state(WirelessLinkState.PART_MISSING);
                  }
               } else {
                  return WirelessLinkRegistry.PersistedTarget.state(
                     link.mode() == WirelessLinkMode.PART ? WirelessLinkState.PART_TYPE_CHANGED : WirelessLinkState.TARGET_TYPE_CHANGED
                  );
               }
            }
         }
      }
   }

   private WirelessLink markState(WirelessLink link, WirelessLinkState state, MinecraftServer server, boolean cleanupPass) {
      long now = currentGameTime(server);
      if (!state.isCleanupCandidate()) {
         return link.withState(state, now).clearInvalidTracking(now);
      } else if (state.isDeterministicFailure()) {
         WirelessLink updated = link.withState(state, now);
         this.removeLink(updated);
         return updated;
      } else {
         long firstInvalid = link.firstInvalidTime() <= 0L ? now : link.firstInvalidTime();
         int checks = link.invalidCheckCount() + (cleanupPass ? 1 : 0);
         WirelessLink updated = link.withState(state, now).withInvalidTracking(firstInvalid, now, checks);
         if (cleanupPass && this.shouldRemoveInvalid(updated, now)) {
            this.removeLink(updated);
         }

         return updated;
      }
   }

   private boolean shouldRemoveInvalid(WirelessLink link, long now) {
      if (!AE2LTCommonConfig.frequencyCardEnableAutoCleanup()) {
         return false;
      } else {
         long delayTicks = (long)AE2LTCommonConfig.frequencyCardInvalidCleanupDelaySeconds() * 20L;
         return link.state().isCleanupCandidate()
            && link.invalidCheckCount() >= AE2LTCommonConfig.frequencyCardInvalidCleanupRequiredChecks()
            && link.firstInvalidTime() > 0L
            && now - link.firstInvalidTime() >= delayTicks;
      }
   }

   private boolean shouldRunCleanup(MinecraftServer server) {
      return !AE2LTCommonConfig.frequencyCardEnableAutoCleanup() ? false : server.m_129783_().m_46467_() >= this.nextCleanupGameTime;
   }

   private void removeLink(WirelessLink link) {
      this.removeLinks(List.of(link));
   }

   private int removeLinks(Collection<WirelessLink> candidates) {
      LinkedHashSet<UUID> removedLinkIds = new LinkedHashSet<>();
      int removed = 0;

      for (WirelessLink candidate : candidates) {
         WirelessLink current = this.links.get(candidate.linkId());
         if (current != null) {
            this.destroyIndexedRuntimeConnections(current);
            if (this.links.remove(current.linkId()) != null) {
               removedLinkIds.add(current.linkId());
               this.unregisterDevice(current);
               removed++;
            }
         }
      }

      if (removed > 0) {
         this.unregisterRuntimeAnchors(removedLinkIds);
         this.m_77762_();
      }

      return removed;
   }

   private void destroyIndexedRuntimeConnections(WirelessLink link) {
      WirelessLinkRegistry.RuntimeEntrances runtime = this.runtimeConnections.remove(link.linkId());
      this.pendingChannelExpansion.remove(link.linkId());
      if (runtime != null) {
         for (Entry<IGridNode, IGridConnection> entry : new ArrayList<>(runtime.entries())) {
            IGridNode anchor = entry.getKey();
            this.unregisterRuntimeAnchor(link.linkId(), anchor);
            WirelessLinkOps.destroy(entry.getValue(), anchor);
            MultiblockLinkReadiness.refreshAfterVirtualConnectionRemoved(anchor);
         }
      }
   }

   private void destroyRuntimeConnection(WirelessLink link, @Nullable IGridNode targetNode) {
      WirelessLinkRegistry.RuntimeEntrances runtime = this.runtimeConnections.remove(link.linkId());
      this.pendingChannelExpansion.remove(link.linkId());
      if (runtime == null) {
         this.unregisterRuntimeAnchor(link.linkId(), targetNode);
      } else {
         for (Entry<IGridNode, IGridConnection> entry : new ArrayList<>(runtime.entries())) {
            IGridNode anchor = entry.getKey();
            this.unregisterRuntimeAnchor(link.linkId(), anchor);
            WirelessLinkOps.destroy(entry.getValue(), anchor);
            MultiblockLinkReadiness.refreshAfterVirtualConnectionRemoved(anchor);
         }

         this.unregisterRuntimeAnchor(link.linkId(), null);
      }
   }

   private void registerRuntimeAnchor(UUID linkId, IGridNode targetNode) {
      this.runtimeLinksByAnchor.computeIfAbsent(targetNode, ignored -> new LinkedHashSet<>()).add(linkId);
   }

   private void unregisterRuntimeAnchor(UUID linkId, @Nullable IGridNode targetNode) {
      if (targetNode != null) {
         LinkedHashSet<UUID> ids = this.runtimeLinksByAnchor.get(targetNode);
         if (ids != null) {
            ids.remove(linkId);
            if (ids.isEmpty()) {
               this.runtimeLinksByAnchor.remove(targetNode);
            }
         }
      } else {
         Iterator<Entry<IGridNode, LinkedHashSet<UUID>>> iterator = this.runtimeLinksByAnchor.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<IGridNode, LinkedHashSet<UUID>> entry = iterator.next();
            entry.getValue().remove(linkId);
            if (entry.getValue().isEmpty()) {
               iterator.remove();
            }
         }
      }
   }

   private void unregisterRuntimeAnchors(Set<UUID> linkIds) {
      if (!linkIds.isEmpty()) {
         Iterator<Entry<IGridNode, LinkedHashSet<UUID>>> iterator = this.runtimeLinksByAnchor.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<IGridNode, LinkedHashSet<UUID>> entry = iterator.next();
            entry.getValue().removeAll(linkIds);
            if (entry.getValue().isEmpty()) {
               iterator.remove();
            }
         }
      }
   }

   private List<WirelessLink> findLinksInCluster(Set<IGridNode> cluster, MinecraftServer server) {
      LinkedHashSet<UUID> ids = new LinkedHashSet<>();
      LinkedHashSet<String> dimensions = new LinkedHashSet<>();

      for (IGridNode node : cluster) {
         LinkedHashSet<UUID> runtimeIds = this.runtimeLinksByAnchor.get(node);
         if (runtimeIds != null) {
            ids.addAll(runtimeIds);
         }

         try {
            ServerLevel level = node.getLevel();
            if (level != null) {
               dimensions.add(level.m_46472_().m_135782_().toString());
            }
         } catch (RuntimeException var9) {
         }
      }

      for (WirelessLink link : this.links.values()) {
         if (!ids.contains(link.linkId()) && !this.runtimeConnections.containsKey(link.linkId()) && dimensions.contains(link.dimensionId())) {
            WirelessLinkRegistry.PersistedTarget target = this.resolvePersistedTarget(link, server);
            if (target.target() != null && cluster.contains(target.target().node())) {
               ids.add(link.linkId());
            }
         }
      }

      ArrayList<WirelessLink> result = new ArrayList<>(ids.size());

      for (UUID id : ids) {
         WirelessLink linkx = this.links.get(id);
         if (linkx != null) {
            result.add(linkx);
         }
      }

      result.sort(LINK_PREFERENCE);
      return result;
   }

   @Nullable
   private IGridNode resolveRuntimeTargetNode(WirelessLink link) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server == null) {
         return null;
      } else {
         WirelessLinkRegistry.PersistedTarget target = this.resolvePersistedTarget(link, server);
         return target.target() == null ? null : target.target().node();
      }
   }

   private WirelessLinkRegistry.TargetResolution resolveTarget(ServerLevel level, BlockPos pos, @Nullable Direction face, @Nullable Vec3 hitVec) {
      BlockEntity be = level.m_7702_(pos);
      if (be instanceof OverloadedControllerBlockEntity || be instanceof WirelessOverloadedControllerBlockEntity || be instanceof ControllerBlockEntity) {
         return WirelessLinkRegistry.TargetResolution.fail("ae2lt.frequency_card.target_is_controller");
      } else if (be instanceof IPartHost partHost) {
         Optional<WirelessLinkRegistry.TargetResolution> partTarget = this.resolvePartTarget(level, pos, partHost, face, hitVec);
         return partTarget.orElseGet(() -> WirelessLinkRegistry.TargetResolution.fail("ae2lt.frequency_card.unsupported_target"));
      } else {
         return this.resolveDeviceTarget(level, pos, face);
      }
   }

   private Optional<WirelessLinkRegistry.TargetResolution> resolvePartTarget(
      ServerLevel level, BlockPos pos, IPartHost partHost, @Nullable Direction face, @Nullable Vec3 hitVec
   ) {
      IPart part = null;
      Direction side = null;
      if (hitVec != null) {
         SelectedPart selected = partHost.selectPartWorld(hitVec);
         if (selected != null && selected.part != null) {
            part = selected.part;
            side = selected.side;
         }
      }

      if (part == null && face != null) {
         part = partHost.getPart(face);
         side = face;
      }

      if (part == null) {
         part = partHost.getPart(null);
         side = null;
      }

      if (part == null) {
         return Optional.empty();
      } else {
         IGridNode node = part.getGridNode();
         return node == null
            ? Optional.of(WirelessLinkRegistry.TargetResolution.fail("ae2lt.frequency_card.unsupported_target"))
            : Optional.of(WirelessLinkRegistry.TargetResolution.target(this.linkTargetForPart(level, pos, part, side, node)));
      }
   }

   private WirelessLinkRegistry.TargetResolution resolveDeviceTarget(ServerLevel level, BlockPos pos, @Nullable Direction face) {
      IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
      IGridNode node = null;
      if (host != null) {
         for (String sideName : WirelessLinkSideProbeOrder.forPreferredSide(face == null ? "" : face.m_122433_())) {
            Direction side = parseDirection(sideName);
            if (side != null) {
               node = host.getGridNode(side);
               if (node != null) {
                  break;
               }
            }
         }
      }

      if (node == null) {
         for (String sideNamex : WirelessLinkSideProbeOrder.forPreferredSide(face == null ? "" : face.m_122433_())) {
            Direction side = parseDirection(sideNamex);
            if (side != null) {
               node = GridHelper.getExposedNode(level, pos, side);
               if (node != null) {
                  break;
               }
            }
         }
      }

      if (node == null) {
         return WirelessLinkRegistry.TargetResolution.fail("ae2lt.frequency_card.unsupported_target");
      } else {
         BlockEntity be = level.m_7702_(pos);
         return be == null
            ? WirelessLinkRegistry.TargetResolution.fail("ae2lt.frequency_card.unsupported_target")
            : WirelessLinkRegistry.TargetResolution.target(
               new WirelessLinkRegistry.LinkTarget(
                  WirelessLinkMode.DEVICE,
                  node,
                  "",
                  BuiltInRegistries.f_256975_.m_7981_(level.m_8055_(pos).m_60734_()).toString(),
                  BuiltInRegistries.f_257049_.m_7981_(be.m_58903_()).toString(),
                  "",
                  ""
               )
            );
      }
   }

   private List<WirelessLinkRegistry.LocatedTarget> resolveAllTargetsAt(ServerLevel level, BlockPos pos) {
      BlockEntity be = level.m_7702_(pos);
      if (be != null
         && !(be instanceof OverloadedControllerBlockEntity)
         && !(be instanceof WirelessOverloadedControllerBlockEntity)
         && !(be instanceof ControllerBlockEntity)) {
         ArrayList<WirelessLinkRegistry.LocatedTarget> result = new ArrayList<>();
         Set<IGridNode> seen = PhysicalGridCluster.newIdentityNodeSet();
         String dimensionId = level.m_46472_().m_135782_().toString();
         if (be instanceof IPartHost partHost) {
            this.addPartTarget(level, pos, partHost.getPart(null), null, dimensionId, seen, result);

            for (Direction side : Direction.values()) {
               this.addPartTarget(level, pos, partHost.getPart(side), side, dimensionId, seen, result);
            }
         }

         IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
         if (host != null && !(host instanceof IPartHost)) {
            for (Direction side : Direction.values()) {
               IGridNode node = host.getGridNode(side);
               if (node != null && seen.add(node)) {
                  result.add(new WirelessLinkRegistry.LocatedTarget(dimensionId, pos.m_121878_(), this.linkTargetForDevice(level, pos, node)));
               }
            }
         }

         if (result.isEmpty()) {
            for (Direction sidex : Direction.values()) {
               IGridNode node = GridHelper.getExposedNode(level, pos, sidex);
               if (node != null && seen.add(node)) {
                  result.add(new WirelessLinkRegistry.LocatedTarget(dimensionId, pos.m_121878_(), this.linkTargetForDevice(level, pos, node)));
               }
            }
         }

         return result;
      } else {
         return List.of();
      }
   }

   private void addPartTarget(
      ServerLevel level,
      BlockPos pos,
      @Nullable IPart part,
      @Nullable Direction side,
      String dimensionId,
      Set<IGridNode> seen,
      List<WirelessLinkRegistry.LocatedTarget> result
   ) {
      if (part != null) {
         IGridNode node = part.getGridNode();
         if (node != null && seen.add(node)) {
            result.add(new WirelessLinkRegistry.LocatedTarget(dimensionId, pos.m_121878_(), this.linkTargetForPart(level, pos, part, side, node)));
         }
      }
   }

   @Nullable
   private WirelessLinkRegistry.LocatedTarget locateNode(IGridNode node) {
      Object owner;
      try {
         owner = node.getOwner();
      } catch (RuntimeException var7) {
         return null;
      }

      if (owner instanceof AEBasePart part) {
         BlockEntity be = part.getBlockEntity();
         if (be != null && be.m_58904_() instanceof ServerLevel level) {
            return new WirelessLinkRegistry.LocatedTarget(
               level.m_46472_().m_135782_().toString(), be.m_58899_().m_121878_(), this.linkTargetForPart(level, be.m_58899_(), part, part.getSide(), node)
            );
         }
      }

      if (owner instanceof BlockEntity be && be.m_58904_() instanceof ServerLevel level) {
         if (be instanceof IPartHost partHost) {
            WirelessLinkRegistry.LocatedTarget locatedPart = this.locatePartNode(level, be.m_58899_(), partHost, node);
            if (locatedPart != null) {
               return locatedPart;
            }
         }

         return new WirelessLinkRegistry.LocatedTarget(
            level.m_46472_().m_135782_().toString(), be.m_58899_().m_121878_(), this.linkTargetForDevice(level, be.m_58899_(), node)
         );
      }

      return null;
   }

   @Nullable
   private WirelessLinkRegistry.LocatedTarget locatePartNode(ServerLevel level, BlockPos pos, IPartHost host, IGridNode expectedNode) {
      IPart center = host.getPart(null);
      if (center != null && center.getGridNode() == expectedNode) {
         return new WirelessLinkRegistry.LocatedTarget(
            level.m_46472_().m_135782_().toString(), pos.m_121878_(), this.linkTargetForPart(level, pos, center, null, expectedNode)
         );
      } else {
         for (Direction side : Direction.values()) {
            IPart part = host.getPart(side);
            if (part != null && part.getGridNode() == expectedNode) {
               return new WirelessLinkRegistry.LocatedTarget(
                  level.m_46472_().m_135782_().toString(), pos.m_121878_(), this.linkTargetForPart(level, pos, part, side, expectedNode)
               );
            }
         }

         return null;
      }
   }

   @Nullable
   private WirelessLinkRegistry.LocatedTarget resolveLocator(WirelessLinkRegistry.TargetLocator locator, MinecraftServer server) {
      ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(locator.dimensionId()));
      ServerLevel level = server.m_129880_(dim);
      if (level == null) {
         return null;
      } else {
         BlockPos pos = BlockPos.m_122022_(locator.posLong());
         if (level.m_7726_().m_7131_(pos.m_123341_() >> 4, pos.m_123343_() >> 4) == null) {
            return null;
         } else if (locator.mode() == WirelessLinkMode.PART) {
            if (level.m_7702_(pos) instanceof IPartHost host) {
               Direction side = parseDirection(locator.sideName());
               IPart part = host.getPart(side);
               return part != null && part.getGridNode() != null
                  ? new WirelessLinkRegistry.LocatedTarget(
                     locator.dimensionId(), locator.posLong(), this.linkTargetForPart(level, pos, part, side, part.getGridNode())
                  )
                  : null;
            } else {
               return null;
            }
         } else {
            WirelessLinkRegistry.TargetResolution resolution = this.resolveDeviceTarget(level, pos, null);
            return resolution.target() == null ? null : new WirelessLinkRegistry.LocatedTarget(locator.dimensionId(), locator.posLong(), resolution.target());
         }
      }
   }

   private static WirelessLinkRegistry.TargetLocator locatorOf(WirelessLink link) {
      return new WirelessLinkRegistry.TargetLocator(link.dimensionId(), link.posLong(), link.mode(), link.sideName());
   }

   private WirelessLinkRegistry.LinkTarget linkTargetForPart(ServerLevel level, BlockPos pos, IPart part, @Nullable Direction side, IGridNode node) {
      BlockEntity be = level.m_7702_(pos);
      String beType = be == null ? "minecraft:empty" : BuiltInRegistries.f_257049_.m_7981_(be.m_58903_()).toString();
      return new WirelessLinkRegistry.LinkTarget(
         WirelessLinkMode.PART,
         node,
         side == null ? "" : side.m_122433_(),
         BuiltInRegistries.f_256975_.m_7981_(level.m_8055_(pos).m_60734_()).toString(),
         beType,
         partId(part),
         part.getClass().getName()
      );
   }

   private WirelessLinkRegistry.LinkTarget linkTargetForDevice(ServerLevel level, BlockPos pos, IGridNode node) {
      BlockEntity be = level.m_7702_(pos);
      return new WirelessLinkRegistry.LinkTarget(
         WirelessLinkMode.DEVICE,
         node,
         "",
         BuiltInRegistries.f_256975_.m_7981_(level.m_8055_(pos).m_60734_()).toString(),
         be == null ? "minecraft:empty" : BuiltInRegistries.f_257049_.m_7981_(be.m_58903_()).toString(),
         "",
         ""
      );
   }

   private static String partId(IPart part) {
      IPartItem<?> item = part.getPartItem();
      ResourceLocation id = item == null ? null : IPartItem.getId(item);
      return id == null ? part.getClass().getName() : id.toString();
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

   private void registerDevice(WirelessLink link) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(link.dimensionId()));
         manager.registerDevice(
            link.frequencyId(),
            new WirelessFrequencyManager.DeviceEntry(dim, BlockPos.m_122022_(link.posLong()), false, false, "ae2lt.frequency_card.device.cluster")
         );
      }
   }

   private void unregisterDevice(WirelessLink link) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null) {
         ResourceKey<Level> dim = ResourceKey.m_135785_(Registries.f_256858_, ResourceLocation.m_135820_(link.dimensionId()));
         manager.unregisterDevice(link.frequencyId(), dim, BlockPos.m_122022_(link.posLong()));
      }
   }

   private static long currentGameTime(MinecraftServer server) {
      return server.m_129783_().m_46467_();
   }

   @Nullable
   private static Direction parseDirection(String name) {
      if (name != null && !name.isBlank()) {
         for (Direction direction : Direction.values()) {
            if (direction.m_122433_().equals(name)) {
               return direction;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void read(CompoundTag root) {
      this.links.clear();
      ListTag list = root.m_128437_("links", 10);

      for (int i = 0; i < list.size(); i++) {
         Optional<WirelessLink> loaded = loadLink(list.m_128728_(i));
         loaded.ifPresent(this.links::put);
      }
   }

   public CompoundTag m_7176_(CompoundTag root) {
      ListTag list = new ListTag();

      for (WirelessLink link : this.links.values()) {
         list.add(saveLink(link));
      }

      root.m_128365_("links", list);
      return root;
   }

   private static CompoundTag saveLink(WirelessLink link) {
      CompoundTag tag = new CompoundTag();

      for (Entry<String, String> entry : link.toPersistentSnapshot().entrySet()) {
         tag.m_128359_(entry.getKey(), entry.getValue());
      }

      return tag;
   }

   private static Optional<WirelessLink> loadLink(CompoundTag tag) {
      HashMap<String, String> map = new HashMap<>();

      for (String key : tag.m_128431_()) {
         map.put(key, tag.m_128461_(key));
      }

      return WirelessLink.fromPersistentSnapshot(map);
   }

   public static record ActionFeedback(String translationKey, ChatFormatting style, Object... args) {
      public static WirelessLinkRegistry.ActionFeedback green(String key, Object... args) {
         return new WirelessLinkRegistry.ActionFeedback(key, ChatFormatting.GREEN, args);
      }

      public static WirelessLinkRegistry.ActionFeedback yellow(String key, Object... args) {
         return new WirelessLinkRegistry.ActionFeedback(key, ChatFormatting.YELLOW, args);
      }

      public static WirelessLinkRegistry.ActionFeedback red(String key, Object... args) {
         return new WirelessLinkRegistry.ActionFeedback(key, ChatFormatting.RED, args);
      }
   }

   private static final class ClusterComponent {
      private final Set<IGridNode> nodes;
      private final List<WirelessLinkRegistry.LocatedTarget> anchorCandidates = new ArrayList<>();
      private final Set<WirelessLinkRegistry.LinkInheritance> inheritedLinks = new LinkedHashSet<>();

      private ClusterComponent(Set<IGridNode> nodes) {
         this.nodes = nodes;
      }
   }

   private static record InheritedClusterSeed(WirelessLinkRegistry.TargetLocator locator, @Nullable WirelessLinkRegistry.LinkInheritance inheritance) {
   }

   public static record InspectedTargetLink(int frequencyId, WirelessLinkState state) {
   }

   private static record LinkInheritance(UUID sourceLinkId, int frequencyId, UUID ownerUuid, long createdTime) {
      static WirelessLinkRegistry.LinkInheritance from(WirelessLink link) {
         return new WirelessLinkRegistry.LinkInheritance(link.linkId(), link.frequencyId(), link.ownerUuid(), link.createdTime());
      }
   }

   private static record LinkTarget(
      WirelessLinkMode mode, IGridNode node, String sideName, String blockId, String blockEntityTypeId, String partId, String partClassName
   ) {
   }

   private static record LocatedTarget(String dimensionId, long posLong, WirelessLinkRegistry.LinkTarget target) {
      WirelessLinkRegistry.TargetLocator locator() {
         return new WirelessLinkRegistry.TargetLocator(this.dimensionId, this.posLong, this.target.mode(), this.target.sideName());
      }
   }

   private static record PendingAutoConnect(
      UUID playerId, String dimensionId, long posLong, String sideName, String expectedPartId, String expectedPartSideName, int delayTicks
   ) {
      WirelessLinkRegistry.PendingAutoConnect tickDown() {
         return new WirelessLinkRegistry.PendingAutoConnect(
            this.playerId, this.dimensionId, this.posLong, this.sideName, this.expectedPartId, this.expectedPartSideName, this.delayTicks - 1
         );
      }

      boolean expectsPlacedPart() {
         return !this.expectedPartId.isBlank();
      }
   }

   private static final class PendingClusterReconcile {
      private final String dimensionId;
      private final long changedPosLong;
      private final Set<WirelessLinkRegistry.InheritedClusterSeed> inheritedSeeds = new LinkedHashSet<>();
      private final Set<UUID> sourceLinkIds = new LinkedHashSet<>();
      private boolean inspectChangedPosition;
      private int delayTicks;

      private PendingClusterReconcile(String dimensionId, long changedPosLong) {
         this.dimensionId = dimensionId;
         this.changedPosLong = changedPosLong;
         this.delayTicks = 2;
      }

      private void postpone() {
         this.delayTicks = 2;
      }
   }

   private static record PersistedTarget(@Nullable WirelessLinkRegistry.LinkTarget target, @Nullable WirelessLinkState state) {
      static WirelessLinkRegistry.PersistedTarget target(WirelessLinkRegistry.LinkTarget target) {
         return new WirelessLinkRegistry.PersistedTarget(target, null);
      }

      static WirelessLinkRegistry.PersistedTarget state(WirelessLinkState state) {
         return new WirelessLinkRegistry.PersistedTarget(null, state);
      }
   }

   private static final class RuntimeEntrances {
      private final IdentityHashMap<IGridNode, IGridConnection> byAnchor = new IdentityHashMap<>();
      private long nextChannelCheckGameTime;

      @Nullable
      IGridConnection get(IGridNode anchor) {
         return this.byAnchor.get(anchor);
      }

      void put(IGridNode anchor, IGridConnection connection) {
         this.byAnchor.put(anchor, connection);
      }

      @Nullable
      IGridConnection remove(IGridNode anchor) {
         return this.byAnchor.remove(anchor);
      }

      boolean isEmpty() {
         return this.byAnchor.isEmpty();
      }

      Set<IGridNode> anchors() {
         return this.byAnchor.keySet();
      }

      Set<Entry<IGridNode, IGridConnection>> entries() {
         return this.byAnchor.entrySet();
      }

      void deferChannelCheck(long gameTime) {
         this.nextChannelCheckGameTime = Math.max(this.nextChannelCheckGameTime, gameTime);
      }

      boolean canCheckChannels(long gameTime) {
         return gameTime >= this.nextChannelCheckGameTime;
      }
   }

   public static record TargetLinkInspection(boolean liveVirtualEntrance, List<WirelessLinkRegistry.InspectedTargetLink> links) {
      public TargetLinkInspection(boolean liveVirtualEntrance, List<WirelessLinkRegistry.InspectedTargetLink> links) {
         links = List.copyOf(links);
         this.liveVirtualEntrance = liveVirtualEntrance;
         this.links = links;
      }

      public boolean isPresent() {
         return !this.links.isEmpty();
      }
   }

   private static record TargetLocator(String dimensionId, long posLong, WirelessLinkMode mode, String sideName) {
      TargetLocator(String dimensionId, long posLong, WirelessLinkMode mode, String sideName) {
         sideName = sideName == null ? "" : sideName;
         this.dimensionId = dimensionId;
         this.posLong = posLong;
         this.mode = mode;
         this.sideName = sideName;
      }
   }

   private static record TargetResolution(@Nullable WirelessLinkRegistry.LinkTarget target, @Nullable String failureKey) {
      static WirelessLinkRegistry.TargetResolution target(WirelessLinkRegistry.LinkTarget target) {
         return new WirelessLinkRegistry.TargetResolution(target, null);
      }

      static WirelessLinkRegistry.TargetResolution fail(String key) {
         return new WirelessLinkRegistry.TargetResolution(null, key);
      }
   }

   private static record TopologyChangeKey(String dimensionId, long posLong) {
   }
}
