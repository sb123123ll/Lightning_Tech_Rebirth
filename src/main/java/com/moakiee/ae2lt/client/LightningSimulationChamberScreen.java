package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.widgets.ToolboxPanel;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.client.gui.LightningStatusIconWidget;
import com.moakiee.ae2lt.client.gui.LightningStatusLines;
import com.moakiee.ae2lt.menu.LightningSimulationChamberMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LightningSimulationChamberScreen extends AEBaseScreen<LightningSimulationChamberMenu> {
   private final LightningSimulationEnergyBar energyBar;
   private final LightningSimulationProcessWidget processWidget;
   private final ToggleButton autoExportButton;
   private final ActionButton configureOutputButton;

   public LightningSimulationChamberScreen(LightningSimulationChamberMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));
      if (menu.getToolbox().isPresent()) {
         this.widgets.add("toolbox", new ToolboxPanel(style, menu.getToolbox().getName()));
      }

      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.processWidget = new LightningSimulationProcessWidget(menu, style.getImage("processOverlay"));
      this.widgets.add("processArea", this.processWidget);
      this.energyBar = new LightningSimulationEnergyBar(menu, style.getImage("energyBar"));
      this.widgets.add("energyBar", this.energyBar);
      this.widgets
         .add(
            "lightningStatus",
            new LightningStatusIconWidget(
               () -> List.of(
                     LightningStatusLines.title(),
                     LightningStatusLines.status(menu.isWorking()),
                     LightningStatusLines.progress(menu.getProgress()),
                     LightningStatusLines.energy(menu.getStoredEnergy(), menu.getEnergyCapacity()),
                     LightningStatusLines.highVoltage(menu.getHighVoltageAvailable()),
                     LightningStatusLines.extremeHighVoltage(menu.getExtremeHighVoltageAvailable()),
                     menu.getMatrixMessage(),
                     menu.getLightningDemandMessage(),
                     menu.getSubstitutionMessage()
                  )
            )
         );
      this.autoExportButton = new ToggleButton(Icon.AUTO_EXPORT_ON, Icon.AUTO_EXPORT_OFF, state -> menu.clientToggleAutoExport());
      this.autoExportButton
         .setTooltipOn(
            List.of(
               Component.m_237115_("ae2lt.gui.lightning_simulation.auto_export.title"), Component.m_237115_("ae2lt.gui.lightning_simulation.auto_export.on")
            )
         );
      this.autoExportButton
         .setTooltipOff(
            List.of(
               Component.m_237115_("ae2lt.gui.lightning_simulation.auto_export.title"), Component.m_237115_("ae2lt.gui.lightning_simulation.auto_export.off")
            )
         );
      this.addToLeftToolbar(this.autoExportButton);
      this.configureOutputButton = new ActionButton(ActionItems.TERMINAL_SETTINGS, () -> this.switchToScreen(new LightningSimulationOutputConfigScreen(this)));
      this.configureOutputButton.m_93666_(Component.m_237115_("ae2lt.gui.lightning_simulation.configure_output"));
      this.addToLeftToolbar(this.configureOutputButton);
   }

   public void m_280092_(GuiGraphics guiGraphics, Slot slot) {
      super.m_280092_(guiGraphics, slot);
      LargeStackCountRenderer.renderSlotCount(guiGraphics, this.f_96547_, slot);
   }

   protected List<Component> m_280553_(ItemStack stack) {
      List<Component> lines = super.m_280553_(stack);
      LargeStackCountRenderer.appendCountTooltip(lines, this.f_97734_);
      return lines;
   }

   private List<Component> getCompatibleUpgrades() {
      ArrayList<Component> list = new ArrayList<>();
      list.add(GuiText.CompatibleUpgrades.text());
      list.addAll(Upgrades.getTooltipLinesForMachine(((LightningSimulationChamberMenu)this.f_97732_).getHost().getUpgrades().getUpgradableItem()));
      return list;
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.autoExportButton.setState(((LightningSimulationChamberMenu)this.f_97732_).isAutoExportEnabled());
      this.configureOutputButton.setVisibility(((LightningSimulationChamberMenu)this.f_97732_).isAutoExportEnabled());
   }
}
