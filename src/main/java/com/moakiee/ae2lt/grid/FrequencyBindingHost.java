package com.moakiee.ae2lt.grid;

import com.moakiee.ae2lt.api.frequency.FrequencyBindingAccess;

public interface FrequencyBindingHost extends com.moakiee.ae2lt.api.frequency.FrequencyBindingHost {
   FrequencyBindingHelper getFrequencyBinding();

   @Override
   default FrequencyBindingAccess getFrequencyBindingAccess() {
      return this.getFrequencyBinding();
   }
}
