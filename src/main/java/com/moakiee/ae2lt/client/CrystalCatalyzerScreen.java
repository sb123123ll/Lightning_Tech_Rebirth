package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.client.gui.LightningStatusIconWidget;
import com.moakiee.ae2lt.client.gui.LightningStatusLines;
import com.moakiee.ae2lt.machine.crystalcatalyzer.recipe.Mode;
import com.moakiee.ae2lt.menu.CrystalCatalyzerMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrystalCatalyzerScreen extends AEBaseScreen<CrystalCatalyzerMenu> {
   private final ToggleButton autoExportButton;
   private final ActionButton configureOutputButton;
   private final TextureToggleButton modeButton;
   private final CrystalCatalyzerFluidWidget fluidWidget;

   public CrystalCatalyzerScreen(CrystalCatalyzerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.f_97726_ = 176;
      this.f_97727_ = 190;
      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.widgets.add("energyBar", new CrystalCatalyzerEnergyBar(menu, style.getImage("energyBar")));
      this.fluidWidget = new CrystalCatalyzerFluidWidget(menu, menu::getFluid, menu::getFluidCapacity);
      this.widgets.add("fluidBar", this.fluidWidget);
      this.widgets.add("processArea", new CrystalCatalyzerProgressWidget(menu, style.getImage("processOverlay")));
      this.autoExportButton = new ToggleButton(Icon.AUTO_EXPORT_ON, Icon.AUTO_EXPORT_OFF, state -> menu.clientToggleAutoExport());
      this.autoExportButton
         .setTooltipOn(
            List.of(Component.m_237115_("ae2lt.gui.crystal_catalyzer.auto_export.title"), Component.m_237115_("ae2lt.gui.crystal_catalyzer.auto_export.on"))
         );
      this.autoExportButton
         .setTooltipOff(
            List.of(Component.m_237115_("ae2lt.gui.crystal_catalyzer.auto_export.title"), Component.m_237115_("ae2lt.gui.crystal_catalyzer.auto_export.off"))
         );
      this.addToLeftToolbar(this.autoExportButton);
      this.configureOutputButton = new ActionButton(ActionItems.TERMINAL_SETTINGS, () -> this.switchToScreen(new CrystalCatalyzerOutputConfigScreen(this)));
      this.configureOutputButton.m_93666_(Component.m_237115_("ae2lt.gui.crystal_catalyzer.configure_output"));
      this.addToLeftToolbar(this.configureOutputButton);
      this.modeButton = new TextureToggleButton(TextureToggleButton.ButtonType.CRYSTAL_CATALYZER_MODE, btn -> menu.clientCycleMode());
      this.modeButton
         .setTooltipOff(
            List.of(
               Component.m_237115_("ae2lt.gui.crystal_catalyzer.mode.title"),
               Component.m_237115_("ae2lt.gui.crystal_catalyzer.mode.crystal"),
               Component.m_237115_("ae2lt.gui.crystal_catalyzer.mode.tooltip.crystal")
            )
         );
      this.modeButton
         .setTooltipOn(
            List.of(
               Component.m_237115_("ae2lt.gui.crystal_catalyzer.mode.title"),
               Component.m_237115_("ae2lt.gui.crystal_catalyzer.mode.dust"),
               Component.m_237115_("ae2lt.gui.crystal_catalyzer.mode.tooltip.dust")
            )
         );
      this.addToLeftToolbar(this.modeButton);
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
      this.autoExportButton.setState(((CrystalCatalyzerMenu)this.f_97732_).isAutoExportEnabled());
      this.configureOutputButton.setVisibility(((CrystalCatalyzerMenu)this.f_97732_).isAutoExportEnabled());
      this.modeButton.setState(((CrystalCatalyzerMenu)this.f_97732_).getMode() == Mode.DUST);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      return this.fluidWidget != null && this.fluidWidget.m_5953_(mouseX, mouseY) && this.fluidWidget.handleClick(button)
         ? true
         : super.m_6375_(mouseX, mouseY, button);
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
