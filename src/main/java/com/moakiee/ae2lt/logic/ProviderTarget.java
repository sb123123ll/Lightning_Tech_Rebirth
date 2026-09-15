package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderTarget;
import com.moakiee.thunderbolt.CoreConfig;
import com.moakiee.thunderbolt.CoreConfig.BatchCopyLimitRules;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class ProviderTarget extends TargetAddress {
   private static final int STORAGE_TARGET_CACHE_TTL = 20;
   private static final int BATCH_HISTORY_TTL = 100;
   private final ProviderTarget.ProviderTargetRuntime runtime = new ProviderTarget.ProviderTargetRuntime();

   public ProviderTarget(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace) {
      super(dimension, pos, boundFace);
   }

   @Nullable
   public final BlockEntity resolveBlockEntity(ServerLevel level) {
      if (this.dimension().equals(level.m_46472_()) && level.m_46749_(this.pos())) {
         BlockEntity current = level.m_7702_(this.pos());
         if (current == null) {
            this.invalidatePhysicalState();
            return null;
         } else {
            if (this.runtime.blockEntityRef == null) {
               this.invalidateResolvedPhysicalCaches();
               this.runtime.blockEntityRef = new WeakReference<>(current);
            } else if (this.runtime.blockEntityRef.get() != current) {
               this.invalidatePhysicalState();
               this.runtime.blockEntityRef = new WeakReference<>(current);
            }

            return current;
         }
      } else {
         this.invalidatePhysicalState();
         return null;
      }
   }

   @Nullable
   public final MachineAdapter resolveAdapter(ServerLevel level) {
      BlockEntity blockEntity = this.resolveBlockEntity(level);
      if (blockEntity == null) {
         return null;
      } else {
         if (!this.runtime.adapterResolved) {
            this.runtime.adapter = MachineAdapterRegistry.find(level, this.pos());
            this.runtime.adapterResolved = true;
         }

         return this.runtime.adapter;
      }
   }

   @Nullable
   public final PatternProviderTarget resolveStorageTarget(ServerLevel level, IActionSource source) {
      return this.resolveStorageTarget(level, this.boundFace(), source);
   }

   @Nullable
   public final PatternProviderTarget resolveStorageTarget(ServerLevel level, Direction face, IActionSource source) {
      BlockEntity blockEntity = this.resolveBlockEntity(level);
      if (blockEntity == null) {
         return null;
      } else {
         long gameTick = level.m_46467_();
         int faceIndex = face.m_122411_();
         ProviderTarget.CachedStorageTarget cached = this.runtime.storageTargets[faceIndex];
         if (cached != null && cached.isValid(blockEntity, gameTick)) {
            return cached.target;
         } else {
            PatternProviderTarget resolved = PatternProviderTarget.get(level, this.pos(), blockEntity, face, source);
            if (resolved == null) {
               this.runtime.storageTargets[faceIndex] = null;
            } else {
               this.runtime.storageTargets[faceIndex] = new ProviderTarget.CachedStorageTarget(blockEntity, resolved, gameTick);
            }

            return resolved;
         }
      }
   }

   private boolean isBlocked(
      ServerLevel level,
      @Nullable PatternProviderTarget target,
      IPatternDetails pattern,
      boolean craftingLocked,
      boolean blockingEnabled,
      boolean samePatternMode,
      Set<AEKey> patternInputs
   ) {
      if (craftingLocked) {
         return true;
      } else if (!blockingEnabled) {
         return false;
      } else {
         BlockEntity blockEntity = this.resolveBlockEntity(level);
         if (blockEntity == null) {
            return true;
         } else if (samePatternMode && BatchBlockingPolicy.samePattern(this.runtime.lastSuccessfulPattern, pattern)) {
            return false;
         } else if (target == null) {
            return false;
         } else {
            long gameTick = level.m_46467_();
            if (this.runtime.blockedGameTick != gameTick) {
               this.runtime.blockedThisTick.clear();
               this.runtime.blockedGameTick = gameTick;
            } else if (this.runtime.blockedThisTick.contains(target)) {
               return true;
            }

            boolean blocked = target.containsPatternInput(patternInputs);
            if (blocked) {
               this.runtime.blockedThisTick.add(target);
            }

            return blocked;
         }
      }
   }

   public final boolean isBlocked(
      ServerLevel level,
      IActionSource source,
      IPatternDetails pattern,
      boolean craftingLocked,
      boolean blockingEnabled,
      boolean samePatternMode,
      Set<AEKey> patternInputs
   ) {
      return this.isBlocked(level, this.resolveStorageTarget(level, source), pattern, craftingLocked, blockingEnabled, samePatternMode, patternInputs);
   }

   public final boolean canAccept(ServerLevel level, IPatternDetails pattern) {
      MachineAdapter resolvedAdapter = this.resolveAdapter(level);
      return resolvedAdapter != null && resolvedAdapter.canAccept(level, this.pos(), this.boundFace(), pattern);
   }

   public final boolean canAccept(ServerLevel level, IPatternDetails pattern, IActionSource source) {
      MachineAdapter resolvedAdapter = this.resolveAdapter(level);
      return resolvedAdapter != null && resolvedAdapter.canAccept(level, this.pos(), this.boundFace(), pattern, this.resolveStorageTarget(level, source));
   }

   public final boolean supportsBatch(ServerLevel level, IPatternDetails pattern) {
      MachineAdapter resolvedAdapter = this.resolveAdapter(level);
      return resolvedAdapter != null && resolvedAdapter.supportsBatch(level, this.pos(), this.boundFace(), pattern);
   }

   public final long batchCopyLimit(ServerLevel level) {
      BlockEntity blockEntity = this.resolveBlockEntity(level);
      if (blockEntity == null) {
         return Long.MAX_VALUE;
      } else {
         BatchCopyLimitRules rules = CoreConfig.batchCopyLimitRules();
         if (this.runtime.batchLimitRulesVersion != rules.version()) {
            ResourceLocation blockId = BuiltInRegistries.f_256975_.m_7981_(blockEntity.m_58900_().m_60734_());
            this.runtime.batchCopyLimit = blockId != null ? rules.limit(blockId.toString()) : Long.MAX_VALUE;
            this.runtime.batchLimitRulesVersion = rules.version();
         }

         return this.runtime.batchCopyLimit;
      }
   }

   public final PushResult pushCopies(
      ServerLevel level, IPatternDetails pattern, KeyCounter[] inputs, int copies, Set<AEKey> patternInputs, IActionSource source
   ) {
      return this.pushCopies(level, pattern, inputs, copies, PatternInputAcceptance.COMPLETE_BATCH, patternInputs, source);
   }

   public final PushResult pushCopies(
      ServerLevel level,
      IPatternDetails pattern,
      KeyCounter[] inputs,
      int copies,
      PatternInputAcceptance inputAcceptance,
      Set<AEKey> patternInputs,
      IActionSource source
   ) {
      MachineAdapter resolvedAdapter = this.resolveAdapter(level);
      return resolvedAdapter == null
         ? PushResult.REJECTED
         : resolvedAdapter.pushCopies(
            level,
            this.pos(),
            this.boundFace(),
            pattern,
            inputs,
            copies,
            inputAcceptance,
            false,
            patternInputs,
            source,
            this.resolveStorageTarget(level, source)
         );
   }

   public final void markPatternDispatched(ServerLevel level, IPatternDetails pattern) {
      if (this.resolveBlockEntity(level) != null) {
         this.runtime.lastSuccessfulPattern = pattern;
      }
   }

   public final void setDirectionalOverflow(RoutedPatternOverflow overflow) {
      this.runtime.directionalOverflow = overflow.isEmpty() ? null : overflow;
   }

   @Nullable
   public final RoutedPatternOverflow directionalOverflow() {
      return this.runtime.directionalOverflow;
   }

   public final void clearDirectionalOverflow() {
      this.runtime.directionalOverflow = null;
   }

   @Nullable
   protected final WirelessOverflowQueue.Bucket wirelessOverflow() {
      return this.runtime.wirelessOverflow;
   }

   protected final void setWirelessOverflow(@Nullable WirelessOverflowQueue.Bucket overflow) {
      this.runtime.wirelessOverflow = overflow;
   }

   public final ProviderTarget.BatchDispatchResult pushPattern(
      IPatternDetails pattern, long maxCopies, boolean batchSupported, BooleanSupplier blocked, IntFunction<ProviderTarget.BatchChunk> pushChunk
   ) {
      if (maxCopies <= 0L) {
         return ProviderTarget.BatchDispatchResult.EMPTY;
      } else if (!batchSupported) {
         if (blocked.getAsBoolean()) {
            return ProviderTarget.BatchDispatchResult.EMPTY;
         } else {
            ProviderTarget.BatchChunk single = pushChunk.apply(1);
            return new ProviderTarget.BatchDispatchResult(single.ownedCopies(), single.globalAbort());
         }
      } else {
         int rememberedChunk = this.runtime.batchChunks.getOrDefault(pattern, Integer.valueOf(1));
         int nextChunk = rememberedChunk;
         long ownedCopies = 0L;
         boolean fullChunkAccepted = false;
         boolean backingOff = false;

         while (ownedCopies < maxCopies && !blocked.getAsBoolean()) {
            long remaining = maxCopies - ownedCopies;
            int chunkCopies = (int)Math.min(Math.min((long)nextChunk, remaining), 2147483647L);
            boolean requestLimited = chunkCopies < nextChunk;
            ProviderTarget.BatchChunk chunk = pushChunk.apply(chunkCopies);
            if (chunk.globalAbort()) {
               return new ProviderTarget.BatchDispatchResult(ownedCopies, true);
            }

            if (chunk.ownedCopies() <= 0L) {
               if (fullChunkAccepted) {
                  break;
               }

               if (chunkCopies <= 1) {
                  this.rememberBatchChunk(pattern, 1);
                  break;
               }

               nextChunk = Math.max(1, chunkCopies / 2);
               this.rememberBatchChunk(pattern, nextChunk);
               backingOff = true;
            } else {
               ownedCopies += chunk.ownedCopies();
               if (chunk.ownedCopies() != (long)chunkCopies || !chunk.fullyInserted()) {
                  if (!fullChunkAccepted) {
                     this.rememberBatchChunk(pattern, Math.max(1, chunkCopies / 2));
                  }
                  break;
               }

               fullChunkAccepted = true;
               if (!requestLimited) {
                  this.rememberBatchChunk(pattern, chunkCopies);
               }

               if (backingOff || chunkCopies == Integer.MAX_VALUE) {
                  break;
               }

               nextChunk = (int)Math.min(ownedCopies, 2147483647L);
            }
         }

         return new ProviderTarget.BatchDispatchResult(ownedCopies, false);
      }
   }

   private void rememberBatchChunk(IPatternDetails pattern, int chunkCopies) {
      int sanitized = Math.max(1, chunkCopies);
      Integer previous = this.runtime.batchChunks.put(pattern, sanitized);
      if (previous == null || previous != sanitized) {
         this.runtime.batchHistoryDirty = true;
      }
   }

   public final ProviderTarget.BatchStepResult pushPatternStep(
      IPatternDetails pattern, long maxCopies, long gameTick, boolean batchSupported, BooleanSupplier blocked, IntFunction<ProviderTarget.BatchChunk> pushChunk
   ) {
      return this.pushPatternStep(pattern, maxCopies, gameTick, batchSupported, false, blocked, pushChunk);
   }

   public final ProviderTarget.BatchStepResult pushPatternStep(
      IPatternDetails pattern,
      long maxCopies,
      long gameTick,
      boolean batchSupported,
      boolean preserveBatchHistoryOnRejection,
      BooleanSupplier blocked,
      IntFunction<ProviderTarget.BatchChunk> pushChunk
   ) {
      if (maxCopies <= 0L || blocked.getAsBoolean()) {
         return ProviderTarget.BatchStepResult.EMPTY;
      } else if (!batchSupported) {
         ProviderTarget.BatchChunk single = pushChunk.apply(1);
         return ProviderTarget.BatchStepResult.from(single, 1, false);
      } else {
         ProviderTarget.BatchStepState state = this.runtime.batchSteps.computeIfAbsent(pattern, ignored -> new ProviderTarget.BatchStepState());
         ProviderTarget.BatchStepSnapshot previousState = state.snapshot();
         state.expireIfIdle(gameTick, preserveBatchHistoryOnRejection);
         state.lastAttemptTick = gameTick;
         ProviderTarget.BatchStepResult result;
         if (!state.backingOff) {
            result = pushSameTickRamp(state, maxCopies, gameTick, preserveBatchHistoryOnRejection, blocked, pushChunk);
         } else {
            result = pushBackoffStep(state, maxCopies, gameTick, preserveBatchHistoryOnRejection, pushChunk);
         }

         if (!previousState.equals(state.snapshot())) {
            this.runtime.batchHistoryDirty = true;
         }

         return result;
      }
   }

   private static ProviderTarget.BatchStepResult pushBackoffStep(
      ProviderTarget.BatchStepState state,
      long maxCopies,
      long gameTick,
      boolean preserveBatchHistoryOnRejection,
      IntFunction<ProviderTarget.BatchChunk> pushChunk
   ) {
      int attemptedCopies = (int)Math.min(Math.min((long)state.nextChunk, maxCopies), 2147483647L);
      boolean requestLimited = attemptedCopies < state.nextChunk;
      ProviderTarget.BatchChunk chunk = pushChunk.apply(attemptedCopies);
      if (chunk.globalAbort()) {
         return ProviderTarget.BatchStepResult.from(chunk, attemptedCopies, requestLimited);
      } else if (chunk.ownedCopies() <= 0L) {
         if (!preserveBatchHistoryOnRejection) {
            state.reject(attemptedCopies, requestLimited);
         }

         return ProviderTarget.BatchStepResult.from(chunk, attemptedCopies, requestLimited);
      } else {
         if (!requestLimited) {
            if (chunk.ownedCopies() == (long)attemptedCopies && chunk.fullyInserted()) {
               state.advance(attemptedCopies);
            } else {
               state.reject(attemptedCopies, false);
            }
         }

         state.lastSuccessfulTick = gameTick;
         return ProviderTarget.BatchStepResult.from(chunk, attemptedCopies, requestLimited);
      }
   }

   private static ProviderTarget.BatchStepResult pushSameTickRamp(
      ProviderTarget.BatchStepState state,
      long maxCopies,
      long gameTick,
      boolean preserveBatchHistoryOnRejection,
      BooleanSupplier blocked,
      IntFunction<ProviderTarget.BatchChunk> pushChunk
   ) {
      int baseline = Math.max(1, state.provenChunk);
      int nextChunk = baseline;
      int baselineSuccesses = 0;
      boolean allowGrowth = !state.growthCapped;
      long ownedCopies = 0L;
      long attemptedCopies = 0L;
      boolean allFullyInserted = true;

      while (ownedCopies < maxCopies && !blocked.getAsBoolean()) {
         long remaining = maxCopies - ownedCopies;
         int desiredCopies = nextChunk;
         int attemptedChunk = (int)Math.min(Math.min((long)nextChunk, remaining), 2147483647L);
         boolean requestLimited = attemptedChunk < nextChunk;
         attemptedCopies = Math.min(2147483647L, attemptedCopies + (long)attemptedChunk);
         ProviderTarget.BatchChunk chunk = pushChunk.apply(attemptedChunk);
         if (chunk.globalAbort()) {
            return new ProviderTarget.BatchStepResult(ownedCopies, (int)attemptedCopies, false, true, requestLimited, ProviderTarget.BaselineStatus.NONE);
         }

         if (chunk.ownedCopies() <= 0L) {
            ProviderTarget.BaselineStatus stopStatus = baselineSuccesses == 1
               ? ProviderTarget.BaselineStatus.PREFIX_COMPLETE
               : ProviderTarget.BaselineStatus.NONE;
            if (!preserveBatchHistoryOnRejection) {
               if (baselineSuccesses == 0) {
                  state.reject(attemptedChunk, requestLimited);
               } else if (baselineSuccesses >= 2) {
                  state.growthCapped = true;
                  state.nextChunk = Math.max(1, state.provenChunk);
               }
            }

            if (ownedCopies > 0L) {
               state.lastSuccessfulTick = gameTick;
            }

            return new ProviderTarget.BatchStepResult(ownedCopies, (int)attemptedCopies, false, false, requestLimited, stopStatus);
         }

         ownedCopies += chunk.ownedCopies();
         boolean fullyInserted = chunk.ownedCopies() == (long)attemptedChunk && chunk.fullyInserted();
         if (!fullyInserted) {
            ProviderTarget.BaselineStatus stopStatusx = baselineSuccesses == 1
               ? ProviderTarget.BaselineStatus.PREFIX_COMPLETE
               : ProviderTarget.BaselineStatus.NONE;
            allFullyInserted = false;
            if (!preserveBatchHistoryOnRejection) {
               if (baselineSuccesses == 0) {
                  state.reject(attemptedChunk, requestLimited);
               } else if (baselineSuccesses >= 2) {
                  state.growthCapped = true;
                  state.nextChunk = Math.max(1, state.provenChunk);
               }
            }

            state.lastSuccessfulTick = gameTick;
            return new ProviderTarget.BatchStepResult(ownedCopies, (int)attemptedCopies, false, false, requestLimited, stopStatusx);
         }

         if (requestLimited) {
            state.lastSuccessfulTick = gameTick;
            return new ProviderTarget.BatchStepResult(ownedCopies, (int)attemptedCopies, allFullyInserted, false, true, ProviderTarget.BaselineStatus.NONE);
         }

         if (baselineSuccesses < 2) {
            baselineSuccesses++;
            state.provenChunk = Math.max(state.provenChunk, baseline);
            state.nextChunk = baseline;
            if (baselineSuccesses < 2) {
               continue;
            }

            if (!allowGrowth) {
               state.lastSuccessfulTick = gameTick;
               return new ProviderTarget.BatchStepResult(ownedCopies, (int)attemptedCopies, true, false, false, ProviderTarget.BaselineStatus.COMPLETE);
            }

            nextChunk = saturatingDouble(baseline);
         } else {
            state.provenChunk = nextChunk;
            state.nextChunk = saturatingDouble(nextChunk);
            state.provenSuccesses = 1;
            state.repeatCurrent = false;
            state.growthCapped = false;
            state.backingOff = false;
            nextChunk = state.nextChunk;
         }

         state.lastSuccessfulTick = gameTick;
         if (desiredCopies == Integer.MAX_VALUE) {
            break;
         }
      }

      return new ProviderTarget.BatchStepResult(
         ownedCopies,
         (int)attemptedCopies,
         allFullyInserted,
         false,
         false,
         baselineSuccesses >= 2 ? ProviderTarget.BaselineStatus.GROWTH_COMPLETE : ProviderTarget.BaselineStatus.NONE
      );
   }

   private static int saturatingDouble(int value) {
      return value >= 1073741823 ? Integer.MAX_VALUE : value * 2;
   }

   final int batchStepCandidate(IPatternDetails pattern, long maxCopies, long gameTick) {
      if (maxCopies <= 0L) {
         return 0;
      } else {
         ProviderTarget.BatchStepState state = this.runtime.batchSteps.computeIfAbsent(pattern, ignored -> new ProviderTarget.BatchStepState());
         ProviderTarget.BatchStepSnapshot previousState = state.snapshot();
         state.expireIfIdle(gameTick, true);
         if (!previousState.equals(state.snapshot())) {
            this.runtime.batchHistoryDirty = true;
         }

         return (int)Math.min(Math.min((long)state.nextChunk, maxCopies), 2147483647L);
      }
   }

   final long batchStepRampAllowance(IPatternDetails pattern, long maxCopies, long gameTick) {
      if (maxCopies <= 0L) {
         return 0L;
      } else {
         ProviderTarget.BatchStepState state = this.runtime.batchSteps.computeIfAbsent(pattern, ignored -> new ProviderTarget.BatchStepState());
         state.expireIfIdle(gameTick, true);
         return state.growthCapped ? (long)this.batchStepCandidate(pattern, maxCopies, gameTick) : maxCopies;
      }
   }

   public final boolean isAlive(ServerLevel level) {
      return this.resolveBlockEntity(level) != null;
   }

   public final OutputReturnResult returnOutputs(ServerLevel level, AllowedOutputFilter allowedOutputs, IActionSource source, MachineAdapter.OutputSink sink) {
      MachineAdapter resolvedAdapter = this.resolveAdapter(level);
      return resolvedAdapter == null
         ? OutputReturnResult.UNAVAILABLE
         : resolvedAdapter.extractOutputs(level, this.pos(), this.boundFace(), allowedOutputs, source, sink);
   }

   public final boolean claimOutputReturnScan(long gameTick) {
      if (this.runtime.lastOutputReturnScanTick == gameTick) {
         return false;
      } else {
         this.runtime.lastOutputReturnScanTick = gameTick;
         return true;
      }
   }

   public final boolean flushOverflow(ServerLevel level, List<GenericStack> overflow, IActionSource source) {
      MachineAdapter resolvedAdapter = this.resolveAdapter(level);
      return resolvedAdapter != null
         && resolvedAdapter.flushOverflow(level, this.pos(), this.boundFace(), overflow, source, this.resolveStorageTarget(level, source));
   }

   public final void clearRuntimeState() {
      this.invalidatePhysicalState();
   }

   final void clearBatchHistory() {
      if (!this.runtime.batchChunks.isEmpty() || !this.runtime.batchSteps.isEmpty()) {
         this.runtime.batchHistoryDirty = true;
      }

      this.runtime.batchChunks.clear();
      this.runtime.batchSteps.clear();
   }

   final Map<IPatternDetails, ProviderTarget.AdaptiveBatchSnapshot> adaptiveBatchSnapshots() {
      IdentityHashMap<IPatternDetails, ProviderTarget.AdaptiveBatchSnapshot> result = new IdentityHashMap<>();
      this.runtime.batchChunks.forEach((pattern, chunk) -> result.put(pattern, new ProviderTarget.AdaptiveBatchSnapshot(chunk, null)));
      this.runtime
         .batchSteps
         .forEach(
            (pattern, state) -> result.merge(
                  pattern,
                  new ProviderTarget.AdaptiveBatchSnapshot(0, state.snapshot()),
                  (left, right) -> new ProviderTarget.AdaptiveBatchSnapshot(left.rememberedChunk(), right.step())
               )
         );
      return result;
   }

   final void restoreAdaptiveBatchSnapshot(IPatternDetails pattern, ProviderTarget.AdaptiveBatchSnapshot snapshot) {
      if (pattern != null && snapshot != null && snapshot.isValid()) {
         if (snapshot.rememberedChunk() > 0) {
            this.runtime.batchChunks.put(pattern, snapshot.rememberedChunk());
         }

         if (snapshot.step() != null) {
            this.runtime.batchSteps.put(pattern, new ProviderTarget.BatchStepState(snapshot.step()));
         }

         this.runtime.batchHistoryDirty = false;
      }
   }

   final boolean consumeAdaptiveBatchHistoryDirty() {
      boolean dirty = this.runtime.batchHistoryDirty;
      this.runtime.batchHistoryDirty = false;
      return dirty;
   }

   private void invalidatePhysicalState() {
      this.invalidateResolvedPhysicalCaches();
      this.clearBatchHistory();
   }

   private void invalidateResolvedPhysicalCaches() {
      this.runtime.blockEntityRef = null;
      this.runtime.adapter = null;
      this.runtime.adapterResolved = false;
      Arrays.fill(this.runtime.storageTargets, null);
      this.runtime.blockedThisTick.clear();
      this.runtime.blockedGameTick = Long.MIN_VALUE;
      this.runtime.lastOutputReturnScanTick = Long.MIN_VALUE;
      this.runtime.lastSuccessfulPattern = null;
      this.runtime.batchLimitRulesVersion = Long.MIN_VALUE;
      this.runtime.batchCopyLimit = Long.MAX_VALUE;
   }

   static record AdaptiveBatchSnapshot(int rememberedChunk, @Nullable ProviderTarget.BatchStepSnapshot step) {
      boolean isValid() {
         return this.rememberedChunk >= 0 && (this.rememberedChunk > 0 || this.step != null) && (this.step == null || this.step.isValid());
      }
   }

   public static enum BaselineStatus {
      NONE,
      PREFIX_COMPLETE,
      COMPLETE,
      GROWTH_COMPLETE;
   }

   public static record BatchChunk(long ownedCopies, boolean fullyInserted, boolean globalAbort) {
      public static final ProviderTarget.BatchChunk REJECTED = new ProviderTarget.BatchChunk(0L, false, false);
      public static final ProviderTarget.BatchChunk GLOBAL_ABORT = new ProviderTarget.BatchChunk(0L, false, true);
   }

   public static record BatchDispatchResult(long ownedCopies, boolean globalAbort) {
      private static final ProviderTarget.BatchDispatchResult EMPTY = new ProviderTarget.BatchDispatchResult(0L, false);
   }

   public static record BatchStepResult(
      long ownedCopies, int attemptedCopies, boolean fullyInserted, boolean globalAbort, boolean requestLimited, ProviderTarget.BaselineStatus baselineStatus
   ) {
      private static final ProviderTarget.BatchStepResult EMPTY = new ProviderTarget.BatchStepResult(
         0L, 0, false, false, false, ProviderTarget.BaselineStatus.NONE
      );

      private static ProviderTarget.BatchStepResult from(ProviderTarget.BatchChunk chunk, int attemptedCopies, boolean requestLimited) {
         return from(chunk, attemptedCopies, requestLimited, ProviderTarget.BaselineStatus.NONE);
      }

      private static ProviderTarget.BatchStepResult from(
         ProviderTarget.BatchChunk chunk, int attemptedCopies, boolean requestLimited, ProviderTarget.BaselineStatus baselineStatus
      ) {
         return new ProviderTarget.BatchStepResult(
            chunk.ownedCopies(), attemptedCopies, chunk.fullyInserted(), chunk.globalAbort(), requestLimited, baselineStatus
         );
      }

      public boolean acceptedFullChunk() {
         return this.ownedCopies == (long)this.attemptedCopies && this.fullyInserted;
      }
   }

   static record BatchStepSnapshot(
      int nextChunk,
      int provenChunk,
      int provenSuccesses,
      boolean repeatCurrent,
      boolean growthCapped,
      boolean backingOff,
      long lastSuccessfulTick,
      long lastAttemptTick
   ) {
      boolean isValid() {
         return this.nextChunk > 0 && this.provenChunk >= 0 && this.provenSuccesses >= 0 && (this.provenChunk > 0 || this.nextChunk == 1 && !this.backingOff);
      }
   }

   private static final class BatchStepState {
      private int nextChunk = 1;
      private int provenChunk;
      private int provenSuccesses;
      private boolean repeatCurrent = true;
      private boolean growthCapped;
      private boolean backingOff;
      private long lastSuccessfulTick = Long.MIN_VALUE;
      private long lastAttemptTick = Long.MIN_VALUE;

      private BatchStepState() {
      }

      private BatchStepState(ProviderTarget.BatchStepSnapshot snapshot) {
         this.nextChunk = snapshot.nextChunk();
         this.provenChunk = snapshot.provenChunk();
         this.provenSuccesses = snapshot.provenSuccesses();
         this.repeatCurrent = snapshot.repeatCurrent();
         this.growthCapped = snapshot.growthCapped();
         this.backingOff = snapshot.backingOff();
         this.lastSuccessfulTick = snapshot.lastSuccessfulTick();
         this.lastAttemptTick = snapshot.lastAttemptTick();
      }

      private ProviderTarget.BatchStepSnapshot snapshot() {
         return new ProviderTarget.BatchStepSnapshot(
            this.nextChunk,
            this.provenChunk,
            this.provenSuccesses,
            this.repeatCurrent,
            this.growthCapped,
            this.backingOff,
            this.lastSuccessfulTick,
            this.lastAttemptTick
         );
      }

      private void expireIfIdle(long gameTick, boolean preserveAttemptHistory) {
         long referenceTick = preserveAttemptHistory ? this.lastAttemptTick : this.lastSuccessfulTick;
         if (referenceTick != Long.MIN_VALUE) {
            boolean expired = preserveAttemptHistory
               ? gameTick < referenceTick || gameTick - referenceTick > 100L
               : gameTick < referenceTick || gameTick - referenceTick >= 100L;
            if (expired) {
               this.nextChunk = 1;
               this.provenChunk = 0;
               this.provenSuccesses = 0;
               this.repeatCurrent = true;
               this.growthCapped = false;
               this.backingOff = false;
               this.lastSuccessfulTick = Long.MIN_VALUE;
               this.lastAttemptTick = Long.MIN_VALUE;
            }
         }
      }

      private void advance(int acceptedChunk) {
         if (this.backingOff && this.provenChunk > 0) {
            this.nextChunk = this.provenChunk;
            this.repeatCurrent = false;
            this.backingOff = false;
         } else {
            if (acceptedChunk != this.provenChunk) {
               this.provenChunk = acceptedChunk;
               this.provenSuccesses = 1;
            } else if (this.provenSuccesses < Integer.MAX_VALUE) {
               this.provenSuccesses++;
            }

            if (this.growthCapped) {
               this.nextChunk = this.provenChunk;
               this.repeatCurrent = false;
            } else if (this.repeatCurrent) {
               this.nextChunk = acceptedChunk;
               this.repeatCurrent = false;
            } else {
               this.nextChunk = acceptedChunk >= 1073741823 ? Integer.MAX_VALUE : acceptedChunk * 2;
            }
         }
      }

      private void reject(int rejectedChunk, boolean requestLimited) {
         if (!requestLimited) {
            if (this.provenChunk > 0 && rejectedChunk > this.provenChunk) {
               this.growthCapped = true;
               this.nextChunk = this.provenChunk;
               this.repeatCurrent = false;
            } else if (this.growthCapped && rejectedChunk >= this.provenChunk && this.provenSuccesses < 3) {
               this.nextChunk = this.provenChunk;
               this.repeatCurrent = false;
            } else {
               this.nextChunk = Math.max(1, rejectedChunk / 2);
               if (this.provenChunk > 0) {
                  this.backingOff = true;
                  this.repeatCurrent = false;
               } else {
                  this.repeatCurrent = true;
               }
            }
         }
      }
   }

   private static final class CachedStorageTarget {
      private final WeakReference<BlockEntity> blockEntity;
      private final PatternProviderTarget target;
      private final long createdTick;

      private CachedStorageTarget(BlockEntity blockEntity, PatternProviderTarget target, long createdTick) {
         this.blockEntity = new WeakReference<>(blockEntity);
         this.target = target;
         this.createdTick = createdTick;
      }

      private boolean isValid(BlockEntity current, long gameTick) {
         return this.blockEntity.get() == current && gameTick - this.createdTick < 20L;
      }
   }

   private static final class ProviderTargetRuntime {
      @Nullable
      private WeakReference<BlockEntity> blockEntityRef;
      @Nullable
      private MachineAdapter adapter;
      private boolean adapterResolved;
      private final ProviderTarget.CachedStorageTarget[] storageTargets = new ProviderTarget.CachedStorageTarget[Direction.values().length];
      private final Set<PatternProviderTarget> blockedThisTick = Collections.newSetFromMap(new IdentityHashMap<>());
      private long blockedGameTick = Long.MIN_VALUE;
      private long lastOutputReturnScanTick = Long.MIN_VALUE;
      private final IdentityHashMap<IPatternDetails, Integer> batchChunks = new IdentityHashMap<>();
      private final IdentityHashMap<IPatternDetails, ProviderTarget.BatchStepState> batchSteps = new IdentityHashMap<>();
      private boolean batchHistoryDirty;
      private long batchLimitRulesVersion = Long.MIN_VALUE;
      private long batchCopyLimit = Long.MAX_VALUE;
      @Nullable
      private IPatternDetails lastSuccessfulPattern;
      @Nullable
      private RoutedPatternOverflow directionalOverflow;
      @Nullable
      private WirelessOverflowQueue.Bucket wirelessOverflow;
   }
}
