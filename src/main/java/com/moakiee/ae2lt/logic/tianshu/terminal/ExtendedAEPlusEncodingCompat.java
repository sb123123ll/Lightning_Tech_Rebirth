package com.moakiee.ae2lt.logic.tianshu.terminal;

import java.lang.reflect.Method;

public final class ExtendedAEPlusEncodingCompat {
   private static final String ARM_METHOD = "eap$clientSetShiftUpload";
   private static final String CONSUME_METHOD = "eap$consumeShiftUploadFlag";
   private static final ExtendedAEPlusEncodingCompat.Suppression NO_OP = () -> {
   };

   public static ExtendedAEPlusEncodingCompat.Suppression suppressAutomaticUpload(Object menu) {
      if (menu == null) {
         return NO_OP;
      } else {
         try {
            Method arm = menu.getClass().getMethod("eap$clientSetShiftUpload", boolean.class);
            Method consume = menu.getClass().getMethod("eap$consumeShiftUploadFlag");
            arm.invoke(menu, true);
            return () -> consumeQuietly(consume, menu);
         } catch (LinkageError | ReflectiveOperationException var3) {
            return NO_OP;
         }
      }
   }

   private static void consumeQuietly(Method consume, Object menu) {
      try {
         consume.invoke(menu);
      } catch (LinkageError | ReflectiveOperationException var3) {
      }
   }

   private ExtendedAEPlusEncodingCompat() {
   }

   @FunctionalInterface
   public interface Suppression extends AutoCloseable {
      @Override
      void close();
   }
}
