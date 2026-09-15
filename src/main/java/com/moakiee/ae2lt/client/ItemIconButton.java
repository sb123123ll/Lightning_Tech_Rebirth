package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public class ItemIconButton extends IconButton {
   private final Item item;

   public ItemIconButton(Item item, Component tooltip, OnPress onPress) {
      super(onPress);
      this.item = item;
      this.setDisableBackground(true);
      this.m_93666_(tooltip);
      this.m_257544_(Tooltip.m_257550_(tooltip));
   }

   protected Icon getIcon() {
      return Icon.TOOLBAR_BUTTON_BACKGROUND;
   }

   protected Item getItemOverlay() {
      return this.item;
   }
}
