package com.moakiee.ae2lt.overload.runtime.pattern;

import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import java.util.Objects;

public record EditableOverloadPatternState(ParsedPatternDefinition parsedPattern, EncodedOverloadPattern encodedPattern) {
   public EditableOverloadPatternState(ParsedPatternDefinition parsedPattern, EncodedOverloadPattern encodedPattern) {
      Objects.requireNonNull(parsedPattern, "parsedPattern");
      Objects.requireNonNull(encodedPattern, "encodedPattern");
      this.parsedPattern = parsedPattern;
      this.encodedPattern = encodedPattern;
   }
}
