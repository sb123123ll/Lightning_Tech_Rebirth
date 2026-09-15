package com.moakiee.ae2lt.overload.runtime.pattern;

import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import java.util.Objects;

public final class OverloadPatternPayload {
   private final PatternExecutionHostKind requiredHostKind;
   private final SourcePatternSnapshot sourcePattern;
   private final EncodedOverloadPattern encodedPattern;

   public OverloadPatternPayload(PatternExecutionHostKind requiredHostKind, SourcePatternSnapshot sourcePattern, EncodedOverloadPattern encodedPattern) {
      this.requiredHostKind = Objects.requireNonNull(requiredHostKind, "requiredHostKind");
      this.sourcePattern = Objects.requireNonNull(sourcePattern, "sourcePattern");
      this.encodedPattern = Objects.requireNonNull(encodedPattern, "encodedPattern");
   }

   public PatternExecutionHostKind requiredHostKind() {
      return this.requiredHostKind;
   }

   public SourcePatternSnapshot sourcePattern() {
      return this.sourcePattern;
   }

   public EncodedOverloadPattern encodedPattern() {
      return this.encodedPattern;
   }
}
