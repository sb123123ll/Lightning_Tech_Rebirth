package com.moakiee.ae2lt.util;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class EnergyText {
   private static final EnergyText.Unit[] UNITS = new EnergyText.Unit[]{
      new EnergyText.Unit(1.0E18, "EFE"),
      new EnergyText.Unit(1.0E15, "PFE"),
      new EnergyText.Unit(1.0E12, "TFE"),
      new EnergyText.Unit(1.0E9, "GFE"),
      new EnergyText.Unit(1000000.0, "MFE"),
      new EnergyText.Unit(1000.0, "kFE")
   };

   private EnergyText() {
   }

   public static Component storedFe(long stored, long capacity) {
      return label("ae2lt.tooltip.stored_fe")
         .m_7220_(Component.m_237113_(": ").m_130940_(ChatFormatting.GRAY))
         .m_7220_(Component.m_237113_(formatFe(stored)).m_130940_(ChatFormatting.GRAY))
         .m_7220_(Component.m_237113_(" / ").m_130940_(ChatFormatting.GRAY))
         .m_7220_(Component.m_237113_(formatFe(capacity)).m_130940_(ChatFormatting.GRAY));
   }

   public static Component capacityFe(long capacity) {
      return label("ae2lt.tooltip.capacity_fe")
         .m_7220_(Component.m_237113_(": ").m_130940_(ChatFormatting.GRAY))
         .m_7220_(Component.m_237113_(formatFe(capacity)).m_130940_(ChatFormatting.GRAY));
   }

   public static String formatFe(long value) {
      return formatEnergyValue(Math.max(0L, value));
   }

   private static String formatEnergyValue(long value) {
      for (EnergyText.Unit unit : UNITS) {
         if ((double)value >= unit.threshold) {
            return formatScaled((double)value / unit.threshold, unit.symbol);
         }
      }

      return Long.toString(value) + " FE";
   }

   private static String formatScaled(double value, String unit) {
      double truncated = Math.floor(value * 100.0) / 100.0;
      String formatted = String.format(Locale.ROOT, "%.2f", truncated);
      if (formatted.indexOf(46) >= 0) {
         formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      }

      return formatted + " " + unit;
   }

   private static MutableComponent label(String key) {
      return Component.m_237115_(key).m_130940_(ChatFormatting.GREEN);
   }

   private static record Unit(double threshold, String symbol) {
   }
}
