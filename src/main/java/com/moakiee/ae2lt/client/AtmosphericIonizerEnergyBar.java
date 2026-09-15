package com.moakiee.ae2lt.client;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import com.moakiee.ae2lt.menu.AtmosphericIonizerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

public class AtmosphericIonizerEnergyBar extends AbstractWidget implements ITooltip {
   private final AtmosphericIonizerMenu menu;
   private final Blitter fill;

   public AtmosphericIonizerEnergyBar(AtmosphericIonizerMenu menu, Blitter fill) {
      super(0, 0, fill.getSrcWidth(), fill.getSrcHeight(), Component.m_237119_());
      this.menu = menu;
      this.fill = fill.copy();
   }

   protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.fill.copy().opacity(0.2F).dest(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_).blit(guiGraphics);
      long totalEnergy = Math.max(1L, this.menu.getTotalEnergyRequired());
      long consumedEnergy = Math.min(this.menu.getConsumedEnergy(), totalEnergy);
      int filled = (int)Math.round((double)this.f_93619_ * (double)consumedEnergy / (double)totalEnergy);
      if (filled > 0) {
         int srcY = this.fill.getSrcY() + this.f_93619_ - filled;
         int destY = this.m_252907_() + this.f_93619_ - filled;
         this.fill.copy().src(this.fill.getSrcX(), srcY, this.f_93618_, filled).dest(this.m_252754_(), destY, this.f_93618_, filled).blit(guiGraphics);
      }
   }

   public List<Component> getTooltipMessage() {
      return List.of(
         Component.m_237110_("ae2lt.gui.atmospheric_ionizer.energy.tooltip", new Object[]{this.menu.getConsumedEnergy(), this.menu.getTotalEnergyRequired()})
      );
   }

   public Rect2i getTooltipArea() {
      return new Rect2i(this.m_252754_() - 2, this.m_252907_() - 2, this.f_93618_ + 4, this.f_93619_ + 4);
   }

   public boolean isTooltipAreaVisible() {
      return true;
   }

   protected void m_168797_(NarrationElementOutput narrationElementOutput) {
   }
}
