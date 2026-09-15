package com.moakiee.ae2lt.grid.wirelesslink;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class WirelessLinkIndex {
   private static final int MIN_TOMBSTONES_BEFORE_COMPACT = 128;
   private final Map<UUID, WirelessLink> byId = new LinkedHashMap<>();
   private final Int2ObjectOpenHashMap<LinkedHashMap<UUID, WirelessLink>> byFrequency = new Int2ObjectOpenHashMap();
   private final Map<String, Long2ObjectOpenHashMap<LinkedHashMap<UUID, WirelessLink>>> byPosition = new HashMap<>();
   private final List<UUID> orderedIds = new ArrayList<>();
   private int cursor;
   private int tombstones;

   boolean isEmpty() {
      return this.byId.isEmpty();
   }

   boolean contains(UUID linkId) {
      return this.byId.containsKey(linkId);
   }

   WirelessLink get(UUID linkId) {
      return this.byId.get(linkId);
   }

   Collection<WirelessLink> values() {
      return this.byId.values();
   }

   Collection<WirelessLink> findAllForFrequency(int frequencyId) {
      LinkedHashMap<UUID, WirelessLink> matches = (LinkedHashMap<UUID, WirelessLink>)this.byFrequency.get(frequencyId);
      return (Collection<WirelessLink>)(matches == null ? List.of() : matches.values());
   }

   List<WirelessLink> findAllInDimension(String dimensionId) {
      ArrayList<WirelessLink> matches = new ArrayList<>();

      for (WirelessLink link : this.byId.values()) {
         if (link.dimensionId().equals(dimensionId)) {
            matches.add(link);
         }
      }

      return matches;
   }

   Collection<WirelessLink> findAllAt(String dimensionId, long posLong) {
      Long2ObjectOpenHashMap<LinkedHashMap<UUID, WirelessLink>> dimension = this.byPosition.get(dimensionId);
      if (dimension == null) {
         return List.of();
      } else {
         LinkedHashMap<UUID, WirelessLink> matches = (LinkedHashMap<UUID, WirelessLink>)dimension.get(posLong);
         return (Collection<WirelessLink>)(matches == null ? List.of() : matches.values());
      }
   }

   void clear() {
      this.byId.clear();
      this.byFrequency.clear();
      this.byPosition.clear();
      this.orderedIds.clear();
      this.cursor = 0;
      this.tombstones = 0;
   }

   void put(WirelessLink link) {
      WirelessLink previous = this.byId.put(link.linkId(), link);
      if (previous != null && previous.frequencyId() != link.frequencyId()) {
         this.removeFromFrequencyIndex(previous);
      }

      if (previous != null && (!previous.dimensionId().equals(link.dimensionId()) || previous.posLong() != link.posLong())) {
         this.removeFromPositionIndex(previous);
      }

      ((LinkedHashMap)this.byFrequency.computeIfAbsent(link.frequencyId(), ignored -> new LinkedHashMap())).put(link.linkId(), link);
      ((LinkedHashMap)this.byPosition
            .computeIfAbsent(link.dimensionId(), ignored -> new Long2ObjectOpenHashMap())
            .computeIfAbsent(link.posLong(), ignored -> new LinkedHashMap()))
         .put(link.linkId(), link);
      if (previous == null) {
         this.orderedIds.add(link.linkId());
      }
   }

   WirelessLink remove(UUID linkId) {
      WirelessLink removed = this.byId.remove(linkId);
      if (removed == null) {
         return null;
      } else {
         this.removeFromFrequencyIndex(removed);
         this.removeFromPositionIndex(removed);
         this.tombstones++;
         this.compactOrderIfNeeded();
         return removed;
      }
   }

   private void removeFromFrequencyIndex(WirelessLink link) {
      LinkedHashMap<UUID, WirelessLink> matches = (LinkedHashMap<UUID, WirelessLink>)this.byFrequency.get(link.frequencyId());
      if (matches != null) {
         matches.remove(link.linkId());
         if (matches.isEmpty()) {
            this.byFrequency.remove(link.frequencyId());
         }
      }
   }

   private void removeFromPositionIndex(WirelessLink link) {
      Long2ObjectOpenHashMap<LinkedHashMap<UUID, WirelessLink>> dimension = this.byPosition.get(link.dimensionId());
      if (dimension != null) {
         LinkedHashMap<UUID, WirelessLink> matches = (LinkedHashMap<UUID, WirelessLink>)dimension.get(link.posLong());
         if (matches != null) {
            matches.remove(link.linkId());
            if (matches.isEmpty()) {
               dimension.remove(link.posLong());
            }

            if (dimension.isEmpty()) {
               this.byPosition.remove(link.dimensionId());
            }
         }
      }
   }

   List<WirelessLink> nextBatch(int requestedBatchSize) {
      if (!this.byId.isEmpty() && requestedBatchSize > 0) {
         int limit = Math.min(requestedBatchSize, this.byId.size());
         ArrayList<WirelessLink> batch = new ArrayList<>(limit);
         int scanned = 0;
         int scanLimit = this.orderedIds.size();

         while (batch.size() < limit && scanned < scanLimit && !this.orderedIds.isEmpty()) {
            if (this.cursor >= this.orderedIds.size()) {
               this.cursor = 0;
            }

            WirelessLink link = this.byId.get(this.orderedIds.get(this.cursor++));
            scanned++;
            if (link != null) {
               batch.add(link);
            }
         }

         return batch;
      } else {
         return List.of();
      }
   }

   private void compactOrderIfNeeded() {
      if (this.tombstones >= 128 && this.tombstones * 4 >= this.orderedIds.size()) {
         this.orderedIds.clear();
         this.orderedIds.addAll(this.byId.keySet());
         if (this.cursor > this.orderedIds.size()) {
            this.cursor = this.orderedIds.size();
         }

         this.tombstones = 0;
      }
   }
}
