package com.moakiee.ae2lt.client;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

abstract class TianshuEncodingModePanel implements ICompositeWidget {
   protected final TianshuPatternEncodingTermScreen<?> screen;
   protected final TianshuPatternEncodingTermMenu menu;
   protected final WidgetContainer widgets;
   protected boolean visible;
   protected int x;
   protected int y;

   TianshuEncodingModePanel(TianshuPatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
      this.screen = screen;
      this.menu = (TianshuPatternEncodingTermMenu)screen.m_6262_();
      this.widgets = widgets;
   }

   abstract ItemStack getTabIconItem();

   abstract Component getTabTooltip();

   public void setPosition(Point position) {
      this.x = position.getX();
      this.y = position.getY();
   }

   public void setSize(int width, int height) {
   }

   public Rect2i getBounds() {
      return new Rect2i(this.x, this.y, 126, 68);
   }

   public final boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }
}
