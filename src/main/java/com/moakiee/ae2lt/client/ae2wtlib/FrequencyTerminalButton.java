package com.moakiee.ae2lt.client.ae2wtlib;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.widgets.VerticalButtonBar;
import appeng.menu.AEBaseMenu;
import com.moakiee.ae2lt.client.FrequencyBindingClient;
import com.moakiee.ae2lt.client.TextureToggleButton;
import com.moakiee.ae2lt.item.OverloadedFrequencyCardItem;
import com.moakiee.ae2lt.item.TerminalCardAccess;
import com.moakiee.ae2lt.mixin.client.AEBaseScreenAccessor;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class FrequencyTerminalButton {
   private FrequencyTerminalButton() {
   }

   public static boolean shouldInject(AEBaseScreen<?> screen) {
      return !ModList.get().isLoaded("ae2wtlib") ? false : screen instanceof IUniversalTerminalCapable;
   }

   public static FrequencyTerminalButton.ToolbarButtons addToToolbar(AEBaseScreen<?> screen) {
      VerticalButtonBar toolbar = ((AEBaseScreenAccessor)screen).ae2lt$getVerticalToolbar();
      FrequencyTerminalButton.ToolbarButtons buttons = new FrequencyTerminalButton.ToolbarButtons(
         FrequencyBindingClient.createCardToolbarButton(), FrequencyBindingClient.createCardAutoConnectToolbarButton()
      );
      toolbar.add(buttons.configureButton());
      toolbar.add(buttons.autoConnectButton());
      buttons.update(screen);
      return buttons;
   }

   private static ItemStack findInstalledFrequencyCard(AEBaseScreen<?> screen) {
      return ((AEBaseMenu)screen.m_6262_()).getTarget() instanceof ItemMenuHost terminalHost
         ? TerminalCardAccess.findCard(terminalHost.getUpgrades())
         : ItemStack.f_41583_;
   }

   public static record ToolbarButtons(TextureToggleButton configureButton, TextureToggleButton autoConnectButton) {
      public void update(AEBaseScreen<?> screen) {
         ItemStack card = FrequencyTerminalButton.findInstalledFrequencyCard(screen);
         boolean hasCard = !card.m_41619_();
         this.configureButton.setVisibility(hasCard);
         this.autoConnectButton.setVisibility(hasCard);
         if (hasCard) {
            this.autoConnectButton.setState(OverloadedFrequencyCardItem.getData(card).autoConnect());
         }
      }
   }
}
