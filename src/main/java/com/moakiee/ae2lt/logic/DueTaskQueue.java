package com.moakiee.ae2lt.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class DueTaskQueue<K> {
   private static final int MIN_COMPACTION_SIZE = 64;
   private static final int COMPACTION_MULTIPLIER = 4;
   private final Map<K, DueTaskQueue.Schedule> schedules = new HashMap<>();
   private final PriorityQueue<DueTaskQueue.Entry<K>> entries = new PriorityQueue<>((left, right) -> {
      int dueComparison = Long.compare(left.dueTick(), right.dueTick());
      return dueComparison != 0 ? dueComparison : Long.compare(left.sequence(), right.sequence());
   });
   private long nextToken;
   private long nextSequence;

   void schedule(K key, long dueTick) {
      long token = ++this.nextToken;
      this.schedules.put(key, new DueTaskQueue.Schedule(dueTick, token));
      this.entries.add(new DueTaskQueue.Entry<>(key, dueTick, token, this.nextSequence++));
      this.compactIfNeeded();
   }

   boolean contains(K key) {
      return this.schedules.containsKey(key);
   }

   void remove(K key) {
      this.schedules.remove(key);
   }

   void retainAll(Set<K> retainedKeys) {
      this.schedules.keySet().retainAll(retainedKeys);
      this.compactIfNeeded();
   }

   @Nullable
   K pollDue(long gameTick) {
      this.discardStaleHead();
      DueTaskQueue.Entry<K> entry = this.entries.peek();
      if (entry != null && entry.dueTick() <= gameTick) {
         this.entries.poll();
         this.schedules.remove(entry.key());
         return entry.key();
      } else {
         return null;
      }
   }

   long nextDueTick() {
      this.discardStaleHead();
      DueTaskQueue.Entry<K> entry = this.entries.peek();
      return entry != null ? entry.dueTick() : Long.MAX_VALUE;
   }

   int size() {
      return this.schedules.size();
   }

   void clear() {
      this.schedules.clear();
      this.entries.clear();
   }

   private void discardStaleHead() {
      while (!this.entries.isEmpty() && !this.isLive(this.entries.peek())) {
         this.entries.poll();
      }
   }

   private boolean isLive(DueTaskQueue.Entry<K> entry) {
      DueTaskQueue.Schedule schedule = this.schedules.get(entry.key());
      return schedule != null && schedule.token() == entry.token() && schedule.dueTick() == entry.dueTick();
   }

   private void compactIfNeeded() {
      int liveCount = this.schedules.size();
      int threshold = Math.max(64, liveCount * 4);
      if (this.entries.size() > threshold) {
         this.entries.clear();

         for (Map.Entry<K, DueTaskQueue.Schedule> scheduled : this.schedules.entrySet()) {
            DueTaskQueue.Schedule value = scheduled.getValue();
            this.entries.add(new DueTaskQueue.Entry<>(scheduled.getKey(), value.dueTick(), value.token(), this.nextSequence++));
         }
      }
   }

   private static record Entry<K>(K key, long dueTick, long token, long sequence) {
   }

   private static record Schedule(long dueTick, long token) {
   }
}
