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
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;

final class TianshuSmithingTableEncodingPanel extends TianshuEncodingModePanel {
   private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(128, 70, 126, 68);
   private final ActionButton clearButton = new ActionButton(ActionItems.CLOSE, action -> this.menu.clear());
   private final ToggleButton substitutionsButton;
   private final Slot resultSlot;

   TianshuSmithingTableEncodingPanel(TianshuPatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
      super(screen, widgets);
      this.clearButton.setHalfSize(true);
      widgets.add("smithingTableClearPattern", this.clearButton);
      this.substitutionsButton = this.createSubstitutionButton();
      this.resultSlot = new Slot(new SimpleContainer(1), 0, 0, 0);
      this.menu.addClientSideSlot(this.resultSlot, SlotSemantics.SMITHING_TABLE_RESULT);
   }

   @Override
   ItemStack getTabIconItem() {
      return Items.f_42775_.m_7968_();
   }

   @Override
   Component getTabTooltip() {
      return GuiText.SmithingTablePattern.text();
   }

   private ToggleButton createSubstitutionButton() {
      ToggleButton button = new ToggleButton(Icon.SUBSTITUTION_ENABLED, Icon.SUBSTITUTION_DISABLED, this.menu::setSubstitute);
      button.setHalfSize(true);
      button.setTooltipOn(List.of(ButtonToolTips.SubstitutionsOn.text(), ButtonToolTips.SubstitutionsDescEnabled.text()));
      button.setTooltipOff(List.of(ButtonToolTips.SubstitutionsOff.text(), ButtonToolTips.SubstitutionsDescDisabled.text()));
      this.widgets.add("smithingTableSubstitutions", button);
      return button;
   }

   public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      BG.dest(bounds.m_110085_() + 9, bounds.m_110086_() + bounds.m_110091_() - 164).blit(graphics);
   }

   public void updateBeforeRender() {
      this.substitutionsButton.setState(this.menu.substitute);
      SimpleContainer recipeInput = new SimpleContainer(3);
      recipeInput.m_6836_(0, this.menu.getSmithingTableTemplateSlot().m_7993_());
      recipeInput.m_6836_(1, this.menu.getSmithingTableBaseSlot().m_7993_());
      recipeInput.m_6836_(2, this.menu.getSmithingTableAdditionSlot().m_7993_());
      Level level = this.menu.getPlayer().m_9236_();
      SmithingRecipe recipe = (SmithingRecipe)level.m_7465_().m_44015_(RecipeType.f_44113_, recipeInput, level).orElse(null);
      this.resultSlot.m_5852_(recipe == null ? ItemStack.f_41583_ : recipe.m_5874_(recipeInput, level.m_9598_()));
   }

   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      this.clearButton.setVisibility(visible);
      this.substitutionsButton.setVisibility(visible);
      this.screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_TEMPLATE, !visible);
      this.screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_BASE, !visible);
      this.screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_ADDITION, !visible);
      this.screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_RESULT, !visible);
   }
}
