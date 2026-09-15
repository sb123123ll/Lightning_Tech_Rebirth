package com.moakiee.ae2lt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import com.moakiee.ae2lt.me.cell.VoidCellMode;
import com.moakiee.ae2lt.menu.VoidCellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class VoidCellScreen extends AEBaseScreen<VoidCellMenu> {
   private final VoidCellModeButton trash;
   private final VoidCellModeButton matterBalls;
   private final VoidCellModeButton singularity;

   public VoidCellScreen(VoidCellMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.trash = new VoidCellModeButton(VoidCellMode.TRASH, button -> menu.selectMode(VoidCellMode.TRASH));
      this.matterBalls = new VoidCellModeButton(VoidCellMode.MATTER_BALLS, button -> menu.selectMode(VoidCellMode.MATTER_BALLS));
      this.singularity = new VoidCellModeButton(VoidCellMode.SINGULARITY, button -> menu.selectMode(VoidCellMode.SINGULARITY));
   }

   public void m_7856_() {
      super.m_7856_();
      this.trash.m_264152_(this.f_97735_ + 22, this.f_97736_ + 20);
      this.matterBalls.m_264152_(this.f_97735_ + 54, this.f_97736_ + 20);
      this.singularity.m_264152_(this.f_97735_ + 84, this.f_97736_ + 20);
      this.m_142416_(this.trash);
      this.m_142416_(this.matterBalls);
      this.m_142416_(this.singularity);
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      int textColor = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
      graphics.m_280614_(
         this.f_96547_, Component.m_237115_("gui.ae2lt.void_cell.mode." + ((VoidCellMenu)this.f_97732_).getMode().ordinal()), 5, 42, textColor, false
      );
   }
}
