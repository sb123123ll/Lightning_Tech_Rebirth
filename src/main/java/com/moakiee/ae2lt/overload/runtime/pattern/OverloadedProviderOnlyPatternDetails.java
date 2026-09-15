package com.moakiee.ae2lt.overload.runtime.pattern;

import com.moakiee.thunderbolt.core.crafting.overload.OverloadedPatternDetails;

public interface OverloadedProviderOnlyPatternDetails extends OverloadedPatternDetails {
   PatternExecutionHostKind requiredHostKind();

   String overloadPatternIdentity();

   OverloadPatternDetails overloadPatternDetailsView();

   default boolean hasFuzzyInputs() {
      OverloadPatternDetails view = this.overloadPatternDetailsView();
      return view != null && view.inputs().stream().anyMatch(input -> input.matchMode().ignoresComponents());
   }

   default boolean isFuzzyInput(int slot) {
      OverloadPatternDetails view = this.overloadPatternDetailsView();
      return view != null && view.inputMode(slot).ignoresComponents();
   }

   default boolean acceptsSameIdVariants(int slot) {
      return this.isFuzzyInput(slot);
   }

   default boolean isFuzzyOutput(int slot) {
      OverloadPatternDetails view = this.overloadPatternDetailsView();
      return view != null && view.outputMode(slot).ignoresComponents();
   }

   default boolean producesSameIdVariants(int slot) {
      return this.isFuzzyOutput(slot);
   }
}
