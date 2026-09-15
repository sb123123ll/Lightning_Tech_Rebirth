package com.moakiee.ae2lt.celestweave.module;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public enum ReachDistanceOption {
   ONE("1x", 2.0, 1.0),
   TWO("2x", 4.0, 2.0),
   FOUR("4x", 8.0, 4.0);

   public static final String CONFIG_KEY = "reach_range";
   private final String label;
   private final double blockBonus;
   private final double entityBonus;

   private ReachDistanceOption(String label, double blockBonus, double entityBonus) {
      this.label = label;
      this.blockBonus = blockBonus;
      this.entityBonus = entityBonus;
   }

   public String id() {
      return this.name();
   }

   public String label() {
      return this.label;
   }

   public double blockBonus() {
      return this.blockBonus;
   }

   public double entityBonus() {
      return this.entityBonus;
   }

   public StringTag toTag() {
      return StringTag.m_129297_(this.id());
   }

   public static ReachDistanceOption fromTag(Tag tag) {
      return tag instanceof StringTag stringTag ? fromId(stringTag.m_7916_()) : ONE;
   }

   public static ReachDistanceOption fromId(String id) {
      if (id != null) {
         for (ReachDistanceOption option : values()) {
            if (option.id().equalsIgnoreCase(id) || option.label.equalsIgnoreCase(id)) {
               return option;
            }
         }
      }

      return ONE;
   }
}
