package com.moakiee.ae2lt.client;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.menu.OverloadedPatternProviderMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class OverloadedPatternProviderAdvancedScreen<M extends OverloadedPatternProviderMenu> extends AESubScreen<M, OverloadedPatternProviderScreen<M>> {
   private static final int LABEL_X = 38;
   private static final int STRATEGY_Y = 33;
   private static final int SPEED_Y = 57;
   private static final int FILTER_Y = 81;
   private final TextureToggleButton wirelessStrategyButton;
   private final TextureToggleButton wirelessSpeedButton;
   private final TextureToggleButton filteredImportButton;

   public OverloadedPatternProviderAdvancedScreen(OverloadedPatternProviderScreen<M> parent) {
      super(parent, "/screens/overloaded_pattern_provider_advanced.json");
      MutableComponent backLabel = Component.m_237115_(((OverloadedPatternProviderMenu)parent.m_6262_()).getTitleTranslationKey());
      this.widgets.add("return", new TabButton(Icon.ARROW_LEFT, backLabel, btn -> this.returnToParent()));
      this.wirelessStrategyButton = new TextureToggleButton(
         TextureToggleButton.ButtonType.WIRELESS_STRATEGY, state -> ((OverloadedPatternProviderMenu)this.f_97732_).clientToggleWirelessDispatchMode()
      );
      this.wirelessStrategyButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.wireless_strategy.even")));
      this.wirelessStrategyButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.wireless_strategy.single")));
      this.widgets.add("wirelessStrategy", this.wirelessStrategyButton);
      this.wirelessSpeedButton = new TextureToggleButton(
         TextureToggleButton.ButtonType.SPEED, state -> ((OverloadedPatternProviderMenu)this.f_97732_).clientToggleWirelessSpeedMode()
      );
      this.wirelessSpeedButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.wireless_speed.fast")));
      this.wirelessSpeedButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.wireless_speed.normal")));
      this.widgets.add("wirelessSpeed", this.wirelessSpeedButton);
      this.filteredImportButton = new TextureToggleButton(
         TextureToggleButton.ButtonType.FILTERED_IMPORT, state -> ((OverloadedPatternProviderMenu)this.f_97732_).clientToggleFilteredImport()
      );
      this.filteredImportButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.filtered_import.on")));
      this.filteredImportButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.filtered_import.off")));
      this.widgets.add("filteredImport", this.filteredImportButton);
   }

   protected void m_7856_() {
      super.m_7856_();
      this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      boolean wirelessTuningVisible = ((OverloadedPatternProviderMenu)this.f_97732_).isWirelessTuningVisible();
      boolean wirelessTuningActive = wirelessTuningVisible && ((OverloadedPatternProviderMenu)this.f_97732_).isWirelessMode();
      this.wirelessStrategyButton.setState(((OverloadedPatternProviderMenu)this.f_97732_).isEvenDistributionMode());
      this.wirelessStrategyButton.setVisibility(wirelessTuningVisible);
      this.wirelessStrategyButton.f_93623_ = wirelessTuningActive;
      this.wirelessSpeedButton.setState(((OverloadedPatternProviderMenu)this.f_97732_).isFastSpeedMode());
      this.wirelessSpeedButton.setVisibility(wirelessTuningVisible);
      this.wirelessSpeedButton.f_93623_ = wirelessTuningActive;
      this.filteredImportButton.setState(((OverloadedPatternProviderMenu)this.f_97732_).isFilteredImport());
      this.filteredImportButton.setVisibility(((OverloadedPatternProviderMenu)this.f_97732_).isFilteredImportVisible());
   }

   public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
      boolean wirelessTuningVisible = ((OverloadedPatternProviderMenu)this.f_97732_).isWirelessTuningVisible();
      int wirelessColor = ((OverloadedPatternProviderMenu)this.f_97732_).isWirelessMode() ? 4210752 : 9474192;
      if (wirelessTuningVisible) {
         guiGraphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.gui.provider_advanced.distribution"), 38, 33, wirelessColor, false);
         guiGraphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.gui.provider_advanced.probe"), 38, 57, wirelessColor, false);
      }

      if (((OverloadedPatternProviderMenu)this.f_97732_).isFilteredImportVisible()) {
         guiGraphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.gui.provider_advanced.input_filter"), 38, 81, 4210752, false);
      }

      if (wirelessTuningVisible && !((OverloadedPatternProviderMenu)this.f_97732_).isWirelessMode()) {
         guiGraphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.gui.provider_advanced.wireless_hint"), 14, 105, 7368816, false);
      }
   }

   public void m_7379_() {
      this.returnToParent();
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !this.f_96541_.f_91066_.f_92092_.m_90832_(keyCode, scanCode)) {
         return super.m_7933_(keyCode, scanCode, modifiers);
      } else {
         this.returnToParent();
         return true;
      }
   }
}
