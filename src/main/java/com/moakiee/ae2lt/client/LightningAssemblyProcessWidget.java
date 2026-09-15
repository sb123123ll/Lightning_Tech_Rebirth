package com.moakiee.ae2lt.client;

import appeng.client.gui.style.Blitter;
import com.moakiee.ae2lt.menu.LightningAssemblyChamberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class LightningAssemblyProcessWidget extends AbstractWidget {
   private final LightningAssemblyChamberMenu menu;
   private final Blitter overlay;

   public LightningAssemblyProcessWidget(LightningAssemblyChamberMenu menu, Blitter overlay) {
      super(0, 0, overlay.getSrcWidth(), overlay.getSrcHeight(), Component.m_237119_());
      this.menu = menu;
      this.overlay = overlay.copy();
   }

   protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int filled = Mth.m_14045_((int)Math.ceil((double)this.f_93618_ * this.menu.getProgress()), 0, this.f_93618_);
      if (filled > 0) {
         this.overlay
            .copy()
            .src(this.overlay.getSrcX(), this.overlay.getSrcY(), filled, this.f_93619_)
            .dest(this.m_252754_(), this.m_252907_(), filled, this.f_93619_)
            .blit(guiGraphics);
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
