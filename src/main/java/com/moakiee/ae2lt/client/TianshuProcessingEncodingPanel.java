package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class TianshuProcessingEncodingPanel extends TianshuEncodingModePanel {
   private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(0, 70, 126, 68);
   private final ActionButton clearButton = new ActionButton(ActionItems.CLOSE, action -> this.menu.clear());
   private final ActionButton cycleOutputButton;
   private final Scrollbar scrollbar;

   TianshuProcessingEncodingPanel(TianshuPatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
      super(screen, widgets);
      this.clearButton.setHalfSize(true);
      widgets.add("processingClearPattern", this.clearButton);
      this.cycleOutputButton = new ActionButton(ActionItems.CYCLE_PROCESSING_OUTPUT, action -> this.menu.cycleProcessingOutput());
      this.cycleOutputButton.setHalfSize(true);
      widgets.add("processingCycleOutput", this.cycleOutputButton);
      this.scrollbar = widgets.addScrollBar("processingPatternModeScrollbar", Scrollbar.SMALL);
      this.scrollbar.setRange(0, this.menu.getProcessingInputSlots().length / 3 - 3, 3);
      this.scrollbar.setCaptureMouseWheel(false);
   }

   public void updateBeforeRender() {
      this.screen.repositionSlots(SlotSemantics.PROCESSING_INPUTS);
      this.screen.repositionSlots(SlotSemantics.PROCESSING_OUTPUTS);

      for (int i = 0; i < this.menu.getProcessingInputSlots().length; i++) {
         FakeSlot slot = this.menu.getProcessingInputSlots()[i];
         int effectiveRow = i / 3 - this.scrollbar.getCurrentScroll();
         slot.setActive(effectiveRow >= 0 && effectiveRow < 3);
         SlotPositionAccess.set(slot, slot.f_40220_, slot.f_40221_ - this.scrollbar.getCurrentScroll() * 18);
      }

      for (int i = 0; i < this.menu.getProcessingOutputSlots().length; i++) {
         FakeSlot slot = this.menu.getProcessingOutputSlots()[i];
         int effectiveRow = i - this.scrollbar.getCurrentScroll();
         slot.setActive(effectiveRow >= 0 && effectiveRow < 3);
         SlotPositionAccess.set(slot, slot.f_40220_, slot.f_40221_ - this.scrollbar.getCurrentScroll() * 18);
      }

      this.updateTooltipVisibility();
   }

   public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      BG.dest(bounds.m_110085_() + 9, bounds.m_110086_() + bounds.m_110091_() - 164).blit(graphics);
   }

   public boolean onMouseWheel(Point mousePosition, double delta) {
      return this.scrollbar.onMouseWheel(mousePosition, delta);
   }

   private void updateTooltipVisibility() {
      this.widgets.setTooltipAreaEnabled("processing-primary-output", this.visible && this.scrollbar.getCurrentScroll() == 0);
      this.widgets.setTooltipAreaEnabled("processing-optional-output1", this.visible && this.scrollbar.getCurrentScroll() > 0);
      this.widgets.setTooltipAreaEnabled("processing-optional-output2", this.visible);
      this.widgets.setTooltipAreaEnabled("processing-optional-output3", this.visible);
   }

   @Override
   ItemStack getTabIconItem() {
      return Items.f_41962_.m_7968_();
   }

   @Override
   Component getTabTooltip() {
      return GuiText.ProcessingPattern.text();
   }

   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      this.scrollbar.setVisible(visible);
      this.clearButton.setVisibility(visible);
      this.cycleOutputButton.setVisibility(visible && this.menu.canCycleProcessingOutputs());
      this.screen.setSlotsHidden(SlotSemantics.PROCESSING_INPUTS, !visible);
      this.screen.setSlotsHidden(SlotSemantics.PROCESSING_OUTPUTS, !visible);
      this.updateTooltipVisibility();
   }
}
