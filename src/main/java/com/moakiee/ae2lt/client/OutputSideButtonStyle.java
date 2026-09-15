package com.moakiee.ae2lt.client;

import net.minecraft.client.gui.GuiGraphics;

final class OutputSideButtonStyle {
   private static final int BLACK = -16777216;
   private static final int WHITE = -1;
   private static final int EDGE = -11184811;
   private static final int DISABLED_FACE = -7631989;
   private static final int ENABLED_FACE = -2105377;

   private OutputSideButtonStyle() {
   }

   static void renderBackground(GuiGraphics graphics, int x, int y, boolean enabled) {
      int face = enabled ? -2105377 : -7631989;
      graphics.m_280509_(x + 2, y + 2, x + 16, y + 16, face);
      graphics.m_280509_(x + 3, y, x + 15, y + 1, -16777216);
      graphics.m_280509_(x + 2, y + 1, x + 3, y + 2, -16777216);
      graphics.m_280509_(x + 15, y + 1, x + 16, y + 2, -16777216);
      graphics.m_280509_(x + 1, y + 2, x + 2, y + 3, -16777216);
      graphics.m_280509_(x + 16, y + 2, x + 17, y + 3, -16777216);
      graphics.m_280509_(x, y + 3, x + 1, y + 15, -16777216);
      graphics.m_280509_(x + 17, y + 3, x + 18, y + 15, -16777216);
      graphics.m_280509_(x + 1, y + 3, x + 2, y + 15, -11184811);
      graphics.m_280509_(x + 16, y + 3, x + 17, y + 15, -11184811);
      graphics.m_280509_(x + 1, y + 15, x + 2, y + 16, -16777216);
      graphics.m_280509_(x + 16, y + 15, x + 17, y + 16, -16777216);
      graphics.m_280509_(x + 2, y + 16, x + 3, y + 17, -16777216);
      graphics.m_280509_(x + 15, y + 16, x + 16, y + 17, -16777216);
      graphics.m_280509_(x + 3, y + 17, x + 15, y + 18, -16777216);
      graphics.m_280509_(x + 3, y + 1, x + 15, y + 2, -1);
      graphics.m_280509_(x + 3, y + 16, x + 15, y + 17, -11184811);
   }

   static void renderClearIcon(GuiGraphics graphics, int x, int y) {
      graphics.m_280509_(x, y, x + 8, y + 8, -16777216);
      graphics.m_280509_(x + 1, y + 1, x + 7, y + 7, -7631989);
      graphics.m_280509_(x + 2, y + 2, x + 3, y + 3, -2105377);
      graphics.m_280509_(x + 5, y + 2, x + 6, y + 3, -2105377);
      graphics.m_280509_(x + 3, y + 3, x + 5, y + 5, -2105377);
      graphics.m_280509_(x + 2, y + 5, x + 3, y + 6, -2105377);
      graphics.m_280509_(x + 5, y + 5, x + 6, y + 6, -2105377);
   }
}
