package com.moakiee.ae2lt.celestweave.service;

import java.util.ArrayList;
import java.util.List;

final class ArmorEnergySpendPlan {
   private final boolean canPay;
   private final List<ArmorEnergySpendPlan.Debit> debits;

   private ArmorEnergySpendPlan(boolean canPay, List<ArmorEnergySpendPlan.Debit> debits) {
      this.canPay = canPay;
      this.debits = List.copyOf(debits);
   }

   static ArmorEnergySpendPlan create(long amount, List<ArmorEnergySpendPlan.Source> sources) {
      if (amount <= 0L) {
         return new ArmorEnergySpendPlan(true, List.of());
      } else {
         long remaining = amount;
         ArrayList<ArmorEnergySpendPlan.Debit> debits = new ArrayList<>();

         for (ArmorEnergySpendPlan.Source source : sources) {
            long available = Math.max(0L, source.stored());
            if (available > 0L) {
               long consumed = Math.min(available, remaining);
               debits.add(new ArmorEnergySpendPlan.Debit(source.index(), consumed));
               remaining -= consumed;
               if (remaining <= 0L) {
                  return new ArmorEnergySpendPlan(true, debits);
               }
            }
         }

         return new ArmorEnergySpendPlan(false, List.of());
      }
   }

   boolean canPay() {
      return this.canPay;
   }

   List<ArmorEnergySpendPlan.Debit> debits() {
      return this.debits;
   }

   static record Debit(int sourceIndex, long amount) {
   }

   static record Source(int index, long stored) {
   }
}
