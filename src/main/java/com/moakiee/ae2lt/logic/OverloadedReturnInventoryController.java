package com.moakiee.ae2lt.logic;

import appeng.api.stacks.GenericStack;
import java.util.Objects;

final class OverloadedReturnInventoryController {
   private static final int PATTERNS_PER_PAGE = 36;
   private static final int RETURN_SLOTS_PER_PAGE = 9;
   private final UnlimitedReturnInventory full;
   private final UnlimitedReturnInventory pageView;
   private final GenericStack[] pageSnapshot = new GenericStack[9];
   private final int totalPages;
   private boolean syncing;
   private int currentPage;

   OverloadedReturnInventoryController(int patternCapacity, Runnable changeListener, ReturnSlotFilter filter) {
      this.totalPages = Math.max(1, (patternCapacity + 36 - 1) / 36);
      int fullSlots = this.totalPages * 9;
      this.full = fullSlots > 9 ? UnlimitedReturnInventory.create(changeListener, filter, fullSlots) : UnlimitedReturnInventory.create(changeListener, filter);
      this.pageView = UnlimitedReturnInventory.create(() -> {
         if (!this.syncing) {
            this.copyPageToFull();
            changeListener.run();
         }
      }, filter);
   }

   UnlimitedReturnInventory full() {
      return this.full;
   }

   UnlimitedReturnInventory pageView() {
      return this.pageView;
   }

   int currentPage() {
      return this.currentPage;
   }

   int totalPages() {
      return this.totalPages;
   }

   void setCurrentPage(int page) {
      int bounded = Math.max(0, Math.min(page, this.totalPages - 1));
      if (bounded != this.currentPage) {
         this.copyPageToFull();
         this.currentPage = bounded;
         this.copyFullToPage();
      }
   }

   void copyFullToPage() {
      this.syncing = true;

      try {
         int offset = this.currentPage * 9;

         for (int i = 0; i < 9; i++) {
            int fullIndex = offset + i;
            GenericStack stack = fullIndex < this.full.size() ? this.full.getStack(fullIndex) : null;
            this.pageView.setStack(i, stack);
            this.pageSnapshot[i] = stack;
         }
      } finally {
         this.syncing = false;
      }
   }

   void copyPageToFull() {
      int offset = this.currentPage * 9;

      for (int i = 0; i < 9; i++) {
         int fullIndex = offset + i;
         if (fullIndex < this.full.size()) {
            GenericStack current = this.pageView.getStack(i);
            if (!Objects.equals(current, this.pageSnapshot[i])) {
               this.full.setStack(fullIndex, current);
               this.pageSnapshot[i] = current;
            }
         }
      }
   }
}
