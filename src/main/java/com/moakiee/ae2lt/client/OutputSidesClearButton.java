package com.moakiee.ae2lt.client;

import appeng.client.gui.widgets.ITooltip;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

final class OutputSidesClearButton extends Button implements ITooltip {
   OutputSidesClearButton(Component tooltip, OnPress onPress) {
      super(0, 0, 8, 8, tooltip, onPress, Button.f_252438_);
   }

   protected void m_87963_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.f_93624_) {
         OutputSideButtonStyle.renderClearIcon(graphics, this.m_252754_(), this.m_252907_());
      }
   }

   public List<Component> getTooltipMessage() {
      return List.of(this.m_6035_());
   }

   public Rect2i getTooltipArea() {
      return new Rect2i(this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_());
   }

   public boolean isTooltipAreaVisible() {
      return this.f_93624_;
   }
}
