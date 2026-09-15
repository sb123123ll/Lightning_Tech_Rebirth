package com.moakiee.ae2lt.client;

import appeng.client.gui.style.Blitter;
import com.moakiee.ae2lt.menu.TeslaCoilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class TeslaCoilProcessWidget extends AbstractWidget {
   private static final int STAGE_COUNT = 20;
   private final TeslaCoilMenu menu;
   private final Blitter overlay;

   public TeslaCoilProcessWidget(TeslaCoilMenu menu, Blitter overlay) {
      super(0, 0, overlay.getSrcWidth(), overlay.getSrcHeight(), Component.m_237119_());
      this.menu = menu;
      this.overlay = overlay.copy();
   }

   protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      double progress = this.menu.getProgress();
      int stage = progress <= 0.0 ? 0 : Mth.m_14045_((int)Math.ceil(progress * 20.0), 1, 20);
      if (stage > 0) {
         int rows = Mth.m_14045_(Mth.m_14167_((float)(this.f_93619_ * stage) / 20.0F), 0, this.f_93619_);
         int topRows = rows / 2;
         int bottomRows = rows - topRows;
         if (topRows > 0) {
            this.overlay
               .copy()
               .src(this.overlay.getSrcX(), this.overlay.getSrcY(), this.f_93618_, topRows)
               .dest(this.m_252754_(), this.m_252907_(), this.f_93618_, topRows)
               .blit(guiGraphics);
         }

         if (bottomRows > 0) {
            int srcY = this.overlay.getSrcY() + this.f_93619_ - bottomRows;
            int destY = this.m_252907_() + this.f_93619_ - bottomRows;
            this.overlay
               .copy()
               .src(this.overlay.getSrcX(), srcY, this.f_93618_, bottomRows)
               .dest(this.m_252754_(), destY, this.f_93618_, bottomRows)
               .blit(guiGraphics);
         }
      }
   }

   protected void m_168797_(NarrationElementOutput narrationElementOutput) {
   }

   public boolean m_5953_(double mouseX, double mouseY) {
      return false;
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      return false;
   }
}
