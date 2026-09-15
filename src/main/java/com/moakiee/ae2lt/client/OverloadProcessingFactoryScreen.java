package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.widgets.ToolboxPanel;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.client.gui.LightningStatusIconWidget;
import com.moakiee.ae2lt.client.gui.LightningStatusLines;
import com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OverloadProcessingFactoryScreen extends AEBaseScreen<OverloadProcessingFactoryMenu> {
   private final ToggleButton autoExportButton;
   private final ActionButton configureOutputButton;
   private final OverloadProcessingFactoryFluidWidget inputFluidWidget;
   private final OverloadProcessingFactoryFluidWidget outputFluidWidget;

   public OverloadProcessingFactoryScreen(OverloadProcessingFactoryMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.f_97726_ = 176;
      this.f_97727_ = 178;
      this.widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), menu::getCompatibleUpgradeLines));
      if (menu.getToolbox().isPresent()) {
         this.widgets.add("toolbox", new ToolboxPanel(style, menu.getToolbox().getName()));
      }

      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.widgets.add("processArea", new OverloadProcessingFactoryProgressWidget(menu, style.getImage("processOverlay")));
      this.widgets.add("energyBar", new OverloadProcessingFactoryEnergyBar(menu, style.getImage("energyBar")));
      this.inputFluidWidget = new OverloadProcessingFactoryFluidWidget(menu, 0, menu::getInputFluid, menu::getInputTankCapacity);
      this.widgets.add("inputFluidBar", this.inputFluidWidget);
      this.outputFluidWidget = new OverloadProcessingFactoryFluidWidget(menu, 1, menu::getOutputFluid, menu::getOutputTankCapacity);
      this.widgets.add("outputFluidBar", this.outputFluidWidget);
      this.autoExportButton = new ToggleButton(Icon.AUTO_EXPORT_ON, Icon.AUTO_EXPORT_OFF, state -> menu.clientToggleAutoExport());
      this.autoExportButton
         .setTooltipOn(
            List.of(Component.m_237115_("ae2lt.gui.overload_factory.auto_export.title"), Component.m_237115_("ae2lt.gui.overload_factory.auto_export.on"))
         );
      this.autoExportButton
         .setTooltipOff(
            List.of(Component.m_237115_("ae2lt.gui.overload_factory.auto_export.title"), Component.m_237115_("ae2lt.gui.overload_factory.auto_export.off"))
         );
      this.addToLeftToolbar(this.autoExportButton);
      this.configureOutputButton = new ActionButton(
         ActionItems.TERMINAL_SETTINGS, () -> this.switchToScreen(new OverloadProcessingFactoryOutputConfigScreen(this))
      );
      this.configureOutputButton.m_93666_(Component.m_237115_("ae2lt.gui.overload_factory.configure_output"));
      this.addToLeftToolbar(this.configureOutputButton);
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
                     LightningStatusLines.extremeHighVoltage(menu.getExtremeHighVoltageAvailable())
                  )
            )
         );
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.autoExportButton.setState(((OverloadProcessingFactoryMenu)this.f_97732_).isAutoExportEnabled());
      this.configureOutputButton.setVisibility(((OverloadProcessingFactoryMenu)this.f_97732_).isAutoExportEnabled());
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (this.inputFluidWidget != null && this.inputFluidWidget.m_5953_(mouseX, mouseY) && this.inputFluidWidget.handleClick(button)) {
         return true;
      } else {
         return this.outputFluidWidget != null && this.outputFluidWidget.m_5953_(mouseX, mouseY) && this.outputFluidWidget.handleClick(button)
            ? true
            : super.m_6375_(mouseX, mouseY, button);
      }
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
}
