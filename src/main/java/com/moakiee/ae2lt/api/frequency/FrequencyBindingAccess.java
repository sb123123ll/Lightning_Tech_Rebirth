package com.moakiee.ae2lt.api.frequency;

import appeng.api.networking.IGridNodeListener.State;
import appeng.util.SettingsFrom;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;

public interface FrequencyBindingAccess {
   int getFrequencyId();

   void setFrequency(int var1);

   void clearFrequency();

   boolean isConnected();

   void serverTick();

   void onReady();

   void setRemoved();

   void clearRemoved();

   void onMainNodeStateChanged(State var1);

   void save(CompoundTag var1);

   void load(CompoundTag var1);

   default void exportMemorySettings(SettingsFrom mode, CompoundTag output, Consumer<CompoundTag> additionalWriter) {
   }

   default void importMemorySettings(SettingsFrom mode, CompoundTag input, Consumer<CompoundTag> additionalReader) {
   }

   int getGridUsedChannels();

   int getGridMaxChannels();
}
