package com.moakiee.ae2lt.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

final class FrequencyDeviceIndex<T> {
   private final Map<Integer, LinkedHashMap<FrequencyDeviceIndex.DeviceKey, T>> byFrequency = new HashMap<>();

   boolean put(int frequencyId, String dimensionId, long posLong, T entry) {
      LinkedHashMap<FrequencyDeviceIndex.DeviceKey, T> devices = this.byFrequency.computeIfAbsent(frequencyId, ignored -> new LinkedHashMap<>());
      FrequencyDeviceIndex.DeviceKey key = new FrequencyDeviceIndex.DeviceKey(dimensionId, posLong);
      T existing = devices.get(key);
      if (Objects.equals(existing, entry)) {
         return false;
      } else {
         devices.put(key, entry);
         return true;
      }
   }

   boolean remove(int frequencyId, String dimensionId, long posLong) {
      LinkedHashMap<FrequencyDeviceIndex.DeviceKey, T> devices = this.byFrequency.get(frequencyId);
      if (devices == null) {
         return false;
      } else {
         boolean removed = devices.remove(new FrequencyDeviceIndex.DeviceKey(dimensionId, posLong)) != null;
         if (devices.isEmpty()) {
            this.byFrequency.remove(frequencyId);
         }

         return removed;
      }
   }

   List<T> get(int frequencyId) {
      LinkedHashMap<FrequencyDeviceIndex.DeviceKey, T> devices = this.byFrequency.get(frequencyId);
      return (List<T>)(devices != null && !devices.isEmpty() ? new ArrayList<>(devices.values()) : List.of());
   }

   void clearFrequency(int frequencyId) {
      this.byFrequency.remove(frequencyId);
   }

   void clear() {
      this.byFrequency.clear();
   }

   void forEach(BiConsumer<Integer, T> consumer) {
      for (Entry<Integer, LinkedHashMap<FrequencyDeviceIndex.DeviceKey, T>> frequency : this.byFrequency.entrySet()) {
         for (T device : frequency.getValue().values()) {
            consumer.accept(frequency.getKey(), device);
         }
      }
   }

   private static record DeviceKey(String dimensionId, long posLong) {
   }
}
