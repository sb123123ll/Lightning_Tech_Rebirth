package com.moakiee.ae2lt.integration.recipeviewer.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class MultiblockPreviewControls {
   private static final int BORDER_COLOR = -16777216;
   private static final int HOVER_BORDER_COLOR = -1;
   private static final int FACE_COLOR = -9539986;
   private static final int PRESSED_FACE_COLOR = -9605779;
   private static final int DISABLED_FACE_COLOR = -13948117;
   private static final int LIGHT_EDGE_COLOR = -5592406;
   private static final int DARK_EDGE_COLOR = -11184811;
   private static final int PRESSED_LIGHT_EDGE_COLOR = -9408400;
   private static final int DISABLED_LIGHT_EDGE_COLOR = -13619152;
   private static final int DISABLED_DARK_EDGE_COLOR = -14408668;
   private static final int ICON_COLOR = -2039584;
   private static final int HOVER_ICON_COLOR = -1;
   private static final int DISABLED_ICON_COLOR = -6250336;
   private static final int INSET_FACE_COLOR = -13619152;

   static void drawIconButton(
      GuiGraphics guiGraphics,
      int x,
      int y,
      int width,
      int height,
      MultiblockPreviewControls.PixelIcon icon,
      boolean pressed,
      boolean contentPressed,
      boolean enabled,
      boolean hovered
   ) {
      drawButtonFrame(guiGraphics, x, y, width, height, pressed, enabled, enabled && hovered);
      int color = !enabled ? -6250336 : (hovered ? -1 : -2039584);
      drawPixelIcon(guiGraphics, x, y, width, height, icon, color, contentPressed);
   }

   static void drawTextButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, Component label, boolean pressed, boolean hovered) {
      drawButtonFrame(guiGraphics, x, y, width, height, pressed, true, hovered);
      String value = label.getString();
      int maxWidth = Math.max(0, width - 6);
      if (font.m_92895_(value) > maxWidth) {
         value = font.m_92834_(value, maxWidth);
      }

      int textX = x + (width - font.m_92895_(value)) / 2;
      int textY = y + (height - 9) / 2 + 1;
      int color = hovered ? -1 : -2039584;
      guiGraphics.m_280056_(font, value, textX, textY, color, false);
   }

   static void drawInsetLabel(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, Component label) {
      int right = x + width;
      int bottom = y + height;
      guiGraphics.m_280509_(x, y, right, bottom, -16777216);
      guiGraphics.m_280509_(x + 1, y + 1, right - 1, bottom - 1, -13619152);
      guiGraphics.m_280509_(x + 1, y + 1, right - 1, y + 2, -11184811);
      guiGraphics.m_280509_(x + 1, y + 1, x + 2, bottom - 1, -11184811);
      guiGraphics.m_280509_(x + 1, bottom - 2, right - 1, bottom - 1, -5592406);
      guiGraphics.m_280509_(right - 2, y + 1, right - 1, bottom - 1, -5592406);
      String value = label.getString();
      int maxWidth = Math.max(0, width - 6);
      if (font.m_92895_(value) > maxWidth) {
         value = font.m_92834_(value, maxWidth);
      }

      int textX = x + (width - font.m_92895_(value)) / 2;
      int textY = y + (height - 9) / 2 + 1;
      guiGraphics.m_280056_(font, value, textX, textY, -2039584, false);
   }

   private static void drawButtonFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean pressed, boolean enabled, boolean hovered) {
      int right = x + width;
      int bottom = y + height;
      int border = hovered ? -1 : -16777216;
      int face = !enabled ? -13948117 : (pressed ? -9605779 : -9539986);
      int topLeft = !enabled ? -13619152 : (pressed ? -11184811 : -5592406);
      int bottomRight = !enabled ? -14408668 : (pressed ? -9408400 : -11184811);
      guiGraphics.m_280509_(x, y, right, bottom, border);
      guiGraphics.m_280509_(x + 1, y + 1, right - 1, bottom - 1, face);
      guiGraphics.m_280509_(x + 1, y + 1, right - 1, y + 2, topLeft);
      guiGraphics.m_280509_(x + 1, y + 1, x + 2, bottom - 1, topLeft);
      guiGraphics.m_280509_(x + 1, bottom - 2, right - 1, bottom - 1, bottomRight);
      guiGraphics.m_280509_(right - 2, y + 1, right - 1, bottom - 1, bottomRight);
   }

   private static void drawPixelIcon(
      GuiGraphics guiGraphics, int x, int y, int width, int height, MultiblockPreviewControls.PixelIcon icon, int color, boolean contentPressed
   ) {
      String[] pixels = icon.pixels;
      double pressOffset = contentPressed ? 0.5 : 0.0;
      double startX = (double)x + (double)(width - icon.inkWidth) / 2.0 - (double)icon.minX + pressOffset;
      double startY = (double)y + (double)(height - icon.inkHeight) / 2.0 - (double)icon.minY + pressOffset;
      PoseStack pose = guiGraphics.m_280168_();
      pose.m_85836_();
      pose.m_85837_(startX, startY, 0.0);

      for (int row = 0; row < pixels.length; row++) {
         String line = pixels[row];
         int column = 0;

         while (column < line.length()) {
            int runStart = line.indexOf(35, column);
            if (runStart < 0) {
               break;
            }

            int runEnd = runStart + 1;

            while (runEnd < line.length() && line.charAt(runEnd) == '#') {
               runEnd++;
            }

            guiGraphics.m_280509_(runStart, row, runEnd, row + 1, color);
            column = runEnd;
         }
      }

      pose.m_85849_();
   }

   private MultiblockPreviewControls() {
   }

   static enum PixelIcon {
      RESET_VIEW("...###...", "...#.#...", "...#.#...", "###...###", "#...#...#", "###...###", "...#.#...", "...#.#...", "...###..."),
      PLAY("..#......", "..##.....", "..###....", "..####...", "..#####..", "..####...", "..###....", "..##.....", "..#......"),
      PAUSE(".........", "..##.##..", "..##.##..", "..##.##..", "..##.##..", "..##.##..", "..##.##..", "..##.##..", "........."),
      SHELL("...###...", ".##...##.", "#.#....#.", "#..#...#.", "#...#..#.", "#....#.#.", ".##...##.", "...###...", "........."),
      SHELL_HIDDEN("...###..#", ".##...###", "#.#...##.", "#..#.##..", "#...##.#.", "#..##..#.", ".###..##.", ".####....", "#........"),
      LAYERS_FULL(".#######.", "..#####..", ".........", ".#######.", "..#####..", ".........", ".#######.", "..#####..", "........."),
      LAYERS_UP_TO(".........", ".........", ".........", ".#######.", "..#####..", ".........", ".#######.", "..#####..", "........."),
      LAYERS_SINGLE(".........", ".........", ".........", ".#######.", "..#####..", ".........", ".........", ".........", "........."),
      MINUS(".........", ".........", ".........", ".........", "..#####..", ".........", ".........", ".........", "........."),
      PLUS(".........", "....#....", "....#....", "....#....", "..#####..", "....#....", "....#....", "....#....", ".........");

      private final String[] pixels;
      private final int minX;
      private final int minY;
      private final int inkWidth;
      private final int inkHeight;

      private PixelIcon(String... pixels) {
         this.pixels = pixels;
         int left = Integer.MAX_VALUE;
         int top = Integer.MAX_VALUE;
         int right = Integer.MIN_VALUE;
         int bottom = Integer.MIN_VALUE;

         for (int y = 0; y < pixels.length; y++) {
            String row = pixels[y];

            for (int x = 0; x < row.length(); x++) {
               if (row.charAt(x) == '#') {
                  left = Math.min(left, x);
                  top = Math.min(top, y);
                  right = Math.max(right, x);
                  bottom = Math.max(bottom, y);
               }
            }
         }

         if (left == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Pixel icon must not be empty");
         } else {
            this.minX = left;
            this.minY = top;
            this.inkWidth = right - left + 1;
            this.inkHeight = bottom - top + 1;
         }
      }
   }
}
