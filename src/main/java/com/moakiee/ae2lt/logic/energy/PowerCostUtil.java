package com.moakiee.ae2lt.logic.energy;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import org.jetbrains.annotations.Nullable;

public final class PowerCostUtil {
   public static final double AE_PER_OPERATION = 1.0;
   private static final double RESERVE_TICKS = 1.0;

   private PowerCostUtil() {
   }

   public static double idleReserve(IGrid grid) {
      return idleReserveForIdlePowerUsage(grid.getEnergyService().getIdlePowerUsage());
   }

   private static double idleReserveForIdlePowerUsage(double idlePowerUsage) {
      return Math.max(0.0, idlePowerUsage) * 1.0;
   }

   public static double cost(AEKey key, long amount) {
      return key != null && amount > 0L ? OverloadedIoCost.cost(amount, amountPerOperation(key)) * 1.0 : 0.0;
   }

   public static double totalCost(KeyCounter[] inputs) {
      if (inputs == null) {
         return 0.0;
      } else {
         double total = 0.0;

         for (KeyCounter counter : inputs) {
            if (counter != null) {
               for (Entry<AEKey> entry : counter) {
                  total += cost((AEKey)entry.getKey(), entry.getLongValue());
               }
            }
         }

         return total;
      }
   }

   public static long maxAffordable(@Nullable IGrid grid, AEKey key, long requested) {
      if (grid != null && key != null && requested > 0L) {
         double need = cost(key, requested);
         if (need <= 0.0) {
            return requested;
         } else {
            IEnergyService energyService = grid.getEnergyService();
            double reserve = idleReserveForIdlePowerUsage(energyService.getIdlePowerUsage());
            double available = energyService.extractAEPower(need + reserve, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            double usable = available - reserve;
            if (usable + 1.0E-6 >= need) {
               return requested;
            } else {
               long affordableOps = (long)Math.floor(usable / 1.0);
               return affordableOps <= 0L ? 0L : OverloadedIoCost.amountForOperations(requested, amountPerOperation(key), affordableOps);
            }
         }
      } else {
         return 0L;
      }
   }

   private static long amountPerOperation(AEKey key) {
      if (AEItemKey.is(key)) {
         return 4L;
      } else {
         return AEFluidKey.is(key) ? 500L : Math.max(1L, (long)key.getAmountPerOperation());
      }
   }

   public static void consume(@Nullable IGrid grid, AEKey key, long amount) {
      if (grid != null && key != null && amount > 0L) {
         double need = cost(key, amount);
         if (!(need <= 0.0)) {
            grid.getEnergyService().extractAEPower(need, Actionable.MODULATE, PowerMultiplier.CONFIG);
         }
      }
   }

   public static boolean canAfford(@Nullable IGrid grid, double need) {
      if (need <= 0.0) {
         return true;
      } else if (grid == null) {
         return false;
      } else {
         IEnergyService energyService = grid.getEnergyService();
         double total = need + idleReserveForIdlePowerUsage(energyService.getIdlePowerUsage());
         double available = energyService.extractAEPower(total, Actionable.SIMULATE, PowerMultiplier.CONFIG);
         return available + 1.0E-6 >= total;
      }
   }

   public static void consumeRaw(@Nullable IGrid grid, double need) {
      if (grid != null && !(need <= 0.0)) {
         grid.getEnergyService().extractAEPower(need, Actionable.MODULATE, PowerMultiplier.CONFIG);
      }
   }
}
