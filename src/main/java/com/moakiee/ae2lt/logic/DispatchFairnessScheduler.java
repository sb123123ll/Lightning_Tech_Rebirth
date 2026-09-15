package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

final class DispatchFairnessScheduler<T, P> {
   static final int WINDOW_TICKS = 100;
   private static final int MIN_READY_COMPACTION_SIZE = 64;
   private static final int READY_COMPACTION_MULTIPLIER = 4;
   private final Map<P, DispatchFairnessScheduler.PatternState<T>> patterns;
   private final Set<T> pausedTargets = new HashSet<>();
   private long sequence;

   DispatchFairnessScheduler() {
      this(new HashMap<>());
   }

   private DispatchFairnessScheduler(Map<P, DispatchFairnessScheduler.PatternState<T>> patterns) {
      this.patterns = patterns;
   }

   static <T> DispatchFairnessScheduler<T, IPatternDetails> forCanonicalPatterns() {
      return new DispatchFairnessScheduler<>(CanonicalPatternMaps.create());
   }

   DispatchFairnessScheduler<T, P>.Pass beginPass(P pattern, Collection<T> currentTargets, long topologyVersion, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> state = this.stateFor(pattern, gameTick);
      this.synchronizeTargets(state, currentTargets, topologyVersion);
      this.activateDueTargets(state, gameTick);
      if (state.passOpen) {
         throw new IllegalStateException("A fairness pass is already open for this pattern");
      } else {
         state.passOpen = true;
         return new DispatchFairnessScheduler.Pass(state, gameTick);
      }
   }

   void excludeUntil(P pattern, T target, long retryAfter, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> patternState = this.stateFor(pattern, gameTick);
      DispatchFairnessScheduler.TargetState<T> targetState = this.ensureTarget(patternState, target);
      this.deactivate(patternState, targetState);
      targetState.cooldownUntil = Math.max(gameTick + 1L, retryAfter);
      targetState.cooldownVersion++;
      patternState.cooldowns.add(new DispatchFairnessScheduler.CooldownEntry<>(targetState.cooldownUntil, targetState.cooldownVersion, targetState));
   }

   void pauseTarget(T target) {
      if (this.pausedTargets.add(target)) {
         for (DispatchFairnessScheduler.PatternState<T> patternState : this.patterns.values()) {
            DispatchFairnessScheduler.TargetState<T> targetState = patternState.targets.get(target);
            if (targetState != null) {
               this.deactivate(patternState, targetState);
            }
         }
      }
   }

   void resumeTarget(T target, long gameTick) {
      if (this.pausedTargets.remove(target)) {
         for (DispatchFairnessScheduler.PatternState<T> patternState : this.patterns.values()) {
            this.advanceWindow(patternState, gameTick);
            this.activateDueTargets(patternState, gameTick);
            DispatchFairnessScheduler.TargetState<T> targetState = patternState.targets.get(target);
            if (targetState != null && targetState.cooldownUntil <= gameTick && !targetState.active) {
               this.activateAtCurrentAverage(patternState, targetState);
            }
         }
      }
   }

   void removeTarget(T target) {
      this.pausedTargets.remove(target);

      for (DispatchFairnessScheduler.PatternState<T> patternState : this.patterns.values()) {
         DispatchFairnessScheduler.TargetState<T> targetState = patternState.targets.remove(target);
         if (targetState != null) {
            this.removeState(patternState, targetState);
         }
      }
   }

   void clear() {
      this.patterns.clear();
      this.pausedTargets.clear();
   }

   long dispatchCount(P pattern, T target, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> patternState = this.stateFor(pattern, gameTick);
      this.activateDueTargets(patternState, gameTick);
      DispatchFairnessScheduler.TargetState<T> targetState = patternState.targets.get(target);
      return targetState == null ? 0L : targetState.ownedCopies;
   }

   long minimumActiveDispatchCount(P pattern, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> patternState = this.stateFor(pattern, gameTick);
      this.activateDueTargets(patternState, gameTick);
      long minimum = Long.MAX_VALUE;

      for (DispatchFairnessScheduler.TargetState<T> targetState : patternState.targets.values()) {
         if (targetState.active) {
            minimum = Math.min(minimum, targetState.ownedCopies);
         }
      }

      return minimum == Long.MAX_VALUE ? 0L : minimum;
   }

   long maximumActiveDispatchCount(P pattern, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> patternState = this.stateFor(pattern, gameTick);
      this.activateDueTargets(patternState, gameTick);
      long maximum = 0L;

      for (DispatchFairnessScheduler.TargetState<T> targetState : patternState.targets.values()) {
         if (targetState.active) {
            maximum = Math.max(maximum, targetState.ownedCopies);
         }
      }

      return maximum;
   }

   int activeTargetCount(P pattern, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> patternState = this.stateFor(pattern, gameTick);
      this.activateDueTargets(patternState, gameTick);
      return patternState.activeCount;
   }

   private DispatchFairnessScheduler.PatternState<T> stateFor(P pattern, long gameTick) {
      DispatchFairnessScheduler.PatternState<T> state = this.patterns.computeIfAbsent(pattern, ignored -> new DispatchFairnessScheduler.PatternState<>());
      this.advanceWindow(state, gameTick);
      return state;
   }

   private void advanceWindow(DispatchFairnessScheduler.PatternState<T> state, long gameTick) {
      if (state.lastAdvancedTick != gameTick) {
         if (state.lastAdvancedTick != Long.MIN_VALUE && gameTick < state.lastAdvancedTick) {
            state.history.clear();
            state.activeSum = 0L;
            state.activeCountFrequencies.clear();

            for (DispatchFairnessScheduler.TargetState<T> targetState : state.targets.values()) {
               targetState.ownedCopies = 0L;
               targetState.schedulingCredit = 0L;
               targetState.leased = false;
               targetState.leasedAllowance = 0L;
               targetState.queueVersion++;
               if (targetState.active) {
                  addFrequency(state, 0L);
                  this.offer(state, targetState);
               }
            }
         }

         state.lastAdvancedTick = gameTick;
         long expireThrough = gameTick - 100L;

         while (!state.history.isEmpty() && state.history.peekFirst().gameTick <= expireThrough) {
            DispatchFairnessScheduler.WindowBucket<T> expired = state.history.removeFirst();
            this.expireAmounts(state, expired.ownedCopies, false);
            this.expireAmounts(state, expired.schedulingCredits, true);
         }
      }
   }

   private void expireAmounts(
      DispatchFairnessScheduler.PatternState<T> state, Map<DispatchFairnessScheduler.TargetState<T>, Long> amounts, boolean schedulingCredit
   ) {
      for (Entry<DispatchFairnessScheduler.TargetState<T>, Long> entry : amounts.entrySet()) {
         DispatchFairnessScheduler.TargetState<T> targetState = entry.getKey();
         if (state.targets.get(targetState.target) == targetState) {
            long amount = Math.min(schedulingCredit ? targetState.schedulingCredit : targetState.ownedCopies, entry.getValue());
            if (amount > 0L) {
               if (targetState.active) {
                  removeFrequency(state, targetState.effectiveCount());
                  state.activeSum -= amount;
               }

               if (schedulingCredit) {
                  targetState.schedulingCredit -= amount;
               } else {
                  targetState.ownedCopies -= amount;
               }

               if (targetState.active) {
                  addFrequency(state, targetState.effectiveCount());
                  this.offer(state, targetState);
               }
            }
         }
      }
   }

   private void synchronizeTargets(DispatchFairnessScheduler.PatternState<T> state, Collection<T> currentTargets, long topologyVersion) {
      if (state.topologyVersion != topologyVersion) {
         HashSet<T> retained = new HashSet<>(currentTargets);
         Iterator<Entry<T, DispatchFairnessScheduler.TargetState<T>>> iterator = state.targets.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<T, DispatchFairnessScheduler.TargetState<T>> entry = iterator.next();
            if (!retained.contains(entry.getKey())) {
               this.removeState(state, entry.getValue());
               iterator.remove();
            }
         }

         for (T target : currentTargets) {
            this.ensureTarget(state, target);
         }

         state.topologyVersion = topologyVersion;
      }
   }

   private DispatchFairnessScheduler.TargetState<T> ensureTarget(DispatchFairnessScheduler.PatternState<T> state, T target) {
      DispatchFairnessScheduler.TargetState<T> existing = state.targets.get(target);
      if (existing != null) {
         return existing;
      } else {
         DispatchFairnessScheduler.TargetState<T> created = new DispatchFairnessScheduler.TargetState<>(target);
         state.targets.put(target, created);
         if (!this.pausedTargets.contains(target)) {
            this.activateAtCurrentAverage(state, created);
         }

         return created;
      }
   }

   private void activateDueTargets(DispatchFairnessScheduler.PatternState<T> state, long gameTick) {
      while (!state.cooldowns.isEmpty() && state.cooldowns.peek().retryAfter <= gameTick) {
         DispatchFairnessScheduler.CooldownEntry<T> due = state.cooldowns.poll();
         DispatchFairnessScheduler.TargetState<T> targetState = due.targetState;
         if (due.cooldownVersion == targetState.cooldownVersion
            && due.retryAfter == targetState.cooldownUntil
            && state.targets.containsKey(targetState.target)
            && !this.pausedTargets.contains(targetState.target)
            && !targetState.active) {
            this.activateAtCurrentAverage(state, targetState);
         }
      }
   }

   private void activateAtCurrentAverage(DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState) {
      long baseline = ceilingAverage(state.activeSum, state.activeCount);
      if (targetState.effectiveCount() < baseline) {
         this.addSchedulingCredit(state, targetState, baseline - targetState.effectiveCount());
      }

      targetState.cooldownUntil = Long.MIN_VALUE;
      targetState.active = true;
      targetState.leased = false;
      state.activeCount++;
      state.activeSum = saturatingAdd(state.activeSum, targetState.effectiveCount());
      addFrequency(state, targetState.effectiveCount());
      this.offer(state, targetState);
   }

   private void deactivate(DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState) {
      if (targetState.active) {
         state.activeCount--;
         state.activeSum = state.activeSum - targetState.effectiveCount();
         removeFrequency(state, targetState.effectiveCount());
      }

      targetState.active = false;
      targetState.leased = false;
      targetState.leasedAllowance = 0L;
      targetState.queueVersion++;
   }

   private void removeState(DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState) {
      this.deactivate(state, targetState);
      targetState.cooldownVersion++;
   }

   private void addSuccessfulCopies(DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState, long ownedCopies) {
      removeFrequency(state, targetState.effectiveCount());
      targetState.ownedCopies = saturatingAdd(targetState.ownedCopies, ownedCopies);
      state.activeSum = saturatingAdd(state.activeSum, ownedCopies);
      addFrequency(state, targetState.effectiveCount());
      this.addHistory(state, targetState, ownedCopies, false);
      targetState.lastSuccessfulSequence = ++this.sequence;
   }

   private void addSchedulingCredit(DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState, long credit) {
      if (credit > 0L) {
         if (targetState.active) {
            removeFrequency(state, targetState.effectiveCount());
            state.activeSum = saturatingAdd(state.activeSum, credit);
         }

         targetState.schedulingCredit = saturatingAdd(targetState.schedulingCredit, credit);
         if (targetState.active) {
            addFrequency(state, targetState.effectiveCount());
            this.offer(state, targetState);
         }

         this.addHistory(state, targetState, credit, true);
      }
   }

   private void addHistory(
      DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState, long amount, boolean schedulingCredit
   ) {
      if (amount > 0L) {
         DispatchFairnessScheduler.WindowBucket<T> bucket = state.history.peekLast();
         if (bucket == null || bucket.gameTick != state.lastAdvancedTick) {
            bucket = new DispatchFairnessScheduler.WindowBucket<>(state.lastAdvancedTick);
            state.history.addLast(bucket);
         }

         Map<DispatchFairnessScheduler.TargetState<T>, Long> amounts = schedulingCredit ? bucket.schedulingCredits : bucket.ownedCopies;
         amounts.merge(targetState, amount, DispatchFairnessScheduler::saturatingAdd);
      }
   }

   private static <T> void addFrequency(DispatchFairnessScheduler.PatternState<T> state, long count) {
      state.activeCountFrequencies.merge(count, 1, Integer::sum);
   }

   private static <T> void removeFrequency(DispatchFairnessScheduler.PatternState<T> state, long count) {
      Integer occurrences = state.activeCountFrequencies.get(count);
      if (occurrences != null) {
         if (occurrences <= 1) {
            state.activeCountFrequencies.remove(count);
         } else {
            state.activeCountFrequencies.put(count, occurrences - 1);
         }
      }
   }

   private void offer(DispatchFairnessScheduler.PatternState<T> state, DispatchFairnessScheduler.TargetState<T> targetState) {
      if (targetState.active && !targetState.leased) {
         long version = ++targetState.queueVersion;
         state.ready
            .add(
               new DispatchFairnessScheduler.ReadyEntry<>(
                  targetState.effectiveCount(), targetState.lastSuccessfulSequence, ++this.sequence, version, targetState
               )
            );
         this.compactReadyIfNeeded(state);
      }
   }

   private void compactReadyIfNeeded(DispatchFairnessScheduler.PatternState<T> state) {
      int threshold = Math.max(64, state.activeCount * 4);
      if (state.ready.size() > threshold) {
         state.ready.clear();

         for (DispatchFairnessScheduler.TargetState<T> targetState : state.targets.values()) {
            if (targetState.active && !targetState.leased) {
               long version = ++targetState.queueVersion;
               state.ready
                  .add(
                     new DispatchFairnessScheduler.ReadyEntry<>(
                        targetState.effectiveCount(), targetState.lastSuccessfulSequence, ++this.sequence, version, targetState
                     )
                  );
            }
         }
      }
   }

   @Nullable
   private DispatchFairnessScheduler.TargetState<T> pollState(DispatchFairnessScheduler.PatternState<T> state) {
      while (!state.ready.isEmpty()) {
         DispatchFairnessScheduler.ReadyEntry<T> entry = state.ready.poll();
         DispatchFairnessScheduler.TargetState<T> targetState = entry.targetState;
         if (entry.queueVersion == targetState.queueVersion && targetState.active && !targetState.leased && state.targets.containsKey(targetState.target)) {
            targetState.leased = true;
            targetState.queueVersion++;
            return targetState;
         }
      }

      return null;
   }

   private static long ceilingAverage(long sum, int count) {
      return sum > 0L && count > 0 ? 1L + (sum - 1L) / (long)count : 0L;
   }

   private static long doubledAverageFloor(long sum, int count) {
      if (sum > 0L && count > 0) {
         long quotient = sum / (long)count;
         long remainder = sum % (long)count;
         long doubledQuotient = saturatingAdd(quotient, quotient);
         long doubledRemainder = remainder * 2L / (long)count;
         return saturatingAdd(doubledQuotient, doubledRemainder);
      } else {
         return 0L;
      }
   }

   private static long saturatingAdd(long left, long right) {
      return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
   }

   private static record CooldownEntry<T>(long retryAfter, long cooldownVersion, DispatchFairnessScheduler.TargetState<T> targetState) {
   }

   final class Pass implements AutoCloseable {
      private final DispatchFairnessScheduler.PatternState<T> state;
      private final long gameTick;
      private final ArrayList<DispatchFairnessScheduler.TargetState<T>> leased = new ArrayList<>();
      private boolean closed;

      private Pass(DispatchFairnessScheduler.PatternState<T> state, long gameTick) {
         this.state = state;
         this.gameTick = gameTick;
      }

      int activeTargetsAtStart() {
         return this.state.activeCount;
      }

      long allowance(T target) {
         DispatchFairnessScheduler.TargetState<T> targetState = this.state.targets.get(target);
         if (targetState == null || !targetState.active) {
            return 0L;
         } else {
            return targetState.leased ? targetState.leasedAllowance : this.allowanceFor(this.state, targetState);
         }
      }

      long raiseAllowance(T target, long minimumAllowance) {
         DispatchFairnessScheduler.TargetState<T> targetState = this.requireLeased(target);
         targetState.leasedAllowance = Math.max(targetState.leasedAllowance, Math.max(0L, minimumAllowance));
         return targetState.leasedAllowance;
      }

      private long allowanceFor(DispatchFairnessScheduler.PatternState<T> patternState, DispatchFairnessScheduler.TargetState<T> targetState) {
         long averageCeiling = DispatchFairnessScheduler.doubledAverageFloor(patternState.activeSum, patternState.activeCount);
         long minimum = patternState.activeCountFrequencies.isEmpty() ? 0L : patternState.activeCountFrequencies.firstKey();
         long ratioCeiling = DispatchFairnessScheduler.saturatingAdd(Math.max(1L, minimum), Math.max(1L, minimum));
         long ceiling = Math.max(1L, Math.min(averageCeiling, ratioCeiling));
         return Math.max(0L, ceiling - targetState.effectiveCount());
      }

      @Nullable
      T poll() {
         this.ensureOpen();
         DispatchFairnessScheduler.TargetState<T> targetState = DispatchFairnessScheduler.this.pollState(this.state);
         if (targetState == null) {
            return null;
         } else {
            targetState.leasedAllowance = this.allowanceFor(this.state, targetState);
            if (targetState.leasedAllowance <= 0L) {
               targetState.leased = false;
               targetState.leasedAllowance = 0L;
               DispatchFairnessScheduler.this.offer(this.state, targetState);
               return null;
            } else {
               this.leased.add(targetState);
               return targetState.target;
            }
         }
      }

      void success(T target, long ownedCopies) {
         this.ensureOpen();
         if (ownedCopies <= 0L) {
            throw new IllegalArgumentException("A successful dispatch must own at least one copy");
         } else {
            DispatchFairnessScheduler.TargetState<T> targetState = this.requireLeased(target);
            if (ownedCopies > targetState.leasedAllowance) {
               throw new IllegalArgumentException("Successful copies exceed this target's fairness allowance");
            } else {
               DispatchFairnessScheduler.this.addSuccessfulCopies(this.state, targetState, ownedCopies);
            }
         }
      }

      void successAndCover(T target, long ownedCopies, int coverageTicks) {
         this.success((long)target, ownedCopies);
         DispatchFairnessScheduler.TargetState<T> targetState = this.requireLeased(target);
         int boundedCoverage = Math.max(1, coverageTicks);
         long delay = (long)boundedCoverage;
         DispatchFairnessScheduler.this.deactivate(this.state, targetState);
         targetState.cooldownUntil = DispatchFairnessScheduler.saturatingAdd(this.gameTick, delay);
         targetState.cooldownVersion++;
         this.state.cooldowns.add(new DispatchFairnessScheduler.CooldownEntry<>(targetState.cooldownUntil, targetState.cooldownVersion, targetState));
      }

      void cooldown(T target, long retryAfter) {
         this.ensureOpen();
         DispatchFairnessScheduler.TargetState<T> targetState = this.requireLeased(target);
         DispatchFairnessScheduler.this.deactivate(this.state, targetState);
         targetState.cooldownUntil = Math.max(this.gameTick + 1L, retryAfter);
         targetState.cooldownVersion++;
         this.state.cooldowns.add(new DispatchFairnessScheduler.CooldownEntry<>(targetState.cooldownUntil, targetState.cooldownVersion, targetState));
      }

      void remove(T target) {
         this.ensureOpen();
         DispatchFairnessScheduler.TargetState<T> targetState = this.requireLeased(target);
         this.state.targets.remove(target);
         DispatchFairnessScheduler.this.removeState(this.state, targetState);
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;

            for (DispatchFairnessScheduler.TargetState<T> targetState : this.leased) {
               if (targetState.leased) {
                  targetState.leased = false;
                  targetState.leasedAllowance = 0L;
                  DispatchFairnessScheduler.this.offer(this.state, targetState);
               }
            }

            this.state.passOpen = false;
         }
      }

      private DispatchFairnessScheduler.TargetState<T> requireLeased(T target) {
         DispatchFairnessScheduler.TargetState<T> targetState = this.state.targets.get(target);
         if (targetState != null && targetState.leased) {
            return targetState;
         } else {
            throw new IllegalStateException("Target is not leased by this fairness pass");
         }
      }

      private void ensureOpen() {
         if (this.closed) {
            throw new IllegalStateException("Fairness pass is already closed");
         }
      }
   }

   private static final class PatternState<T> {
      private final Map<T, DispatchFairnessScheduler.TargetState<T>> targets = new HashMap<>();
      private final PriorityQueue<DispatchFairnessScheduler.ReadyEntry<T>> ready = new PriorityQueue<>(
         Comparator.<DispatchFairnessScheduler.ReadyEntry<T>>comparingLong(entry -> entry.dispatchCount)
            .thenComparingLong(entry -> entry.lastSuccessfulSequence)
            .thenComparingLong(entry -> entry.tieSequence)
      );
      private final PriorityQueue<DispatchFairnessScheduler.CooldownEntry<T>> cooldowns = new PriorityQueue<>(
         Comparator.comparingLong(entry -> entry.retryAfter)
      );
      private final TreeMap<Long, Integer> activeCountFrequencies = new TreeMap<>();
      private final ArrayDeque<DispatchFairnessScheduler.WindowBucket<T>> history = new ArrayDeque<>();
      private long lastAdvancedTick = Long.MIN_VALUE;
      private long topologyVersion = Long.MIN_VALUE;
      private long activeSum;
      private int activeCount;
      private boolean passOpen;
   }

   private static record ReadyEntry<T>(
      long dispatchCount, long lastSuccessfulSequence, long tieSequence, long queueVersion, DispatchFairnessScheduler.TargetState<T> targetState
   ) {
   }

   private static final class TargetState<T> {
      private final T target;
      private long ownedCopies;
      private long schedulingCredit;
      private long lastSuccessfulSequence;
      private long queueVersion;
      private long cooldownVersion;
      private long cooldownUntil = Long.MIN_VALUE;
      private long leasedAllowance;
      private boolean active;
      private boolean leased;

      private TargetState(T target) {
         this.target = target;
      }

      private long effectiveCount() {
         return DispatchFairnessScheduler.saturatingAdd(this.ownedCopies, this.schedulingCredit);
      }
   }

   private static final class WindowBucket<T> {
      private final long gameTick;
      private final Map<DispatchFairnessScheduler.TargetState<T>, Long> ownedCopies = new HashMap<>();
      private final Map<DispatchFairnessScheduler.TargetState<T>, Long> schedulingCredits = new HashMap<>();

      private WindowBucket(long gameTick) {
         this.gameTick = gameTick;
      }
   }
}
