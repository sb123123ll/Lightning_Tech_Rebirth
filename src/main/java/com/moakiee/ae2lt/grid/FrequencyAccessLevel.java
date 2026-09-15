package com.moakiee.ae2lt.grid;

import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;

public enum FrequencyAccessLevel {
   OWNER(ChatFormatting.DARK_PURPLE),
   ADMIN(ChatFormatting.BLUE),
   USER(ChatFormatting.GREEN),
   BLOCKED(ChatFormatting.RED);

   public static final FrequencyAccessLevel[] VALUES = values();
   private final ChatFormatting formatting;

   private FrequencyAccessLevel(ChatFormatting formatting) {
      this.formatting = formatting;
   }

   @Nonnull
   public static FrequencyAccessLevel fromId(byte id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : BLOCKED;
   }

   public byte getId() {
      return (byte)this.ordinal();
   }

   public ChatFormatting getFormatting() {
      return this.formatting;
   }

   public boolean canUse() {
      return this != BLOCKED;
   }

   public boolean isManager() {
      return this == OWNER || this == ADMIN;
   }

   public boolean isOwner() {
      return this == OWNER;
   }

   public int rank() {
      return switch (this) {
         case OWNER -> 3;
         case ADMIN -> 2;
         case USER -> 1;
         case BLOCKED -> 0;
      };
   }

   public boolean canActOnLevel(@Nonnull FrequencyAccessLevel level) {
      if (!this.isManager()) {
         return false;
      } else {
         int ar = this.rank();
         int lr = level.rank();
         return lr > ar ? false : lr != ar || this == OWNER;
      }
   }

   @Nonnull
   public static FrequencyAccessLevel higher(@Nonnull FrequencyAccessLevel a, @Nonnull FrequencyAccessLevel b) {
      return a.rank() >= b.rank() ? a : b;
   }
}
