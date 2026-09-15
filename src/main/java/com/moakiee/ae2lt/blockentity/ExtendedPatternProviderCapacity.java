package com.moakiee.ae2lt.blockentity;

public final class ExtendedPatternProviderCapacity {
   public static final int DEFAULT_PAGES = 4;
   public static final int MAX_PAGES = 64;
   public static final int SLOTS_PER_PAGE = 36;

   private ExtendedPatternProviderCapacity() {
   }

   public static int clampPages(int pages) {
      return Math.max(1, Math.min(64, pages));
   }

   public static int slotsForPages(int pages) {
      return clampPages(pages) * 36;
   }
}
