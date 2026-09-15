package com.moakiee.ae2lt.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class TianshuPatternConfigLayout {
   static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/tianshu_pattern_config.png");
   static final int TEXTURE_SIZE = 256;
   static final int GUI_WIDTH = 190;
   static final int HEADER_HEIGHT = 32;
   static final int ROW_HEIGHT = 25;
   static final int VISIBLE_ROWS = 5;
   static final int FOOTER_HEIGHT = 30;
   static final int GUI_HEIGHT = 187;
   static final int ROW_LEFT = 8;
   static final int ROW_TEXTURE_X = 9;
   static final int ROW_TEXTURE_Y = 190;
   static final int ROW_TEXTURE_WIDTH = 158;
   static final int ROW_CONTENT_X_OFFSET = 4;
   static final int SCROLLBAR_HEIGHT = 129;

   private TianshuPatternConfigLayout() {
   }

   static void drawBackground(GuiGraphics graphics, int offsetX, int offsetY) {
      graphics.m_280163_(TEXTURE, offsetX, offsetY, 0.0F, 0.0F, 190, 187, 256, 256);

      for (int visible = 0; visible < 5; visible++) {
         int top = offsetY + 32 + visible * 25;
         graphics.m_280163_(TEXTURE, offsetX + 9, top, 9.0F, 190.0F, 158, 25, 256, 256);
      }
   }
}
