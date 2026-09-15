package com.moakiee.ae2lt.logic.tianshu.maintenance;

final class FixedPeriodPollingSchedule {
   private final int period;
   private long credit;
   private int cursor;

   FixedPeriodPollingSchedule(int period) {
      if (period <= 0) {
         throw new IllegalArgumentException("period must be positive");
      } else {
         this.period = period;
      }
   }

   int checksThisTick(int ruleCount) {
      if (ruleCount <= 0) {
         return 0;
      } else {
         this.credit += (long)ruleCount;
         int checks = (int)(this.credit / (long)this.period);
         this.credit = this.credit % (long)this.period;
         return checks;
      }
   }

   int nextIndex(int ruleCount) {
      if (ruleCount <= 0) {
         throw new IllegalArgumentException("ruleCount must be positive");
      } else {
         if (this.cursor >= ruleCount) {
            this.cursor = 0;
         }

         int result = this.cursor++;
         if (this.cursor >= ruleCount) {
            this.cursor = 0;
         }

         return result;
      }
   }

   void reset() {
      this.credit = 0L;
      this.cursor = 0;
   }
}
