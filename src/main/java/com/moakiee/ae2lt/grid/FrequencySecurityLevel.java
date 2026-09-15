package com.moakiee.ae2lt.grid;

import javax.annotation.Nonnull;

public enum FrequencySecurityLevel {
   PUBLIC,
   ENCRYPTED,
   PRIVATE;

   public static final FrequencySecurityLevel[] VALUES = values();

   @Nonnull
   public static FrequencySecurityLevel fromId(byte id) {
      return id >= 0 && id < VALUES.length ? VALUES[id] : PUBLIC;
   }

   public byte getId() {
      return (byte)this.ordinal();
   }
}
