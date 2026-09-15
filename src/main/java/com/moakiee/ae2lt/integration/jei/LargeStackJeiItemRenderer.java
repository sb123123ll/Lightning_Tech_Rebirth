package com.moakiee.ae2lt.integration.jei;

import com.moakiee.ae2lt.client.gui.LargeStackCountRenderer;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class LargeStackJeiItemRenderer implements IIngredientRenderer<ItemStack> {
   public static final LargeStackJeiItemRenderer INSTANCE = new LargeStackJeiItemRenderer();

   private LargeStackJeiItemRenderer() {
   }

   public void render(GuiGraphics guiGraphics, ItemStack ingredient) {
      this.render(guiGraphics, ingredient, 0, 0);
   }

   public void render(GuiGraphics guiGraphics, ItemStack ingredient, int posX, int posY) {
      if (ingredient != null && !ingredient.m_41619_()) {
         guiGraphics.m_280203_(ingredient, posX, posY);
         LargeStackCountRenderer.renderCountAt(guiGraphics, this.getFontRenderer(Minecraft.m_91087_(), ingredient), posX, posY, (long)ingredient.m_41613_());
      }
   }

   public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
      return ingredient.m_41651_(Minecraft.m_91087_().f_91074_, tooltipFlag);
   }

   public void getTooltip(ITooltipBuilder tooltip, ItemStack ingredient, TooltipFlag tooltipFlag) {
      tooltip.addAll(ingredient.m_41651_(Minecraft.m_91087_().f_91074_, tooltipFlag));
   }

   public Font getFontRenderer(Minecraft minecraft, ItemStack ingredient) {
      return minecraft.f_91062_;
   }
}
