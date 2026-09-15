package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.blockentity.TianshuSupercomputerPortBlockEntity;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class TianshuSeedRefillService {
   public static TianshuSeedRefillService.RefillResult refillAll(TianshuSupercomputerPortBlockEntity target) {
      if (target == null) {
         return TianshuSeedRefillService.RefillResult.UNAVAILABLE;
      } else {
         ClosedLoopPatternRepository repository = target.getClosedLoopPatternRepository();
         if (repository == null) {
            return TianshuSeedRefillService.RefillResult.UNAVAILABLE;
         } else {
            LinkedHashMap<AEKey, Long> required = new LinkedHashMap<>();

            for (ClosedLoopPatternPayload payload : repository.patterns()) {
               if (payload.enabled()) {
                  for (Entry<AEKey, Long> entry : requirements(payload).entrySet()) {
                     required.merge(entry.getKey(), entry.getValue(), Math::max);
                  }
               }
            }

            return refill(target, required);
         }
      }
   }

   public static Map<AEKey, Long> requirements(ClosedLoopPatternPayload payload) {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();
      if (payload != null) {
         for (GenericStack seed : payload.seeds()) {
            long perTask = Sat.mul(seed.amount(), (long)payload.executionSeedMultiplier());
            result.merge(seed.what(), Long.valueOf(Sat.mul(perTask, (long)payload.storedTaskMultiplier())), Sat::add);
         }
      }

      return Map.copyOf(result);
   }

   private static TianshuSeedRefillService.RefillResult refill(TianshuSupercomputerPortBlockEntity target, Map<AEKey, Long> required) {
      IGrid grid = target.getGrid();
      if (target.isFormed() && grid != null && target.getFunctionProfile().supportsClosedLoopSeeds()) {
         LinkedHashMap<AEKey, Long> moved = new LinkedHashMap<>();
         LinkedHashMap<AEKey, Long> networkMissing = new LinkedHashMap<>();
         LinkedHashMap<AEKey, Long> storageBlocked = new LinkedHashMap<>();
         MEStorage network = grid.getStorageService().getInventory();

         for (Entry<AEKey, Long> entry : required.entrySet()) {
            long need = Math.max(0L, entry.getValue() - target.reusableSeedAmount(entry.getKey()));
            if (need > 0L) {
               long canStore = target.insertReusableSeed(entry.getKey(), need, Actionable.SIMULATE);
               long extracted = canStore > 0L ? network.extract(entry.getKey(), canStore, Actionable.MODULATE, target.getActionSource()) : 0L;
               long inserted = extracted > 0L ? target.insertReusableSeed(entry.getKey(), extracted, Actionable.MODULATE) : 0L;
               if (inserted < extracted) {
                  network.insert(entry.getKey(), extracted - inserted, Actionable.MODULATE, target.getActionSource());
               }

               if (inserted > 0L) {
                  moved.put(entry.getKey(), Long.valueOf(inserted));
               }

               long unavailableFromNetwork = Math.max(0L, canStore - extracted);
               long rejectedByStorage = Math.max(0L, need - canStore) + Math.max(0L, extracted - inserted);
               if (unavailableFromNetwork > 0L) {
                  networkMissing.put(entry.getKey(), Long.valueOf(unavailableFromNetwork));
               }

               if (rejectedByStorage > 0L) {
                  storageBlocked.put(entry.getKey(), Long.valueOf(rejectedByStorage));
               }
            }
         }

         return new TianshuSeedRefillService.RefillResult(true, Map.copyOf(moved), Map.copyOf(networkMissing), Map.copyOf(storageBlocked));
      } else {
         return TianshuSeedRefillService.RefillResult.UNAVAILABLE;
      }
   }

   private TianshuSeedRefillService() {
   }

   public static record RefillResult(boolean available, Map<AEKey, Long> moved, Map<AEKey, Long> networkMissing, Map<AEKey, Long> storageBlocked) {
      private static final TianshuSeedRefillService.RefillResult UNAVAILABLE = new TianshuSeedRefillService.RefillResult(false, Map.of(), Map.of(), Map.of());

      public boolean complete() {
         return this.available && this.networkMissing.isEmpty() && this.storageBlocked.isEmpty();
      }
   }
}
