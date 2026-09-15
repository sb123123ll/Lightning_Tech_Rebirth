package com.moakiee.ae2lt.overload.runtime.cpu;

import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import java.util.Objects;

final class OutputRouteDecision {
   private OutputRouteDecision() {
   }

   static boolean routesToRequester(MatchMode matchMode, boolean exactMatchesFinal, boolean identityMatchesFinal) {
      Objects.requireNonNull(matchMode, "matchMode");

      return switch (matchMode) {
         case STRICT -> exactMatchesFinal;
         case ID_ONLY -> identityMatchesFinal;
      };
   }
}
