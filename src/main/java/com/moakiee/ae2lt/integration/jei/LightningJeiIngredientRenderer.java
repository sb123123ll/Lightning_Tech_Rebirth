package com.moakiee.ae2lt.integration.jei;

import appeng.api.client.AEKeyRendering;
import appeng.util.Platform;
import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

public class LightningJeiIngredientRenderer implements IIngredientRenderer<LightningKey> {
   public void render(GuiGraphics guiGraphics, LightningKey ingredient) {
      this.render(guiGraphics, ingredient, 0, 0);
   }

   public void render(GuiGraphics guiGraphics, LightningKey ingredient, int posX, int posY) {
      if (ingredient != null) {
         AEKeyRendering.drawInGui(Minecraft.m_91087_(), guiGraphics, posX, posY, ingredient);
      }
   }

   public List<Component> getTooltip(LightningKey ingredient, TooltipFlag tooltipFlag) {
      return AEKeyRendering.getTooltip(ingredient);
   }

   public void getTooltip(ITooltipBuilder tooltip, LightningKey ingredient, TooltipFlag tooltipFlag) {
      tooltip.addAll(getJeiTooltip(ingredient));
   }

   public Font getFontRenderer(Minecraft minecraft, LightningKey ingredient) {
      return minecraft.f_91062_;
   }

   private static List<Component> getJeiTooltip(LightningKey ingredient) {
      ArrayList<Component> tooltip = new ArrayList<>(AEKeyRendering.getTooltip(ingredient));
      if (tooltip.isEmpty()) {
         return tooltip;
      } else {
         String modName = Platform.formatModName(ingredient.getModId());
         String lastLine = tooltip.get(tooltip.size() - 1).getString();
         if (lastLine.equals(modName)) {
            tooltip.remove(tooltip.size() - 1);
         }

         return tooltip;
      }
   }
}
