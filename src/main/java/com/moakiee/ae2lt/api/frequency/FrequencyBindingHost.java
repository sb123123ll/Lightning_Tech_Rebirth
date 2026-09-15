package com.moakiee.ae2lt.api.frequency;

import appeng.blockentity.grid.AENetworkBlockEntity;

public interface FrequencyBindingHost {
   AENetworkBlockEntity getFrequencyBindingBlockEntity();

   void saveFrequencyBindingChanges();

   void markFrequencyBindingForUpdate();

   FrequencyBindingAccess getFrequencyBindingAccess();

   default String getFrequencyBindingDeviceName() {
      return this.getFrequencyBindingBlockEntity().m_58900_().m_60734_().m_7705_();
   }

   default int getFrequencyId() {
      return this.getFrequencyBindingAccess().getFrequencyId();
   }

   default void setFrequency(int frequencyId) {
      this.getFrequencyBindingAccess().setFrequency(frequencyId);
   }

   default void clearFrequency() {
      this.getFrequencyBindingAccess().clearFrequency();
   }

   default boolean isFrequencyConnected() {
      return this.getFrequencyBindingAccess().isConnected();
   }

   default int getGridUsedChannels() {
      return this.getFrequencyBindingAccess().getGridUsedChannels();
   }

   default int getGridMaxChannels() {
      return this.getFrequencyBindingAccess().getGridMaxChannels();
   }
}
