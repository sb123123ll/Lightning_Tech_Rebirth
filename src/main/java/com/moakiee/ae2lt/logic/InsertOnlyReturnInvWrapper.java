package com.moakiee.ae2lt.logic;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

public class InsertOnlyReturnInvWrapper implements GenericInternalInventory {
   private final UnlimitedReturnInventory delegate;
   private final OverloadedPatternProviderLogic logic;

   public InsertOnlyReturnInvWrapper(UnlimitedReturnInventory delegate, OverloadedPatternProviderLogic logic) {
      this.delegate = delegate;
      this.logic = logic;
   }

   public int size() {
      return this.delegate.size();
   }

   @Nullable
   public GenericStack getStack(int slot) {
      return null;
   }

   @Nullable
   public AEKey getKey(int slot) {
      return null;
   }

   public long getAmount(int slot) {
      return 0L;
   }

   public long getMaxAmount(AEKey key) {
      return Long.MAX_VALUE;
   }

   public long getCapacity(AEKeyType space) {
      return Long.MAX_VALUE;
   }

   public boolean canInsert() {
      return true;
   }

   public boolean canExtract() {
      return false;
   }

   public boolean isAllowed(AEKey what) {
      return what != null && this.delegate.isAllowed(what);
   }

   public long insert(int slot, AEKey what, long amount, Actionable mode) {
      if (what != null && amount > 0L) {
         long affordable = this.logic.maxAffordableExternalReturn(what, amount);
         if (affordable <= 0L) {
            return 0L;
         } else {
            long inserted = this.delegate.insert(slot, what, affordable, mode);
            if (inserted > 0L && mode == Actionable.MODULATE) {
               this.logic.consumeExternalReturnPower(what, inserted);
            }

            return inserted;
         }
      } else {
         return 0L;
      }
   }

   public long extract(int slot, AEKey what, long amount, Actionable mode) {
      return 0L;
   }

   public void setStack(int slot, @Nullable GenericStack stack) {
      this.delegate.setStack(slot, stack);
   }

   public void beginBatch() {
      this.delegate.beginBatch();
   }

   public void endBatch() {
      this.delegate.endBatch();
   }

   public void endBatchSuppressed() {
      this.delegate.endBatchSuppressed();
   }

   public void onChange() {
      this.delegate.onChange();
   }
}
