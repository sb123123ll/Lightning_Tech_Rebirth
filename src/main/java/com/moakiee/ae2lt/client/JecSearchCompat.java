package com.moakiee.ae2lt.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class JecSearchCompat {
   private static final String MATCH_CLASS = "me.towdium.jecharacters.utils.Match";
   private static volatile MethodHandle containsMethod = findContainsMethod();

   private JecSearchCompat() {
   }

   static boolean contains(String text, String query) {
      MethodHandle method = containsMethod;
      if (method != null && text != null && query != null && !query.isBlank()) {
         try {
            return (boolean)method.invokeExact((String)text, (CharSequence)query);
         } catch (Throwable var4) {
            containsMethod = null;
            return false;
         }
      } else {
         return false;
      }
   }

   private static MethodHandle findContainsMethod() {
      try {
         Class<?> matchClass = Class.forName("me.towdium.jecharacters.utils.Match");
         return MethodHandles.publicLookup().findStatic(matchClass, "contains", MethodType.methodType(boolean.class, String.class, CharSequence.class));
      } catch (LinkageError | ReflectiveOperationException var1) {
         return null;
      }
   }
}
