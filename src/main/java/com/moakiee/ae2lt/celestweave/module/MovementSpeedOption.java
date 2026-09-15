package com.moakiee.ae2lt.celestweave.module;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public enum MovementSpeedOption {
   HALF("0.5", 0.5),
   ONE("1", 1.0),
   ONE_AND_HALF("1.5", 1.5),
   TWO("2", 2.0),
   THREE("3", 3.0),
   FOUR("4", 4.0);

   private final String label;
   private final double multiplier;

   private MovementSpeedOption(String label, double multiplier) {
      this.label = label;
      this.multiplier = multiplier;
   }

   public String label() {
      return this.label;
   }

   public double multiplier() {
      return this.multiplier;
   }

   public StringTag toTag() {
      return StringTag.m_129297_(this.name());
   }

   public static MovementSpeedOption fromTag(Tag tag) {
      if (tag instanceof StringTag stringTag) {
         String id = stringTag.m_7916_();

         for (MovementSpeedOption option : values()) {
            if (option.name().equalsIgnoreCase(id) || option.label.equalsIgnoreCase(id)) {
               return option;
            }
         }
      }

      return ONE;
   }
}
