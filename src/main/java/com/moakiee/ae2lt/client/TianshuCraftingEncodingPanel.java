package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class TianshuCraftingEncodingPanel extends TianshuEncodingModePanel {
   private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(0, 0, 126, 68);
   private final ActionButton clearButton = new ActionButton(ActionItems.CLOSE, action -> this.menu.clear());
   private final ToggleButton substitutionsButton;
   private final ToggleButton fluidSubstitutionsButton;

   TianshuCraftingEncodingPanel(TianshuPatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
      super(screen, widgets);
      this.clearButton.setHalfSize(true);
      widgets.add("craftingClearPattern", this.clearButton);
      this.substitutionsButton = this.createCraftingSubstitutionButton();
      this.fluidSubstitutionsButton = this.createCraftingFluidSubstitutionButton();
   }

   @Override
   ItemStack getTabIconItem() {
      return Items.f_41960_.m_7968_();
   }

   @Override
   Component getTabTooltip() {
      return GuiText.CraftingPattern.text();
   }

   private ToggleButton createCraftingSubstitutionButton() {
      ToggleButton button = new ToggleButton(Icon.SUBSTITUTION_ENABLED, Icon.SUBSTITUTION_DISABLED, this.menu::setSubstitute);
      button.setHalfSize(true);
      button.setTooltipOn(List.of(ButtonToolTips.SubstitutionsOn.text(), ButtonToolTips.SubstitutionsDescEnabled.text()));
      button.setTooltipOff(List.of(ButtonToolTips.SubstitutionsOff.text(), ButtonToolTips.SubstitutionsDescDisabled.text()));
      this.widgets.add("craftingSubstitutions", button);
      return button;
   }

   private ToggleButton createCraftingFluidSubstitutionButton() {
      ToggleButton button = new ToggleButton(Icon.FLUID_SUBSTITUTION_ENABLED, Icon.FLUID_SUBSTITUTION_DISABLED, this.menu::setSubstituteFluids);
      button.setHalfSize(true);
      button.setTooltipOn(List.of(ButtonToolTips.FluidSubstitutions.text(), ButtonToolTips.FluidSubstitutionsDescEnabled.text()));
      button.setTooltipOff(List.of(ButtonToolTips.FluidSubstitutions.text(), ButtonToolTips.FluidSubstitutionsDescDisabled.text()));
      this.widgets.add("craftingFluidSubstitutions", button);
      return button;
   }

   public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      BG.dest(bounds.m_110085_() + 9, bounds.m_110086_() + bounds.m_110091_() - 164).blit(graphics);
      int absoluteMouseX = bounds.m_110085_() + mouse.getX();
      int absoluteMouseY = bounds.m_110086_() + mouse.getY();
      if (this.menu.substituteFluids && this.fluidSubstitutionsButton.m_5953_((double)absoluteMouseX, (double)absoluteMouseY)) {
         IntIterator var6 = this.menu.slotsSupportingFluidSubstitution.iterator();

         while (var6.hasNext()) {
            Integer slotIndex = (Integer)var6.next();
            this.drawSlotGreenBackground(bounds, graphics, this.menu.getCraftingGridSlots()[slotIndex]);
         }
      }
   }

   private void drawSlotGreenBackground(Rect2i bounds, GuiGraphics graphics, Slot slot) {
      int slotX = bounds.m_110085_() + slot.f_40220_;
      int slotY = bounds.m_110086_() + slot.f_40221_;
      graphics.m_280509_(slotX, slotY, slotX + 16, slotY + 16, 2130771712);
   }

   public void updateBeforeRender() {
      this.substitutionsButton.setState(this.menu.substitute);
      this.fluidSubstitutionsButton.setState(this.menu.substituteFluids);
   }

   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      this.clearButton.setVisibility(visible);
      this.substitutionsButton.setVisibility(visible);
      this.fluidSubstitutionsButton.setVisibility(visible);
      this.screen.setSlotsHidden(SlotSemantics.CRAFTING_GRID, !visible);
      this.screen.setSlotsHidden(SlotSemantics.CRAFTING_RESULT, !visible);
   }
}
