package com.moakiee.ae2lt.celestweave.module;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public enum StepHeightOption {
   VANILLA("0.6", 0.6),
   ONE("1", 1.0),
   ONE_AND_HALF("1.5", 1.5),
   TWO("2", 2.0),
   THREE("3", 3.0);

   private final String label;
   private final double height;

   private StepHeightOption(String label, double height) {
      this.label = label;
      this.height = height;
   }

   public String label() {
      return this.label;
   }

   public double height() {
      return this.height;
   }

   public StringTag toTag() {
      return StringTag.m_129297_(this.name());
   }

   public static StepHeightOption fromTag(Tag tag) {
      if (tag instanceof StringTag stringTag) {
         String id = stringTag.m_7916_();

         for (StepHeightOption option : values()) {
            if (option.name().equalsIgnoreCase(id) || option.label.equalsIgnoreCase(id)) {
               return option;
            }
         }
      }

      return ONE;
   }
}
