package com.moakiee.ae2lt.logic.tianshu.loop;

import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import java.util.Objects;

public record ClosedLoopMemberPattern(SourcePatternSnapshot pattern, long copiesPerCycle) {
   public ClosedLoopMemberPattern(SourcePatternSnapshot pattern, long copiesPerCycle) {
      pattern = Objects.requireNonNull(pattern, "pattern");
      if (copiesPerCycle < 1L) {
         throw new IllegalArgumentException("member copies per cycle must be positive");
      } else {
         this.pattern = pattern;
         this.copiesPerCycle = copiesPerCycle;
      }
   }
}
