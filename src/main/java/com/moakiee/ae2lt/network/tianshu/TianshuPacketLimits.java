package com.moakiee.ae2lt.network.tianshu;

import io.netty.handler.codec.DecoderException;

public final class TianshuPacketLimits {
   public static final int MAX_LIST_ENTRIES = 2048;

   private TianshuPacketLimits() {
   }

   public static int requireListSize(String field, int size) {
      if (size >= 0 && size <= 2048) {
         return size;
      } else {
         throw new IllegalArgumentException("Invalid " + field + " count " + size + " (maximum 2048)");
      }
   }

   public static int requireDecodedListSize(String field, int size) {
      if (size >= 0 && size <= 2048) {
         return size;
      } else {
         throw new DecoderException("Invalid " + field + " count " + size + " (maximum 2048)");
      }
   }
}
