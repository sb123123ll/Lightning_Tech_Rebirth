package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import com.moakiee.thunderbolt.core.crafting.batch.SharedBatchInputPattern;
import com.moakiee.thunderbolt.core.crafting.loop.ClosedLoopBatchPatternDetails;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class SingleSeedClosedLoopMolecularPatternDetails
   extends ClosedLoopMolecularPatternDetails
   implements SharedBatchInputPattern,
   ClosedLoopBatchPatternDetails {
   SingleSeedClosedLoopMolecularPatternDetails(
      IMolecularAssemblerSupportedPattern molecular,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      Map<Integer, AEKey> plannedSeedInputSlots,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      super(molecular, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder);
   }

   public boolean isSharedBatchInput(int slot, AEKey concreteKey) {
      return this.isReusableSeedInput(slot, concreteKey);
   }

   @Override
   public long sharedBatchOutputAmount(AEKey outputKey) {
      return super.sharedBatchOutputAmount(outputKey);
   }
}
