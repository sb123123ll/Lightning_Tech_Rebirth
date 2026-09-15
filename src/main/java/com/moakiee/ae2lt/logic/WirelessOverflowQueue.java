package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class WirelessOverflowQueue {
   private static final int MAX_BUCKETS = 1024;
   private static final int REARM_BUCKETS = 768;
   private static final int RETRY_MIN = 5;
   private static final int RETRY_MAX = 20;
   private static final int RETRY_STEP = 5;
   private final Map<OverloadedPatternProviderBlockEntity.WirelessConnection, OverloadedPatternProviderBlockEntity.WirelessConnection> ownersByAddress = new HashMap<>();
   private final DueTaskQueue<OverloadedPatternProviderBlockEntity.WirelessConnection> retries = new DueTaskQueue<>();
   private final WirelessOverflowPatternTable patterns = new WirelessOverflowPatternTable();
   private long lastFlushTick = Long.MIN_VALUE;
   private boolean backpressured;

   boolean isEmpty() {
      return this.ownersByAddress.isEmpty();
   }

   boolean contains(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      return this.ownersByAddress.containsKey(connection);
   }

   int size() {
      return this.ownersByAddress.size();
   }

   Set<OverloadedPatternProviderBlockEntity.WirelessConnection> connections() {
      return Set.copyOf(this.ownersByAddress.values());
   }

   Iterable<WirelessOverflowQueue.Bucket> buckets() {
      ArrayList<WirelessOverflowQueue.Bucket> result = new ArrayList<>(this.ownersByAddress.size());

      for (OverloadedPatternProviderBlockEntity.WirelessConnection owner : this.ownersByAddress.values()) {
         WirelessOverflowQueue.Bucket bucket = owner.wirelessOverflow();
         if (bucket != null) {
            result.add(bucket);
         }
      }

      return result;
   }

   @Nullable
   WirelessOverflowQueue.Bucket get(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      OverloadedPatternProviderBlockEntity.WirelessConnection owner = this.canonical(connection);
      return owner == null ? null : owner.wirelessOverflow();
   }

   OverloadedPatternProviderBlockEntity.WirelessConnection adopt(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      OverloadedPatternProviderBlockEntity.WirelessConnection owner = this.canonical(connection);
      return owner == null ? connection : owner;
   }

   boolean isBackpressured() {
      return this.backpressured;
   }

   WirelessOverflowQueue.Bucket store(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      IPatternDetails pattern,
      List<GenericStack> overflow,
      boolean forceFallback,
      long gameTick
   ) {
      short patternId = this.patterns.intern(pattern, this::buckets);
      WirelessOverflowQueue.Bucket bucket;
      if (!forceFallback && WirelessOverflowPatternTable.isCompactEligible(pattern)) {
         IInput[] inputs = pattern.getInputs();
         GenericStack first = overflow.get(0);
         int stuckIndex = WirelessOverflowPatternTable.findSlotIndex(inputs, first.what());
         if (stuckIndex >= 0 && WirelessOverflowPatternTable.verifySequentialOverflow(inputs, stuckIndex, overflow)) {
            bucket = WirelessOverflowQueue.Bucket.compact(patternId, (short)stuckIndex, first.amount());
         } else {
            bucket = WirelessOverflowQueue.Bucket.fallback(patternId, overflow);
         }
      } else {
         bucket = WirelessOverflowQueue.Bucket.fallback(patternId, overflow);
      }

      this.put(connection, bucket, gameTick);
      return bucket;
   }

   WirelessOverflowQueue.Bucket storeRouted(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection, IPatternDetails pattern, List<RoutedPatternOverflow.Entry> overflow, long gameTick
   ) {
      WirelessOverflowQueue.Bucket bucket = WirelessOverflowQueue.Bucket.routedFallback(this.patterns.intern(pattern, this::buckets), overflow);
      this.put(connection, bucket, gameTick);
      return bucket;
   }

   void restoreBucket(OverloadedPatternProviderBlockEntity.WirelessConnection connection, WirelessOverflowQueue.Bucket bucket, long gameTick) {
      this.put(connection, bucket, gameTick);
   }

   @Nullable
   WirelessOverflowQueue.Bucket remove(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      OverloadedPatternProviderBlockEntity.WirelessConnection owner = this.canonical(connection);
      if (owner == null) {
         return null;
      } else {
         this.retries.remove(owner);
         this.ownersByAddress.remove(owner);
         WirelessOverflowQueue.Bucket removed = owner.wirelessOverflow();
         owner.setWirelessOverflow(null);
         this.refreshBackpressure();
         return removed;
      }
   }

   boolean beginFlush(long gameTick) {
      if (!this.ownersByAddress.isEmpty() && this.lastFlushTick != gameTick) {
         this.lastFlushTick = gameTick;
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   OverloadedPatternProviderBlockEntity.WirelessConnection pollDue(long gameTick) {
      return this.retries.pollDue(gameTick);
   }

   void rescheduleBlocked(OverloadedPatternProviderBlockEntity.WirelessConnection connection, WirelessOverflowQueue.Bucket bucket, long gameTick) {
      bucket.retryDelay = nextRetryDelay(bucket.retryDelay, WirelessOverflowQueue.OverflowAttemptResult.BLOCKED);
      this.schedule(connection, bucket, gameTick + (long)bucket.retryDelay);
   }

   void reschedule(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      WirelessOverflowQueue.Bucket bucket,
      long gameTick,
      WirelessOverflowQueue.OverflowAttemptResult result
   ) {
      bucket.retryDelay = nextRetryDelay(bucket.retryDelay, result);
      this.schedule(connection, bucket, gameTick + (long)bucket.retryDelay);
   }

   long nextDueTick() {
      return this.retries.nextDueTick();
   }

   @Nullable
   IPatternDetails pattern(int unsignedId) {
      return this.patterns.get(unsignedId);
   }

   void restorePattern(int id, IPatternDetails pattern) {
      this.patterns.restore(id, pattern);
   }

   void clear() {
      for (OverloadedPatternProviderBlockEntity.WirelessConnection owner : this.ownersByAddress.values()) {
         owner.setWirelessOverflow(null);
      }

      this.ownersByAddress.clear();
      this.retries.clear();
      this.patterns.clear();
      this.lastFlushTick = Long.MIN_VALUE;
      this.backpressured = false;
   }

   void refreshBackpressure() {
      int total = this.ownersByAddress.size();
      if (this.backpressured) {
         if (total <= 768) {
            this.backpressured = false;
         }
      } else if (total >= 1024) {
         this.backpressured = true;
      }
   }

   private void put(OverloadedPatternProviderBlockEntity.WirelessConnection connection, WirelessOverflowQueue.Bucket bucket, long gameTick) {
      OverloadedPatternProviderBlockEntity.WirelessConnection owner = this.adopt(connection);
      owner.setWirelessOverflow(bucket);
      this.ownersByAddress.putIfAbsent(owner, owner);
      bucket.retryDelay = initialRetryDelay();
      this.schedule(connection, bucket, gameTick + (long)bucket.retryDelay);
      this.refreshBackpressure();
   }

   private void schedule(OverloadedPatternProviderBlockEntity.WirelessConnection connection, WirelessOverflowQueue.Bucket bucket, long dueTick) {
      OverloadedPatternProviderBlockEntity.WirelessConnection owner = this.canonical(connection);
      if (owner != null && owner.wirelessOverflow() == bucket) {
         this.retries.schedule(owner, dueTick);
      }
   }

   @Nullable
   private OverloadedPatternProviderBlockEntity.WirelessConnection canonical(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      return this.ownersByAddress.get(connection);
   }

   static int initialRetryDelay() {
      return 5;
   }

   static int nextRetryDelay(int currentDelay, WirelessOverflowQueue.OverflowAttemptResult result) {
      if (result == WirelessOverflowQueue.OverflowAttemptResult.PROGRESSED) {
         return 5;
      } else if (result == WirelessOverflowQueue.OverflowAttemptResult.CLEARED) {
         return 0;
      } else {
         int normalized = Math.max(5, Math.min(20, currentDelay));
         return Math.min(20, normalized + 5);
      }
   }

   static final class Bucket implements WirelessOverflowPatternTable.PatternReference {
      final boolean compactMode;
      short patternId;
      short stuckIndex;
      long remaining;
      final RoutedPatternOverflow fallback;
      int retryDelay = WirelessOverflowQueue.initialRetryDelay();

      private Bucket(boolean compactMode, short patternId, short stuckIndex, long remaining, RoutedPatternOverflow fallback) {
         this.compactMode = compactMode;
         this.patternId = patternId;
         this.stuckIndex = stuckIndex;
         this.remaining = remaining;
         this.fallback = fallback;
      }

      static WirelessOverflowQueue.Bucket compact(short patternId, short stuckIndex, long remaining) {
         return new WirelessOverflowQueue.Bucket(true, patternId, stuckIndex, remaining, RoutedPatternOverflow.unrouted(List.of()));
      }

      static WirelessOverflowQueue.Bucket fallback(short patternId, List<GenericStack> overflow) {
         return new WirelessOverflowQueue.Bucket(false, patternId, (short)0, 0L, RoutedPatternOverflow.unrouted(overflow));
      }

      static WirelessOverflowQueue.Bucket routedFallback(short patternId, List<RoutedPatternOverflow.Entry> overflow) {
         return new WirelessOverflowQueue.Bucket(false, patternId, (short)0, 0L, RoutedPatternOverflow.routed(overflow));
      }

      @Override
      public boolean usesPatternDefinition() {
         return this.compactMode;
      }

      @Override
      public int unsignedPatternId() {
         return Short.toUnsignedInt(this.patternId);
      }

      @Override
      public void setPatternId(short patternId) {
         this.patternId = patternId;
      }
   }

   static enum OverflowAttemptResult {
      CLEARED(true, false, true),
      PROGRESSED(false, true, true),
      BLOCKED(false, true, false);

      private final boolean removeBucket;
      private final boolean reschedule;
      private final boolean persistentStateChanged;

      private OverflowAttemptResult(boolean removeBucket, boolean reschedule, boolean persistentStateChanged) {
         this.removeBucket = removeBucket;
         this.reschedule = reschedule;
         this.persistentStateChanged = persistentStateChanged;
      }

      boolean removeBucket() {
         return this.removeBucket;
      }

      boolean reschedule() {
         return this.reschedule;
      }

      boolean persistentStateChanged() {
         return this.persistentStateChanged;
      }
   }
}
