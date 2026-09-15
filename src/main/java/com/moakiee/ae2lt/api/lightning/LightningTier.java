package com.moakiee.ae2lt.api.lightning;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;

public enum LightningTier implements StringRepresentable {
   HIGH_VOLTAGE("high_voltage"),
   EXTREME_HIGH_VOLTAGE("extreme_high_voltage");

   public static final Codec<LightningTier> CODEC = StringRepresentable.m_216439_(LightningTier::values);
   private final String serializedName;

   private LightningTier(String serializedName) {
      this.serializedName = serializedName;
   }

   public String m_7912_() {
      return this.serializedName;
   }

   public static LightningTier fromSerializedName(String serializedName) {
      for (LightningTier value : values()) {
         if (value.serializedName.equals(serializedName)) {
            return value;
         }
      }

      return HIGH_VOLTAGE;
   }

   public static LightningTier fromOrdinal(int ordinal) {
      return ordinal == EXTREME_HIGH_VOLTAGE.ordinal() ? EXTREME_HIGH_VOLTAGE : HIGH_VOLTAGE;
   }

   public static LightningTier fromNetwork(FriendlyByteBuf buffer) {
      return fromOrdinal(buffer.readByte());
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      buffer.writeByte(this.ordinal());
   }
}
