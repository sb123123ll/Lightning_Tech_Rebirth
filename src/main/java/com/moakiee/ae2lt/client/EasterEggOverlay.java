package com.moakiee.ae2lt.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class EasterEggOverlay implements IGuiOverlay {
   public static final EasterEggOverlay INSTANCE = new EasterEggOverlay();
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/easter_egg.png");
   private static final int DISPLAY_TICKS = 40;
   private static int ticksRemaining = 0;

   private EasterEggOverlay() {
   }

   public static void trigger() {
      ticksRemaining = 40;
   }

   public static boolean isActive() {
      return ticksRemaining > 0;
   }

   public static void tick() {
      if (ticksRemaining > 0) {
         ticksRemaining--;
      }
   }

   public static void reset() {
      ticksRemaining = 0;
   }

   public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
      if (ticksRemaining > 0) {
         int imgWidth = 512;
         int imgHeight = 436;
         float aspect = (float)imgWidth / (float)imgHeight;
         int maxW = screenWidth * 3 / 4;
         int maxH = screenHeight * 3 / 4;
         int drawW;
         int drawH;
         if ((float)maxW / (float)maxH > aspect) {
            drawH = maxH;
            drawW = (int)((float)maxH * aspect);
         } else {
            drawW = maxW;
            drawH = (int)((float)maxW / aspect);
         }

         int x = (screenWidth - drawW) / 2;
         int y = (screenHeight - drawH) / 2;
         float alpha = Math.min(1.0F, (float)ticksRemaining / 10.0F);
         guiGraphics.m_280168_().m_85836_();
         guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, alpha);
         guiGraphics.m_280411_(TEXTURE, x, y, drawW, drawH, 0.0F, 0.0F, imgWidth, imgHeight, imgWidth, imgHeight);
         guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, 1.0F);
         guiGraphics.m_280168_().m_85849_();
      }
   }
}
