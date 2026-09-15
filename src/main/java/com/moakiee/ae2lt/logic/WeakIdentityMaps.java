package com.moakiee.ae2lt.logic;

import com.google.common.collect.MapMaker;
import java.util.Map;

final class WeakIdentityMaps {
   static <K, V> Map<K, V> weakKeys() {
      return new MapMaker().weakKeys().concurrencyLevel(1).makeMap();
   }

   static <K, V> Map<K, V> weakKeysAndValues() {
      return new MapMaker().weakKeys().weakValues().concurrencyLevel(1).makeMap();
   }

   private WeakIdentityMaps() {
   }
}
