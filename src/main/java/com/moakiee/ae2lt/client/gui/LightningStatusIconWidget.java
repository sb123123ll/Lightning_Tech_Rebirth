package com.moakiee.ae2lt.client.gui;

import appeng.client.gui.widgets.ITooltip;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LightningStatusIconWidget extends AbstractWidget implements ITooltip {
   public static final ResourceLocation ICON = new ResourceLocation("ae2lt", "textures/gui/buttons/lightning.png");
   public static final int SIZE = 16;
   private final Supplier<List<Component>> tooltipSupplier;

   public LightningStatusIconWidget(Supplier<List<Component>> tooltipSupplier) {
      super(0, 0, 16, 16, Component.m_237119_());
      this.tooltipSupplier = tooltipSupplier;
   }

   protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.m_280163_(ICON, this.m_252754_(), this.m_252907_(), 0.0F, 0.0F, 16, 16, 16, 16);
   }

   public List<Component> getTooltipMessage() {
      List<Component> lines = this.tooltipSupplier.get();
      return lines != null ? lines : List.of();
   }

   public Rect2i getTooltipArea() {
      return new Rect2i(this.m_252754_(), this.m_252907_(), this.f_93618_, this.f_93619_);
   }

   public boolean isTooltipAreaVisible() {
      return true;
   }

   protected void m_168797_(NarrationElementOutput narrationElementOutput) {
   }
}
