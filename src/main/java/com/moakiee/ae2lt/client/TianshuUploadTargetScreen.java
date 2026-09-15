package com.moakiee.ae2lt.client;

import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.logic.tianshu.terminal.TianshuUploadTargetData;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;

public final class TianshuUploadTargetScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuPatternEncodingTermScreen<M>> {
   private static final String STYLE = "/screens/tianshu_upload_targets.json";
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/tianshu_upload_targets.png");
   private static final int TEXTURE_SIZE = 256;
   private static final int GUI_WIDTH = 190;
   private static final int GUI_HEADER_HEIGHT = 33;
   private static final int GUI_FOOTER_HEIGHT = 18;
   private static final int GUI_VERTICAL_PADDING = 54;
   private static final int ROW_HEIGHT = 17;
   private static final int ROW_LEFT = 7;
   private static final int ROW_RIGHT = 169;
   private static final int ROW_TEXTURE_X = 9;
   private static final int ROW_TEXTURE_WIDTH = 158;
   private static final int ROW_NORMAL_TEXTURE_Y = 222;
   private static final int ROW_SELECTED_TEXTURE_Y = 239;
   private static final int ROW_LABEL_WIDTH = 136;
   private static final int BACKGROUND_TOP_HEIGHT = 35;
   private static final int BACKGROUND_STRETCH_TEXTURE_Y = 35;
   private static final int BACKGROUND_BOTTOM_BORDER_TEXTURE_Y = 202;
   private static final int BACKGROUND_FOOTER_TEXTURE_Y = 203;
   private static final int HIDDEN_SLOT_POS = -10000;
   private final AETextField sourceField;
   private final AETextField aliasField;
   private final Scrollbar scrollbar;
   private final List<String> defaultAliases = new ArrayList<>();
   private final List<TianshuUploadTargetScreen.IndexedTarget> filtered = new ArrayList<>();
   private final boolean directUploadRequested;
   private int visibleRows;
   private int defaultAliasIndex = -1;
   private int focusedIndex = -1;
   private int requestedRevision;
   private boolean queryRefresh = true;
   private boolean awaitingTargets = true;
   private boolean awaitingUpload;
   private boolean uploadFailed;
   private boolean updatingAliasField;
   private boolean configuredAliasInserted;
   private boolean directUploadAttempted;
   private boolean initialAliasSelectionPending;
   private String configuredAlias = "";
   private String initialAlias = "";
   private Component uploadTargetName = Component.m_237119_();

   public TianshuUploadTargetScreen(TianshuPatternEncodingTermScreen<M> parent) {
      this(parent, false);
   }

   public TianshuUploadTargetScreen(TianshuPatternEncodingTermScreen<M> parent, boolean directUploadRequested) {
      super(parent, "/screens/tianshu_upload_targets.json");
      this.directUploadRequested = directUploadRequested;
      this.f_97726_ = 190;
      this.addToLeftToolbar(new SettingToggleButton(Settings.TERMINAL_STYLE, AEConfig.instance().getTerminalStyle(), this::toggleTerminalStyle));
      this.widgets.add("button_back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
      this.addToLeftToolbar(
         new TianshuUploadTargetScreen.AliasActionButton(Icon.ENTER, Component.m_237115_("ae2lt.tianshu.upload.alias.add"), this::addMapping)
      );
      this.addToLeftToolbar(
         new TianshuUploadTargetScreen.AliasActionButton(Icon.CLEAR, Component.m_237115_("ae2lt.tianshu.upload.alias.remove"), this::removeMappings)
      );
      this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
      this.sourceField = this.widgets.addTextField("field_search");
      this.sourceField.m_94199_(256);
      this.sourceField.setPlaceholder(Component.m_237115_("ae2lt.tianshu.upload.keyword"));
      this.aliasField = this.widgets.addTextField("field_alias");
      this.aliasField.m_94199_(256);
      this.aliasField.setPlaceholder(Component.m_237115_("ae2lt.tianshu.upload.alias"));
      TianshuUploadSourceSelection.Selection selection = TianshuUploadSourceSelection.collect((TianshuPatternEncodingTermMenu)this.f_97732_);
      String sourceKey = selection.sourceKey();
      this.defaultAliases.addAll(selection.defaultAliases());
      this.sourceField.m_94144_(sourceKey);
      String storedAlias = selection.savedAlias();
      if (storedAlias != null && !storedAlias.isBlank()) {
         this.setConfiguredAlias(storedAlias);
         this.defaultAliasIndex = this.indexOfDefaultAlias(storedAlias);
         this.aliasField.m_94144_(storedAlias);
      } else if (!selection.initialQuery().isBlank()) {
         this.defaultAliasIndex = 0;
         this.aliasField.m_94144_(selection.initialQuery());
      }

      this.initialAlias = this.aliasField.m_94155_();
      this.initialAliasSelectionPending = storedAlias.isBlank() && !this.initialAlias.isBlank() && !this.defaultAliases.isEmpty();
      this.aliasField.m_94151_(value -> {
         if (!this.updatingAliasField) {
            this.defaultAliasIndex = this.indexOfDefaultAlias(value);
            this.initialAliasSelectionPending = false;
         }

         this.queryRefresh = true;
      });
      this.rebuildDefaultAliasTooltip();
      this.requestedRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargetsRevision();
      ((TianshuPatternEncodingTermMenu)this.f_97732_).requestUploadTargets();
   }

   private void setConfiguredAlias(String alias) {
      this.clearConfiguredAliasCandidate();
      this.configuredAlias = alias == null ? "" : alias;
      if (!this.configuredAlias.isBlank() && this.indexOfDefaultAlias(this.configuredAlias) < 0) {
         this.defaultAliases.add(Math.min(1, this.defaultAliases.size()), this.configuredAlias);
         this.configuredAliasInserted = true;
      }
   }

   private void clearConfiguredAliasCandidate() {
      if (this.configuredAliasInserted) {
         int index = this.indexOfDefaultAlias(this.configuredAlias);
         if (index >= 0) {
            this.defaultAliases.remove(index);
         }
      }

      this.configuredAlias = "";
      this.configuredAliasInserted = false;
   }

   private int indexOfDefaultAlias(String alias) {
      if (alias == null) {
         return -1;
      } else {
         for (int i = 0; i < this.defaultAliases.size(); i++) {
            if (this.defaultAliases.get(i).equalsIgnoreCase(alias)) {
               return i;
            }
         }

         return -1;
      }
   }

   private void rebuildDefaultAliasTooltip() {
      if (this.defaultAliases.size() <= 1) {
         this.aliasField.setTooltipMessage(List.of());
      } else {
         ArrayList<Component> lines = new ArrayList<>();
         lines.add(Component.m_237115_("ae2lt.tianshu.upload.alias.defaults").m_130944_(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}));

         for (int i = 0; i < this.defaultAliases.size(); i++) {
            lines.add(
               Component.m_237113_(i == this.defaultAliasIndex ? "→ " : "  ")
                  .m_130940_(i == this.defaultAliasIndex ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                  .m_130946_(this.defaultAliases.get(i))
            );
         }

         this.aliasField.setTooltipMessage(lines);
      }
   }

   protected void m_7856_() {
      this.visibleRows = Math.max(6, this.config.getTerminalStyle().getRows((this.f_96544_ - 33 - 18 - 54) / 17));
      this.f_97727_ = 51 + this.visibleRows * 17;
      this.scrollbar.setHeight(this.visibleRows * 17 - 1);
      super.m_7856_();
      this.hideAllMenuSlots();
      this.queryRefresh = true;
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.hideAllMenuSlots();
   }

   private void hideAllMenuSlots() {
      for (Slot slot : ((TianshuPatternEncodingTermMenu)this.f_97732_).f_38839_) {
         SlotPositionAccess.set(slot, -10000, -10000);
      }
   }

   public void m_181908_() {
      super.m_181908_();
      if (((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargetsRevision() != this.requestedRevision) {
         this.requestedRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargetsRevision();
         this.awaitingTargets = false;
         this.queryRefresh = true;
      }

      if (this.awaitingUpload && ((TianshuPatternEncodingTermMenu)this.f_97732_).uploadState == 1) {
         this.awaitingUpload = false;
         if (this.f_96541_.f_91074_ != null) {
            this.f_96541_.f_91074_.m_5661_(Component.m_237110_("ae2lt.tianshu.upload.success_target", new Object[]{this.uploadTargetName}), false);
         }

         this.returnToParent();
      } else {
         if (this.awaitingUpload && ((TianshuPatternEncodingTermMenu)this.f_97732_).uploadState == 3) {
            this.awaitingUpload = false;
            this.uploadFailed = true;
            this.requestedRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargetsRevision();
            this.queryRefresh = true;
         }

         if (this.queryRefresh) {
            this.selectInitialPopulatedAlias();
            this.queryRefresh = false;
            this.rebuildDefaultAliasTooltip();
            this.rebuildFilteredTargets();
            this.tryDirectUpload();
         }
      }
   }

   private void selectInitialPopulatedAlias() {
      if (this.initialAliasSelectionPending && !this.awaitingTargets) {
         this.initialAliasSelectionPending = false;
         String selectedAlias = TianshuUploadTargetMatcher.findClosestUniqueAlias(
            ((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargets(), this.initialAlias, this.defaultAliases
         );
         if (!selectedAlias.equals(this.aliasField.m_94155_())) {
            this.defaultAliasIndex = this.indexOfDefaultAlias(selectedAlias);
            this.updatingAliasField = true;
            this.aliasField.m_94144_(selectedAlias);
            this.updatingAliasField = false;
         }
      }
   }

   private void rebuildFilteredTargets() {
      String query = this.aliasField.m_94155_().strip().toLowerCase(Locale.ROOT);
      this.filtered.clear();
      List<TianshuUploadTargetData> targets = ((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargets();

      for (int i = 0; i < targets.size(); i++) {
         TianshuUploadTargetData target = targets.get(i);
         if (query.isEmpty() || matches(target, query)) {
            this.filtered.add(new TianshuUploadTargetScreen.IndexedTarget(i, target));
         }
      }

      this.focusedIndex = -1;
      this.scrollbar.setRange(0, Math.max(0, this.filtered.size() - this.visibleRows), 2);
   }

   private static boolean matches(TianshuUploadTargetData target, String query) {
      return TianshuUploadTargetMatcher.matches(target, query);
   }

   private void tryDirectUpload() {
      if (this.directUploadRequested && !this.directUploadAttempted && !this.awaitingTargets && !this.awaitingUpload) {
         this.directUploadAttempted = true;
         TianshuUploadTargetData selected = TianshuUploadTargetMatcher.findUniqueCandidate(
            ((TianshuPatternEncodingTermMenu)this.f_97732_).getUploadTargets(), this.aliasField.m_94155_()
         );
         if (selected != null) {
            this.select(new TianshuUploadTargetScreen.IndexedTarget(-1, selected));
         }
      }
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      int textColor = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
      int start = this.scrollbar.getCurrentScroll();

      for (int row = 0; row < this.visibleRows; row++) {
         int index = start + row;
         if (index >= this.filtered.size()) {
            break;
         }

         TianshuUploadTargetData target = this.filtered.get(index).target();
         int y = 33 + row * 17;
         if (target.group().icon() != null) {
            graphics.m_280480_(target.group().icon().getReadOnlyStack(), 9, y);
         }

         String suffix = " [≈" + target.availableSlots() + "]";
         MutableComponent label = target.group().name().m_6881_().m_130946_(suffix);
         graphics.m_280649_(
            this.f_96547_,
            Language.m_128107_().m_5536_(this.f_96547_.m_92854_(label, 136)),
            29,
            y + 4,
            target.availableSlots() > 0 ? textColor : -5622989,
            false
         );
      }

      Component status = null;
      int statusColor = -8947849;
      if (this.awaitingUpload) {
         status = Component.m_237115_("ae2lt.tianshu.upload.pending");
         statusColor = -5605632;
      } else if (this.uploadFailed) {
         status = Component.m_237115_("ae2lt.tianshu.upload.failed");
         statusColor = -5627358;
      } else if (this.awaitingTargets) {
         status = Component.m_237115_("ae2lt.tianshu.upload.loading");
      } else if (this.filtered.isEmpty()) {
         status = Component.m_237115_("ae2lt.tianshu.upload.empty");
      }

      if (status != null) {
         String text = this.f_96547_.m_92834_(status.getString(), 158);
         graphics.m_280056_(this.f_96547_, text, (190 - this.f_96547_.m_92895_(text)) / 2, 33 + this.visibleRows * 17 - 14, statusColor, false);
      }
   }

   public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
      int rowAreaHeight = this.visibleRows * 17;
      graphics.m_280163_(TEXTURE, offsetX, offsetY, 0.0F, 0.0F, 190, 35, 256, 256);
      int stretchHeight = Math.max(0, rowAreaHeight - 3);
      if (stretchHeight > 0) {
         graphics.m_280411_(TEXTURE, offsetX, offsetY + 35, 190, stretchHeight, 0.0F, 35.0F, 190, 1, 256, 256);
      }

      int bottomBorderY = offsetY + 35 + stretchHeight;
      graphics.m_280163_(TEXTURE, offsetX, bottomBorderY, 0.0F, 202.0F, 190, 1, 256, 256);
      graphics.m_280163_(TEXTURE, offsetX, bottomBorderY + 1, 0.0F, 203.0F, 190, 18, 256, 256);
      int start = this.scrollbar.getCurrentScroll();

      for (int row = 0; row < this.visibleRows; row++) {
         int index = start + row;
         int top = offsetY + 33 + row * 17;
         graphics.m_280163_(TEXTURE, offsetX + 9, top, 9.0F, index == this.focusedIndex ? 239.0F : 222.0F, 158, 17, 256, 256);
      }
   }

   protected void m_280072_(GuiGraphics graphics, int mouseX, int mouseY) {
      super.m_280072_(graphics, mouseX, mouseY);
      int index = this.hoveredIndex((double)mouseX, (double)mouseY);
      if (index >= 0) {
         TianshuUploadTargetData target = this.filtered.get(index).target();
         ArrayList<Component> lines = new ArrayList<>();
         lines.add(target.group().name());
         lines.addAll(target.group().tooltip());
         lines.add(Component.m_237110_("ae2lt.tianshu.upload.providers", new Object[]{target.providerCount()}).m_130940_(ChatFormatting.GRAY));
         lines.add(
            Component.m_237110_("ae2lt.tianshu.upload.slots", new Object[]{target.availableSlots()})
               .m_130940_(target.availableSlots() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)
         );
         lines.add(Component.m_237115_("ae2lt.tianshu.upload.alias.quick_bind").m_130940_(ChatFormatting.DARK_GRAY));
         this.drawTooltip(graphics, mouseX, mouseY, lines);
      }
   }

   private int hoveredIndex(double mouseX, double mouseY) {
      double x = mouseX - (double)this.f_97735_;
      double y = mouseY - (double)this.f_97736_ - 33.0;
      if (!(x < 7.0) && !(x >= 169.0) && !(y < 0.0) && !(y >= (double)(this.visibleRows * 17))) {
         int index = this.scrollbar.getCurrentScroll() + (int)(y / 17.0);
         return index >= 0 && index < this.filtered.size() ? index : -1;
      } else {
         return -1;
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 1) {
         int index = this.hoveredIndex(mouseX, mouseY);
         if (m_96638_() && index >= 0 && !this.awaitingUpload) {
            this.focusedIndex = index;
            this.bindAliasFromTarget(this.filtered.get(index));
            playDownSound(Minecraft.m_91087_().m_91106_());
            return true;
         }

         if (this.sourceField.m_5953_(mouseX, mouseY)) {
            this.sourceField.m_94144_("");
            return true;
         }

         if (this.aliasField.m_5953_(mouseX, mouseY)) {
            this.aliasField.m_94144_("");
            return true;
         }
      } else if (button == 0 && !this.awaitingUpload) {
         int indexx = this.hoveredIndex(mouseX, mouseY);
         if (indexx >= 0) {
            this.focusedIndex = indexx;
            playDownSound(Minecraft.m_91087_().m_91106_());
            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      if (button == 0 && this.focusedIndex >= 0 && !this.awaitingUpload) {
         int index = this.hoveredIndex(mouseX, mouseY);
         if (index == this.focusedIndex) {
            this.select(this.filtered.get(index));
            return true;
         }
      }

      return super.m_6348_(mouseX, mouseY, button);
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.defaultAliases.size() > 1 && this.aliasField.m_5953_(mouseX, mouseY)) {
         this.initialAliasSelectionPending = false;
         if (this.defaultAliasIndex < 0) {
            this.defaultAliasIndex = delta > 0.0 ? this.defaultAliases.size() - 1 : 0;
         } else {
            this.defaultAliasIndex = Math.floorMod(this.defaultAliasIndex + (delta > 0.0 ? -1 : 1), this.defaultAliases.size());
         }

         this.updatingAliasField = true;
         this.aliasField.m_94144_(this.defaultAliases.get(this.defaultAliasIndex));
         this.updatingAliasField = false;
         this.queryRefresh = true;
         if (this.f_96541_.f_91074_ != null) {
            this.f_96541_.f_91074_.m_5496_((SoundEvent)SoundEvents.f_12490_.m_203334_(), 0.1F, 1.0F);
         }

         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 257 || keyCode == 335) && this.focusedIndex >= 0 && this.focusedIndex < this.filtered.size() && !this.awaitingUpload) {
         this.select(this.filtered.get(this.focusedIndex));
         return true;
      } else if (this.filtered.isEmpty()) {
         return super.m_7933_(keyCode, scanCode, modifiers);
      } else {
         int direction = switch (keyCode) {
            case 264 -> 1;
            case 265 -> -1;
            default -> 0;
         };
         if (direction == 0) {
            return super.m_7933_(keyCode, scanCode, modifiers);
         } else {
            this.focusedIndex = Math.max(0, Math.min(this.filtered.size() - 1, this.focusedIndex < 0 ? 0 : this.focusedIndex + direction));
            if (this.focusedIndex < this.scrollbar.getCurrentScroll()) {
               this.scrollbar.setCurrentScroll(this.focusedIndex);
            } else if (this.focusedIndex >= this.scrollbar.getCurrentScroll() + this.visibleRows) {
               this.scrollbar.setCurrentScroll(this.focusedIndex - this.visibleRows + 1);
            }

            return true;
         }
      }
   }

   public void m_264158_(ComponentPath path) {
      super.m_264158_(path);
      this.focusedIndex = -1;
   }

   public void m_7379_() {
      this.returnToParent();
   }

   private void select(TianshuUploadTargetScreen.IndexedTarget selected) {
      if (selected.target().availableSlots() <= 0) {
         this.uploadFailed = true;
      } else {
         this.awaitingUpload = true;
         this.uploadFailed = false;
         this.uploadTargetName = selected.target().group().name().m_6881_();
         ((TianshuPatternEncodingTermMenu)this.f_97732_).uploadTianshuPatternToTarget(selected.target().group());
      }
   }

   private void addMapping() {
      String source = this.sourceField.m_94155_().strip();
      String alias = this.aliasField.m_94155_().strip();
      if (!AE2LTClientConfig.setUploadAlias(source, alias)) {
         this.showMessage(Component.m_237115_(source.isEmpty() ? "ae2lt.tianshu.upload.alias.empty_keyword" : "ae2lt.tianshu.upload.alias.empty_alias"));
      } else {
         this.setConfiguredAlias(alias);
         this.defaultAliasIndex = this.indexOfDefaultAlias(alias);
         this.queryRefresh = true;
         this.showMessage(Component.m_237110_("ae2lt.tianshu.upload.alias.added", new Object[]{source, alias}));
      }
   }

   private void bindAliasFromTarget(TianshuUploadTargetScreen.IndexedTarget selected) {
      PatternContainerGroup group = selected.target().group();
      String currentName = group.name().getString();
      String machineId = group.icon() == null ? "" : group.icon().getId().toString();
      String defaultName = group.icon() == null ? "" : group.icon().getDisplayName().getString();
      String alias = TianshuUploadTargetMatcher.preferredAlias(machineId, defaultName, currentName);
      this.aliasField.m_94144_(alias);
      this.addMapping();
   }

   private void removeMappings() {
      String alias = this.aliasField.m_94155_().strip();
      if (alias.isEmpty()) {
         this.showMessage(Component.m_237115_("ae2lt.tianshu.upload.alias.empty_alias"));
      } else {
         int removed = AE2LTClientConfig.removeUploadAliases(alias);
         if (removed <= 0) {
            this.showMessage(Component.m_237110_("ae2lt.tianshu.upload.alias.not_found", new Object[]{alias}));
         } else {
            if (this.configuredAlias.equalsIgnoreCase(alias)) {
               this.clearConfiguredAliasCandidate();
            }

            this.defaultAliasIndex = -1;
            this.aliasField.m_94144_("");
            this.queryRefresh = true;
            this.showMessage(Component.m_237110_("ae2lt.tianshu.upload.alias.removed", new Object[]{removed, alias}));
         }
      }
   }

   private void showMessage(Component message) {
      if (this.f_96541_.f_91074_ != null) {
         this.f_96541_.f_91074_.m_5661_(message, false);
      }
   }

   private void toggleTerminalStyle(SettingToggleButton<TerminalStyle> button, boolean backwards) {
      TerminalStyle next = (TerminalStyle)button.getNextValue(backwards);
      AEConfig.instance().setTerminalStyle(next);
      button.set(next);
      this.m_6702_().removeIf(child -> this.f_169369_.contains(child));
      this.f_169369_.clear();
      this.m_7856_();
   }

   private static void playDownSound(SoundManager soundManager) {
      soundManager.m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
   }

   private static final class AliasActionButton extends IconButton {
      private final Icon icon;

      private AliasActionButton(Icon icon, Component message, Runnable action) {
         super(ignored -> action.run());
         this.icon = icon;
         this.m_93666_(message);
      }

      protected Icon getIcon() {
         return this.icon;
      }
   }

   private static record IndexedTarget(int sourceIndex, TianshuUploadTargetData target) {
   }
}
