package com.moakiee.ae2lt.client;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.GenericStack;
import appeng.core.localization.GuiText;
import com.moakiee.ae2lt.menu.MatrixPortMenu;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MatrixPortScreen extends AbstractContainerScreen<MatrixPortMenu> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/guis/matrix_pattern_manager.png");
   private static final int TEXTURE_SIZE = 256;
   private static final int GUI_WIDTH = 190;
   private static final int GUI_HEIGHT = 223;
   private static final int SLOT_SPACING = 18;
   private static final int OFFSCREEN_SLOT = -10000;
   private static final int SEARCH_X = 78;
   private static final int SEARCH_Y = 7;
   private static final int SEARCH_WIDTH = 94;
   private static final int SEARCH_HEIGHT = 11;
   private static final int SCROLLBAR_X = 175;
   private static final int SCROLLBAR_Y = 22;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int SCROLLBAR_HEIGHT = 103;
   private static final int HANDLE_HEIGHT = 15;
   private static final int HANDLE_U_NORMAL = 192;
   private static final int HANDLE_U_HOVERED = 206;
   private static final int HANDLE_V = 4;
   private static final int PATTERN_AREA_X = 12;
   private static final int PATTERN_AREA_Y = 20;
   private static final int PATTERN_AREA_WIDTH = 175;
   private static final int PATTERN_AREA_HEIGHT = 108;
   private final List<Integer> filteredRows = new ArrayList<>();
   private final Set<Integer> matchingPatternSlots = new HashSet<>();
   private final Map<String, Boolean> nameMatchCache = new HashMap<>();
   private final List<MatrixPortMenu.MatrixPatternSlot> visiblePatternSlots = new ArrayList<>(54);
   private final List<Slot> allMenuSlots;
   private final List<Slot> playerMenuSlots;
   private final List<Slot> visibleMenuSlots = new ArrayList<>(90);
   private EditBox searchField;
   private int scrollRow;
   private boolean draggingScrollbar;
   private long lastPatternContentRevision;

   public MatrixPortScreen(MatrixPortMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 190;
      this.f_97727_ = 223;
      this.f_97729_ = 10000;
      this.f_97731_ = 10000;
      this.allMenuSlots = List.copyOf(menu.f_38839_);
      this.playerMenuSlots = List.copyOf(this.allMenuSlots.subList(menu.getPatternSlotCount(), this.allMenuSlots.size()));
      this.visibleMenuSlots.addAll(this.playerMenuSlots);

      for (MatrixPortMenu.MatrixPatternSlot slot : menu.getPatternSlots()) {
         slot.setActive(false);
      }
   }

   protected void m_7856_() {
      super.m_7856_();
      this.searchField = new EditBox(this.f_96547_, this.f_97735_ + 78, this.f_97736_ + 7, 94, 11, Component.m_237115_("ae2lt.gui.matrix_port.search"));
      this.searchField.m_94182_(false);
      this.searchField.m_94199_(64);
      this.searchField.m_94202_(15921906);
      this.searchField.m_257771_(GuiText.SearchPlaceholder.text().m_6881_().m_6270_(Style.f_131099_.m_131148_(TextColor.m_131266_(14606307))));
      this.searchField.m_94151_(ignored -> this.refreshList(true));
      this.m_142416_(this.searchField);
      this.m_264313_(this.searchField);
      this.refreshList(true);
   }

   protected void m_181908_() {
      super.m_181908_();
      if (this.searchField != null && !this.searchField.m_94155_().isBlank()) {
         long revision = ((MatrixPortMenu)this.f_97732_).getPatternContentRevision();
         if (revision != this.lastPatternContentRevision) {
            this.refreshList(false);
         }
      }
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.m_280163_(TEXTURE, this.f_97735_, this.f_97736_, 0.0F, 0.0F, this.f_97726_, this.f_97727_, 256, 256);
      this.renderSearchHighlights(graphics);
      this.renderScrollbar(graphics, mouseX, mouseY);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.gui.matrix_port.title"), 7, 7, 4210752, false);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderVisibleBatch(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   private void renderVisibleBatch(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      ((MatrixPortMenu)this.f_97732_).f_38839_.clear();
      ((MatrixPortMenu)this.f_97732_).f_38839_.addAll(this.visibleMenuSlots);

      try {
         super.m_88315_(graphics, mouseX, mouseY, partialTick);
      } finally {
         ((MatrixPortMenu)this.f_97732_).f_38839_.clear();
         ((MatrixPortMenu)this.f_97732_).f_38839_.addAll(this.allMenuSlots);
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 1 && this.searchField != null && this.searchField.m_5953_(mouseX, mouseY)) {
         this.searchField.m_94144_("");
      }

      if (button == 0 && this.isWithinScrollbar(mouseX, mouseY) && this.maxScrollRow() > 0) {
         this.draggingScrollbar = true;
         this.updateScrollFromMouse(mouseY);
         return true;
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
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
      if (this.isWithinPatternArea(mouseX, mouseY) && this.maxScrollRow() > 0 && delta != 0.0) {
         this.setScrollRow(this.scrollRow - (int)Math.signum(delta));
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   private void refreshList(boolean resetScroll) {
      this.filteredRows.clear();
      this.matchingPatternSlots.clear();
      this.nameMatchCache.clear();
      List<String> searchTokens = tokenize(this.searchField == null ? "" : this.searchField.m_94155_());
      int rowCount = (((MatrixPortMenu)this.f_97732_).getPatternSlotCount() + 9 - 1) / 9;

      for (int row = 0; row < rowCount; row++) {
         if (searchTokens.isEmpty() || this.rowMatches(row, searchTokens)) {
            this.filteredRows.add(row);
         }
      }

      if (resetScroll) {
         this.scrollRow = 0;
      } else {
         this.scrollRow = Mth.m_14045_(this.scrollRow, 0, this.maxScrollRow());
      }

      this.updateVisibleSlots();
      this.lastPatternContentRevision = ((MatrixPortMenu)this.f_97732_).getPatternContentRevision();
   }

   private boolean rowMatches(int row, List<String> searchTokens) {
      boolean rowMatches = false;
      int firstSlot = row * 9;
      int lastSlot = Math.min(firstSlot + 9, ((MatrixPortMenu)this.f_97732_).getPatternSlotCount());

      for (int slotIndex = firstSlot; slotIndex < lastSlot; slotIndex++) {
         ItemStack stack = ((MatrixPortMenu)this.f_97732_).getPatternSlots().get(slotIndex).m_7993_();
         if (this.patternMatches(stack, searchTokens)) {
            this.matchingPatternSlots.add(slotIndex);
            rowMatches = true;
         }
      }

      return rowMatches;
   }

   private boolean patternMatches(ItemStack patternStack, List<String> searchTokens) {
      if (!patternStack.m_41619_() && this.f_96541_ != null && this.f_96541_.f_91073_ != null) {
         IPatternDetails details;
         try {
            details = PatternDetailsHelper.decodePattern(patternStack, this.f_96541_.f_91073_);
         } catch (RuntimeException var9) {
            return false;
         }

         if (details == null) {
            return false;
         } else {
            for (GenericStack output : details.getOutputs()) {
               if (output != null && this.nameMatches(output.what().getDisplayName().getString(), searchTokens)) {
                  return true;
               }
            }

            for (IInput input : details.getInputs()) {
               if (input != null) {
                  GenericStack[] possibleInputs = input.getPossibleInputs();
                  if (possibleInputs.length > 0
                     && possibleInputs[0] != null
                     && this.nameMatches(possibleInputs[0].what().getDisplayName().getString(), searchTokens)) {
                     return true;
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean nameMatches(String displayName, List<String> searchTokens) {
      return this.nameMatchCache.computeIfAbsent(displayName, name -> {
         if (compareTokens(searchTokens, tokenize(name))) {
            return true;
         } else {
            String compactQuery = String.join("", searchTokens);
            return JecSearchCompat.contains(name.toLowerCase(Locale.ROOT), compactQuery);
         }
      });
   }

   private static List<String> tokenize(String text) {
      if (text != null && !text.isBlank()) {
         ArrayList<String> tokens = new ArrayList<>();

         for (String token : text.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!token.isBlank()) {
               tokens.add(token);
            }
         }

         return tokens;
      } else {
         return List.of();
      }
   }

   private static boolean compareTokens(List<String> filter, List<String> target) {
      for (int start = 0; start <= target.size() - filter.size(); start++) {
         int targetIndex = start;

         int filterIndex;
         for (filterIndex = 0; targetIndex < target.size() && filterIndex < filter.size(); targetIndex++) {
            if (target.get(targetIndex).contains(filter.get(filterIndex))) {
               filterIndex++;
            }
         }

         if (filterIndex >= filter.size()) {
            return true;
         }
      }

      return false;
   }

   private void updateVisibleSlots() {
      List<MatrixPortMenu.MatrixPatternSlot> slots = ((MatrixPortMenu)this.f_97732_).getPatternSlots();

      for (MatrixPortMenu.MatrixPatternSlot slot : this.visiblePatternSlots) {
         slot.setActive(false);
         SlotPositionAccess.set(slot, -10000, -10000);
      }

      this.visiblePatternSlots.clear();
      int visibleRows = Math.min(6, Math.max(0, this.filteredRows.size() - this.scrollRow));

      for (int visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
         int patternRow = this.filteredRows.get(this.scrollRow + visibleRow);
         int firstSlot = patternRow * 9;
         int lastSlot = Math.min(firstSlot + 9, slots.size());

         for (int slotIndex = firstSlot; slotIndex < lastSlot; slotIndex++) {
            int column = slotIndex - firstSlot;
            MatrixPortMenu.MatrixPatternSlot slot = slots.get(slotIndex);
            SlotPositionAccess.set(slot, 13 + column * 18, 21 + visibleRow * 18);
            slot.setActive(true);
            this.visiblePatternSlots.add(slot);
         }
      }

      this.visibleMenuSlots.clear();
      this.visibleMenuSlots.addAll(this.visiblePatternSlots);
      this.visibleMenuSlots.addAll(this.playerMenuSlots);
   }

   private void renderSearchHighlights(GuiGraphics graphics) {
      if (this.searchField != null && !this.searchField.m_94155_().isBlank()) {
         for (MatrixPortMenu.MatrixPatternSlot slot : this.visiblePatternSlots) {
            int color = this.matchingPatternSlots.contains(slot.getPatternIndex()) ? -1979646208 : 1778384896;
            graphics.m_280509_(
               this.f_97735_ + slot.f_40220_, this.f_97736_ + slot.f_40221_, this.f_97735_ + slot.f_40220_ + 16, this.f_97736_ + slot.f_40221_ + 16, color
            );
         }
      }
   }

   private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
      int handleY = this.handleY();
      boolean hovered = this.draggingScrollbar || this.isMouseOverHandle((double)mouseX, (double)mouseY, handleY);
      graphics.m_280163_(TEXTURE, this.f_97735_ + 175, this.f_97736_ + handleY, hovered ? 206.0F : 192.0F, 4.0F, 12, 15, 256, 256);
   }

   private int handleY() {
      int maxScroll = this.maxScrollRow();
      if (maxScroll <= 0) {
         return 22;
      } else {
         int travel = 88;
         return 22 + Math.round((float)this.scrollRow / (float)maxScroll * (float)travel);
      }
   }

   private void updateScrollFromMouse(double mouseY) {
      int maxScroll = this.maxScrollRow();
      if (maxScroll <= 0) {
         this.setScrollRow(0);
      } else {
         int travel = 88;
         double relative = mouseY - (double)(this.f_97736_ + 22) - 7.5;
         double fraction = Mth.m_14008_(relative / (double)travel, 0.0, 1.0);
         this.setScrollRow((int)Math.round(fraction * (double)maxScroll));
      }
   }

   private void setScrollRow(int value) {
      int clamped = Mth.m_14045_(value, 0, this.maxScrollRow());
      if (clamped != this.scrollRow) {
         this.scrollRow = clamped;
         this.updateVisibleSlots();
      }
   }

   private int maxScrollRow() {
      return Math.max(0, this.filteredRows.size() - 6);
   }

   private boolean isWithinScrollbar(double mouseX, double mouseY) {
      return mouseX >= (double)(this.f_97735_ + 175)
         && mouseX < (double)(this.f_97735_ + 175 + 12)
         && mouseY >= (double)(this.f_97736_ + 22)
         && mouseY < (double)(this.f_97736_ + 22 + 103);
   }

   private boolean isMouseOverHandle(double mouseX, double mouseY, int handleY) {
      return mouseX >= (double)(this.f_97735_ + 175)
         && mouseX < (double)(this.f_97735_ + 175 + 12)
         && mouseY >= (double)(this.f_97736_ + handleY)
         && mouseY < (double)(this.f_97736_ + handleY + 15);
   }

   private boolean isWithinPatternArea(double mouseX, double mouseY) {
      return mouseX >= (double)(this.f_97735_ + 12)
         && mouseX < (double)(this.f_97735_ + 12 + 175)
         && mouseY >= (double)(this.f_97736_ + 20)
         && mouseY < (double)(this.f_97736_ + 20 + 108);
   }
}
