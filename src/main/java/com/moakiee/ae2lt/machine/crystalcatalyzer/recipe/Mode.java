package com.moakiee.ae2lt.machine.crystalcatalyzer.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Mode implements StringRepresentable {
   CRYSTAL("crystal", 20),
   DUST("dust", 40);

   public static final Codec<Mode> CODEC = StringRepresentable.m_216439_(Mode::values);
   private final String name;
   private final int minProcessTicks;

   private Mode(String name, int minProcessTicks) {
      this.name = name;
      this.minProcessTicks = minProcessTicks;
   }

   public int getMinProcessTicks() {
      return this.minProcessTicks;
   }

   public Mode next() {
      Mode[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }

   public String m_7912_() {
      return this.name;
   }

   public String translationKey() {
      return "ae2lt.gui.crystal_catalyzer.mode." + this.name;
   }

   public String tooltipKey() {
      return "ae2lt.gui.crystal_catalyzer.mode.tooltip." + this.name;
   }
}
