package com.moakiee.ae2lt.api.frequency;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface FrequencyApiProvider {
   OptionalInt getBoundFrequencyId(BlockEntity var1);

   Optional<FrequencyInfo> getFrequencyInfo(MinecraftServer var1, int var2);

   Optional<TransmitterInfo> getTransmitter(MinecraftServer var1, int var2);

   boolean isValidFrequency(MinecraftServer var1, int var2);

   FrequencyBindingAccess createBinding(FrequencyBindingHost var1);

   void openBindingScreen(AbstractContainerMenu var1);
}
