package com.moakiee.ae2lt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import com.moakiee.ae2lt.menu.PigmeeMolecularAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class PigmeeMolecularAssemblerScreen extends AEBaseScreen<PigmeeMolecularAssemblerMenu> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/pigmee_molecular_assembler.png");
   private static final int CRAFTING_GRID_X = 29;
   private static final int CRAFTING_GRID_Y = 43;
   private static final int CRAFTING_GRID_U = 179;
   private static final int CRAFTING_GRID_V = 2;
   private static final int CRAFTING_GRID_SIZE = 52;
   private static final int PATTERN_SLOT_X = 126;
   private static final int PATTERN_SLOT_Y = 29;
   private static final int PATTERN_SLOT_U = 179;
   private static final int PATTERN_SLOT_V = 57;
   private static final int PATTERN_SLOT_SIZE = 16;
   private static final int TEXTURE_SIZE = 256;
   private final ProgressBar progressBar;

   public PigmeeMolecularAssemblerScreen(PigmeeMolecularAssemblerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.progressBar = new ProgressBar(menu, style.getImage("progressBar"), Direction.VERTICAL);
      this.widgets.add("progressBar", this.progressBar);
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.progressBar.setFullMsg(Component.m_237113_(((PigmeeMolecularAssemblerMenu)this.f_97732_).getCurrentProgress() + "%"));
   }

   public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
      super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
      graphics.m_280163_(TEXTURE, offsetX + 29, offsetY + 43, 179.0F, 2.0F, 52, 52, 256, 256);
      graphics.m_280163_(TEXTURE, offsetX + 126, offsetY + 29, 179.0F, 57.0F, 16, 16, 256, 256);
   }
}
