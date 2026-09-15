package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.moakiee.thunderbolt.core.crafting.batch.SharedBatchInputPattern;
import com.moakiee.thunderbolt.core.crafting.loop.ClosedLoopBatchPatternDetails;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class SingleSeedClosedLoopPatternDetails extends ClosedLoopExpandedPatternDetails implements SharedBatchInputPattern, ClosedLoopBatchPatternDetails {
   SingleSeedClosedLoopPatternDetails(
      IPatternDetails delegate,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      Map<Integer, AEKey> plannedSeedInputSlots,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      super(delegate, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder);
   }

   public boolean isSharedBatchInput(int slot, AEKey concreteKey) {
      return this.isReusableSeedInput(slot, concreteKey);
   }

   @Override
   public long sharedBatchOutputAmount(AEKey outputKey) {
      return super.sharedBatchOutputAmount(outputKey);
   }
}
