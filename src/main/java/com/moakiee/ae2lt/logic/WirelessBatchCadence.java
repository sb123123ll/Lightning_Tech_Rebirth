package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import java.util.HashMap;
import java.util.Map;

final class WirelessBatchCadence<T> {
   static final int MAX_COVERAGE_TICKS = 100;
   private static final int HISTORY_TTL = 100;
   private static final int FILL_FALLBACK_BASELINE_TICKS = 100;
   private static final int FILL_FALLBACK_RETRY_TICKS = 25;
   private static final int FILL_FALLBACK_RECOVERY_SUCCESSES = 2;
   private static final int FILL_FALLBACK_MIN_REJECTIONS = 64;
   private static final int FILL_FALLBACK_MIN_BATCH_COPIES = 64;
   private static final int MAX_FAILURE_PRESSURE = 8;
   private static final int HIGH_FAILURE_PRESSURE = 7;
   private static final int MODERATE_FAILURE_PRESSURE = 2;
   private static final int CONFIRMED_COVERAGE_PRESSURE_DECAY = 3;
   private final Map<T, Map<IPatternDetails, WirelessBatchCadence.State>> states = new HashMap<>();

   int recordSuccess(T target, IPatternDetails pattern, long gameTick, long ownedCopies, boolean acceptedFullChunk, boolean requestLimited) {
      return this.recordSuccess(target, pattern, gameTick, ownedCopies, acceptedFullChunk, requestLimited, false, ProviderTarget.BaselineStatus.NONE);
   }

   int recordSuccess(
      T target, IPatternDetails pattern, long gameTick, long ownedCopies, boolean acceptedFullChunk, boolean requestLimited, boolean exploratoryAttempt
   ) {
      return this.recordSuccess(
         target, pattern, gameTick, ownedCopies, acceptedFullChunk, requestLimited, exploratoryAttempt, ProviderTarget.BaselineStatus.NONE
      );
   }

   int recordSuccess(
      T target,
      IPatternDetails pattern,
      long gameTick,
      long ownedCopies,
      boolean acceptedFullChunk,
      boolean requestLimited,
      boolean exploratoryAttempt,
      ProviderTarget.BaselineStatus baselineStatus
   ) {
      if (ownedCopies <= 0L) {
         throw new IllegalArgumentException("Successful cadence samples must own at least one copy");
      } else {
         WirelessBatchCadence.State state = this.state(target, pattern);
         state.expireIfIdle(gameTick);
         state.lastActivityTick = gameTick;
         boolean capacitySuccess = acceptedFullChunk && !requestLimited;
         if (state.fillFallback) {
            if (!capacitySuccess) {
               state.fillFallbackSuccesses = 0;
               return 25;
            } else {
               state.lastSuccessTick = gameTick;
               state.lastOwnedCopies = ownedCopies;
               state.lastCapacitySuccessTick = gameTick;
               state.fillFallbackRejections = 0;
               state.firstProvenRejectionTick = Long.MIN_VALUE;
               if (++state.fillFallbackSuccesses < 2) {
                  return 25;
               } else {
                  state.fillFallback = false;
                  state.fillFallbackSuccesses = 0;
                  state.learnedCoverage = 1;
                  state.nextAttemptExploratory = false;
                  state.exploratorySuccesses = 0;
                  state.exploratoryProbeRejected = false;
                  state.growthProbeRejected = false;
                  state.provenChunkRejections = 0;
                  state.failurePressure = 0;
                  return 1;
               }
            }
         } else if (!capacitySuccess && shouldEnterFillFallback(state, gameTick)) {
            return enterFillFallback(state);
         } else if (baselineStatus != ProviderTarget.BaselineStatus.NONE) {
            return recordBaselineResult(state, gameTick, ownedCopies, baselineStatus);
         } else {
            boolean confirmedFasterCoverage = false;
            int coverage = 1;
            if (acceptedFullChunk && !requestLimited && state.lastSuccessTick != Long.MIN_VALUE && ownedCopies == state.lastOwnedCopies) {
               long elapsed = Math.max(1L, gameTick - state.lastSuccessTick);
               if (exploratoryAttempt) {
                  if (++state.exploratorySuccesses >= 2) {
                     state.learnedCoverage = Math.max(1, state.learnedCoverage - 1);
                     state.exploratorySuccesses = 0;
                     confirmedFasterCoverage = true;
                  }

                  elapsed = (long)state.learnedCoverage;
               } else if (state.exploratoryProbeRejected) {
                  state.exploratorySuccesses = 0;
                  elapsed = (long)state.learnedCoverage;
               } else if (state.growthProbeRejected && state.provenChunkRejections == 0) {
                  elapsed = 1L;
               } else if (state.provenChunkRejections > 0) {
                  state.learnedCoverage = (int)Math.max(1L, Math.min(100L, elapsed));
               } else {
                  elapsed = (long)state.learnedCoverage;
               }

               coverage = (int)Math.min(100L, elapsed);
            } else if (acceptedFullChunk && !requestLimited && state.provenChunkRejections > 0 && state.lastOwnedCopies > 0L) {
               long scaledCoverage = ownedCopies * (long)state.learnedCoverage;
               coverage = (int)Math.max(1L, Math.min(100L, ceilingDivide(scaledCoverage, state.lastOwnedCopies)));
            }

            int pressureFloor = pressureFloor(state.failurePressure);
            coverage = Math.max(coverage, pressureFloor);
            state.nextAttemptExploratory = false;
            if (acceptedFullChunk
               && !requestLimited
               && !state.fillFallback
               && state.lastSuccessTick != Long.MIN_VALUE
               && ownedCopies == state.lastOwnedCopies
               && state.learnedCoverage > 1) {
               int exploratoryCoverage = Math.max(pressureFloor, state.learnedCoverage - 1);
               if (exploratoryCoverage < state.learnedCoverage) {
                  coverage = exploratoryCoverage;
                  state.nextAttemptExploratory = true;
               }
            }

            state.failurePressure = Math.max(0, state.failurePressure - (confirmedFasterCoverage ? 3 : 1));
            state.lastSuccessTick = gameTick;
            state.lastOwnedCopies = ownedCopies;
            state.growthProbeRejected = false;
            state.provenChunkRejections = 0;
            if (capacitySuccess) {
               state.lastCapacitySuccessTick = gameTick;
               state.fillFallbackRejections = 0;
               state.firstProvenRejectionTick = Long.MIN_VALUE;
            }

            state.exploratoryProbeRejected = false;
            return coverage;
         }
      }
   }

   int recordFailure(T target, IPatternDetails pattern, long gameTick, int attemptedCopies) {
      return this.recordFailure(target, pattern, gameTick, attemptedCopies, false);
   }

   int recordFailure(T target, IPatternDetails pattern, long gameTick, int attemptedCopies, boolean exploratoryAttempt) {
      WirelessBatchCadence.State state = this.state(target, pattern);
      state.expireIfIdle(gameTick);
      state.lastActivityTick = gameTick;
      state.nextAttemptExploratory = false;
      if (state.fillFallback) {
         state.exploratorySuccesses = 0;
         state.exploratoryProbeRejected = false;
         state.fillFallbackSuccesses = 0;
         return 25;
      } else if (exploratoryAttempt) {
         state.exploratorySuccesses = 0;
         state.exploratoryProbeRejected = true;
         return 1;
      } else {
         state.exploratoryProbeRejected = false;
         state.exploratorySuccesses = 0;
         if (state.lastOwnedCopies > 0L && (long)attemptedCopies > state.lastOwnedCopies) {
            state.growthProbeRejected = true;
            state.failurePressure = Math.min(8, state.failurePressure + 1);
            return 1;
         } else {
            state.provenChunkRejections++;
            state.failurePressure = Math.min(8, state.failurePressure + 2);
            if (state.firstProvenRejectionTick == Long.MIN_VALUE) {
               state.firstProvenRejectionTick = gameTick;
            }

            if (state.fillFallbackRejections < Integer.MAX_VALUE) {
               state.fillFallbackRejections++;
            }

            return shouldEnterFillFallback(state, gameTick) ? enterFillFallback(state) : 1;
         }
      }
   }

   boolean isExploratoryAttempt(T target, IPatternDetails pattern) {
      Map<IPatternDetails, WirelessBatchCadence.State> byPattern = this.states.get(target);
      if (byPattern == null) {
         return false;
      } else {
         WirelessBatchCadence.State state = byPattern.get(pattern);
         return state != null && state.nextAttemptExploratory;
      }
   }

   boolean isFillFallback(T target, IPatternDetails pattern) {
      Map<IPatternDetails, WirelessBatchCadence.State> byPattern = this.states.get(target);
      if (byPattern == null) {
         return false;
      } else {
         WirelessBatchCadence.State state = byPattern.get(pattern);
         return state != null && state.fillFallback;
      }
   }

   boolean shouldPreserveBatchHistory(T target, IPatternDetails pattern, long gameTick) {
      Map<IPatternDetails, WirelessBatchCadence.State> byPattern = this.states.get(target);
      if (byPattern == null) {
         return false;
      } else {
         WirelessBatchCadence.State state = byPattern.get(pattern);
         if (state == null) {
            return false;
         } else {
            state.expireIfIdle(gameTick);
            return state.fillFallback || state.nextAttemptExploratory || shouldEnterFillFallback(state, gameTick);
         }
      }
   }

   void removeTarget(T target) {
      this.states.remove(target);
   }

   void clear() {
      this.states.clear();
   }

   private WirelessBatchCadence.State state(T target, IPatternDetails pattern) {
      Map<IPatternDetails, WirelessBatchCadence.State> byPattern = this.states.computeIfAbsent(target, ignored -> CanonicalPatternMaps.create());
      return byPattern.computeIfAbsent(pattern, ignored -> new WirelessBatchCadence.State());
   }

   private static long ceilingDivide(long amount, long divisor) {
      return amount <= 0L ? 0L : 1L + (amount - 1L) / Math.max(1L, divisor);
   }

   private static int recordBaselineResult(WirelessBatchCadence.State state, long gameTick, long ownedCopies, ProviderTarget.BaselineStatus baselineStatus) {
      if (baselineStatus == ProviderTarget.BaselineStatus.GROWTH_COMPLETE) {
         state.baselineCoverage = 1;
      } else if ((baselineStatus == ProviderTarget.BaselineStatus.COMPLETE || baselineStatus == ProviderTarget.BaselineStatus.PREFIX_COMPLETE)
         && state.lastSuccessTick != Long.MIN_VALUE
         && ownedCopies == state.lastOwnedCopies) {
         state.baselineCoverage = (int)Math.max(1L, Math.min(100L, gameTick - state.lastSuccessTick));
      }

      finishBaselineSample(state, gameTick, ownedCopies, true);
      state.nextAttemptExploratory = false;
      state.exploratorySuccesses = 0;
      state.exploratoryProbeRejected = false;
      return state.baselineCoverage;
   }

   private static void finishBaselineSample(WirelessBatchCadence.State state, long gameTick, long ownedCopies, boolean capacitySuccess) {
      state.nextAttemptExploratory = false;
      state.growthProbeRejected = false;
      state.provenChunkRejections = 0;
      state.failurePressure = 0;
      state.lastSuccessTick = gameTick;
      state.lastOwnedCopies = ownedCopies;
      if (capacitySuccess) {
         state.lastCapacitySuccessTick = gameTick;
         state.fillFallbackRejections = 0;
         state.firstProvenRejectionTick = Long.MIN_VALUE;
      }
   }

   private static boolean shouldEnterFillFallback(WirelessBatchCadence.State state, long gameTick) {
      if (state.fillFallbackRejections >= 64 && state.lastOwnedCopies >= 64L) {
         long noCapacitySuccessSince = state.lastCapacitySuccessTick != Long.MIN_VALUE ? state.lastCapacitySuccessTick : state.firstProvenRejectionTick;
         return noCapacitySuccessSince != Long.MIN_VALUE && gameTick - noCapacitySuccessSince >= 100L;
      } else {
         return false;
      }
   }

   private static int enterFillFallback(WirelessBatchCadence.State state) {
      state.fillFallback = true;
      state.fillFallbackSuccesses = 0;
      state.learnedCoverage = 100;
      state.nextAttemptExploratory = false;
      state.exploratorySuccesses = 0;
      state.exploratoryProbeRejected = false;
      return 25;
   }

   private static int pressureFloor(int failurePressure) {
      if (failurePressure >= 7) {
         return 3;
      } else {
         return failurePressure >= 2 ? 2 : 1;
      }
   }

   private static final class State {
      private long lastSuccessTick = Long.MIN_VALUE;
      private long lastCapacitySuccessTick = Long.MIN_VALUE;
      private long lastOwnedCopies;
      private int learnedCoverage = 1;
      private int provenChunkRejections;
      private int failurePressure;
      private boolean growthProbeRejected;
      private boolean nextAttemptExploratory;
      private boolean exploratoryProbeRejected;
      private int exploratorySuccesses;
      private boolean fillFallback;
      private int fillFallbackSuccesses;
      private long lastActivityTick = Long.MIN_VALUE;
      private long firstProvenRejectionTick = Long.MIN_VALUE;
      private int fillFallbackRejections;
      private int baselineCoverage = 1;

      private void expireIfIdle(long gameTick) {
         if (this.lastActivityTick != Long.MIN_VALUE) {
            if (gameTick < this.lastActivityTick || gameTick - this.lastActivityTick > 100L) {
               this.lastSuccessTick = Long.MIN_VALUE;
               this.lastCapacitySuccessTick = Long.MIN_VALUE;
               this.lastOwnedCopies = 0L;
               this.learnedCoverage = 1;
               this.provenChunkRejections = 0;
               this.failurePressure = 0;
               this.growthProbeRejected = false;
               this.nextAttemptExploratory = false;
               this.exploratoryProbeRejected = false;
               this.exploratorySuccesses = 0;
               this.fillFallback = false;
               this.fillFallbackSuccesses = 0;
               this.lastActivityTick = Long.MIN_VALUE;
               this.firstProvenRejectionTick = Long.MIN_VALUE;
               this.fillFallbackRejections = 0;
               this.baselineCoverage = 1;
            }
         }
      }
   }
}
