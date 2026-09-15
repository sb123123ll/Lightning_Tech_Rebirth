package com.moakiee.ae2lt.logic;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public class DirectMEInsertInventory implements GenericInternalInventory {
   private final IManagedGridNode mainNode;
   private final IActionSource actionSource;
   @Nullable
   private Predicate<AEKey> filter;

   public DirectMEInsertInventory(IManagedGridNode mainNode, IActionSource actionSource) {
      this.mainNode = mainNode;
      this.actionSource = actionSource;
   }

   public void setFilter(@Nullable Predicate<AEKey> filter) {
      this.filter = filter;
   }

   public int size() {
      return 1;
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

   public boolean isSupportedType(AEKeyType type) {
      return true;
   }

   public boolean isAllowed(AEKey what) {
      return this.filter == null || this.filter.test(what);
   }

   public boolean isAllowedIn(int slot, AEKey what) {
      return this.isAllowed(what);
   }

   public long insert(int slot, AEKey what, long amount, Actionable mode) {
      if (this.filter != null && !this.filter.test(what)) {
         return 0L;
      } else {
         IGrid grid = this.mainNode.getGrid();
         return grid == null ? 0L : grid.getStorageService().getInventory().insert(what, amount, mode, this.actionSource);
      }
   }

   public long extract(int slot, AEKey what, long amount, Actionable mode) {
      return 0L;
   }

   public void setStack(int slot, @Nullable GenericStack stack) {
   }

   public void beginBatch() {
   }

   public void endBatch() {
   }

   public void endBatchSuppressed() {
   }

   public void onChange() {
   }
}
