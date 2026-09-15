package com.moakiee.ae2lt.item.railgun;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum RailgunExecutionMode implements StringRepresentable {
   OFF("off"),
   NORMAL("normal"),
   FORCED("forced");

   public static final Codec<RailgunExecutionMode> CODEC = StringRepresentable.m_216439_(RailgunExecutionMode::values);
   private final String serializedName;

   private RailgunExecutionMode(String serializedName) {
      this.serializedName = serializedName;
   }

   public RailgunExecutionMode next() {
      return switch (this) {
         case OFF -> NORMAL;
         case NORMAL -> FORCED;
         case FORCED -> OFF;
      };
   }

   public boolean entersExecutionFlow() {
      return this != OFF;
   }

   public boolean forcesRemoval() {
      return this == FORCED;
   }

   public String translationKey() {
      return "ae2lt.railgun.config.overload_execution_mode." + this.serializedName;
   }

   public String m_7912_() {
      return this.serializedName;
   }
}
