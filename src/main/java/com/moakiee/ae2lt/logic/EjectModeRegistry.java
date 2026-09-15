package com.moakiee.ae2lt.logic;

import com.moakiee.ae2lt.blockentity.GhostOutputBlockEntity;
import com.moakiee.thunderbolt.api.eject.EjectCapabilityRegistry;
import com.moakiee.thunderbolt.api.eject.EjectEndpoint;
import com.moakiee.thunderbolt.api.eject.EjectOfflinePolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

@Deprecated(
   forRemoval = false
)
public final class EjectModeRegistry {
   private static final Map<EjectModeRegistry.EndpointKey, List<EjectModeRegistry.EjectEntry>> LEGACY_ENTRIES = new HashMap<>();

   private EjectModeRegistry() {
   }

   public static void setBypass(boolean value) {
      EjectCapabilityRegistry.setBypass(value);
   }

   public static boolean isBypassed() {
      return EjectCapabilityRegistry.isBypassed();
   }

   public static boolean isEmpty() {
      return LEGACY_ENTRIES.isEmpty();
   }

   public static void onServerStart(MinecraftServer server) {
   }

   public static void onServerStop() {
      LEGACY_ENTRIES.clear();
   }

   public static void register(ResourceKey<Level> dim, long pos, Direction face, EjectModeRegistry.EjectEntry entry) {
      EjectEndpoint endpoint = new EjectEndpoint(dim, BlockPos.m_122022_(pos), face, entry.hostDim(), entry.hostPos(), EjectOfflinePolicy.REJECT);
      EjectCapabilityRegistry.register(endpoint, (server, ignored) -> {
         BlockEntity referenced = entry.getHost();
         if (referenced != null) {
            return referenced;
         } else {
            ServerLevel level = server.m_129880_(entry.hostDim());
            return level != null ? level.m_7702_(entry.hostPos()) : null;
         }
      });
      LEGACY_ENTRIES.computeIfAbsent(new EjectModeRegistry.EndpointKey(dim, pos, face), ignored -> new ArrayList<>()).add(entry);
   }

   public static void unregister(ResourceKey<Level> dim, long pos, Direction face) {
      EjectCapabilityRegistry.unregister(dim, BlockPos.m_122022_(pos), face);
      LEGACY_ENTRIES.remove(new EjectModeRegistry.EndpointKey(dim, pos, face));
   }

   @Nullable
   public static EjectModeRegistry.EjectEntry lookupByFace(ResourceKey<Level> dim, long pos, Direction face) {
      List<EjectModeRegistry.EjectEntry> entries = LEGACY_ENTRIES.get(new EjectModeRegistry.EndpointKey(dim, pos, face));
      if (entries != null && !entries.isEmpty()) {
         for (EjectModeRegistry.EjectEntry entry : entries) {
            if (entry.getHost() != null) {
               return entry;
            }
         }

         return entries.get(0);
      } else {
         return null;
      }
   }

   @Nullable
   public static EjectModeRegistry.EjectEntry lookupAny(ResourceKey<Level> dim, long pos) {
      EjectModeRegistry.EjectEntry fallback = null;

      for (Entry<EjectModeRegistry.EndpointKey, List<EjectModeRegistry.EjectEntry>> entry : LEGACY_ENTRIES.entrySet()) {
         if (entry.getKey().dimension().equals(dim) && entry.getKey().pos() == pos) {
            for (EjectModeRegistry.EjectEntry candidate : entry.getValue()) {
               if (candidate.getHost() != null) {
                  return candidate;
               }

               if (fallback == null) {
                  fallback = candidate;
               }
            }
         }
      }

      return fallback;
   }

   public static List<EjectModeRegistry.DimPos> unregisterAll(BlockEntity host, boolean persistToSavedData) {
      Level hostLevel = host.m_58904_();
      ResourceKey<Level> hostDimension = hostLevel != null ? hostLevel.m_46472_() : null;
      List<EjectModeRegistry.DimPos> removed = EjectCapabilityRegistry.unregisterAll(host)
         .stream()
         .map(pos -> new EjectModeRegistry.DimPos(pos.dimension(), pos.pos()))
         .toList();
      LEGACY_ENTRIES.entrySet().removeIf(mapEntry -> mapEntry.getValue().removeIf(entry -> {
            BlockEntity referenced = entry.getHost();
            return referenced == host || hostDimension != null && entry.hostDim().equals(hostDimension) && entry.hostPos().equals(host.m_58899_());
         }) || mapEntry.getValue().isEmpty());
      return removed;
   }

   public static record DimPos(ResourceKey<Level> dimension, BlockPos pos) {
   }

   public static record EjectEntry(
      @Nullable WeakReference<? extends BlockEntity> hostRef, GhostOutputBlockEntity ghostBE, ResourceKey<Level> hostDim, BlockPos hostPos
   ) {
      @Nullable
      public BlockEntity getHost() {
         return this.hostRef != null ? this.hostRef.get() : null;
      }
   }

   private static record EndpointKey(ResourceKey<Level> dimension, long pos, Direction face) {
   }
}
