package com.moakiee.ae2lt.grid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public class WirelessFrequency {
   public static final int MAX_NAME_LENGTH = 24;
   public static final int MAX_PASSWORD_LENGTH = 16;
   public static final String TAG_ID = "id";
   public static final String TAG_NAME = "name";
   public static final String TAG_COLOR = "color";
   public static final String TAG_OWNER = "owner";
   public static final String TAG_SECURITY = "security";
   public static final String TAG_PASSWORD = "password";
   public static final String TAG_MEMBERS = "members";
   public static final byte NBT_BASIC = 0;
   public static final byte NBT_SAVE_ALL = 1;
   public static final byte NBT_MEMBERS_ONLY = 2;
   private int id;
   private String name;
   private int color;
   private UUID ownerUUID;
   private FrequencySecurityLevel security;
   private String password;
   private final Map<UUID, FrequencyMember> members = new HashMap<>();
   public static final byte MEMBERSHIP_SET_USER = 0;
   public static final byte MEMBERSHIP_SET_ADMIN = 1;
   public static final byte MEMBERSHIP_CANCEL = 2;
   public static final byte MEMBERSHIP_TRANSFER_OWNERSHIP = 3;
   public static final int RESPONSE_SUCCESS = 0;
   public static final int RESPONSE_NO_PERMISSION = 1;
   public static final int RESPONSE_INVALID_USER = 2;

   public WirelessFrequency() {
      this(-1, "", 16777215, new UUID(0L, 0L), FrequencySecurityLevel.PUBLIC, "");
   }

   public WirelessFrequency(int id, String name, int color, @Nonnull UUID ownerUUID, @Nonnull FrequencySecurityLevel security, @Nonnull String password) {
      this.id = id;
      this.name = name;
      this.color = color & 16777215;
      this.ownerUUID = ownerUUID;
      this.security = security;
      this.password = hashPassword(password, id);
   }

   public WirelessFrequency(int id, String name, int color, @Nonnull Player owner, @Nonnull FrequencySecurityLevel security, @Nonnull String password) {
      this(id, name, color, owner.m_20148_(), security, password);
      this.members.put(this.ownerUUID, FrequencyMember.create(owner, FrequencyAccessLevel.OWNER));
   }

   @Nonnull
   public static String hashPassword(@Nonnull String plaintext, int salt) {
      if (plaintext.isEmpty()) {
         return "";
      } else {
         try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Integer.toString(salt).getBytes(StandardCharsets.UTF_8));
            md.update((byte)0);
            byte[] out = md.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
         } catch (NoSuchAlgorithmException var4) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", var4);
         }
      }
   }

   private static boolean isHashShape(@Nonnull String s) {
      if (s.length() != 64) {
         return false;
      } else {
         for (int i = 0; i < 64; i++) {
            char c = s.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
               return false;
            }
         }

         return true;
      }
   }

   public int getId() {
      return this.id;
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   public boolean setName(@Nonnull String name) {
      if (!name.equals(this.name) && !name.isBlank() && name.length() <= 24) {
         this.name = name;
         return true;
      } else {
         return false;
      }
   }

   public int getColor() {
      return this.color;
   }

   public boolean setColor(int color) {
      color &= 16777215;
      if (this.color != color) {
         this.color = color;
         return true;
      } else {
         return false;
      }
   }

   @Nonnull
   public UUID getOwnerUUID() {
      return this.ownerUUID;
   }

   @Nonnull
   public FrequencySecurityLevel getSecurity() {
      return this.security;
   }

   public boolean setSecurity(@Nonnull FrequencySecurityLevel security) {
      if (this.security != security) {
         this.security = security;
         return true;
      } else {
         return false;
      }
   }

   @Nonnull
   public String getPassword() {
      return this.password;
   }

   public void setPassword(@Nonnull String password) {
      this.password = hashPassword(password, this.id);
   }

   @Nonnull
   public FrequencyAccessLevel getPlayerAccess(@Nonnull Player player) {
      return this.getPlayerAccess(player.m_20148_());
   }

   @Nonnull
   public FrequencyAccessLevel getPlayerAccess(@Nonnull UUID uuid) {
      FrequencyMember member = this.members.get(uuid);
      if (member != null) {
         return member.getAccessLevel();
      } else {
         return this.security == FrequencySecurityLevel.PUBLIC ? FrequencyAccessLevel.USER : FrequencyAccessLevel.BLOCKED;
      }
   }

   public boolean canPlayerAccess(@Nonnull Player player, @Nonnull String password) {
      FrequencyAccessLevel access = this.getPlayerAccess(player);
      if (access.canUse()) {
         return true;
      } else {
         return this.security == FrequencySecurityLevel.ENCRYPTED && !password.isEmpty() ? hashPassword(password, this.id).equals(this.password) : false;
      }
   }

   public boolean isMember(@Nonnull Player player) {
      return this.members.containsKey(player.m_20148_());
   }

   public boolean enrollAsUser(@Nonnull Player player) {
      UUID uuid = player.m_20148_();
      if (this.members.containsKey(uuid)) {
         return false;
      } else {
         this.members.put(uuid, FrequencyMember.create(player, FrequencyAccessLevel.USER));
         return true;
      }
   }

   public int changeMembership(@Nonnull Player actor, @Nonnull UUID targetUUID, byte type) {
      FrequencyAccessLevel actorAccess = this.getPlayerAccess(actor);
      FrequencyMember current = this.members.get(targetUUID);
      FrequencyAccessLevel targetLevel = current == null ? FrequencyAccessLevel.BLOCKED : current.getAccessLevel();
      boolean targetIsOwner = targetLevel == FrequencyAccessLevel.OWNER;
      boolean self = actor.m_20148_().equals(targetUUID);
      switch (type) {
         case 0:
            FrequencyAccessLevel effectivex = FrequencyAccessLevel.higher(targetLevel, FrequencyAccessLevel.USER);
            if (!actorAccess.canActOnLevel(effectivex)) {
               return 1;
            } else if (targetIsOwner && self) {
               return 2;
            } else {
               if (current == null) {
                  MinecraftServer serverxx = actor.m_9236_().m_7654_();
                  if (serverxx == null) {
                     return 2;
                  }

                  Player target = serverxx.m_6846_().m_11259_(targetUUID);
                  if (target == null) {
                     return 2;
                  }

                  this.members.put(targetUUID, FrequencyMember.create(target, FrequencyAccessLevel.USER));
                  return 0;
               }

               return current.setAccessLevel(FrequencyAccessLevel.USER) ? 0 : 2;
            }
         case 1:
            FrequencyAccessLevel effective = FrequencyAccessLevel.higher(targetLevel, FrequencyAccessLevel.ADMIN);
            if (!actorAccess.canActOnLevel(effective)) {
               return 1;
            } else if (targetIsOwner && self) {
               return 2;
            } else {
               if (current == null) {
                  MinecraftServer serverx = actor.m_9236_().m_7654_();
                  if (serverx == null) {
                     return 2;
                  }

                  Player target = serverx.m_6846_().m_11259_(targetUUID);
                  if (target == null) {
                     return 2;
                  }

                  this.members.put(targetUUID, FrequencyMember.create(target, FrequencyAccessLevel.ADMIN));
                  return 0;
               }

               return current.setAccessLevel(FrequencyAccessLevel.ADMIN) ? 0 : 2;
            }
         case 2:
            if (current == null) {
               return 2;
            } else {
               if (self) {
                  if (targetIsOwner) {
                     return 2;
                  }
               } else if (!actorAccess.canActOnLevel(targetLevel)) {
                  return 1;
               }

               this.members.remove(targetUUID);
               return 0;
            }
         case 3:
            if (!actorAccess.canActOnLevel(FrequencyAccessLevel.OWNER)) {
               return 1;
            } else if (targetIsOwner) {
               return 2;
            } else {
               if (current != null) {
                  current.setAccessLevel(FrequencyAccessLevel.OWNER);
               } else {
                  MinecraftServer server = actor.m_9236_().m_7654_();
                  if (server == null) {
                     return 2;
                  }

                  Player target = server.m_6846_().m_11259_(targetUUID);
                  if (target == null) {
                     return 2;
                  }

                  this.members.put(targetUUID, FrequencyMember.create(target, FrequencyAccessLevel.OWNER));
               }

               return 0;
            }
         default:
            return 2;
      }
   }

   public void writeToTag(@Nonnull CompoundTag tag, byte type) {
      if (type == 0 || type == 1) {
         tag.m_128405_("id", this.id);
         tag.m_128359_("name", this.name);
         tag.m_128405_("color", this.color);
         tag.m_128362_("owner", this.ownerUUID);
         tag.m_128344_("security", this.security.getId());
      }

      if (type == 1) {
         tag.m_128359_("password", this.password);
         if (!this.members.isEmpty()) {
            ListTag list = new ListTag();

            for (FrequencyMember m : this.members.values()) {
               CompoundTag sub = new CompoundTag();
               m.writeNBT(sub);
               list.add(sub);
            }

            tag.m_128365_("members", list);
         }
      }

      if (type == 2) {
         tag.m_128405_("id", this.id);
         ListTag list = new ListTag();

         for (FrequencyMember m : this.members.values()) {
            CompoundTag sub = new CompoundTag();
            m.writeNBT(sub);
            list.add(sub);
         }

         tag.m_128365_("members", list);
      }
   }

   public void readFromTag(@Nonnull CompoundTag tag, byte type) {
      if (type == 0 || type == 1) {
         this.id = tag.m_128451_("id");
         this.name = tag.m_128461_("name");
         this.color = tag.m_128451_("color");
         this.ownerUUID = tag.m_128342_("owner");
         this.security = FrequencySecurityLevel.fromId(tag.m_128445_("security"));
      }

      if (type == 1) {
         String stored = tag.m_128461_("password");
         this.password = !stored.isEmpty() && !isHashShape(stored) ? hashPassword(stored, this.id) : stored;
         this.members.clear();
         ListTag list = tag.m_128437_("members", 10);

         for (int i = 0; i < list.size(); i++) {
            FrequencyMember m = new FrequencyMember(list.m_128728_(i));
            this.members.put(m.getPlayerUUID(), m);
         }
      }

      if (type == 2) {
         this.members.clear();
         ListTag list = tag.m_128437_("members", 10);

         for (int i = 0; i < list.size(); i++) {
            FrequencyMember m = new FrequencyMember(list.m_128728_(i));
            this.members.put(m.getPlayerUUID(), m);
         }
      }
   }

   @Override
   public String toString() {
      return "WirelessFrequency{id=" + this.id + ", name='" + this.name + "', owner=" + this.ownerUUID + "}";
   }
}
