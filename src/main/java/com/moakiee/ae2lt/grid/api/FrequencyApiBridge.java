package com.moakiee.ae2lt.grid.api;

import com.moakiee.ae2lt.api.frequency.FrequencyApiProvider;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingAccess;
import com.moakiee.ae2lt.api.frequency.FrequencyBindingHost;
import com.moakiee.ae2lt.api.frequency.FrequencyInfo;
import com.moakiee.ae2lt.api.frequency.FrequencySecurity;
import com.moakiee.ae2lt.api.frequency.TransmitterInfo;
import com.moakiee.ae2lt.grid.FrequencyBindingHelper;
import com.moakiee.ae2lt.grid.FrequencySecurityLevel;
import com.moakiee.ae2lt.grid.WirelessFrequency;
import com.moakiee.ae2lt.grid.WirelessFrequencyManager;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.OpenFrequencyMenuPacket;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FrequencyApiBridge implements FrequencyApiProvider {
   @Override
   public OptionalInt getBoundFrequencyId(BlockEntity blockEntity) {
      if (blockEntity instanceof FrequencyBindingHost host) {
         int id = host.getFrequencyId();
         return id > 0 ? OptionalInt.of(id) : OptionalInt.empty();
      } else if (blockEntity instanceof WirelessFrequencyManager.WirelessTransmitterNodeProvider provider) {
         int id = provider.getTransmitterFrequencyId();
         return id > 0 ? OptionalInt.of(id) : OptionalInt.empty();
      } else {
         return OptionalInt.empty();
      }
   }

   @Override
   public Optional<FrequencyInfo> getFrequencyInfo(MinecraftServer server, int frequencyId) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null && frequencyId > 0) {
         WirelessFrequency freq = manager.getFrequency(frequencyId);
         return freq == null
            ? Optional.empty()
            : Optional.of(new FrequencyInfo(freq.getId(), freq.getName(), freq.getColor(), freq.getOwnerUUID(), toApiSecurity(freq.getSecurity())));
      } else {
         return Optional.empty();
      }
   }

   @Override
   public Optional<TransmitterInfo> getTransmitter(MinecraftServer server, int frequencyId) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      if (manager != null && frequencyId > 0) {
         WirelessFrequencyManager.TransmitterEntry entry = manager.findTransmitter(frequencyId);
         return entry == null ? Optional.empty() : Optional.of(new TransmitterInfo(entry.dimension(), entry.pos(), entry.advanced()));
      } else {
         return Optional.empty();
      }
   }

   @Override
   public boolean isValidFrequency(MinecraftServer server, int frequencyId) {
      WirelessFrequencyManager manager = WirelessFrequencyManager.get();
      return manager != null && manager.isFrequencyValid(frequencyId);
   }

   @Override
   public FrequencyBindingAccess createBinding(FrequencyBindingHost host) {
      return new FrequencyBindingHelper(host);
   }

   @Override
   public void openBindingScreen(AbstractContainerMenu menu) {
      NetworkInit.sendToServer(OpenFrequencyMenuPacket.forBlock());
   }

   private static FrequencySecurity toApiSecurity(FrequencySecurityLevel level) {
      return switch (level) {
         case PUBLIC -> FrequencySecurity.PUBLIC;
         case ENCRYPTED -> FrequencySecurity.ENCRYPTED;
         case PRIVATE -> FrequencySecurity.PRIVATE;
      };
   }
}
