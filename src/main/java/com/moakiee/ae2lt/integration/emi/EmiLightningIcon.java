package com.moakiee.ae2lt.integration.emi;

import appeng.api.client.AEKeyRendering;
import com.moakiee.ae2lt.me.key.LightningKey;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class EmiLightningIcon implements EmiRenderable {
   private final boolean extreme;

   EmiLightningIcon(boolean extreme) {
      this.extreme = extreme;
   }

   public void render(GuiGraphics graphics, int x, int y, float delta) {
      AEKeyRendering.drawInGui(Minecraft.m_91087_(), graphics, x, y, this.extreme ? LightningKey.EXTREME_HIGH_VOLTAGE : LightningKey.HIGH_VOLTAGE);
   }
}
