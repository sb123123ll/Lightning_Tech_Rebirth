package com.moakiee.ae2lt.client;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class MultiblockControllerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
   protected static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/guis/multiblock_controller.png");
   protected static final int TEXT_X = 12;
   protected static final int VALUE_RIGHT = 197;
   protected static final int TITLE_Y = 5;
   protected static final int STATUS_TEXT_Y = 22;
   protected static final int ROW_Y = 56;
   protected static final int LINE_H = 13;
   protected static final int HINT_GAP = 17;
   protected static final int FOOTER_ROW_Y = 138;
   protected static final int FOOTER_MID_Y = 144;
   protected static final int COL_TITLE = 2764351;
   protected static final int COL_LABEL = 5133168;
   protected static final int COL_VALUE = 1316383;
   protected static final int COL_MUTED = 5659768;
   protected static final int COL_GREEN = 1862452;
   protected static final int COL_AMBER = 8019476;
   protected static final int COL_RED = 9382691;
   protected static final int COL_BLUE = 2051705;

   protected MultiblockControllerScreen(T menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 209;
      this.f_97727_ = 167;
      this.f_97731_ = 10000;
      this.f_97729_ = 10000;
   }

   protected void m_7286_(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      guiGraphics.m_280163_(TEXTURE, this.f_97735_, this.f_97736_, 0.0F, 0.0F, this.f_97726_, this.f_97727_, 256, 256);
   }

   public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(guiGraphics);
      super.m_88315_(guiGraphics, mouseX, mouseY, partialTick);
      this.m_280072_(guiGraphics, mouseX, mouseY);
   }

   protected void drawTitle(GuiGraphics guiGraphics) {
      guiGraphics.m_280614_(this.f_96547_, this.f_96539_, 8, 5, 2764351, false);
   }

   protected void drawStatus(GuiGraphics guiGraphics, Component text, int color) {
      guiGraphics.m_280509_(12, 24, 16, 28, 0xFF000000 | color);
      guiGraphics.m_280614_(this.f_96547_, text, 20, 22, color, false);
   }

   protected void drawRow(GuiGraphics guiGraphics, int y, Component label, String value, int valueColor) {
      guiGraphics.m_280614_(this.f_96547_, label, 12, y, 5133168, false);
      guiGraphics.m_280056_(this.f_96547_, value, 197 - this.f_96547_.m_92895_(value), y, valueColor, false);
   }

   protected void drawUnformed(GuiGraphics guiGraphics, Component issue, String hintKey) {
      int y = 56;

      for (FormattedCharSequence line : this.f_96547_.m_92923_(issue, 185)) {
         guiGraphics.m_280649_(this.f_96547_, line, 12, y, 9382691, false);
         y += 11;
      }

      guiGraphics.m_280614_(this.f_96547_, Component.m_237115_(hintKey), 12, y - 11 + 17, 5659768, false);
   }

   protected void drawGauge(GuiGraphics guiGraphics, double fill, int fillColor, double markLo, double markHi) {
      int x0 = 12;
      int y0 = 149;
      int w = 185;
      int h = 10;
      guiGraphics.m_280509_(x0 - 1, y0 - 1, x0 + w + 1, y0, -9867896);
      guiGraphics.m_280509_(x0 - 1, y0 - 1, x0, y0 + h + 1, -9867896);
      guiGraphics.m_280509_(x0 - 1, y0 + h, x0 + w + 1, y0 + h + 1, -855310);
      guiGraphics.m_280509_(x0 + w, y0, x0 + w + 1, y0 + h + 1, -855310);
      guiGraphics.m_280509_(x0, y0, x0 + w, y0 + h, -6643788);

      for (int i = 1; i < 4; i++) {
         int tx = x0 + w * i / 4;
         guiGraphics.m_280509_(tx, y0, tx + 1, y0 + h, -7959644);
      }

      int lo = x0 + (int)Math.round((double)w * markLo);
      int hi = x0 + (int)Math.round((double)w * markHi);
      if (markLo >= 0.0) {
         guiGraphics.m_280509_(lo, y0, hi, y0 + h, -8806010);
      }

      int fw = Math.max(0, Math.min(w, (int)Math.round((double)w * fill)));
      if (fw > 0) {
         guiGraphics.m_280509_(x0, y0, x0 + fw, y0 + 1, mix(fillColor, 16777215, 0.45));
         guiGraphics.m_280509_(x0, y0 + 1, x0 + fw, y0 + h - 1, fillColor);
         guiGraphics.m_280509_(x0, y0 + h - 1, x0 + fw, y0 + h, mix(fillColor, 0, 0.3));
      }

      if (markLo >= 0.0) {
         guiGraphics.m_280509_(lo, y0, lo + 1, y0 + h, -855310);
         guiGraphics.m_280509_(hi - 1, y0, hi, y0 + h, -855310);
      }
   }

   private static int mix(int color, int target, double k) {
      int r = (int)Math.round((double)(color >> 16 & 0xFF) * (1.0 - k) + (double)(target >> 16 & 0xFF) * k);
      int g = (int)Math.round((double)(color >> 8 & 0xFF) * (1.0 - k) + (double)(target >> 8 & 0xFF) * k);
      int b = (int)Math.round((double)(color & 0xFF) * (1.0 - k) + (double)(target & 0xFF) * k);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   protected static String formatCount(long value) {
      return value != Long.MAX_VALUE && value != 2147483647L ? String.format(Locale.ROOT, "%,d", value) : "∞";
   }

   protected static String percent(double value) {
      return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
   }
}
