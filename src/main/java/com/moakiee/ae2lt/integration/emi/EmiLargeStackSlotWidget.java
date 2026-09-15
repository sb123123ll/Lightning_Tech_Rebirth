package com.moakiee.ae2lt.integration.emi;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class EmiLargeStackSlotWidget extends SlotWidget {
   EmiLargeStackSlotWidget(EmiIngredient stack, int x, int y) {
      super(stack, x, y);
   }

   public void drawStack(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      Bounds bounds = this.getBounds();
      int iconX = bounds.x() + (bounds.width() - 16) / 2;
      int iconY = bounds.y() + (bounds.height() - 16) / 2;
      this.getStack().render(graphics, iconX, iconY, delta, 13);
      LargeStackCountRenderer.renderCountAt(graphics, Minecraft.m_91087_().f_91062_, iconX, iconY, this.getStack().getAmount());
   }

   protected void addSlotTooltip(List<ClientTooltipComponent> tooltip) {
      long count = this.getStack().getAmount();
      if (count > 1L) {
         MutableComponent line = Component.m_237110_("ae2lt.gui.slot_count", new Object[]{String.format("%,d", count)}).m_130940_(ChatFormatting.GRAY);
         tooltip.add(ClientTooltipComponent.m_169948_(line.m_7532_()));
      }

      super.addSlotTooltip(tooltip);
   }
}
