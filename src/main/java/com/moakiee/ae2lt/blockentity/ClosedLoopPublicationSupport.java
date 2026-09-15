package com.moakiee.ae2lt.blockentity;

import appeng.api.stacks.AEKey;
import com.moakiee.thunderbolt.core.crafting.loop.ReusableSeedPattern;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

final class ClosedLoopPublicationSupport {
   private ClosedLoopPublicationSupport() {
   }

   @Nullable
   static <T> T reusePublishedOrValidate(T candidate, Set<T> published, Supplier<T> validator) {
      Objects.requireNonNull(candidate, "candidate");
      Objects.requireNonNull(published, "published");
      Objects.requireNonNull(validator, "validator");
      return published.contains(candidate) ? candidate : validator.get();
   }

   static final class SeedSnapshotMemoizer implements Function<ReusableSeedPattern, Map<AEKey, Long>> {
      private final Function<ReusableSeedPattern, Map<AEKey, Long>> source;
      @Nullable
      private Map<AEKey, Long> cached;

      SeedSnapshotMemoizer(Function<ReusableSeedPattern, Map<AEKey, Long>> source) {
         this.source = Objects.requireNonNull(source, "source");
      }

      public Map<AEKey, Long> apply(ReusableSeedPattern pattern) {
         Map<AEKey, Long> result = this.cached;
         if (result == null) {
            result = Map.copyOf(Objects.requireNonNull(this.source.apply(pattern), "seed snapshot source result"));
            this.cached = result;
         }

         return result;
      }
   }
}
