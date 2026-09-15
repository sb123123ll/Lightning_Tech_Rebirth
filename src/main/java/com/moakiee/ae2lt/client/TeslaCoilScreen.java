package com.moakiee.ae2lt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import com.moakiee.ae2lt.client.gui.LightningStatusIconWidget;
import com.moakiee.ae2lt.client.gui.LightningStatusLines;
import com.moakiee.ae2lt.menu.TeslaCoilMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TeslaCoilScreen extends AEBaseScreen<TeslaCoilMenu> {
   private final TeslaCoilEnergyBar energyBar;
   private TeslaCoilModeButton modeButton;

   public TeslaCoilScreen(TeslaCoilMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.energyBar = new TeslaCoilEnergyBar(menu, style.getImage("energyBar"));
      this.widgets.add("energyBar", this.energyBar);
      this.widgets
         .add(
            "lightningStatus",
            new LightningStatusIconWidget(
               () -> List.of(
                     LightningStatusLines.title(),
                     menu.getStatusMessage(),
                     LightningStatusLines.progress(menu.getProgress()),
                     LightningStatusLines.energy(menu.getStoredEnergy(), menu.getEnergyCapacity()),
                     LightningStatusLines.highVoltage(menu.getHighVoltageAvailable()),
                     LightningStatusLines.extremeHighVoltage(menu.getExtremeHighVoltageAvailable()),
                     menu.getMatrixMessage()
                  )
            )
         );
      this.modeButton = new TeslaCoilModeButton(btn -> menu.clientCycleMode());
      this.addToLeftToolbar(this.modeButton);
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      if (this.modeButton != null) {
         this.modeButton.setMode(((TeslaCoilMenu)this.f_97732_).getMode());
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
