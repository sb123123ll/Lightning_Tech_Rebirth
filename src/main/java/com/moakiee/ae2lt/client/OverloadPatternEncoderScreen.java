package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.menu.OverloadPatternEncoderMenu;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternEditState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OverloadPatternEncoderScreen extends AbstractContainerScreen<OverloadPatternEncoderMenu> {
   private static final Component SCREEN_TITLE = Component.m_237115_("item.ae2lt.overload_pattern_encoder");
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/ae2lt_pattern_encoder.png");
   private static final ResourceLocation CHECKBOX_TEXTURE = new ResourceLocation("ae2", "textures/guis/checkbox.png");
   private static final int TEXTURE_SIZE = 256;
   private static final int CHECKBOX_TEXTURE_SIZE = 64;
   private static final int GUI_WIDTH = 176;
   private static final int GUI_HEIGHT = 191;
   private static final int PANEL_X = 43;
   private static final int PANEL_Y = 17;
   private static final int PANEL_WIDTH = 125;
   private static final int PANEL_HEIGHT = 75;
   private static final int TRACK_X = 45;
   private static final int TRACK_Y = 21;
   private static final int TRACK_HEIGHT = 69;
   private static final int SLIDER_U = 177;
   private static final int SLIDER_V = 29;
   private static final int SLIDER_WIDTH = 7;
   private static final int SLIDER_HEIGHT = 15;
   private static final int SLOT_U = 177;
   private static final int SLOT_V = 0;
   private static final int SLOT_SIZE = 18;
   private static final int ENTRY_TOP_OFFSET = 3;
   private static final int ENTRY_CONTENT_Y_OFFSET = 3;
   private static final int ENTRY_SLOT_X = 60;
   private static final int ENTRY_TEXT_X = 90;
   private static final int ENTRY_SWITCH_X = 138;
   private static final int ENTRY_ROW_HEIGHT = 22;
   private static final int ENTRY_SWITCH_WIDTH = 14;
   private static final int ENTRY_SWITCH_HEIGHT = 14;
   private static final int VISIBLE_ROWS = 3;
   private int scrollOffset;
   private boolean draggingScrollbar;

   public OverloadPatternEncoderScreen(OverloadPatternEncoderMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 176;
      this.f_97727_ = 191;
      this.f_97730_ = 8;
      this.f_97731_ = 96;
   }

   protected void m_7856_() {
      super.m_7856_();
      this.clampScroll();
   }

   protected void m_181908_() {
      super.m_181908_();
      this.clampScroll();
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.m_280163_(TEXTURE, this.f_97735_, this.f_97736_, 0.0F, 0.0F, this.f_97726_, this.f_97727_, 256, 256);
      this.renderEntries(graphics, mouseX, mouseY);
      this.renderScrollbar(graphics);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, SCREEN_TITLE, 8, 6, 4210752, false);
      graphics.m_280614_(this.f_96547_, this.f_169604_, this.f_97730_, this.f_97731_, 4210752, false);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.renderEntryTooltip(graphics, mouseX, mouseY);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.isWithinScrollbar(mouseX, mouseY)) {
            this.draggingScrollbar = true;
            this.updateScrollFromMouse(mouseY);
            return true;
         }

         OverloadPatternEncoderScreen.Entry entry = this.getEntryAt(mouseX, mouseY);
         if (entry != null && this.isWithinEntrySwitch(mouseX, mouseY, entry.row())) {
            this.toggleEntry(entry);
            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingScrollbar) {
         this.updateScrollFromMouse(mouseY);
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.draggingScrollbar = false;
      return super.m_6348_(mouseX, mouseY, button);
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.isWithinPanel(mouseX, mouseY) && this.maxScrollOffset() > 0) {
         this.scrollOffset = Mth.m_14045_(this.scrollOffset - (int)Math.signum(delta), 0, this.maxScrollOffset());
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
      List<OverloadPatternEncoderScreen.Entry> entries = this.buildEntries();
      int start = Math.min(this.scrollOffset, Math.max(0, entries.size() - 3));
      int end = Math.min(entries.size(), start + 3);
      graphics.m_280588_(this.f_97735_ + 43, this.f_97736_ + 17, this.f_97735_ + 43 + 125, this.f_97736_ + 17 + 75);

      for (int visibleRow = 0; visibleRow < end - start; visibleRow++) {
         OverloadPatternEncoderScreen.Entry entry = entries.get(start + visibleRow);
         int rowY = this.f_97736_ + 17 + 3 + visibleRow * 22;
         this.renderEntry(graphics, mouseX, mouseY, entry, visibleRow, rowY);
      }

      graphics.m_280618_();
   }

   private void renderEntry(GuiGraphics graphics, int mouseX, int mouseY, OverloadPatternEncoderScreen.Entry entry, int visibleRow, int rowY) {
      int slotX = this.f_97735_ + 60;
      int textX = this.f_97735_ + 90;
      int switchX = this.f_97735_ + 138;
      int contentY = rowY + 3;
      graphics.m_280163_(TEXTURE, slotX, contentY, 177.0F, 0.0F, 18, 18, 256, 256);
      graphics.m_280480_(entry.stack(), slotX + 1, contentY + 1);
      graphics.m_280370_(this.f_96547_, entry.stack(), slotX + 1, contentY + 1);
      graphics.m_280614_(this.f_96547_, this.entryLabel(entry), textX, contentY + 5, 4210752, false);
      this.renderModeSwitch(graphics, switchX, contentY + 3, entry.mode());
   }

   private void renderModeSwitch(GuiGraphics graphics, int x, int y, MatchMode mode) {
      int v = mode.ignoresComponents() ? 14 : 0;
      graphics.m_280163_(CHECKBOX_TEXTURE, x, y, 0.0F, (float)v, 14, 14, 64, 64);
   }

   private void renderScrollbar(GuiGraphics graphics) {
      if (this.maxScrollOffset() <= 0) {
         graphics.m_280163_(TEXTURE, this.f_97735_ + 45 - 1, this.f_97736_ + 21, 177.0F, 29.0F, 7, 15, 256, 256);
      } else {
         int sliderTravel = 54;
         int sliderY = this.f_97736_ + 21 + Math.round((float)this.scrollOffset / (float)this.maxScrollOffset() * (float)sliderTravel);
         graphics.m_280163_(TEXTURE, this.f_97735_ + 45 - 1, sliderY, 177.0F, 29.0F, 7, 15, 256, 256);
      }
   }

   private void renderEntryTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      OverloadPatternEncoderScreen.Entry entry = this.getEntryAt((double)mouseX, (double)mouseY);
      if (entry == null) {
         this.m_280072_(graphics, mouseX, mouseY);
      } else {
         int rowY = this.f_97736_ + 17 + 3 + entry.row() * 22 + 3;
         int slotX = this.f_97735_ + 60;
         if (isWithin((double)mouseX, (double)mouseY, slotX, rowY, 18, 18)) {
            graphics.m_280153_(this.f_96547_, entry.stack(), mouseX, mouseY);
         } else if (this.isWithinEntrySwitch((double)mouseX, (double)mouseY, entry.row())) {
            graphics.m_280666_(this.f_96547_, this.modeTooltip(entry), mouseX, mouseY);
         } else {
            this.m_280072_(graphics, mouseX, mouseY);
         }
      }
   }

   private List<Component> modeTooltip(OverloadPatternEncoderScreen.Entry entry) {
      ArrayList<Component> lines = new ArrayList<>();
      lines.add(
         entry.mode() == MatchMode.ID_ONLY
            ? Component.m_237115_("ae2lt.gui.overload_pattern_encoder.mode.id_only")
            : Component.m_237115_("ae2lt.gui.overload_pattern_encoder.mode.strict")
      );
      return lines;
   }

   @Nullable
   private OverloadPatternEncoderScreen.Entry getEntryAt(double mouseX, double mouseY) {
      if (!this.isWithinPanel(mouseX, mouseY)) {
         return null;
      } else {
         int relativeRow = (int)((mouseY - (double)(this.f_97736_ + 17 + 3)) / 22.0);
         if (relativeRow >= 0 && relativeRow < 3) {
            List<OverloadPatternEncoderScreen.Entry> entries = this.buildEntries();
            int index = this.scrollOffset + relativeRow;
            return index >= 0 && index < entries.size() ? entries.get(index).withRow(relativeRow) : null;
         } else {
            return null;
         }
      }
   }

   private List<OverloadPatternEncoderScreen.Entry> buildEntries() {
      ArrayList<OverloadPatternEncoderScreen.Entry> entries = new ArrayList<>();
      OverloadPatternEditState state = ((OverloadPatternEncoderMenu)this.f_97732_).syncedState;

      for (int i = 0; i < state.inputSlots().size(); i++) {
         OverloadPatternEditState.ConfiguredSlot configured = state.inputSlots().get(i);
         entries.add(
            new OverloadPatternEncoderScreen.Entry(
               true, configured.slotIndex(), configured.matchMode(), false, ((OverloadPatternEncoderMenu)this.f_97732_).getInputPreviewStack(i), -1
            )
         );
      }

      for (int i = 0; i < state.outputSlots().size(); i++) {
         OverloadPatternEditState.ConfiguredSlot configured = state.outputSlots().get(i);
         entries.add(
            new OverloadPatternEncoderScreen.Entry(
               false, configured.slotIndex(), configured.matchMode(), false, ((OverloadPatternEncoderMenu)this.f_97732_).getOutputPreviewStack(i), -1
            )
         );
      }

      return entries;
   }

   private void toggleEntry(OverloadPatternEncoderScreen.Entry entry) {
      if (entry.input()) {
         ((OverloadPatternEncoderMenu)this.f_97732_).clientToggleInputMode(entry.slotIndex());
      } else {
         ((OverloadPatternEncoderMenu)this.f_97732_).clientToggleOutputMode(entry.slotIndex());
      }
   }

   private void updateScrollFromMouse(double mouseY) {
      if (this.maxScrollOffset() <= 0) {
         this.scrollOffset = 0;
      } else {
         float progress = (float)((mouseY - (double)(this.f_97736_ + 21) - 7.5) / 54.0);
         progress = Mth.m_14036_(progress, 0.0F, 1.0F);
         this.scrollOffset = Math.round(progress * (float)this.maxScrollOffset());
      }
   }

   private void clampScroll() {
      this.scrollOffset = Mth.m_14045_(this.scrollOffset, 0, this.maxScrollOffset());
   }

   private int maxScrollOffset() {
      return Math.max(0, this.buildEntries().size() - 3);
   }

   private boolean isWithinPanel(double mouseX, double mouseY) {
      return isWithin(mouseX, mouseY, this.f_97735_ + 43, this.f_97736_ + 17, 125, 75);
   }

   private boolean isWithinScrollbar(double mouseX, double mouseY) {
      return isWithin(mouseX, mouseY, this.f_97735_ + 45 - 1, this.f_97736_ + 21, 7, 69);
   }

   private boolean isWithinEntrySwitch(double mouseX, double mouseY, int row) {
      int y = this.f_97736_ + 17 + 3 + row * 22 + 3 + 3;
      return isWithin(mouseX, mouseY, this.f_97735_ + 138, y, 14, 14);
   }

   private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= (double)x && mouseX < (double)(x + width) && mouseY >= (double)y && mouseY < (double)(y + height);
   }

   private Component entryLabel(OverloadPatternEncoderScreen.Entry entry) {
      return entry.input()
         ? Component.m_237115_("ae2lt.gui.overload_pattern_encoder.entry.input")
         : Component.m_237115_("ae2lt.gui.overload_pattern_encoder.entry.output");
   }

   private static record Entry(boolean input, int slotIndex, MatchMode mode, boolean primaryOutput, ItemStack stack, int row) {
      private OverloadPatternEncoderScreen.Entry withRow(int newRow) {
         return new OverloadPatternEncoderScreen.Entry(this.input, this.slotIndex, this.mode, this.primaryOutput, this.stack, newRow);
      }
   }
}
