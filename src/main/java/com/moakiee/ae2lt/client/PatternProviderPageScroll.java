package com.moakiee.ae2lt.client;

public final class PatternProviderPageScroll {
   private PatternProviderPageScroll() {
   }

   public static PatternProviderPageScroll.Direction directionForDelta(double scrollY) {
      if (scrollY > 0.0) {
         return PatternProviderPageScroll.Direction.PREVIOUS;
      } else {
         return scrollY < 0.0 ? PatternProviderPageScroll.Direction.NEXT : PatternProviderPageScroll.Direction.NONE;
      }
   }

   public static enum Direction {
      PREVIOUS,
      NEXT,
      NONE;
   }
}
