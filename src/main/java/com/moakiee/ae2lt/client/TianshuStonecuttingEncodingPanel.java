package com.moakiee.ae2lt.client;

import appeng.client.Point;
import appeng.client.gui.Tooltip;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.jetbrains.annotations.Nullable;

final class TianshuStonecuttingEncodingPanel extends TianshuEncodingModePanel {
   private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(0, 141, 126, 68);
   private static final Blitter BG_SLOT = BG.copy().src(126, 141, 16, 18);
   private static final Blitter BG_SLOT_SELECTED = BG.copy().src(126, 159, 16, 18);
   private static final Blitter BG_SLOT_HOVER = BG.copy().src(126, 177, 16, 18);
   private static final int COLUMNS = 4;
   private static final int ROWS = 3;
   private final Scrollbar scrollbar;

   TianshuStonecuttingEncodingPanel(TianshuPatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
      super(screen, widgets);
      this.scrollbar = widgets.addScrollBar("stonecuttingPatternModeScrollbar", Scrollbar.SMALL);
      this.scrollbar.setRange(0, 0, 4);
      this.scrollbar.setCaptureMouseWheel(false);
   }

   public void updateBeforeRender() {
      int totalRows = (this.menu.getStonecuttingRecipes().size() + 4 - 1) / 4;
      this.scrollbar.setRange(0, totalRows - 3, 3);
   }

   public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      BG.dest(bounds.m_110085_() + 9, bounds.m_110086_() + bounds.m_110091_() - 164).blit(graphics);
      this.drawRecipes(graphics, bounds, mouse);
   }

   private RegistryAccess getRegistryAccess() {
      return Objects.requireNonNull(Minecraft.m_91087_().f_91073_).m_9598_();
   }

   private void drawRecipes(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      List<StonecutterRecipe> recipes = this.menu.getStonecuttingRecipes();
      int startIndex = this.scrollbar.getCurrentScroll() * 4;
      int endIndex = startIndex + 12;
      ResourceLocation selectedRecipe = this.menu.getStonecuttingRecipeId();

      for (int i = startIndex; i < endIndex && i < recipes.size(); i++) {
         Rect2i slotBounds = this.getRecipeBounds(i - startIndex);
         StonecutterRecipe recipe = recipes.get(i);
         boolean selected = selectedRecipe != null && selectedRecipe.equals(recipe.m_6423_());
         Blitter background = selected ? BG_SLOT_SELECTED : (mouse.isIn(slotBounds) ? BG_SLOT_HOVER : BG_SLOT);
         int renderX = bounds.m_110085_() + slotBounds.m_110085_();
         int renderY = bounds.m_110086_() + slotBounds.m_110086_();
         background.dest(renderX, renderY - 1).blit(graphics);
         ItemStack result = recipe.m_8043_(this.getRegistryAccess());
         graphics.m_280480_(result, renderX, renderY);
         graphics.m_280370_(Minecraft.m_91087_().f_91062_, result, renderX, renderY);
      }
   }

   public boolean onMouseDown(Point mousePosition, int button) {
      StonecutterRecipe recipe = this.getRecipeAt(mousePosition);
      if (recipe == null) {
         return false;
      } else {
         this.menu.setStonecuttingRecipeId(recipe.m_6423_());
         Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119752_(SoundEvents.f_12495_, 1.0F));
         return true;
      }
   }

   @Nullable
   public Tooltip getTooltip(int mouseX, int mouseY) {
      StonecutterRecipe recipe = this.getRecipeAt(new Point(mouseX, mouseY));
      if (recipe == null) {
         return null;
      } else {
         ItemStack result = recipe.m_8043_(this.getRegistryAccess());
         return new Tooltip(this.screen.m_280553_(result));
      }
   }

   @Nullable
   private StonecutterRecipe getRecipeAt(Point point) {
      List<StonecutterRecipe> recipes = this.menu.getStonecuttingRecipes();
      if (recipes.isEmpty()) {
         return null;
      } else {
         int startIndex = this.scrollbar.getCurrentScroll() * 4;
         int endIndex = startIndex + 12;

         for (int i = startIndex; i < endIndex && i < recipes.size(); i++) {
            if (point.isIn(this.getRecipeBounds(i - startIndex))) {
               return recipes.get(i);
            }
         }

         return null;
      }
   }

   private Rect2i getRecipeBounds(int index) {
      int column = index % 4;
      int row = index / 4;
      int slotX = this.x + 44 + column * BG_SLOT.getSrcWidth();
      int slotY = this.y + 8 + row * BG_SLOT.getSrcHeight();
      return new Rect2i(slotX, slotY, BG_SLOT.getSrcWidth(), BG_SLOT.getSrcHeight());
   }

   public boolean onMouseWheel(Point mousePosition, double delta) {
      return this.scrollbar.onMouseWheel(mousePosition, delta);
   }

   @Override
   ItemStack getTabIconItem() {
      return Items.f_42776_.m_7968_();
   }

   @Override
   Component getTabTooltip() {
      return GuiText.StonecuttingPattern.text();
   }

   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      this.scrollbar.setVisible(visible);
      this.screen.setSlotsHidden(SlotSemantics.STONECUTTING_INPUT, !visible);
   }
}
