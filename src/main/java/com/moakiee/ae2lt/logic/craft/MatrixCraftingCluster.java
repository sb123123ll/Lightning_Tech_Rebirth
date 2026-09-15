package com.moakiee.ae2lt.logic.craft;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.moakiee.ae2lt.crafting.matrix.core.CopyAssembler;
import com.moakiee.ae2lt.crafting.matrix.core.CraftingCore;
import com.moakiee.ae2lt.crafting.matrix.core.CraftingCoreHost;
import com.moakiee.ae2lt.crafting.matrix.core.CraftingCoreRegistry;
import com.moakiee.thunderbolt.api.crafting.batch.BatchDispatchMode;
import com.moakiee.thunderbolt.core.crafting.batch.BatchCopyLimitPattern;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

public final class MatrixCraftingCluster {
   private static final String NBT_HEAT = "heat";
   private static final String NBT_LAST_LIMITER_TICK = "lastLimiterTick";
   private static final String NBT_CONSUMED_OPERATIONS = "consumedOperations";
   private static final String NBT_LAST_OPERATIONS_PER_TICK = "lastOperationsPerTick";
   private final BooleanSupplier formed;
   private final List<MatrixPatternCore> patternCores;
   private final List<MatrixCraftCore> craftCores;
   private final MatrixCraftingCluster.MatrixHost host;
   private final CraftingCore engine;
   private final MatrixCraftingEnergy energy;
   private double heat;
   private long lastLimiterTick = Long.MIN_VALUE;
   private long limiterRemaining;
   private int providerCallsRemaining;
   private long operationsConsumedThisTick;
   private long lastOperationsPerTick;
   private MatrixCraftingMath.Snapshot lastLimiterSnapshot = MatrixCraftingMath.idleSnapshot(0.0, 0.0);

   public MatrixCraftingCluster(
      BooleanSupplier formed,
      List<? extends MatrixPatternCore> patternCores,
      List<? extends MatrixCraftCore> craftCores,
      CraftingCoreHost host,
      CopyAssembler assembler,
      CraftingCoreRegistry registry,
      MatrixCraftingEnergy energy
   ) {
      this.formed = Objects.requireNonNull(formed);
      this.patternCores = new ArrayList<>(patternCores);
      this.craftCores = new ArrayList<>(craftCores);
      this.host = new MatrixCraftingCluster.MatrixHost(Objects.requireNonNull(host));
      this.engine = new CraftingCore(this.host, assembler, registry);
      this.energy = Objects.requireNonNull(energy);
   }

   public void addPatternCore(MatrixPatternCore core) {
      if (core != null && !this.patternCores.contains(core)) {
         this.patternCores.add(core);
      }
   }

   public void removePatternCore(MatrixPatternCore core) {
      this.patternCores.remove(core);
   }

   public void addCraftCore(MatrixCraftCore core) {
      if (core != null && !this.craftCores.contains(core)) {
         this.craftCores.add(core);
      }
   }

   public void removeCraftCore(MatrixCraftCore core) {
      this.craftCores.remove(core);
   }

   public List<IPatternDetails> getAvailablePatterns() {
      if (!this.formed.getAsBoolean()) {
         return List.of();
      } else {
         IdentityHashMap<IPatternDetails, Boolean> seen = new IdentityHashMap<>();
         ArrayList<IPatternDetails> result = new ArrayList<>();

         for (MatrixPatternCore core : this.patternCores) {
            for (IPatternDetails pattern : core.getAvailablePatterns()) {
               if (MatrixPatternRepository.isSupportedPattern(pattern) && !seen.containsKey(pattern)) {
                  seen.put(pattern, Boolean.TRUE);
                  result.add(pattern);
               }
            }
         }

         return List.copyOf(result);
      }
   }

   public boolean hasPattern(IPatternDetails details) {
      if (this.formed.getAsBoolean() && MatrixPatternRepository.isSupportedPattern(details)) {
         for (MatrixPatternCore core : this.patternCores) {
            if (core.hasPattern(details)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public long getBatchCapacity(IPatternDetails details) {
      if (!this.hasPattern(details)) {
         return 0L;
      } else {
         this.refreshLimiterBudget();
         if (this.providerCallsRemaining <= 0) {
            return 0L;
         } else {
            long capacity = this.availableCapacity();
            capacity = Math.min(capacity, Math.max(0L, this.energy.affordableOperations(capacity)));
            if (details instanceof BatchCopyLimitPattern limited) {
               capacity = Math.min(capacity, Math.max(1L, limited.maxBatchCopies()));
            }

            return capacity;
         }
      }
   }

   public long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
      if (!this.hasPattern(details)) {
         return maxCraft;
      } else {
         long copies = Math.min(maxCraft, this.getBatchCapacity(details));
         if (copies <= 0L) {
            return maxCraft;
         } else {
            this.providerCallsRemaining--;
            long accepted = this.engine.pushBatch(details, oneCopyTemplate, copies);
            this.limiterRemaining = Math.max(0L, this.limiterRemaining - accepted);
            this.operationsConsumedThisTick = saturatedAdd(this.operationsConsumedThisTick, accepted);
            if (accepted > 0L) {
               this.energy.consumeOperations(accepted);
            }

            return maxCraft - accepted;
         }
      }
   }

   public boolean pushSingle(IPatternDetails details, KeyCounter[] oneCopyTemplate) {
      if (this.hasPattern(details) && this.availableCapacity() > 0L && this.providerCallsRemaining > 0 && this.energy.affordableOperations(1L) >= 1L) {
         this.providerCallsRemaining--;
         long accepted = this.engine.pushBatch(details, oneCopyTemplate, 1L);
         this.limiterRemaining = Math.max(0L, this.limiterRemaining - accepted);
         this.operationsConsumedThisTick = saturatedAdd(this.operationsConsumedThisTick, accepted);
         if (accepted == 1L) {
            this.energy.consumeOperations(1L);
         }

         return accepted == 1L;
      } else {
         return false;
      }
   }

   public BatchDispatchMode batchDispatchMode() {
      MatrixCraftingProfile profile = this.craftingProfile();
      return profile.isValid() && profile.mode() == MatrixCoreMode.MULTIDIMENSIONAL ? BatchDispatchMode.UNBOUNDED : BatchDispatchMode.NORMAL;
   }

   public boolean isBusy() {
      return !this.formed.getAsBoolean() || this.availableCapacity() <= 0L || this.providerCallsRemaining <= 0 || this.energy.affordableOperations(1L) < 1L;
   }

   public long availableCapacity() {
      if (!this.formed.getAsBoolean()) {
         return 0L;
      } else {
         this.refreshLimiterBudget();
         return this.limiterRemaining;
      }
   }

   public int availableProviderCalls() {
      if (!this.formed.getAsBoolean()) {
         return 0;
      } else {
         this.refreshLimiterBudget();
         return this.providerCallsRemaining;
      }
   }

   public long threadsInFlight() {
      return this.engine.threadsInFlight();
   }

   public MatrixCraftingProfile craftingProfile() {
      if (!this.formed.getAsBoolean()) {
         return MatrixCraftingProfile.empty();
      } else {
         ArrayList<MatrixCraftingUnit> units = new ArrayList<>();

         for (MatrixCraftCore core : this.craftCores) {
            if (core != null) {
               units.addAll(core.craftingUnits());
            }
         }

         return MatrixCraftingProfile.fromUnits(units);
      }
   }

   public MatrixCraftingMath.Snapshot tickLimiter() {
      this.refreshLimiterBudget();
      return this.lastLimiterSnapshot;
   }

   public MatrixCraftingMath.Snapshot previewSnapshot() {
      return this.craftingProfile().snapshot(this.heat);
   }

   public double heat() {
      return this.heat;
   }

   public long lastLimiterTick() {
      return this.lastLimiterTick;
   }

   public MatrixCraftingMath.Snapshot lastLimiterSnapshot() {
      return this.lastLimiterSnapshot;
   }

   public void writeEngineTo(CompoundTag tag, RegistryAccess registries) {
      this.engine.writeTo(tag, registries);
      tag.m_128347_("heat", this.heat);
      tag.m_128356_("lastLimiterTick", this.lastLimiterTick);
      tag.m_128356_("consumedOperations", this.operationsConsumedThisTick);
      tag.m_128356_("lastOperationsPerTick", this.lastOperationsPerTick);
   }

   public void readEngineFrom(CompoundTag tag, RegistryAccess registries) {
      this.engine.readFrom(tag, registries);
      this.heat = tag.m_128425_("heat", 6) ? tag.m_128459_("heat") : 0.0;
      this.lastLimiterTick = tag.m_128425_("lastLimiterTick", 4) ? tag.m_128454_("lastLimiterTick") : Long.MIN_VALUE;
      this.limiterRemaining = 0L;
      this.providerCallsRemaining = 0;
      this.operationsConsumedThisTick = tag.m_128425_("consumedOperations", 4) ? Math.max(0L, tag.m_128454_("consumedOperations")) : 0L;
      this.lastOperationsPerTick = tag.m_128425_("lastOperationsPerTick", 4) ? Math.max(0L, tag.m_128454_("lastOperationsPerTick")) : 0L;
      this.lastLimiterSnapshot = MatrixCraftingMath.idleSnapshot(this.heat, 0.0);
   }

   public void suspendRuntime() {
      this.engine.suspend();
      this.heat = 0.0;
      this.lastLimiterTick = Long.MIN_VALUE;
      this.limiterRemaining = 0L;
      this.providerCallsRemaining = 0;
      this.operationsConsumedThisTick = 0L;
      this.lastOperationsPerTick = 0L;
      this.lastLimiterSnapshot = MatrixCraftingMath.idleSnapshot(0.0, 0.0);
   }

   public void tryReleaseOutputs() {
      this.engine.drainAll(false);
   }

   public int totalThreadCapacity() {
      if (!this.formed.getAsBoolean()) {
         return 0;
      } else {
         long total = 0L;

         for (MatrixCraftCore core : this.craftCores) {
            int capacity = core.threadCapacity();
            if (capacity > 0) {
               total += (long)capacity;
               if (total >= 2147483647L) {
                  return Integer.MAX_VALUE;
               }
            }
         }

         return (int)total;
      }
   }

   private void refreshLimiterBudget() {
      long now = this.host.getGameTime();
      if (this.lastLimiterTick != now) {
         MatrixCraftingProfile profile = this.craftingProfile();
         if (this.lastLimiterTick != Long.MIN_VALUE) {
            this.heat = MatrixCraftingMath.advanceHeatForCompletedTick(
               this.heat, profile.mode(), this.operationsConsumedThisTick, this.lastOperationsPerTick, profile.threadPower(), profile.coolPower()
            );
         }

         this.operationsConsumedThisTick = 0L;
         this.lastLimiterSnapshot = profile.snapshot(this.heat);
         this.heat = this.lastLimiterSnapshot.heat();
         this.limiterRemaining = Math.max(0L, this.lastLimiterSnapshot.operationsPerTick());
         this.lastOperationsPerTick = this.limiterRemaining;
         this.providerCallsRemaining = 16384;
         this.lastLimiterTick = now;
      }
   }

   private static long saturatedAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }

   private final class MatrixHost implements CraftingCoreHost {
      private final CraftingCoreHost delegate;

      private MatrixHost(CraftingCoreHost delegate) {
         this.delegate = delegate;
      }

      @Override
      public long getGameTime() {
         return this.delegate.getGameTime();
      }

      @Override
      public boolean m_58901_() {
         return this.delegate.m_58901_();
      }

      @Override
      public boolean isConnected() {
         return MatrixCraftingCluster.this.formed.getAsBoolean() && this.delegate.isConnected();
      }

      @Override
      public long insertToNetwork(AEKey key, long amount) {
         return this.delegate.insertToNetwork(key, amount);
      }

      @Override
      public void spawnToWorld(AEKey key, long amount) {
         this.delegate.spawnToWorld(key, amount);
      }
   }
}
