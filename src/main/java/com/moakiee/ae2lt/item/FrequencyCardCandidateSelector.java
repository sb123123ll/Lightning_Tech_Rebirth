package com.moakiee.ae2lt.item;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FrequencyCardCandidateSelector {
   private FrequencyCardCandidateSelector() {
   }

   public static <T> FrequencyCardCandidateSelector.Selection<T> select(List<FrequencyCardCandidateSelector.Candidate<T>> candidates) {
      if (candidates != null && !candidates.isEmpty()) {
         Optional<FrequencyCardCandidateSelector.Candidate<T>> best = candidates.stream()
            .min(Comparator.comparingInt(candidate -> candidate.source().priority()));
         if (best.isEmpty()) {
            return FrequencyCardCandidateSelector.Selection.empty();
         } else {
            int bestPriority = best.get().source().priority();
            long bestCount = candidates.stream().filter(candidate -> candidate.source().priority() == bestPriority).count();
            return bestCount > 1L
               ? FrequencyCardCandidateSelector.Selection.withAmbiguity()
               : FrequencyCardCandidateSelector.Selection.selected(best.get().value());
         }
      } else {
         return FrequencyCardCandidateSelector.Selection.empty();
      }
   }

   public static record Candidate<T>(FrequencyCardCandidateSelector.Source source, T value) {
   }

   public static record Selection<T>(Optional<T> selected, boolean ambiguous) {
      private static <T> FrequencyCardCandidateSelector.Selection<T> empty() {
         return new FrequencyCardCandidateSelector.Selection<>(Optional.empty(), false);
      }

      private static <T> FrequencyCardCandidateSelector.Selection<T> withAmbiguity() {
         return new FrequencyCardCandidateSelector.Selection<>(Optional.empty(), true);
      }

      private static <T> FrequencyCardCandidateSelector.Selection<T> selected(T value) {
         return new FrequencyCardCandidateSelector.Selection<>(Optional.of(value), false);
      }
   }

   public static enum Source {
      MAIN_HAND(0),
      OFF_HAND(1),
      CURIOS(2),
      HOTBAR(3),
      BACKPACK(4),
      WIRELESS_TERMINAL(5);

      private final int priority;

      private Source(int priority) {
         this.priority = priority;
      }

      int priority() {
         return this.priority;
      }
   }
}
