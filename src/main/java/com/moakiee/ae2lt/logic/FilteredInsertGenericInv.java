package com.moakiee.ae2lt.logic;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public class FilteredInsertGenericInv implements GenericInternalInventory {
   private final GenericInternalInventory delegate;
   private final Predicate<AEKey> insertAllowed;

   public FilteredInsertGenericInv(GenericInternalInventory delegate, Predicate<AEKey> insertAllowed) {
      this.delegate = delegate;
      this.insertAllowed = insertAllowed;
   }

   public int size() {
      return this.delegate.size();
   }

   @Nullable
   public GenericStack getStack(int slot) {
      return this.delegate.getStack(slot);
   }

   @Nullable
   public AEKey getKey(int slot) {
      return this.delegate.getKey(slot);
   }

   public long getAmount(int slot) {
      return this.delegate.getAmount(slot);
   }

   public long getMaxAmount(AEKey key) {
      return this.delegate.getMaxAmount(key);
   }

   public long getCapacity(AEKeyType keyType) {
      return this.delegate.getCapacity(keyType);
   }

   public boolean canInsert() {
      return this.delegate.canInsert();
   }

   public boolean canExtract() {
      return this.delegate.canExtract();
   }

   public void setStack(int slot, @Nullable GenericStack newStack) {
      if (newStack == null || this.insertAllowed.test(newStack.what())) {
         this.delegate.setStack(slot, newStack);
      }
   }

   public boolean isAllowed(AEKey what) {
      return this.insertAllowed.test(what) && this.delegate.isAllowed(what);
   }

   public long insert(int slot, AEKey what, long amount, Actionable mode) {
      return what != null && this.insertAllowed.test(what) ? this.delegate.insert(slot, what, amount, mode) : 0L;
   }

   public long extract(int slot, AEKey what, long amount, Actionable mode) {
      return this.delegate.extract(slot, what, amount, mode);
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
