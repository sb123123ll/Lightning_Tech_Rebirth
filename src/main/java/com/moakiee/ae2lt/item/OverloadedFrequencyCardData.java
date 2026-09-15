package com.moakiee.ae2lt.item;

import java.util.Optional;
import java.util.UUID;

public record OverloadedFrequencyCardData(
   int frequencyId, boolean autoConnect, Optional<String> boundControllerDimension, Optional<Long> boundControllerPos, Optional<UUID> ownerUuid
) {
   public static final int NO_FREQUENCY = -1;

   public OverloadedFrequencyCardData(
      int frequencyId, boolean autoConnect, Optional<String> boundControllerDimension, Optional<Long> boundControllerPos, Optional<UUID> ownerUuid
   ) {
      boundControllerDimension = boundControllerDimension == null ? Optional.empty() : boundControllerDimension;
      boundControllerPos = boundControllerPos == null ? Optional.empty() : boundControllerPos;
      ownerUuid = ownerUuid == null ? Optional.empty() : ownerUuid;
      this.frequencyId = frequencyId;
      this.autoConnect = autoConnect;
      this.boundControllerDimension = boundControllerDimension;
      this.boundControllerPos = boundControllerPos;
      this.ownerUuid = ownerUuid;
   }

   public static OverloadedFrequencyCardData empty() {
      return new OverloadedFrequencyCardData(-1, false, Optional.empty(), Optional.empty(), Optional.empty());
   }

   public boolean isBound() {
      return this.frequencyId > 0;
   }

   public boolean canBeUsedBy(UUID playerUuid) {
      return this.ownerUuid.isPresent() && this.ownerUuid.get().equals(playerUuid);
   }

   public OverloadedFrequencyCardData bindFrequency(int newFrequencyId, String controllerDimension, long controllerPos, UUID owner) {
      return newFrequencyId <= 0
         ? this
         : new OverloadedFrequencyCardData(
            newFrequencyId, this.autoConnect, Optional.ofNullable(controllerDimension), Optional.of(controllerPos), Optional.ofNullable(owner)
         );
   }

   public OverloadedFrequencyCardData clearFrequency() {
      return new OverloadedFrequencyCardData(-1, this.autoConnect, Optional.empty(), Optional.empty(), Optional.empty());
   }

   public OverloadedFrequencyCardData withAutoConnect(boolean enabled) {
      return new OverloadedFrequencyCardData(this.frequencyId, enabled, this.boundControllerDimension, this.boundControllerPos, this.ownerUuid);
   }

   public OverloadedFrequencyCardData toggleAutoConnect() {
      return this.withAutoConnect(!this.autoConnect);
   }
}
