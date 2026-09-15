package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ClosedLoopCycleKeys {
   static Set<AEKey> analyze(List<IPatternDetails> members, Collection<AEKey> seeds) {
      HashMap<AEKey, Set<AEKey>> forward = new HashMap<>();
      HashMap<AEKey, Set<AEKey>> reverse = new HashMap<>();

      for (IPatternDetails details : members) {
         LinkedHashSet<AEKey> explicitOutputs = new LinkedHashSet<>();

         for (GenericStack output : details.getOutputs()) {
            if (output.what() != null) {
               explicitOutputs.add(output.what());
            }
         }

         for (IInput input : details.getInputs()) {
            for (GenericStack candidate : input.getPossibleInputs()) {
               AEKey consumed = candidate.what();
               if (consumed != null) {
                  forward.computeIfAbsent(consumed, ignored -> new LinkedHashSet<>());
                  reverse.computeIfAbsent(consumed, ignored -> new LinkedHashSet<>());

                  for (AEKey outputx : explicitOutputs) {
                     forward.computeIfAbsent(consumed, ignored -> new LinkedHashSet<>()).add(outputx);
                     reverse.computeIfAbsent(outputx, ignored -> new LinkedHashSet<>()).add(consumed);
                     forward.computeIfAbsent(outputx, ignored -> new LinkedHashSet<>());
                  }

                  AEKey remainder = input.getRemainingKey(consumed);
                  if (remainder != null) {
                     forward.computeIfAbsent(consumed, ignored -> new LinkedHashSet<>()).add(remainder);
                     reverse.computeIfAbsent(remainder, ignored -> new LinkedHashSet<>()).add(consumed);
                     forward.computeIfAbsent(remainder, ignored -> new LinkedHashSet<>());
                  }
               }
            }
         }
      }

      LinkedHashSet<AEKey> result = new LinkedHashSet<>();

      for (AEKey seed : seeds) {
         if (seed != null) {
            Set<AEKey> reachable = reachable(seed, forward);
            Set<AEKey> canReturn = reachable(seed, reverse);
            reachable.retainAll(canReturn);
            result.addAll(reachable);
            result.add(seed);
         }
      }

      return Set.copyOf(result);
   }

   private static Set<AEKey> reachable(AEKey start, Map<AEKey, Set<AEKey>> graph) {
      HashSet<AEKey> result = new HashSet<>();
      ArrayDeque<AEKey> queue = new ArrayDeque<>();
      result.add(start);
      queue.add(start);

      while (!queue.isEmpty()) {
         AEKey current = queue.removeFirst();

         for (AEKey next : graph.getOrDefault(current, Set.of())) {
            if (result.add(next)) {
               queue.addLast(next);
            }
         }
      }

      return result;
   }

   private ClosedLoopCycleKeys() {
   }
}
