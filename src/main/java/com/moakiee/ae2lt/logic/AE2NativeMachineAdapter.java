package com.moakiee.ae2lt.logic;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.parts.automation.StackWorldBehaviors;
import com.google.common.util.concurrent.Runnables;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

final class AE2NativeMachineAdapter implements MachineAdapter {
   static final AE2NativeMachineAdapter INSTANCE = new AE2NativeMachineAdapter();
   private static final int WRAPPER_REFRESH_TICKS = 20;
   private final Map<AE2NativeMachineAdapter.TargetFaceKey, AE2NativeMachineAdapter.StorageCacheEntry> storageCache = new HashMap<>();
   private static final int MACHINE_CACHE_TTL_TICKS = 20;
   private final Map<AE2NativeMachineAdapter.TargetFaceKey, AE2NativeMachineAdapter.MachineCacheEntry> machineCache = new HashMap<>();
   private static final int SWEEP_INTERVAL_TICKS = 600;
   private long lastSweepTick = -600L;

   private AE2NativeMachineAdapter() {
   }

   @Override
   public boolean supports(ServerLevel level, BlockPos pos) {
      return level.m_46749_(pos) && level.m_7702_(pos) != null;
   }

   @Override
   public boolean canAccept(ServerLevel level, BlockPos pos, Direction face, IPatternDetails pattern) {
      ICraftingMachine machine = ICraftingMachine.of(level.m_7702_(pos), face);
      return machine != null && machine.acceptsPlans() ? true : pattern.supportsPushInputsToExternalInventory();
   }

   @Override
   public boolean canAccept(ServerLevel level, BlockPos pos, Direction face, IPatternDetails pattern, @Nullable PatternProviderTarget cachedTarget) {
      ICraftingMachine machine = ICraftingMachine.of(level.m_7702_(pos), face);
      return machine != null && machine.acceptsPlans() ? true : cachedTarget != null && pattern.supportsPushInputsToExternalInventory();
   }

   @Override
   public boolean supportsBatch(ServerLevel level, BlockPos pos, Direction face, IPatternDetails pattern) {
      return ICraftingMachine.of(level.m_7702_(pos), face) == null;
   }

   @Override
   public PushResult pushCopies(
      ServerLevel level,
      BlockPos pos,
      Direction face,
      IPatternDetails pattern,
      KeyCounter[] inputs,
      int maxCopies,
      boolean blocking,
      Set<AEKey> patternInputs,
      IActionSource source,
      @Nullable PatternProviderTarget cachedTarget
   ) {
      return this.pushCopies(level, pos, face, pattern, inputs, maxCopies, PatternInputAcceptance.COMPLETE_BATCH, blocking, patternInputs, source, cachedTarget);
   }

   @Override
   public PushResult pushCopies(
      ServerLevel level,
      BlockPos pos,
      Direction face,
      IPatternDetails pattern,
      KeyCounter[] inputs,
      int maxCopies,
      PatternInputAcceptance inputAcceptance,
      boolean blocking,
      Set<AEKey> patternInputs,
      IActionSource source,
      @Nullable PatternProviderTarget cachedTarget
   ) {
      long now = level.m_46467_();
      this.sweepStaleEntries(now);
      BlockEntity be = level.m_7702_(pos);
      ICraftingMachine machine = be != null ? this.resolveCraftingMachine(level, pos, face, be, now) : null;
      if (machine != null && machine.acceptsPlans()) {
         return machine.pushPattern(pattern, inputs, face) ? new PushResult(1, List.of()) : PushResult.REJECTED;
      } else if (!pattern.supportsPushInputsToExternalInventory()) {
         return PushResult.REJECTED;
      } else {
         PatternProviderTarget target;
         if (cachedTarget != null) {
            target = cachedTarget;
         } else {
            target = PatternProviderTarget.get(level, pos, be, face, source);
         }

         if (target == null) {
            return PushResult.REJECTED;
         } else if (blocking && target.containsPatternInput(patternInputs)) {
            return PushResult.REJECTED;
         } else {
            List<GenericStack> plannedInputs = planScaledInputs(pattern, inputs, maxCopies);
            return plannedInputs != null && !plannedInputs.isEmpty()
               ? pushPlannedInputs(target, plannedInputs, maxCopies, inputAcceptance)
               : PushResult.REJECTED;
         }
      }
   }

   static PushResult pushPlannedInputs(PatternProviderTarget target, List<GenericStack> plannedInputs, int maxCopies, PatternInputAcceptance inputAcceptance) {
      if (!adapterAcceptsInputs(target, plannedInputs, inputAcceptance)) {
         return PushResult.REJECTED;
      } else if (inputAcceptance == PatternInputAcceptance.VANILLA_SINGLE_COPY) {
         if (maxCopies != 1) {
            throw new IllegalArgumentException("Vanilla provider dispatch must contain exactly one copy");
         } else {
            ArrayList<GenericStack> overflow = new ArrayList<>();

            for (GenericStack input : plannedInputs) {
               long inserted = target.insert(input.what(), input.amount(), Actionable.MODULATE);
               if (inserted < input.amount()) {
                  overflow.add(new GenericStack(input.what(), input.amount() - inserted));
               }
            }

            return new PushResult(1, overflow);
         }
      } else {
         ArrayList<GenericStack> overflow = new ArrayList<>();
         boolean insertedAny = false;

         for (int i = 0; i < plannedInputs.size(); i++) {
            GenericStack inputx = plannedInputs.get(i);
            long inserted = target.insert(inputx.what(), inputx.amount(), Actionable.MODULATE);
            if (inserted > 0L) {
               insertedAny = true;
            }

            if (inserted < inputx.amount()) {
               if (!insertedAny) {
                  return PushResult.REJECTED;
               }

               overflow.add(new GenericStack(inputx.what(), inputx.amount() - inserted));

               for (int remaining = i + 1; remaining < plannedInputs.size(); remaining++) {
                  overflow.add(plannedInputs.get(remaining));
               }
               break;
            }
         }

         return insertedAny ? new PushResult(maxCopies, overflow) : PushResult.REJECTED;
      }
   }

   @Override
   public OutputReturnResult extractOutputs(
      ServerLevel level, BlockPos pos, Direction face, AllowedOutputFilter allowedOutputs, IActionSource source, MachineAdapter.OutputSink sink
   ) {
      MEStorage directStorage = resolveDirectStorage(level, pos, face);
      List<MEStorage> storages = preferredExtractionStorages(directStorage, () -> this.resolveExternalStorages(level, pos, face));
      return extractOutputsFromStorages(storages, allowedOutputs, source, sink);
   }

   static OutputReturnResult extractOutputsFromStorages(
      List<MEStorage> storages, AllowedOutputFilter allowedOutputs, IActionSource source, MachineAdapter.OutputSink sink
   ) {
      if (storages.isEmpty()) {
         return OutputReturnResult.UNAVAILABLE;
      } else {
         boolean extractedAny = false;
         boolean matchingOutputBlocked = false;

         for (MEStorage storage : storages) {
            KeyCounter scanBuffer = new KeyCounter();
            storage.getAvailableStacks(scanBuffer);

            for (Entry<AEKey> entry : scanBuffer) {
               AEKey key = (AEKey)entry.getKey();
               long amount = entry.getLongValue();
               if (amount > 0L && allowedOutputs.matches(key)) {
                  long cap = sink.maxAccept(key, amount);
                  if (cap <= 0L) {
                     matchingOutputBlocked = true;
                  } else {
                     long taken = storage.extract(key, Math.min(cap, amount), Actionable.MODULATE, source);
                     if (taken <= 0L) {
                        matchingOutputBlocked = true;
                     } else {
                        extractedAny = true;
                        long leftover = taken - sink.accept(key, taken);
                        if (leftover > 0L) {
                           leftover -= storage.insert(key, leftover, Actionable.MODULATE, source);
                           if (leftover > 0L) {
                              sink.acceptOverflow(key, leftover);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (extractedAny) {
            return OutputReturnResult.EXTRACTED;
         } else {
            return matchingOutputBlocked ? OutputReturnResult.BLOCKED : OutputReturnResult.EMPTY;
         }
      }
   }

   static List<MEStorage> preferredExtractionStorages(@Nullable MEStorage directStorage, Supplier<List<MEStorage>> externalStorageFallback) {
      return directStorage != null ? List.of(directStorage) : externalStorageFallback.get();
   }

   @Nullable
   private static MEStorage resolveDirectStorage(ServerLevel level, BlockPos pos, Direction face) {
      BlockEntity blockEntity = level.m_7702_(pos);
      return blockEntity == null ? null : (MEStorage)blockEntity.getCapability(Capabilities.STORAGE, face).orElse(null);
   }

   private List<MEStorage> resolveExternalStorages(ServerLevel level, BlockPos pos, Direction face) {
      AE2NativeMachineAdapter.StorageCacheEntry cached = this.resolveCache(level, pos, face);
      if (cached == null) {
         return List.of();
      } else {
         Map<AEKeyType, MEStorage> wrappers = cached.getWrappers(level.m_46467_());
         return wrappers == null ? List.of() : List.copyOf(wrappers.values());
      }
   }

   @Nullable
   private ICraftingMachine resolveCraftingMachine(ServerLevel level, BlockPos pos, Direction face, BlockEntity be, long now) {
      AE2NativeMachineAdapter.TargetFaceKey key = new AE2NativeMachineAdapter.TargetFaceKey(level.m_46472_(), pos.m_121878_(), face);
      AE2NativeMachineAdapter.MachineCacheEntry cached = this.machineCache.get(key);
      if (cached != null && cached.isValid(be, now)) {
         return cached.machine;
      } else {
         ICraftingMachine machine = ICraftingMachine.of(be, face);
         this.machineCache.put(key, new AE2NativeMachineAdapter.MachineCacheEntry(be, machine, now));
         return machine;
      }
   }

   @Nullable
   private static List<GenericStack> planScaledInputs(IPatternDetails pattern, KeyCounter[] inputHolder, int copies) {
      if (copies <= 0) {
         return null;
      } else {
         ArrayList<GenericStack> planned = new ArrayList<>();

         try {
            pattern.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
               long scaled = Math.multiplyExact(amount, (long)copies);
               if (scaled > 0L) {
                  planned.add(new GenericStack(what, scaled));
               }
            });
            return planned;
         } catch (ArithmeticException var5) {
            return null;
         }
      }
   }

   private static boolean adapterAcceptsInputs(PatternProviderTarget target, List<GenericStack> plannedInputs, PatternInputAcceptance inputAcceptance) {
      for (GenericStack input : plannedInputs) {
         long simulated = target.insert(input.what(), input.amount(), Actionable.SIMULATE);
         boolean accepted = inputAcceptance == PatternInputAcceptance.VANILLA_SINGLE_COPY ? simulated > 0L : input.amount() > 0L && simulated >= input.amount();
         if (!accepted) {
            return false;
         }
      }

      return true;
   }

   @Nullable
   private AE2NativeMachineAdapter.StorageCacheEntry resolveCache(ServerLevel level, BlockPos pos, Direction face) {
      this.sweepStaleEntries(level.m_46467_());
      AE2NativeMachineAdapter.TargetFaceKey cacheKey = new AE2NativeMachineAdapter.TargetFaceKey(level.m_46472_(), pos.m_121878_(), face);
      BlockEntity blockEntity = level.m_7702_(pos);
      if (blockEntity == null) {
         this.storageCache.remove(cacheKey);
         return null;
      } else {
         AE2NativeMachineAdapter.StorageCacheEntry cached = this.storageCache.get(cacheKey);
         if (cached == null || !cached.isValid(blockEntity)) {
            Map<AEKeyType, ExternalStorageStrategy> strategies = StackWorldBehaviors.createExternalStorageStrategies(level, pos, face);
            if (strategies.isEmpty()) {
               this.storageCache.remove(cacheKey);
               return null;
            }

            cached = new AE2NativeMachineAdapter.StorageCacheEntry(blockEntity, strategies);
            this.storageCache.put(cacheKey, cached);
         }

         return cached;
      }
   }

   private void sweepStaleEntries(long gameTick) {
      if (gameTick < this.lastSweepTick || gameTick - this.lastSweepTick >= 600L) {
         this.lastSweepTick = gameTick;
         this.storageCache.values().removeIf(entry -> {
            BlockEntity be = entry.blockEntityRef.get();
            return be == null || be.m_58901_();
         });
         this.machineCache.values().removeIf(entry -> {
            BlockEntity be = entry.beRef.get();
            return be == null || be.m_58901_();
         });
      }
   }

   private static final class MachineCacheEntry {
      private final WeakReference<BlockEntity> beRef;
      @Nullable
      private final ICraftingMachine machine;
      private final long createdTick;

      MachineCacheEntry(BlockEntity be, @Nullable ICraftingMachine machine, long tick) {
         this.beRef = new WeakReference<>(be);
         this.machine = machine;
         this.createdTick = tick;
      }

      boolean isValid(BlockEntity currentBE, long now) {
         return this.beRef.get() == currentBE && now - this.createdTick < 20L;
      }
   }

   private static final class StorageCacheEntry {
      private final WeakReference<BlockEntity> blockEntityRef;
      private final Map<AEKeyType, ExternalStorageStrategy> strategies;
      private Map<AEKeyType, MEStorage> wrappers;
      private long wrapperCreatedTick;

      StorageCacheEntry(BlockEntity be, Map<AEKeyType, ExternalStorageStrategy> strategies) {
         this.blockEntityRef = new WeakReference<>(be);
         this.strategies = strategies;
      }

      boolean isValid(BlockEntity currentBE) {
         return this.blockEntityRef.get() == currentBE;
      }

      @Nullable
      Map<AEKeyType, MEStorage> getWrappers(long gameTick) {
         if (this.wrappers == null || gameTick - this.wrapperCreatedTick >= 20L) {
            this.rebuildWrappers(gameTick);
         }

         return this.wrappers;
      }

      private void rebuildWrappers(long gameTick) {
         IdentityHashMap<AEKeyType, MEStorage> map = new IdentityHashMap<>(this.strategies.size());

         for (java.util.Map.Entry<AEKeyType, ExternalStorageStrategy> entry : this.strategies.entrySet()) {
            MEStorage wrapper = entry.getValue().createWrapper(false, Runnables.doNothing());
            if (wrapper != null) {
               map.put(entry.getKey(), wrapper);
            }
         }

         this.wrappers = map.isEmpty() ? null : map;
         this.wrapperCreatedTick = gameTick;
      }
   }

   private static record TargetFaceKey(ResourceKey<Level> dimension, long posLong, Direction face) {
   }
}
