package com.moakiee.ae2lt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.moakiee.ae2lt.client.gui.LightningStatusIconWidget;
import com.moakiee.ae2lt.client.gui.LightningStatusLines;
import com.moakiee.ae2lt.menu.OverloadedPowerSupplyMenu;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OverloadedPowerSupplyScreen extends AEBaseScreen<OverloadedPowerSupplyMenu> {
   private final TextureToggleButton modeButton;

   public OverloadedPowerSupplyScreen(OverloadedPowerSupplyMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.modeButton = new TextureToggleButton(TextureToggleButton.ButtonType.OVERLOAD_MODE, btn -> menu.clientCycleMode());
      this.modeButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.overloaded_power_supply.mode.normal")));
      this.modeButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.overloaded_power_supply.mode.overload")));
      this.addToLeftToolbar(this.modeButton);
      this.widgets
         .add(
            "lightningStatus",
            new LightningStatusIconWidget(
               () -> List.of(
                     LightningStatusLines.title(),
                     menu.getStatusMessage(),
                     menu.getCellMessage(),
                     menu.getConnectionsMessage(),
                     menu.getTicketsMessage(),
                     menu.getBufferMessage()
                  )
            )
         );
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.modeButton.setState(((OverloadedPowerSupplyMenu)this.f_97732_).getMode() == OverloadedPowerSupplyBlockEntity.PowerMode.OVERLOAD);
   }
}
