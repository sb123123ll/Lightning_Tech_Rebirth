package com.moakiee.ae2lt.logic.craft;

import appeng.api.crafting.IPatternDetails;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopExpandedPatternDetails;
import com.moakiee.thunderbolt.core.crafting.loop.ClosedLoopBatchPatternDetails;
import com.moakiee.thunderbolt.core.crafting.pattern.IWrappedPatternDetails;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Predicate;

public final class BatchPatternEligibility {
   private BatchPatternEligibility() {
   }

   public static boolean isEligible(IPatternDetails details) {
      return isEligible(
         details, candidate -> candidate instanceof ClosedLoopExpandedPatternDetails, candidate -> candidate instanceof ClosedLoopBatchPatternDetails
      );
   }

   static boolean isEligible(IPatternDetails details, Predicate<IPatternDetails> isClosedLoop, Predicate<IPatternDetails> isClosedLoopBatchSafe) {
      Set<IPatternDetails> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      IPatternDetails current = details;

      while (current != null && visited.add(current)) {
         if (isClosedLoop.test(current)) {
            return isClosedLoopBatchSafe.test(current);
         }

         if (!(current instanceof IWrappedPatternDetails wrapped)) {
            return current instanceof IMolecularAssemblerSupportedPattern || current.supportsPushInputsToExternalInventory();
         }

         current = wrapped.wrappedPatternDetails();
      }

      return false;
   }
}
