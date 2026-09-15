package com.moakiee.ae2lt.api.frequency;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class FrequencyApi {
   private static volatile FrequencyApiProvider provider;

   private FrequencyApi() {
   }

   public static OptionalInt getBoundFrequencyId(BlockEntity blockEntity) {
      FrequencyApiProvider p = provider;
      return p == null ? OptionalInt.empty() : p.getBoundFrequencyId(blockEntity);
   }

   public static Optional<FrequencyInfo> getFrequencyInfo(MinecraftServer server, int frequencyId) {
      FrequencyApiProvider p = provider;
      return p == null ? Optional.empty() : p.getFrequencyInfo(server, frequencyId);
   }

   public static Optional<TransmitterInfo> getTransmitter(MinecraftServer server, int frequencyId) {
      FrequencyApiProvider p = provider;
      return p == null ? Optional.empty() : p.getTransmitter(server, frequencyId);
   }

   public static boolean isValidFrequency(MinecraftServer server, int frequencyId) {
      FrequencyApiProvider p = provider;
      return p != null && p.isValidFrequency(server, frequencyId);
   }

   public static FrequencyBindingAccess createBinding(FrequencyBindingHost host) {
      FrequencyApiProvider p = provider;
      if (p == null) {
         throw new IllegalStateException("FrequencyApi provider not yet initialised");
      } else {
         return p.createBinding(host);
      }
   }

   public static void openBindingScreen(AbstractContainerMenu menu) {
      if (!(menu instanceof FrequencyBindingMenuHost)) {
         throw new IllegalArgumentException("Menu " + menu.getClass().getName() + " does not implement FrequencyBindingMenuHost");
      } else {
         FrequencyApiProvider p = provider;
         if (p == null) {
            throw new IllegalStateException("FrequencyApi provider not yet initialised");
         } else {
            p.openBindingScreen(menu);
         }
      }
   }

   @Internal
   public static void setProvider(FrequencyApiProvider newProvider) {
      if (provider != null) {
         throw new IllegalStateException("FrequencyApi provider already set");
      } else {
         provider = newProvider;
      }
   }
}
