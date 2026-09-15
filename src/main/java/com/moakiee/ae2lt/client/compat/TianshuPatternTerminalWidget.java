package com.moakiee.ae2lt.client.compat;

import appeng.menu.SlotSemantics;
import com.illusivesoulworks.polymorph.client.recipe.widget.PlayerRecipesWidget;
import com.moakiee.ae2lt.client.TianshuPatternEncodingTermScreen;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuEncodingMode;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

final class TianshuPatternTerminalWidget extends PlayerRecipesWidget {
   private final TianshuPatternEncodingTermScreen<?> screen;

   TianshuPatternTerminalWidget(TianshuPatternEncodingTermScreen<?> screen) {
      super(screen, (Slot)((TianshuPatternEncodingTermMenu)screen.m_6262_()).getSlots(SlotSemantics.CRAFTING_RESULT).get(0));
      this.screen = screen;
   }

   public void selectRecipe(ResourceLocation id) {
      super.selectRecipe(id);
      ((TianshuPatternEncodingTermMenu)this.screen.m_6262_())
         .getPlayer()
         .m_9236_()
         .m_7465_()
         .m_44043_(id)
         .ifPresent(recipe -> ((TianshuPatternEncodingTermMenu)this.screen.m_6262_()).refreshPolymorphRecipe());
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.isCraftingMode()) {
         super.render(graphics, mouseX, mouseY, partialTick);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.isCraftingMode() && super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean isCraftingMode() {
      return ((TianshuPatternEncodingTermMenu)this.screen.m_6262_()).tianshuMode == TianshuEncodingMode.CRAFTING;
   }
}
