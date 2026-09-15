package com.moakiee.ae2lt.logic.craft;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import java.util.List;
import java.util.Objects;

public interface MatrixPatternCore {
   List<IPatternDetails> getAvailablePatterns();

   default boolean hasPattern(IPatternDetails details) {
      if (details == null) {
         return false;
      } else {
         for (IPatternDetails pattern : this.getAvailablePatterns()) {
            if (samePattern(pattern, details)) {
               return true;
            }
         }

         return false;
      }
   }

   static boolean samePattern(IPatternDetails stored, IPatternDetails requested) {
      if (stored == requested) {
         return true;
      } else if (stored != null && requested != null) {
         AEItemKey storedDefinition = stored.getDefinition();
         AEItemKey requestedDefinition = requested.getDefinition();
         return storedDefinition != null && requestedDefinition != null && Objects.equals(storedDefinition, requestedDefinition)
            ? true
            : stored.equals(requested);
      } else {
         return false;
      }
   }
}
