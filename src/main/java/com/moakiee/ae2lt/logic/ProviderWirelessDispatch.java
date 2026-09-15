package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

final class ProviderWirelessDispatch {
   private static final int COOLDOWN_INIT = 5;
   private static final int COOLDOWN_MIN = 1;
   private static final int COOLDOWN_MAX = 40;
   private static final int COOLDOWN_NEAR_BAND = 4;
   private static final int COOLDOWN_STABLE_SUCCESSES = 2;
   private static final float[] PROBE_LEVELS = new float[]{5.0F, 3.0F, 2.0F, 1.0F, 0.5F, 0.3F, 0.1F};
   private static final int WHEEL_BITS = 6;
   private static final int WHEEL_SIZE = 64;
   private static final int WHEEL_MASK = 63;
   private final WirelessOverflowQueue overflow = new WirelessOverflowQueue();
   private final Predicate<OverloadedPatternProviderBlockEntity.WirelessConnection> blocked = this.overflow::contains;
   private final Map<OverloadedPatternProviderBlockEntity.WirelessConnection, ProviderWirelessDispatch.ConnectionState> states = new HashMap<>();
   private final List<OverloadedPatternProviderBlockEntity.WirelessConnection>[] wheel;
   private final ProviderWirelessDispatch.ReadyQueue<OverloadedPatternProviderBlockEntity.WirelessConnection, ProviderWirelessDispatch.ConnectionState> singleReady;
   private final ProviderWirelessDispatch.ReadyQueue<OverloadedPatternProviderBlockEntity.WirelessConnection, ProviderWirelessDispatch.ConnectionState> evenReady;
   private final DispatchFairnessScheduler<OverloadedPatternProviderBlockEntity.WirelessConnection, IPatternDetails> fairness = DispatchFairnessScheduler.forCanonicalPatterns();
   private final WirelessBatchCadence<OverloadedPatternProviderBlockEntity.WirelessConnection> batchCadence = new WirelessBatchCadence<>();
   private final Map<OverloadedPatternProviderBlockEntity.WirelessConnection, Map<IPatternDetails, ProviderWirelessDispatch.Penalty>> penalties = new HashMap<>();
   private final DueTaskQueue<TargetPatternKey<OverloadedPatternProviderBlockEntity.WirelessConnection>> penaltyExpirations = new DueTaskQueue<>();
   private List<OverloadedPatternProviderBlockEntity.WirelessConnection> validReference = List.of();
   private boolean structuresDirty = true;
   private long lastWheelTick = -1L;
   private long topologyVersion;

   ProviderWirelessDispatch() {
      this.wheel = new List[64];

      for (int i = 0; i < this.wheel.length; i++) {
         this.wheel[i] = new ArrayList<>();
      }

      this.singleReady = new ProviderWirelessDispatch.ReadyQueue<>(this.states::get);
      this.evenReady = new ProviderWirelessDispatch.ReadyQueue<>(this.states::get);
   }

   WirelessOverflowQueue overflow() {
      return this.overflow;
   }

   ProviderWirelessDispatch.ConnectionState state(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      return this.states.computeIfAbsent(connection, ignored -> new ProviderWirelessDispatch.ConnectionState());
   }

   @Nullable
   ProviderWirelessDispatch.ConnectionState existingState(OverloadedPatternProviderBlockEntity.WirelessConnection connection) {
      return this.states.get(connection);
   }

   ProviderWirelessDispatch.ReadyQueue<OverloadedPatternProviderBlockEntity.WirelessConnection, ProviderWirelessDispatch.ConnectionState> readyQueue(
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode
   ) {
      return mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.SINGLE_TARGET ? this.singleReady : this.evenReady;
   }

   DispatchFairnessScheduler<OverloadedPatternProviderBlockEntity.WirelessConnection, IPatternDetails>.Pass beginFairPass(
      IPatternDetails pattern, long gameTick
   ) {
      return this.fairness.beginPass(pattern, this.validReference, this.topologyVersion, gameTick);
   }

   long retryAfter(OverloadedPatternProviderBlockEntity.WirelessConnection target, IPatternDetails pattern, long gameTick) {
      this.purgeExpiredPenalties(gameTick);
      Map<IPatternDetails, ProviderWirelessDispatch.Penalty> byPattern = this.penalties.get(target);
      if (byPattern == null) {
         return Long.MIN_VALUE;
      } else {
         ProviderWirelessDispatch.Penalty penalty = byPattern.get(pattern);
         return penalty == null ? Long.MIN_VALUE : penalty.retryAfter;
      }
   }

   long recordRejection(OverloadedPatternProviderBlockEntity.WirelessConnection target, IPatternDetails pattern, long gameTick, boolean fast) {
      this.purgeExpiredPenalties(gameTick);
      int initial = fast ? 2 : 5;
      int maximum = fast ? 10 : 40;
      Map<IPatternDetails, ProviderWirelessDispatch.Penalty> byPattern = this.penalties.computeIfAbsent(target, ignored -> CanonicalPatternMaps.create());
      ProviderWirelessDispatch.Penalty previous = byPattern.get(pattern);
      int cooldown = previous == null ? initial : Math.min(maximum, previous.cooldown * 2);
      long retryAfter = gameTick + (long)cooldown;
      byPattern.put(pattern, new ProviderWirelessDispatch.Penalty(retryAfter, cooldown));
      this.penaltyExpirations.schedule(new TargetPatternKey<>(target, pattern), retryAfter);
      return retryAfter;
   }

   void recordSuccess(OverloadedPatternProviderBlockEntity.WirelessConnection target, IPatternDetails pattern) {
      Map<IPatternDetails, ProviderWirelessDispatch.Penalty> byPattern = this.penalties.get(target);
      if (byPattern != null) {
         byPattern.remove(pattern);
         this.penaltyExpirations.remove(new TargetPatternKey<>(target, pattern));
         if (byPattern.isEmpty()) {
            this.penalties.remove(target);
         }
      }
   }

   void pauseTarget(OverloadedPatternProviderBlockEntity.WirelessConnection target) {
      this.fairness.pauseTarget(target);
   }

   void excludeUntil(IPatternDetails pattern, OverloadedPatternProviderBlockEntity.WirelessConnection target, long retryAfter, long gameTick) {
      this.fairness.excludeUntil(pattern, target, retryAfter, gameTick);
   }

   void resumeTarget(OverloadedPatternProviderBlockEntity.WirelessConnection target, long gameTick) {
      this.fairness.resumeTarget(target, gameTick);
   }

   void removeTarget(OverloadedPatternProviderBlockEntity.WirelessConnection target) {
      this.removePenalties(target);
      this.batchCadence.removeTarget(target);
      this.fairness.removeTarget(target);
      this.evenReady.remove(target);
      this.singleReady.remove(target);
      if (this.states.remove(target) != null) {
         this.structuresDirty = true;
      }
   }

   void patternsChanged() {
      this.penalties.clear();
      this.penaltyExpirations.clear();
      this.batchCadence.clear();
      this.fairness.clear();

      for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : this.states.keySet()) {
         connection.clearBatchHistory();
      }
   }

   void prepare(
      List<OverloadedPatternProviderBlockEntity.WirelessConnection> valid,
      long gameTick,
      boolean fastMode,
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode
   ) {
      if (this.structuresDirty || valid != this.validReference) {
         if (!this.structuresDirty && valid.equals(this.validReference)) {
            this.validReference = valid;
         } else {
            this.rebuild(valid, gameTick, mode);
         }
      }

      this.advance(gameTick, fastMode, mode);
   }

   long dispatchBatch(
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode,
      IPatternDetails pattern,
      long maxCopies,
      long gameTick,
      boolean fastMode,
      ProviderWirelessDispatch.BatchAttempt attempt,
      Predicate<OverloadedPatternProviderBlockEntity.WirelessConnection> alive,
      Consumer<OverloadedPatternProviderBlockEntity.WirelessConnection> targetRemoved
   ) {
      return mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.SINGLE_TARGET
         ? this.dispatchSingleTargetBatch(maxCopies, gameTick, fastMode, attempt, alive, targetRemoved)
         : this.dispatchFairBatch(pattern, maxCopies, gameTick, fastMode, attempt, alive, targetRemoved);
   }

   boolean dispatchSingleCopy(
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode,
      IPatternDetails pattern,
      long gameTick,
      boolean fastMode,
      int targetAttempts,
      ProviderWirelessDispatch.SingleAttempt attempt,
      Predicate<OverloadedPatternProviderBlockEntity.WirelessConnection> alive,
      Consumer<OverloadedPatternProviderBlockEntity.WirelessConnection> targetRemoved
   ) {
      ProviderWirelessDispatch.ReadyQueue<OverloadedPatternProviderBlockEntity.WirelessConnection, ProviderWirelessDispatch.ConnectionState> readyQueue = this.readyQueue(
         mode
      );
      int scanBudget = Math.min(targetAttempts, readyQueue.size());

      while (scanBudget-- > 0 && !readyQueue.isEmpty()) {
         OverloadedPatternProviderBlockEntity.WirelessConnection connection = readyQueue.peek();
         if (this.blocked.test(connection)) {
            readyQueue.removeHead();
         } else if (mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION
            && this.retryAfter(connection, pattern, gameTick) > gameTick) {
            readyQueue.rotateHeadToTail();
         } else {
            ProviderWirelessDispatch.ConnectionState state = this.existingState(connection);
            if (state != null && state.ready) {
               boolean probing = isProbing(state, gameTick);
               WirelessPushOutcome outcome = attempt.push(connection);
               if (outcome.consumesTargetAttempt()) {
                  state.probeArmed = false;
               }

               switch (outcome) {
                  case SUCCESS:
                     if (mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION) {
                        this.recordSuccess(connection, pattern);
                     }

                     recordPushSuccess(state, probing, gameTick);
                     if (this.blocked.test(connection)) {
                        if (mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION) {
                           this.pauseTarget(connection);
                        }

                        readyQueue.removeHead();
                     } else if (mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION) {
                        readyQueue.rotateHeadToTail();
                     }

                     return true;
                  case HARD_FAIL:
                     readyQueue.removeHead();
                     if (!alive.test(connection)) {
                        this.removeTarget(connection);
                        targetRemoved.accept(connection);
                     } else if (mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.EVEN_DISTRIBUTION) {
                        readyQueue.offer(connection);
                     }
                     break;
                  case SOFT_FAIL:
                     if (mode == OverloadedPatternProviderBlockEntity.WirelessDispatchMode.SINGLE_TARGET) {
                        readyQueue.removeHead();
                        recordPushFailure(state, probing, gameTick);
                        this.scheduleAfterFailure(connection, state, fastMode, mode);
                     } else {
                        long due = this.recordRejection(connection, pattern, gameTick, fastMode);
                        this.excludeUntil(pattern, connection, due, gameTick);
                        readyQueue.rotateHeadToTail();
                     }
                     break;
                  case GLOBAL_ABORT:
                     return false;
               }
            } else {
               readyQueue.removeHead();
            }
         }
      }

      return false;
   }

   private long dispatchSingleTargetBatch(
      long maxCopies,
      long gameTick,
      boolean fastMode,
      ProviderWirelessDispatch.BatchAttempt attempt,
      Predicate<OverloadedPatternProviderBlockEntity.WirelessConnection> alive,
      Consumer<OverloadedPatternProviderBlockEntity.WirelessConnection> targetRemoved
   ) {
      ProviderWirelessDispatch.ReadyQueue<OverloadedPatternProviderBlockEntity.WirelessConnection, ProviderWirelessDispatch.ConnectionState> readyQueue = this.singleReady;
      long remaining = maxCopies;
      int attemptBudget = Math.min(1, readyQueue.size());

      for (int attempts = 0; attempts < attemptBudget && remaining > 0L && !readyQueue.isEmpty(); attempts++) {
         OverloadedPatternProviderBlockEntity.WirelessConnection connection = readyQueue.peek();
         if (this.blocked.test(connection)) {
            readyQueue.removeHead();
         } else {
            ProviderWirelessDispatch.ConnectionState state = this.existingState(connection);
            if (state != null && state.ready) {
               boolean probing = isProbing(state, gameTick);
               ProviderWirelessDispatch.BatchAttemptResult result = attempt.push(connection, remaining, false, false);
               if (result.outcome.consumesTargetAttempt()) {
                  state.probeArmed = false;
               }

               if (result.ownedCopies > 0L) {
                  remaining -= result.ownedCopies;
                  recordPushSuccess(state, probing, gameTick);
                  if (this.blocked.test(connection)) {
                     readyQueue.removeHead();
                  }

                  return remaining;
               }

               switch (result.outcome) {
                  case SUCCESS:
                  default:
                     break;
                  case HARD_FAIL:
                     readyQueue.removeHead();
                     if (!alive.test(connection)) {
                        this.removeTarget(connection);
                        targetRemoved.accept(connection);
                     }
                     break;
                  case SOFT_FAIL:
                     readyQueue.removeHead();
                     recordPushFailure(state, probing, gameTick);
                     this.scheduleAfterFailure(connection, state, fastMode, OverloadedPatternProviderBlockEntity.WirelessDispatchMode.SINGLE_TARGET);
                     break;
                  case GLOBAL_ABORT:
                     return remaining;
               }
            } else {
               readyQueue.removeHead();
            }
         }
      }

      return remaining;
   }

   private long dispatchFairBatch(
      IPatternDetails pattern,
      long maxCopies,
      long gameTick,
      boolean fastMode,
      ProviderWirelessDispatch.BatchAttempt attempt,
      Predicate<OverloadedPatternProviderBlockEntity.WirelessConnection> alive,
      Consumer<OverloadedPatternProviderBlockEntity.WirelessConnection> targetRemoved
   ) {
      long remaining = maxCopies;

      try (DispatchFairnessScheduler<OverloadedPatternProviderBlockEntity.WirelessConnection, IPatternDetails>.Pass pass = this.beginFairPass(pattern, gameTick)) {
         int attemptBudget = pass.activeTargetsAtStart();

         for (int attempts = 0; attempts < attemptBudget && remaining > 0L; attempts++) {
            OverloadedPatternProviderBlockEntity.WirelessConnection connection = (OverloadedPatternProviderBlockEntity.WirelessConnection)pass.poll();
            if (connection == null) {
               break;
            }

            if (this.blocked.test(connection)) {
               this.pauseTarget(connection);
            } else {
               long retryAfter = this.retryAfter(connection, pattern, gameTick);
               if (retryAfter > gameTick) {
                  pass.cooldown((long)connection, retryAfter);
               } else {
                  ProviderWirelessDispatch.ConnectionState state = this.existingState(connection);
                  if (state == null) {
                     pass.remove(connection);
                  } else if (!state.ready) {
                     pass.cooldown((long)connection, Math.max(gameTick + 1L, state.cooldownUntil));
                  } else {
                     boolean probing = isProbing(state, gameTick);
                     long share = Math.min(remaining, pass.allowance(connection));
                     int targetsLeft = attemptBudget - attempts;
                     long equalShareLimit = targetsLeft <= 0 ? 0L : remaining / (long)targetsLeft;
                     long rampAllowance = connection.batchStepRampAllowance(pattern, equalShareLimit, gameTick);
                     if (rampAllowance > share && rampAllowance <= equalShareLimit) {
                        share = Math.min(remaining, pass.raiseAllowance((long)connection, rampAllowance));
                     }

                     boolean fillFallback = this.batchCadence.isFillFallback(connection, pattern);
                     if (fillFallback) {
                        int candidate = connection.batchStepCandidate(pattern, remaining, gameTick);
                        share = Math.min(remaining, pass.raiseAllowance((long)connection, (long)candidate));
                     }

                     if (share > 0L) {
                        boolean exploratoryAttempt = this.batchCadence.isExploratoryAttempt(connection, pattern);
                        boolean preserveBatchHistory = this.batchCadence.shouldPreserveBatchHistory(connection, pattern, gameTick);
                        ProviderWirelessDispatch.BatchAttemptResult result = attempt.push(connection, share, exploratoryAttempt, preserveBatchHistory);
                        if (result.outcome.consumesTargetAttempt()) {
                           state.probeArmed = false;
                        }

                        if (result.ownedCopies > 0L) {
                           int coverageTicks = this.batchCadence
                              .recordSuccess(
                                 connection,
                                 pattern,
                                 gameTick,
                                 result.ownedCopies,
                                 result.acceptedFullChunk,
                                 result.requestLimited,
                                 exploratoryAttempt,
                                 result.baselineStatus
                              );
                           pass.successAndCover((long)connection, result.ownedCopies, coverageTicks);
                           this.recordSuccess(connection, pattern);
                           remaining -= result.ownedCopies;
                           recordPushSuccess(state, probing, gameTick);
                           if (this.blocked.test(connection)) {
                              this.pauseTarget(connection);
                              this.evenReady.remove(connection);
                           }

                           if (result.outcome == WirelessPushOutcome.GLOBAL_ABORT) {
                              return remaining;
                           }
                        } else {
                           switch (result.outcome) {
                              case SUCCESS:
                              default:
                                 break;
                              case HARD_FAIL: {
                                 int retryDelay = this.recordBatchFailure(connection, pattern, gameTick, result, exploratoryAttempt);
                                 if (!alive.test(connection)) {
                                    pass.remove(connection);
                                    this.removeTarget(connection);
                                    targetRemoved.accept(connection);
                                 } else {
                                    long duex = this.batchRetryAfter(connection, pattern, gameTick, fastMode, result, retryDelay);
                                    pass.cooldown((long)connection, duex);
                                 }
                                 break;
                              }
                              case SOFT_FAIL: {
                                 int retryDelay = this.recordBatchFailure(connection, pattern, gameTick, result, exploratoryAttempt);
                                 long due = this.batchRetryAfter(connection, pattern, gameTick, fastMode, result, retryDelay);
                                 pass.cooldown((long)connection, due);
                                 break;
                              }
                              case GLOBAL_ABORT:
                                 return remaining;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return remaining;
   }

   private int recordBatchFailure(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      IPatternDetails pattern,
      long gameTick,
      ProviderWirelessDispatch.BatchAttemptResult result,
      boolean exploratoryAttempt
   ) {
      return result.attemptedCopies > 0 ? this.batchCadence.recordFailure(connection, pattern, gameTick, result.attemptedCopies, exploratoryAttempt) : 0;
   }

   private long batchRetryAfter(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      IPatternDetails pattern,
      long gameTick,
      boolean fastMode,
      ProviderWirelessDispatch.BatchAttemptResult result,
      int retryDelay
   ) {
      return result.attemptedCopies > 0 ? gameTick + (long)Math.max(1, retryDelay) : this.recordRejection(connection, pattern, gameTick, fastMode);
   }

   private static boolean isProbing(ProviderWirelessDispatch.ConnectionState state, long gameTick) {
      return state.probeArmed && state.cooldownUntil >= 0L && gameTick < state.cooldownUntil;
   }

   private static void recordPushSuccess(ProviderWirelessDispatch.ConnectionState state, boolean probing, long gameTick) {
      if (probing) {
         state.onProbeSuccess();
      } else {
         state.onPushSuccess(gameTick);
      }
   }

   private static void recordPushFailure(ProviderWirelessDispatch.ConnectionState state, boolean probing, long gameTick) {
      if (probing) {
         state.onProbeFail();
      } else {
         state.onPushFail(gameTick);
      }
   }

   void scheduleAfterFailure(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      ProviderWirelessDispatch.ConnectionState state,
      boolean fastMode,
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode
   ) {
      if (!this.blocked.test(connection)) {
         state.ready = false;
         state.probeArmed = false;
         if (state.cooldownUntil < 0L) {
            state.ready = true;
            this.enqueue(connection, state, mode);
         } else {
            long targetTick = state.cooldownUntil;
            if (fastMode && !state.probedThisCycle) {
               float level = PROBE_LEVELS[state.probeLevelIndex];
               if (level >= 1.0F) {
                  long probeTick = state.cooldownUntil - (long)((int)level);
                  if (probeTick > this.lastWheelTick) {
                     targetTick = probeTick;
                  }
               } else {
                  int interval = Math.round(1.0F / level);
                  if (state.probeSkipCounter >= interval) {
                     long probeTick = state.cooldownUntil - 1L;
                     if (probeTick > this.lastWheelTick) {
                        targetTick = probeTick;
                     }
                  }
               }
            }

            targetTick = Math.max(targetTick, this.lastWheelTick + 1L);
            this.wheel[(int)(targetTick & 63L)].add(connection);
         }
      }
   }

   void markDirty() {
      this.structuresDirty = true;
   }

   void retainStates(Iterable<OverloadedPatternProviderBlockEntity.WirelessConnection> valid) {
      HashSet<OverloadedPatternProviderBlockEntity.WirelessConnection> retained = new HashSet<>();

      for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : valid) {
         retained.add(connection);
      }

      ArrayList<OverloadedPatternProviderBlockEntity.WirelessConnection> removed = new ArrayList<>();

      for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : this.states.keySet()) {
         if (!retained.contains(connection)) {
            removed.add(connection);
         }
      }

      for (OverloadedPatternProviderBlockEntity.WirelessConnection connectionx : removed) {
         this.removeTarget(connectionx);
      }
   }

   void clear() {
      this.validReference = List.of();
      this.structuresDirty = true;
      this.lastWheelTick = -1L;

      for (List<OverloadedPatternProviderBlockEntity.WirelessConnection> slot : this.wheel) {
         slot.clear();
      }

      this.clearReadyQueues();
      this.patternsChanged();
      this.states.clear();
   }

   private void removePenalties(OverloadedPatternProviderBlockEntity.WirelessConnection target) {
      Map<IPatternDetails, ProviderWirelessDispatch.Penalty> byPattern = this.penalties.remove(target);
      if (byPattern != null) {
         for (IPatternDetails pattern : byPattern.keySet()) {
            this.penaltyExpirations.remove(new TargetPatternKey<>(target, pattern));
         }
      }
   }

   private void purgeExpiredPenalties(long gameTick) {
      TargetPatternKey<OverloadedPatternProviderBlockEntity.WirelessConnection> expired;
      while ((expired = this.penaltyExpirations.pollDue(gameTick)) != null) {
         Map<IPatternDetails, ProviderWirelessDispatch.Penalty> byPattern = this.penalties.get(expired.target());
         if (byPattern != null) {
            ProviderWirelessDispatch.Penalty penalty = byPattern.get(expired.pattern());
            if (penalty != null && penalty.retryAfter <= gameTick) {
               byPattern.remove(expired.pattern());
               if (byPattern.isEmpty()) {
                  this.penalties.remove(expired.target());
               }
            }
         }
      }
   }

   private void rebuild(
      List<OverloadedPatternProviderBlockEntity.WirelessConnection> valid, long gameTick, OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode
   ) {
      for (List<OverloadedPatternProviderBlockEntity.WirelessConnection> slot : this.wheel) {
         slot.clear();
      }

      this.clearReadyQueues();

      for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : valid) {
         if (!this.blocked.test(connection)) {
            ProviderWirelessDispatch.ConnectionState state = this.state(connection);
            if (state.cooldownUntil >= 0L && gameTick < state.cooldownUntil) {
               state.ready = false;
               state.probeArmed = false;
               this.wheel[(int)(state.cooldownUntil & 63L)].add(connection);
            } else {
               state.ready = true;
               state.probeArmed = false;
               this.enqueue(connection, state, mode);
            }
         }
      }

      this.validReference = valid;
      this.topologyVersion++;
      this.structuresDirty = false;
      this.lastWheelTick = gameTick;
   }

   private void advance(long gameTick, boolean fastMode, OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode) {
      if (this.lastWheelTick < 0L) {
         this.lastWheelTick = gameTick - 1L;
      }

      long delta = gameTick - this.lastWheelTick;
      if (delta > 0L) {
         int steps = (int)Math.min(delta, 64L);

         for (int i = 0; i < steps; i++) {
            long tick = this.lastWheelTick + 1L + (long)i;
            List<OverloadedPatternProviderBlockEntity.WirelessConnection> slot = this.wheel[(int)(tick & 63L)];
            Iterator<OverloadedPatternProviderBlockEntity.WirelessConnection> iterator = slot.iterator();

            while (iterator.hasNext()) {
               OverloadedPatternProviderBlockEntity.WirelessConnection connection = iterator.next();
               if (this.blocked.test(connection)) {
                  iterator.remove();
               } else {
                  ProviderWirelessDispatch.ConnectionState state = this.states.get(connection);
                  if (state == null) {
                     iterator.remove();
                  } else {
                     boolean fire = state.cooldownUntil < 0L || tick >= state.cooldownUntil;
                     boolean probe = false;
                     if (!fire && fastMode && !state.probedThisCycle && state.isInProbeWindow(tick)) {
                        fire = true;
                        probe = true;
                     }

                     if (fire) {
                        iterator.remove();
                        state.ready = true;
                        state.probeArmed = probe;
                        this.enqueue(connection, state, mode);
                     }
                  }
               }
            }
         }

         this.lastWheelTick = gameTick;
      }
   }

   private void enqueue(
      OverloadedPatternProviderBlockEntity.WirelessConnection connection,
      ProviderWirelessDispatch.ConnectionState state,
      OverloadedPatternProviderBlockEntity.WirelessDispatchMode mode
   ) {
      if (state.ready && !this.blocked.test(connection)) {
         this.readyQueue(mode).offer(connection);
      }
   }

   private void clearReadyQueues() {
      this.singleReady.clear();
      this.evenReady.clear();

      for (ProviderWirelessDispatch.ConnectionState state : this.states.values()) {
         state.setQueued(false);
      }
   }

   @FunctionalInterface
   interface BatchAttempt {
      ProviderWirelessDispatch.BatchAttemptResult push(OverloadedPatternProviderBlockEntity.WirelessConnection var1, long var2, boolean var4, boolean var5);
   }

   static record BatchAttemptResult(
      long ownedCopies,
      int attemptedCopies,
      boolean acceptedFullChunk,
      boolean requestLimited,
      ProviderTarget.BaselineStatus baselineStatus,
      WirelessPushOutcome outcome
   ) {
   }

   static final class ConnectionState implements ProviderWirelessDispatch.ReadyQueue.State {
      long cooldownUntil = -1L;
      int cooldown = 5;
      int searchLow = 1;
      int searchHigh = 40;
      int stableSuccesses;
      int failureStreak;
      int probeLevelIndex;
      int probeSkipCounter;
      boolean probedThisCycle;
      boolean ready = true;
      boolean probeArmed;
      boolean queued;

      boolean isInProbeWindow(long gameTick) {
         if (this.cooldownUntil >= 0L && gameTick < this.cooldownUntil && !this.probedThisCycle) {
            float level = ProviderWirelessDispatch.PROBE_LEVELS[this.probeLevelIndex];
            if (level >= 1.0F) {
               return gameTick == this.cooldownUntil - (long)((int)level);
            } else {
               int interval = Math.round(1.0F / level);
               return this.probeSkipCounter >= interval && gameTick == this.cooldownUntil - 1L;
            }
         } else {
            return false;
         }
      }

      void onProbeSuccess() {
         this.probeLevelIndex = 0;
         this.probeSkipCounter = 0;
         this.probedThisCycle = true;
         this.cooldownUntil = -1L;
      }

      void onProbeFail() {
         this.probeLevelIndex = Math.min(this.probeLevelIndex + 1, ProviderWirelessDispatch.PROBE_LEVELS.length - 1);
         this.probeSkipCounter = 0;
         this.probedThisCycle = true;
      }

      void onPushSuccess(long gameTick) {
         if (this.cooldownUntil >= 0L) {
            this.failureStreak = 0;
            if (this.nearBand()) {
               this.stableSuccesses++;
               if (this.stableSuccesses >= 2) {
                  this.cooldown = Math.max(1, this.cooldown - 1);
                  this.stableSuccesses = 0;
               }
            } else {
               this.stableSuccesses = 0;
               this.searchHigh = this.cooldown;
               this.cooldown = Math.max(this.searchLow, (this.searchLow + this.searchHigh) / 2);
               this.cooldown = Math.max(1, Math.min(40, this.cooldown));
            }

            this.cooldownUntil = -1L;
         }
      }

      void onPushFail(long gameTick) {
         if (this.cooldownUntil < 0L) {
            this.cooldownUntil = gameTick + (long)this.cooldown;
            this.stableSuccesses = 0;
            this.probedThisCycle = false;
            this.probeSkipCounter++;
         } else {
            this.stableSuccesses = 0;
            if (this.nearBand()) {
               this.failureStreak++;
               int step = Math.min(2, 1 + (this.failureStreak - 1) / 4);
               this.cooldown = Math.min(40, this.cooldown + step);
            } else {
               this.failureStreak = 0;
               this.searchLow = this.cooldown + 1;
               if (this.searchLow > this.searchHigh) {
                  this.searchLow = 40;
                  this.searchHigh = 40;
                  this.cooldown = 40;
               } else {
                  this.cooldown = (this.searchLow + this.searchHigh) / 2;
                  this.cooldown = Math.max(1, Math.min(40, this.cooldown));
               }
            }

            this.cooldownUntil = gameTick + (long)this.cooldown;
            this.probedThisCycle = false;
            this.probeSkipCounter++;
         }
      }

      private boolean nearBand() {
         return this.searchHigh - this.searchLow <= 4;
      }

      @Override
      public boolean isQueued() {
         return this.queued;
      }

      @Override
      public void setQueued(boolean queued) {
         this.queued = queued;
      }
   }

   private static record Penalty(long retryAfter, int cooldown) {
   }

   static final class ReadyQueue<T, S extends ProviderWirelessDispatch.ReadyQueue.State> {
      private final ArrayDeque<T> queue = new ArrayDeque<>();
      private final Function<T, S> stateLookup;

      ReadyQueue(Function<T, S> stateLookup) {
         this.stateLookup = stateLookup;
      }

      boolean offer(T target) {
         S state = this.stateLookup.apply(target);
         if (state != null && !state.isQueued()) {
            state.setQueued(true);
            this.queue.addLast(target);
            return true;
         } else {
            return false;
         }
      }

      T peek() {
         return this.queue.peekFirst();
      }

      T removeHead() {
         T target = this.queue.pollFirst();
         if (target != null) {
            S state = this.stateLookup.apply(target);
            if (state != null) {
               state.setQueued(false);
            }
         }

         return target;
      }

      T rotateHeadToTail() {
         T target = this.queue.pollFirst();
         if (target != null) {
            this.queue.addLast(target);
         }

         return target;
      }

      boolean remove(T target) {
         if (!this.queue.remove(target)) {
            return false;
         } else {
            S state = this.stateLookup.apply(target);
            if (state != null) {
               state.setQueued(false);
            }

            return true;
         }
      }

      boolean isEmpty() {
         return this.queue.isEmpty();
      }

      int size() {
         return this.queue.size();
      }

      void clear() {
         this.queue.clear();
      }

      void clear(Collection<S> knownStates) {
         this.queue.clear();

         for (S state : knownStates) {
            state.setQueued(false);
         }
      }

      interface State {
         boolean isQueued();

         void setQueued(boolean var1);
      }
   }

   @FunctionalInterface
   interface SingleAttempt {
      WirelessPushOutcome push(OverloadedPatternProviderBlockEntity.WirelessConnection var1);
   }
}
