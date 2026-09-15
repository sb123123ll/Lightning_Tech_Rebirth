package com.moakiee.ae2lt.logic.tianshu.maintenance;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MaintenanceTopologyService {
   private static final int MAX_DEPTH = 32;
   private static final int MAX_KEYS = 2049;

   public static List<MaintenanceTopologyService.Entry> build(ICraftingService crafting, AEKey target) {
      return crafting == null ? List.of() : build(crafting::getCraftingFor, crafting::canEmitFor, target);
   }

   public static List<MaintenanceTopologyService.Entry> build(Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor, AEKey target) {
      return build(patternsFor, ignored -> false, target);
   }

   public static List<MaintenanceTopologyService.Entry> build(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor, Predicate<AEKey> emittable, AEKey target
   ) {
      if (patternsFor != null && target != null) {
         LinkedHashMap<AEKey, Integer> depths = new LinkedHashMap<>();
         LinkedHashMap<AEKey, Boolean> craftable = new LinkedHashMap<>();
         ArrayDeque<AEKey> queue = new ArrayDeque<>();
         depths.put(target, Integer.valueOf(0));
         queue.add(target);

         while (!queue.isEmpty() && depths.size() < 2049) {
            AEKey key = queue.removeFirst();
            int depth = depths.get(key);
            if (depth < 32) {
               boolean hasPattern = emittable != null && emittable.test(key);
               if (!hasPattern) {
                  Iterable<IPatternDetails> patterns = (Iterable<IPatternDetails>)patternsFor.apply(key);
                  if (patterns != null) {
                     for (IPatternDetails pattern : patterns) {
                        if (produces(pattern, key)) {
                           hasPattern = true;

                           for (IInput input : pattern.getInputs()) {
                              for (GenericStack possible : input.getPossibleInputs()) {
                                 AEKey inputKey = possible.what();
                                 if (inputKey != null && !depths.containsKey(inputKey)) {
                                    depths.put(inputKey, Integer.valueOf(depth + 1));
                                    queue.addLast(inputKey);
                                    if (depths.size() >= 2049) {
                                       break;
                                    }
                                 }
                              }

                              if (depths.size() >= 2049) {
                                 break;
                              }
                           }

                           if (depths.size() >= 2049) {
                              break;
                           }
                        }
                     }
                  }
               }

               craftable.put(key, Boolean.valueOf(hasPattern));
            }
         }

         ArrayList<MaintenanceTopologyService.Entry> result = new ArrayList<>(depths.size());

         for (Map.Entry<AEKey, Integer> entry : depths.entrySet()) {
            result.add(new MaintenanceTopologyService.Entry(entry.getKey(), entry.getValue(), craftable.getOrDefault(entry.getKey(), false)));
         }

         return List.copyOf(result);
      } else {
         return List.of();
      }
   }

   private static boolean produces(IPatternDetails pattern, AEKey key) {
      if (pattern == null) {
         return false;
      } else {
         for (GenericStack output : pattern.getOutputs()) {
            if (output != null && output.amount() > 0L && key.equals(output.what())) {
               return true;
            }
         }

         return false;
      }
   }

   private MaintenanceTopologyService() {
   }

   public static record Entry(AEKey key, int depth, boolean craftable) {
   }
}
