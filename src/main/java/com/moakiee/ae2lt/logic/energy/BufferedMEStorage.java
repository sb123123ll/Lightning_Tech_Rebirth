package com.moakiee.ae2lt.logic.energy;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class BufferedMEStorage implements MEStorage {
   private static final int HISTORY_SIZE = 20;
   private final MEStorage delegate;
   @Nullable
   private final Supplier<MEStorage> cellSupplier;
   @Nullable
   private final Runnable cellPersistCallback;
   private final long[] consumptionHistory = new long[20];
   private long feBuffer;
   private boolean batchOnly;
   private int historyPointer;
   private int costMultiplier = 1;

   private static long saturatingAdd(long a, long b) {
      long r = a + b;
      return ((a ^ r) & (b ^ r)) < 0L ? Long.MAX_VALUE : r;
   }

   private static long saturatingMul(long a, long b) {
      if (a > 0L && b > 0L) {
         return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
      } else {
         return 0L;
      }
   }

   public BufferedMEStorage(MEStorage delegate) {
      this(delegate, null);
   }

   public BufferedMEStorage(MEStorage delegate, @Nullable Supplier<MEStorage> cellSupplier) {
      this(delegate, cellSupplier, null);
   }

   public BufferedMEStorage(MEStorage delegate, @Nullable Supplier<MEStorage> cellSupplier, @Nullable Runnable cellPersistCallback) {
      this.delegate = delegate;
      this.cellSupplier = cellSupplier;
      this.cellPersistCallback = cellPersistCallback;
   }

   public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (this.isFeKey(what) && amount > 0L) {
         AEKey feKey = AppFluxBridge.FE_KEY;
         if (feKey == null) {
            return 0L;
         } else {
            long needed = saturatingMul(amount, (long)this.costMultiplier);
            if (this.batchOnly) {
               return this.extractFromBuffer(needed, mode) / (long)this.costMultiplier;
            } else {
               MEStorage cell = this.resolveCell();
               if (cell != null) {
                  return this.extractThroughCell(cell, feKey, needed, mode, source) / (long)this.costMultiplier;
               } else {
                  return this.feBuffer > 0L
                     ? this.extractFromBuffer(needed, mode) / (long)this.costMultiplier
                     : this.delegate.extract(feKey, needed, mode, source) / (long)this.costMultiplier;
               }
            }
         }
      } else {
         return this.delegate.extract(what, amount, mode, source);
      }
   }

   public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
      if (this.isFeKey(what) && amount > 0L) {
         AEKey feKey = AppFluxBridge.FE_KEY;
         if (feKey == null) {
            return this.delegate.insert(what, amount, mode, source);
         } else {
            long credit = saturatingMul(amount, (long)this.costMultiplier);
            if (this.batchOnly) {
               if (mode == Actionable.MODULATE && credit > 0L) {
                  this.feBuffer = saturatingAdd(this.feBuffer, credit);
                  this.recordReturn(credit);
               }

               return credit / (long)this.costMultiplier;
            } else {
               MEStorage cell = this.resolveCell();
               long accepted;
               if (cell != null) {
                  long intoCell = cell.insert(feKey, credit, mode, source);
                  long overflow = credit - intoCell;
                  long intoDelegate = overflow > 0L ? this.delegate.insert(feKey, overflow, mode, source) : 0L;
                  accepted = saturatingAdd(intoCell, intoDelegate);
                  if (mode == Actionable.MODULATE && intoCell > 0L) {
                     this.recordReturn(intoCell);
                  }
               } else if (this.feBuffer > 0L) {
                  accepted = credit;
                  if (mode == Actionable.MODULATE && credit > 0L) {
                     this.feBuffer = saturatingAdd(this.feBuffer, credit);
                     this.recordReturn(credit);
                  }
               } else {
                  accepted = this.delegate.insert(feKey, credit, mode, source);
               }

               return accepted / (long)this.costMultiplier;
            }
         }
      } else {
         return this.delegate.insert(what, amount, mode, source);
      }
   }

   public Component getDescription() {
      return this.delegate.getDescription();
   }

   public long extractForDirectSend(long amount, IActionSource source) {
      AEKey feKey = AppFluxBridge.FE_KEY;
      if (feKey != null && amount > 0L) {
         long needed = saturatingMul(amount, (long)this.costMultiplier);
         MEStorage cell = this.resolveCell();
         if (cell != null) {
            long extracted = cell.extract(feKey, needed, Actionable.MODULATE, source);
            long credited = extracted / (long)this.costMultiplier;
            long consumed = credited * (long)this.costMultiplier;
            long remainder = extracted - consumed;
            if (remainder > 0L) {
               long returned = cell.insert(feKey, remainder, Actionable.MODULATE, source);
               if (returned < remainder) {
                  this.delegate.insert(feKey, remainder - returned, Actionable.MODULATE, source);
               }
            }

            if (consumed > 0L) {
               this.recordConsumption(consumed);
            }

            return credited;
         } else if (!this.batchOnly && this.feBuffer <= 0L) {
            return 0L;
         } else {
            long extractedx = Math.min(needed, this.feBuffer);
            long creditedx = extractedx / (long)this.costMultiplier;
            long consumedx = creditedx * (long)this.costMultiplier;
            this.feBuffer -= consumedx;
            if (consumedx > 0L) {
               this.recordConsumption(consumedx);
            }

            return creditedx;
         }
      } else {
         return 0L;
      }
   }

   public long returnFromDirectSend(long amount, IActionSource source) {
      AEKey feKey = AppFluxBridge.FE_KEY;
      if (feKey != null && amount > 0L) {
         long credit = saturatingMul(amount, (long)this.costMultiplier);
         MEStorage cell = this.resolveCell();
         long accepted;
         if (cell != null) {
            long intoCell = cell.insert(feKey, credit, Actionable.MODULATE, source);
            long overflow = credit - intoCell;
            long intoDelegate = overflow > 0L ? this.delegate.insert(feKey, overflow, Actionable.MODULATE, source) : 0L;
            accepted = saturatingAdd(intoCell, intoDelegate);
         } else if (!this.batchOnly && this.feBuffer <= 0L) {
            accepted = this.delegate.insert(feKey, credit, Actionable.MODULATE, source);
         } else {
            this.feBuffer = saturatingAdd(this.feBuffer, credit);
            accepted = credit;
         }

         if (accepted > 0L) {
            this.recordReturn(accepted);
         }

         return accepted / (long)this.costMultiplier;
      } else {
         return 0L;
      }
   }

   public long refillBudgetForDirectSend(long currentDemand) {
      if (currentDemand > 0L && this.resolveCell() != null) {
         long currentCost = saturatingMul(currentDemand, (long)this.costMultiplier);
         return saturatingAdd(currentCost, this.recentConsumptionBeforeCurrentTick());
      } else {
         return 0L;
      }
   }

   public void refillForDirectSend(long refillBudget, IActionSource source) {
      AEKey feKey = AppFluxBridge.FE_KEY;
      MEStorage cell = this.resolveCell();
      if (feKey != null && refillBudget > 0L && cell != null) {
         this.refillFromDelegateInline(cell, feKey, refillBudget, source);
      }
   }

   public long beginMemoryBatch(AEKey feKey, long demand, IActionSource source) {
      if (demand <= 0L) {
         return 0L;
      } else {
         this.batchOnly = true;
         MEStorage cell = this.resolveCell();
         if (cell != null) {
            long pulled = cell.extract(feKey, demand, Actionable.MODULATE, source);
            if (pulled < demand) {
               long shortfall = demand - pulled;
               long ahead = this.recentConsumptionBeforeCurrentTick();
               long ideal = saturatingAdd(shortfall, ahead);
               this.refillFromDelegateInline(cell, feKey, ideal, source);
               long retry = cell.extract(feKey, shortfall, Actionable.MODULATE, source);
               pulled += retry;
            }

            if (pulled > 0L) {
               this.feBuffer = saturatingAdd(this.feBuffer, pulled);
            }

            return pulled;
         } else {
            long pulledx = this.delegate.extract(feKey, demand, Actionable.MODULATE, source);
            if (pulledx > 0L) {
               this.feBuffer = saturatingAdd(this.feBuffer, pulledx);
            }

            return pulledx;
         }
      }
   }

   public long endBatch(AEKey feKey, IActionSource source) {
      long var12;
      try {
         if (this.feBuffer <= 0L) {
            return 0L;
         }

         MEStorage cell = this.resolveCell();
         if (cell == null) {
            long returned = this.delegate.insert(feKey, this.feBuffer, Actionable.MODULATE, source);
            this.feBuffer -= returned;
            return returned;
         }

         long intoCell = cell.insert(feKey, this.feBuffer, Actionable.MODULATE, source);
         long overflow = this.feBuffer - intoCell;
         long intoDelegate = overflow > 0L ? this.delegate.insert(feKey, overflow, Actionable.MODULATE, source) : 0L;
         long absorbed = intoCell + intoDelegate;
         this.feBuffer -= absorbed;
         var12 = absorbed;
      } finally {
         this.batchOnly = false;
      }

      return var12;
   }

   public long flush(AEKey feKey, IActionSource source) {
      if (this.feBuffer <= 0L) {
         return 0L;
      } else {
         MEStorage cell = this.resolveCell();
         if (cell != null) {
            long intoCell = cell.insert(feKey, this.feBuffer, Actionable.MODULATE, source);
            long overflow = this.feBuffer - intoCell;
            long intoDelegate = overflow > 0L ? this.delegate.insert(feKey, overflow, Actionable.MODULATE, source) : 0L;
            long absorbed = intoCell + intoDelegate;
            this.feBuffer -= absorbed;
            return absorbed;
         } else {
            long returned = this.delegate.insert(feKey, this.feBuffer, Actionable.MODULATE, source);
            this.feBuffer -= returned;
            return returned;
         }
      }
   }

   public long endTick(AEKey feKey, IActionSource source) {
      long flushed = this.feBuffer > 0L ? this.flush(feKey, source) : 0L;
      if (this.cellPersistCallback != null && this.resolveCell() != null) {
         this.cellPersistCallback.run();
      }

      return flushed;
   }

   public long flushAll(AEKey feKey, IActionSource source) {
      long flushed = this.endTick(feKey, source);
      this.clearBuffer();
      return flushed;
   }

   public void advanceHistory() {
      this.historyPointer = (this.historyPointer + 1) % this.consumptionHistory.length;
      this.consumptionHistory[this.historyPointer] = 0L;
   }

   public void setCostMultiplier(int multiplier) {
      this.costMultiplier = Math.max(1, multiplier);
   }

   public long getBufferedEnergy() {
      MEStorage cell = this.resolveCell();
      if (cell != null) {
         AEKey feKey = AppFluxBridge.FE_KEY;
         return feKey == null ? 0L : cell.extract(feKey, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty());
      } else {
         return this.feBuffer;
      }
   }

   public void clearBuffer() {
      this.feBuffer = 0L;
   }

   private long extractThroughCell(MEStorage cell, AEKey feKey, long needed, Actionable mode, IActionSource source) {
      long extracted = cell.extract(feKey, needed, mode, source);
      if (extracted >= needed) {
         if (mode == Actionable.MODULATE && extracted > 0L) {
            this.recordConsumption(extracted);
         }

         return extracted;
      } else {
         if (mode == Actionable.MODULATE) {
            long shortfall = needed - extracted;
            long ahead = this.recentConsumptionBeforeCurrentTick();
            long ideal = saturatingAdd(shortfall, ahead);
            this.refillFromDelegateInline(cell, feKey, ideal, source);
            long retry = cell.extract(feKey, shortfall, Actionable.MODULATE, source);
            extracted += retry;
         }

         if (mode == Actionable.MODULATE && extracted > 0L) {
            this.recordConsumption(extracted);
         }

         return extracted;
      }
   }

   private void refillFromDelegateInline(MEStorage cell, AEKey feKey, long request, IActionSource source) {
      if (request > 0L) {
         long freeSpace = cell.insert(feKey, Long.MAX_VALUE, Actionable.SIMULATE, source);
         long refill = Math.min(request, freeSpace);
         if (refill > 0L) {
            long pulled = this.delegate.extract(feKey, refill, Actionable.MODULATE, source);
            if (pulled > 0L) {
               long inserted = cell.insert(feKey, pulled, Actionable.MODULATE, source);
               if (inserted < pulled) {
                  this.delegate.insert(feKey, pulled - inserted, Actionable.MODULATE, source);
               }
            }
         }
      }
   }

   private long extractFromBuffer(long needed, Actionable mode) {
      if (mode == Actionable.SIMULATE) {
         return Math.min(needed, this.feBuffer);
      } else {
         long extracted = Math.min(needed, this.feBuffer);
         this.feBuffer -= extracted;
         if (extracted > 0L) {
            this.recordConsumption(extracted);
         }

         return extracted;
      }
   }

   @Nullable
   private MEStorage resolveCell() {
      return this.cellSupplier != null ? this.cellSupplier.get() : null;
   }

   private long recentConsumptionBeforeCurrentTick() {
      long sum = 0L;

      for (int i = 0; i < this.consumptionHistory.length; i++) {
         if (i != this.historyPointer) {
            sum = saturatingAdd(sum, this.consumptionHistory[i]);
         }
      }

      return sum;
   }

   private void recordConsumption(long amount) {
      this.consumptionHistory[this.historyPointer] = saturatingAdd(this.consumptionHistory[this.historyPointer], amount);
   }

   private void recordReturn(long amount) {
      this.consumptionHistory[this.historyPointer] = Math.max(0L, this.consumptionHistory[this.historyPointer] - amount);
   }

   private boolean isFeKey(AEKey what) {
      AEKey feKey = AppFluxBridge.FE_KEY;
      return feKey != null && feKey.equals(what);
   }
}
