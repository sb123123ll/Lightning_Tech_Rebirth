package com.moakiee.ae2lt.logic.craft;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.moakiee.thunderbolt.api.crafting.batch.BatchDispatchMode;
import com.moakiee.thunderbolt.api.crafting.batch.IBatchCraftingProvider;
import java.util.List;
import java.util.Objects;

public final class MatrixMultiblockInterface implements IBatchCraftingProvider {
   private final MatrixCraftingCluster cluster;
   private final MatrixPatternRepository patternRepository;

   public MatrixMultiblockInterface(MatrixCraftingCluster cluster, MatrixPatternRepository patternRepository) {
      this.cluster = Objects.requireNonNull(cluster);
      this.patternRepository = Objects.requireNonNull(patternRepository);
      this.cluster.addPatternCore(patternRepository);
   }

   public MatrixMultiblockPortRole role() {
      return MatrixMultiblockPortRole.INTERFACE;
   }

   public MatrixPatternRepository patternRepository() {
      return this.patternRepository;
   }

   public boolean insertPattern(IPatternDetails pattern) {
      return this.patternRepository.insert(pattern);
   }

   public List<IPatternDetails> insertPatterns(List<? extends IPatternDetails> patterns) {
      return this.patternRepository.insertAll(patterns);
   }

   public MatrixPatternStorageUnit exposedPatternUnit() {
      return this.patternRepository.exposedUnit();
   }

   public MatrixPatternRepository.UpgradeResult upgradeT1PatternUnits(int maxUpgrades) {
      return this.patternRepository.upgradeT1Units(maxUpgrades);
   }

   public List<IPatternDetails> getAvailablePatterns() {
      return this.cluster.getAvailablePatterns();
   }

   public boolean isBusy() {
      return this.cluster.isBusy();
   }

   public long getBatchCapacity(IPatternDetails details) {
      return this.cluster.getBatchCapacity(details);
   }

   public BatchDispatchMode getBatchDispatchMode(IPatternDetails details) {
      return this.cluster.batchDispatchMode();
   }

   public long pushBatch(IPatternDetails details, KeyCounter[] oneCopyTemplate, long maxCraft) {
      return this.cluster.pushBatch(details, oneCopyTemplate, maxCraft);
   }
}
