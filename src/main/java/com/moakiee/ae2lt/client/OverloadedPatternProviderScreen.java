package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.VerticalButtonBar;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.api.client.PatternProviderToolbarButtonHider;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.menu.OverloadedPatternProviderMenu;
import com.moakiee.ae2lt.mixin.client.AEBaseScreenAccessor;
import com.moakiee.ae2lt.mixin.client.PatternProviderScreenAccessor;
import com.moakiee.ae2lt.mixin.client.VerticalButtonBarAccessor;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class OverloadedPatternProviderScreen<M extends OverloadedPatternProviderMenu> extends PatternProviderScreen<M> {
   private static final List<Component> RETURN_TIP_OFF = List.of(Component.m_237115_("ae2lt.gui.return_mode.off"));
   private static final List<Component> RETURN_TIP_AUTO = List.of(Component.m_237115_("ae2lt.gui.return_mode.auto"));
   private static final List<Component> RETURN_TIP_EJECT = List.of(Component.m_237115_("ae2lt.gui.return_mode.eject"));
   private static final List<Component> ADAPTIVE_BATCH_TIP_ON = List.of(Component.m_237115_("ae2lt.gui.adaptive_batch.on"));
   private static final List<Component> ADAPTIVE_BATCH_TIP_OFF = List.of(Component.m_237115_("ae2lt.gui.adaptive_batch.off"));
   private final TextureToggleButton modeButton;
   private final TextureToggleButton autoReturnButton;
   private final ProviderBlockingModeButton blockingModeButton;
   private final TextureToggleButton adaptiveBatchButton;
   private final ActionButton advancedSettingsButton;
   private static final int SLOTS_PER_PAGE = 36;

   public OverloadedPatternProviderScreen(M menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.removeHiddenToolbarButtons();
      this.removeVanillaBlockingModeButton();
      this.blockingModeButton = new ProviderBlockingModeButton(btn -> menu.clientCycleBlockingMode());
      this.addToLeftToolbar(this.blockingModeButton);
      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.autoReturnButton = new TextureToggleButton(TextureToggleButton.ButtonType.AUTO_RETURN, btn -> menu.clientToggleAutoReturn());
      this.addToLeftToolbar(this.autoReturnButton);
      this.modeButton = new TextureToggleButton(TextureToggleButton.ButtonType.MODE, btn -> menu.clientToggleMode());
      this.modeButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.provider_mode.wireless")));
      this.modeButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.provider_mode.normal")));
      this.addToLeftToolbar(this.modeButton);
      this.adaptiveBatchButton = new TextureToggleButton(TextureToggleButton.ButtonType.ADAPTIVE_BATCH, state -> menu.clientToggleAdaptiveBatch());
      this.adaptiveBatchButton.setTooltipOn(ADAPTIVE_BATCH_TIP_ON);
      this.adaptiveBatchButton.setTooltipOff(ADAPTIVE_BATCH_TIP_OFF);
      this.addToLeftToolbar(this.adaptiveBatchButton);
      this.advancedSettingsButton = new ActionButton(
         ActionItems.TERMINAL_SETTINGS, () -> this.switchToScreen(new OverloadedPatternProviderAdvancedScreen<M>(this))
      );
      this.advancedSettingsButton.m_93666_(Component.m_237115_("ae2lt.gui.provider_advanced.open"));
      this.addToLeftToolbar(this.advancedSettingsButton);
   }

   protected void m_7856_() {
      super.m_7856_();
      this.alignSlotPositions();
   }

   private void alignSlotPositions() {
      List<Slot> patternSlots = ((OverloadedPatternProviderMenu)this.f_97732_).getSlots(SlotSemantics.ENCODED_PATTERN);
      int total = patternSlots.size();
      if (total > 36) {
         for (int i = 36; i < total; i++) {
            int ref = i % 36;
            SlotPositionAccess.set(patternSlots.get(i), patternSlots.get(ref).f_40220_, patternSlots.get(ref).f_40221_);
         }
      }
   }

   public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
      int tp = ((OverloadedPatternProviderMenu)this.f_97732_).getTotalPages();
      if (tp > 1) {
         String pageText = ((OverloadedPatternProviderMenu)this.f_97732_).getCurrentPage() + 1 + "/" + tp;
         int textWidth = this.f_96547_.m_92895_(pageText);
         guiGraphics.m_280056_(this.f_96547_, pageText, PatternProviderPageIndicator.centeredX(this.f_97726_, textWidth), 33, 4210752, false);
      }
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      ((OverloadedPatternProviderMenu)this.f_97732_).showPage(((OverloadedPatternProviderMenu)this.f_97732_).getCurrentPage());
      this.setTextContent("dialog_title", Component.m_237115_(((OverloadedPatternProviderMenu)this.f_97732_).getTitleTranslationKey()));
      this.blockingModeButton.setState(((OverloadedPatternProviderMenu)this.f_97732_).getBlockingModeOrdinal());
      this.blockingModeButton.setVisibility(((OverloadedPatternProviderMenu)this.f_97732_).isBlockingModeVisible());
      this.modeButton.setState(((OverloadedPatternProviderMenu)this.f_97732_).isWirelessMode());
      this.modeButton.setVisibility(((OverloadedPatternProviderMenu)this.f_97732_).isModeSwitchVisible());
      this.autoReturnButton.setTooltipAt(OverloadedPatternProviderBlockEntity.ReturnMode.OFF.ordinal(), RETURN_TIP_OFF);
      this.autoReturnButton.setTooltipAt(OverloadedPatternProviderBlockEntity.ReturnMode.AUTO.ordinal(), RETURN_TIP_AUTO);
      this.autoReturnButton.setTooltipAt(OverloadedPatternProviderBlockEntity.ReturnMode.EJECT.ordinal(), RETURN_TIP_EJECT);
      this.autoReturnButton.setStateIndex(((OverloadedPatternProviderMenu)this.f_97732_).getReturnModeOrdinal());
      this.adaptiveBatchButton.setState(((OverloadedPatternProviderMenu)this.f_97732_).isAdaptiveBatchEnabled());
      this.advancedSettingsButton
         .setVisibility(
            ((OverloadedPatternProviderMenu)this.f_97732_).isWirelessTuningVisible()
               || ((OverloadedPatternProviderMenu)this.f_97732_).isFilteredImportVisible()
         );
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (((OverloadedPatternProviderMenu)this.f_97732_).getTotalPages() > 1) {
         PatternProviderPageScroll.Direction direction = PatternProviderPageScroll.directionForDelta(delta);
         if (direction == PatternProviderPageScroll.Direction.PREVIOUS) {
            ((OverloadedPatternProviderMenu)this.f_97732_).clientPrevPage();
            return true;
         }

         if (direction == PatternProviderPageScroll.Direction.NEXT) {
            ((OverloadedPatternProviderMenu)this.f_97732_).clientNextPage();
            return true;
         }
      }

      return super.m_6050_(mouseX, mouseY, delta);
   }

   private void removeVanillaBlockingModeButton() {
      VerticalButtonBar toolbar = ((AEBaseScreenAccessor)this).ae2lt$getVerticalToolbar();
      List<Button> buttons = ((VerticalButtonBarAccessor)toolbar).ae2lt$getButtons();
      buttons.remove(((PatternProviderScreenAccessor)this).ae2lt$getBlockingModeButton());
   }

   private void removeHiddenToolbarButtons() {
      VerticalButtonBar toolbar = ((AEBaseScreenAccessor)this).ae2lt$getVerticalToolbar();
      List<Button> buttons = ((VerticalButtonBarAccessor)toolbar).ae2lt$getButtons();
      PatternProviderToolbarButtonHider.removeHiddenToolbarButtons(buttons);
   }
}
