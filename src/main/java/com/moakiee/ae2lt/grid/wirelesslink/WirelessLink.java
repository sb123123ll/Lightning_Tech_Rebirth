package com.moakiee.ae2lt.grid.wirelesslink;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record WirelessLink(
   UUID linkId,
   int frequencyId,
   String dimensionId,
   long posLong,
   WirelessLinkMode mode,
   String sideName,
   String blockId,
   String blockEntityTypeId,
   String partId,
   String partClassName,
   UUID ownerUuid,
   WirelessLinkState state,
   boolean enabled,
   long createdTime,
   long updatedTime,
   long firstInvalidTime,
   long lastCheckedTime,
   int invalidCheckCount
) {
   public WirelessLink(
      UUID linkId,
      int frequencyId,
      String dimensionId,
      long posLong,
      WirelessLinkMode mode,
      String sideName,
      String blockId,
      String blockEntityTypeId,
      String partId,
      String partClassName,
      UUID ownerUuid,
      WirelessLinkState state,
      boolean enabled,
      long createdTime,
      long updatedTime,
      long firstInvalidTime,
      long lastCheckedTime,
      int invalidCheckCount
   ) {
      Objects.requireNonNull(linkId, "linkId");
      Objects.requireNonNull(dimensionId, "dimensionId");
      Objects.requireNonNull(mode, "mode");
      Objects.requireNonNull(blockId, "blockId");
      Objects.requireNonNull(blockEntityTypeId, "blockEntityTypeId");
      Objects.requireNonNull(ownerUuid, "ownerUuid");
      Objects.requireNonNull(state, "state");
      sideName = sideName == null ? "" : sideName;
      partId = partId == null ? "" : partId;
      partClassName = partClassName == null ? "" : partClassName;
      this.linkId = linkId;
      this.frequencyId = frequencyId;
      this.dimensionId = dimensionId;
      this.posLong = posLong;
      this.mode = mode;
      this.sideName = sideName;
      this.blockId = blockId;
      this.blockEntityTypeId = blockEntityTypeId;
      this.partId = partId;
      this.partClassName = partClassName;
      this.ownerUuid = ownerUuid;
      this.state = state;
      this.enabled = enabled;
      this.createdTime = createdTime;
      this.updatedTime = updatedTime;
      this.firstInvalidTime = firstInvalidTime;
      this.lastCheckedTime = lastCheckedTime;
      this.invalidCheckCount = invalidCheckCount;
   }

   public boolean canBeRemovedBy(UUID actorUuid, boolean actorIsFrequencyManager) {
      return this.ownerUuid.equals(actorUuid) || actorIsFrequencyManager;
   }

   public boolean ownerCanUseFrequency(boolean ownerCanUseFrequency) {
      return ownerCanUseFrequency;
   }

   public static WirelessLink createDevice(
      UUID linkId, int frequencyId, String dimensionId, long posLong, String blockId, String blockEntityTypeId, UUID ownerUuid, long now
   ) {
      return new WirelessLink(
         linkId,
         frequencyId,
         dimensionId,
         posLong,
         WirelessLinkMode.DEVICE,
         "",
         blockId,
         blockEntityTypeId,
         "",
         "",
         ownerUuid,
         WirelessLinkState.DISCONNECTED,
         true,
         now,
         now,
         0L,
         0L,
         0
      );
   }

   public static WirelessLink createPart(
      UUID linkId,
      int frequencyId,
      String dimensionId,
      long posLong,
      String sideName,
      String cableBlockId,
      String cableBlockEntityTypeId,
      String partId,
      String partClassName,
      UUID ownerUuid,
      long now
   ) {
      return new WirelessLink(
         linkId,
         frequencyId,
         dimensionId,
         posLong,
         WirelessLinkMode.PART,
         sideName,
         cableBlockId,
         cableBlockEntityTypeId,
         partId,
         partClassName,
         ownerUuid,
         WirelessLinkState.DISCONNECTED,
         true,
         now,
         now,
         0L,
         0L,
         0
      );
   }

   public WirelessLink withState(WirelessLinkState newState, long now) {
      return new WirelessLink(
         this.linkId,
         this.frequencyId,
         this.dimensionId,
         this.posLong,
         this.mode,
         this.sideName,
         this.blockId,
         this.blockEntityTypeId,
         this.partId,
         this.partClassName,
         this.ownerUuid,
         newState,
         this.enabled,
         this.createdTime,
         now,
         this.firstInvalidTime,
         this.lastCheckedTime,
         this.invalidCheckCount
      );
   }

   public WirelessLink withEnabled(boolean newEnabled, long now) {
      return new WirelessLink(
         this.linkId,
         this.frequencyId,
         this.dimensionId,
         this.posLong,
         this.mode,
         this.sideName,
         this.blockId,
         this.blockEntityTypeId,
         this.partId,
         this.partClassName,
         this.ownerUuid,
         this.state,
         newEnabled,
         this.createdTime,
         now,
         this.firstInvalidTime,
         this.lastCheckedTime,
         this.invalidCheckCount
      );
   }

   public WirelessLink withInvalidTracking(long firstInvalid, long lastChecked, int checkCount) {
      return new WirelessLink(
         this.linkId,
         this.frequencyId,
         this.dimensionId,
         this.posLong,
         this.mode,
         this.sideName,
         this.blockId,
         this.blockEntityTypeId,
         this.partId,
         this.partClassName,
         this.ownerUuid,
         this.state,
         this.enabled,
         this.createdTime,
         this.updatedTime,
         firstInvalid,
         lastChecked,
         checkCount
      );
   }

   public WirelessLink clearInvalidTracking(long now) {
      return this.withInvalidTracking(0L, now, 0);
   }

   public Map<String, String> toPersistentSnapshot() {
      LinkedHashMap<String, String> out = new LinkedHashMap<>();
      out.put("LinkId", this.linkId.toString());
      out.put("FrequencyId", Integer.toString(this.frequencyId));
      out.put("Dimension", this.dimensionId);
      out.put("Pos", Long.toString(this.posLong));
      out.put("Mode", this.mode.name());
      out.put("Side", this.sideName);
      out.put("BlockId", this.blockId);
      out.put("BlockEntityType", this.blockEntityTypeId);
      out.put("PartId", this.partId);
      out.put("PartClass", this.partClassName);
      out.put("OwnerUuid", this.ownerUuid.toString());
      out.put("State", this.state.name());
      out.put("Enabled", Boolean.toString(this.enabled));
      out.put("CreatedTime", Long.toString(this.createdTime));
      out.put("UpdatedTime", Long.toString(this.updatedTime));
      out.put("FirstInvalidTime", Long.toString(this.firstInvalidTime));
      out.put("LastCheckedTime", Long.toString(this.lastCheckedTime));
      out.put("InvalidCheckCount", Integer.toString(this.invalidCheckCount));
      return out;
   }

   public static Optional<WirelessLink> fromPersistentSnapshot(Map<String, String> snapshot) {
      try {
         return Optional.of(
            new WirelessLink(
               UUID.fromString(snapshot.get("LinkId")),
               Integer.parseInt(snapshot.get("FrequencyId")),
               snapshot.get("Dimension"),
               Long.parseLong(snapshot.get("Pos")),
               WirelessLinkMode.valueOf(snapshot.getOrDefault("Mode", WirelessLinkMode.DEVICE.name())),
               snapshot.getOrDefault("Side", ""),
               snapshot.getOrDefault("BlockId", ""),
               snapshot.getOrDefault("BlockEntityType", ""),
               snapshot.getOrDefault("PartId", ""),
               snapshot.getOrDefault("PartClass", ""),
               UUID.fromString(snapshot.get("OwnerUuid")),
               WirelessLinkState.valueOf(snapshot.getOrDefault("State", WirelessLinkState.DISCONNECTED.name())),
               Boolean.parseBoolean(snapshot.getOrDefault("Enabled", "true")),
               Long.parseLong(snapshot.getOrDefault("CreatedTime", "0")),
               Long.parseLong(snapshot.getOrDefault("UpdatedTime", "0")),
               Long.parseLong(snapshot.getOrDefault("FirstInvalidTime", "0")),
               Long.parseLong(snapshot.getOrDefault("LastCheckedTime", "0")),
               Integer.parseInt(snapshot.getOrDefault("InvalidCheckCount", "0"))
            )
         );
      } catch (NullPointerException | IllegalArgumentException var2) {
         return Optional.empty();
      }
   }
}
