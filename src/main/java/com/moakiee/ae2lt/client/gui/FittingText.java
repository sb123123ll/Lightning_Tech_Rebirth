package com.moakiee.ae2lt.client.gui;

import java.util.function.ToIntFunction;

public final class FittingText {
   private static final String ELLIPSIS = "...";

   private FittingText() {
   }

   public static String fit(String text, int maxWidth, ToIntFunction<String> width) {
      if (text != null && !text.isEmpty() && maxWidth > 0) {
         if (width.applyAsInt(text) <= maxWidth) {
            return text;
         } else if (width.applyAsInt("...") > maxWidth) {
            return "";
         } else {
            int lo = 0;
            int hi = text.length();

            while (lo < hi) {
               int mid = (lo + hi + 1) / 2;
               if (width.applyAsInt(text.substring(0, mid).stripTrailing() + "...") <= maxWidth) {
                  lo = mid;
               } else {
                  hi = mid - 1;
               }
            }

            return text.substring(0, lo).stripTrailing() + "...";
         }
      } else {
         return "";
      }
   }
}
