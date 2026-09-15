package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.stacks.GenericStack;
import java.util.List;
import java.util.Objects;

public record ClosedLoopResultPage(int revision, ClosedLoopResultPage.Kind kind, int offset, int total, List<GenericStack> entries) {
   public static final int MAX_RESULTS = 243;
   public static final int PAGE_SIZE = 5;

   public ClosedLoopResultPage(int revision, ClosedLoopResultPage.Kind kind, int offset, int total, List<GenericStack> entries) {
      kind = Objects.requireNonNull(kind, "kind");
      entries = entries == null ? List.of() : entries;
      if (offset < 0 || offset >= 243) {
         throw new IllegalArgumentException("invalid closed-loop result offset: " + offset);
      } else if (total >= 0 && total <= 243) {
         if ((total != 0 || offset == 0) && (total <= 0 || offset < total)) {
            if (entries.size() <= 5 && offset + entries.size() <= total) {
               for (GenericStack entry : entries) {
                  if (entry == null || entry.what() == null || entry.amount() <= 0L) {
                     throw new IllegalArgumentException("invalid closed-loop result entry");
                  }
               }

               entries = List.copyOf(entries);
               this.revision = revision;
               this.kind = kind;
               this.offset = offset;
               this.total = total;
               this.entries = entries;
            } else {
               throw new IllegalArgumentException("invalid closed-loop result page size");
            }
         } else {
            throw new IllegalArgumentException("closed-loop result offset " + offset + " exceeds count " + total);
         }
      } else {
         throw new IllegalArgumentException("invalid closed-loop result count: " + total);
      }
   }

   public static ClosedLoopResultPage from(int revision, ClosedLoopResultPage.Kind kind, List<GenericStack> source, int requestedOffset) {
      source = source == null ? List.of() : source;
      int total = Math.min(243, source.size());
      int offset = total == 0 ? 0 : Math.max(0, Math.min(requestedOffset, total - 1));
      int end = Math.min(total, offset + 5);
      return new ClosedLoopResultPage(revision, kind, offset, total, source.subList(offset, end));
   }

   public static enum Kind {
      EXTERNAL_INPUTS,
      SEEDS;
   }
}
