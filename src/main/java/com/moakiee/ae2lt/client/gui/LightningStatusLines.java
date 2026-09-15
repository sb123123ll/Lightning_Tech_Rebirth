package com.moakiee.ae2lt.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class LightningStatusLines {
   private LightningStatusLines() {
   }

   public static Component title() {
      return Component.m_237115_("ae2lt.gui.lightning_status.title").m_130944_(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD});
   }

   public static Component status(boolean working) {
      return Component.m_237110_("ae2lt.gui.status.label", new Object[]{Component.m_237115_(working ? "ae2lt.gui.status.working" : "ae2lt.gui.status.idle")});
   }

   public static Component progress(double progress) {
      int percent = (int)Math.round(Math.max(0.0, Math.min(1.0, progress)) * 100.0);
      return Component.m_237110_("ae2lt.gui.progress.label", new Object[]{percent});
   }

   public static Component energy(long stored, long capacity) {
      return Component.m_237110_("ae2lt.gui.energy.label", new Object[]{stored, capacity});
   }

   public static Component highVoltage(long amount) {
      return Component.m_237110_("ae2lt.gui.lightning_status.high_voltage", new Object[]{amount});
   }

   public static Component extremeHighVoltage(long amount) {
      return Component.m_237110_("ae2lt.gui.lightning_status.extreme_high_voltage", new Object[]{amount});
   }
}
