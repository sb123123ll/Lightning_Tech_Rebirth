package com.moakiee.ae2lt.grid;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class FrequencyMember {
   private final UUID playerUUID;
   private String cachedName;
   private FrequencyAccessLevel accessLevel;

   private FrequencyMember(UUID playerUUID, String cachedName, FrequencyAccessLevel accessLevel) {
      this.playerUUID = playerUUID;
      this.cachedName = cachedName;
      this.accessLevel = accessLevel;
   }

   public FrequencyMember(CompoundTag tag) {
      this.playerUUID = tag.m_128342_("uuid");
      this.cachedName = tag.m_128461_("name");
      this.accessLevel = FrequencyAccessLevel.fromId(tag.m_128445_("access"));
   }

   public static FrequencyMember create(Player player, FrequencyAccessLevel access) {
      return new FrequencyMember(player.m_20148_(), player.m_36316_().getName(), access);
   }

   public UUID getPlayerUUID() {
      return this.playerUUID;
   }

   public String getCachedName() {
      return this.cachedName;
   }

   public FrequencyAccessLevel getAccessLevel() {
      return this.accessLevel;
   }

   public boolean setAccessLevel(FrequencyAccessLevel level) {
      if (this.accessLevel != level) {
         this.accessLevel = level;
         return true;
      } else {
         return false;
      }
   }

   public void writeNBT(CompoundTag tag) {
      tag.m_128362_("uuid", this.playerUUID);
      tag.m_128359_("name", this.cachedName);
      tag.m_128344_("access", this.accessLevel.getId());
   }
}
