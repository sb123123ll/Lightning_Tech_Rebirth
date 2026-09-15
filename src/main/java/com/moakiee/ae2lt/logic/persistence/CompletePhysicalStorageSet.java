package com.moakiee.ae2lt.logic.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CompletePhysicalStorageSet {
   public static <P, T> Optional<List<T>> resolve(List<P> positions, Function<P, T> resolver) {
      if (positions != null && resolver != null) {
         ArrayList<T> result = new ArrayList<>(positions.size());

         for (P position : positions) {
            T storage = resolver.apply(position);
            if (storage == null) {
               return Optional.empty();
            }

            result.add(storage);
         }

         return Optional.of(List.copyOf(result));
      } else {
         return Optional.empty();
      }
   }

   private CompletePhysicalStorageSet() {
   }
}
