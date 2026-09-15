package com.moakiee.ae2lt.client;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.slot.FakeSlot;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceBadge;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.network.tianshu.MaintenanceSummarySyncPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public final class TianshuGlobalReserveScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuPatternEncodingTermScreen<M>> {
   private static final int LIST_LEFT = 9;
   private static final int LIST_RIGHT = 187;
   private static final int LIST_CENTER_X = 98;
   private static final int CONTENT_TEXT_LEFT = 37;
   private static final int STOCK_COLUMN_RIGHT = 143;
   private static final int VALUE_COLUMN_RIGHT = 184;
   private static final int FIRST_ROW = 64;
   private static final int ROW_HEIGHT = 20;
   private static final int VISIBLE_ROWS = 6;
   private static final int EMPTY_TEXT_Y = 121;
   private final AETextField search;
   private final Scrollbar scrollbar;
   private final Button rulesButton;
   private final Button reservesButton;
   private final AETextField reserveAmount;
   private final Button addReserveButton;
   private final boolean restoreMaintainableView;
   private TianshuGlobalReserveScreen.View view = TianshuGlobalReserveScreen.View.RULES;
   private boolean awaitingRuleEditor;
   private int requestedRuleEditorRevision;

   public TianshuGlobalReserveScreen(TianshuPatternEncodingTermScreen<M> parent) {
      super(parent, "/screens/tianshu_inventory_overview.json");
      this.restoreMaintainableView = ((TianshuPatternEncodingTermMenu)this.f_97732_).maintainableView;
      if (this.restoreMaintainableView) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).setMaintainableViewTemporarily(false);
      }

      this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
      this.scrollbar.setCaptureMouseWheel(false);
      this.search = this.widgets.addTextField("search");
      this.search.setPlaceholder(GuiText.SearchPlaceholder.text());
      this.search.m_94151_(ignored -> this.scrollbar.setCurrentScroll(0));
      this.rulesButton = this.widgets
         .addButton("rules", this.tabLabel(TianshuGlobalReserveScreen.View.RULES), () -> this.selectView(TianshuGlobalReserveScreen.View.RULES));
      this.reservesButton = this.widgets
         .addButton("reserves", this.tabLabel(TianshuGlobalReserveScreen.View.RESERVES), () -> this.selectView(TianshuGlobalReserveScreen.View.RESERVES));
      this.reserveAmount = this.widgets.addTextField("reserveAmount");
      this.reserveAmount.m_94199_(19);
      this.reserveAmount.m_94153_(TianshuGlobalReserveScreen::validAddAmountDraft);
      this.reserveAmount.setPlaceholder(Component.m_237115_("ae2lt.tianshu.reserve.add_amount"));
      this.reserveAmount.m_94144_("1");
      this.addReserveButton = this.widgets.addButton("addReserve", Component.m_237115_("ae2lt.tianshu.reserve.add"), this::addMarkedReserve);
      this.widgets.add("back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
   }

   public void m_7856_() {
      this.hideSlots();
      super.m_7856_();
      this.updateAddControls();
   }

   protected void onReturnToParent() {
      this.restoreParentViewMode();
   }

   private void restoreParentViewMode() {
      if (this.restoreMaintainableView) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).setMaintainableViewTemporarily(true);
      }
   }

   void hideSlots() {
      for (SlotSemantic semantic : List.of(
         SlotSemantics.CRAFTING_GRID,
         SlotSemantics.CRAFTING_RESULT,
         SlotSemantics.PROCESSING_INPUTS,
         SlotSemantics.PROCESSING_OUTPUTS,
         SlotSemantics.SMITHING_TABLE_TEMPLATE,
         SlotSemantics.SMITHING_TABLE_BASE,
         SlotSemantics.SMITHING_TABLE_ADDITION,
         SlotSemantics.SMITHING_TABLE_RESULT,
         SlotSemantics.STONECUTTING_INPUT,
         SlotSemantics.BLANK_PATTERN,
         SlotSemantics.ENCODED_PATTERN,
         SlotSemantics.PLAYER_INVENTORY,
         SlotSemantics.PLAYER_HOTBAR
      )) {
         this.setSlotsHidden(semantic, true);
      }
   }

   private void selectView(TianshuGlobalReserveScreen.View selected) {
      if (this.view != selected) {
         this.view = selected;
         this.scrollbar.setCurrentScroll(0);
         this.updateTabLabels();
         this.updateAddControls();
      }
   }

   private Component tabLabel(TianshuGlobalReserveScreen.View tab) {
      return Component.m_237115_(tab.translationKey).m_130940_(tab == this.view ? ChatFormatting.AQUA : ChatFormatting.WHITE);
   }

   private void updateTabLabels() {
      this.rulesButton.m_93666_(this.tabLabel(TianshuGlobalReserveScreen.View.RULES));
      this.reservesButton.m_93666_(this.tabLabel(TianshuGlobalReserveScreen.View.RESERVES));
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      if (this.awaitingRuleEditor
         && ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorRevision() != this.requestedRuleEditorRevision
         && ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorData() != null) {
         this.awaitingRuleEditor = false;
         this.switchToScreen(new TianshuMaintenanceRuleScreen(this, ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorData()));
      } else {
         int max = Math.max(0, this.entries().size() - 6);
         this.scrollbar.setRange(0, max, Math.max(1, 5));
         this.updateTabLabels();
         this.updateAddControls();
      }
   }

   private void updateAddControls() {
      boolean visible = this.view == TianshuGlobalReserveScreen.View.RESERVES;
      this.reserveAmount.m_94194_(visible);
      this.addReserveButton.f_93624_ = visible;
      FakeSlot markSlot = ((TianshuPatternEncodingTermMenu)this.f_97732_).getGlobalReserveMarkSlot();
      markSlot.setActive(visible);
      this.setSlotsHidden(Ae2ltSlotSemantics.TIANSHU_GLOBAL_RESERVE_MARK, !visible);
      this.addReserveButton.f_93623_ = visible
         && ((TianshuPatternEncodingTermMenu)this.f_97732_).maintenanceAvailable
         && this.markedReserveKey() != null
         && this.parsedAddAmount() != Long.MIN_VALUE;
   }

   private static boolean validAddAmountDraft(String value) {
      if (!value.isEmpty() && !value.equals("-1")) {
         try {
            return Long.parseLong(value) > 0L;
         } catch (NumberFormatException var2) {
            return false;
         }
      } else {
         return true;
      }
   }

   private long parsedAddAmount() {
      try {
         long value = Long.parseLong(this.reserveAmount.m_94155_());
         return value != -1L && value <= 0L ? Long.MIN_VALUE : value;
      } catch (NumberFormatException var3) {
         return Long.MIN_VALUE;
      }
   }

   private AEKey markedReserveKey() {
      GenericStack marked = GenericStack.fromItemStack(((TianshuPatternEncodingTermMenu)this.f_97732_).getGlobalReserveMarkSlot().m_7993_());
      return marked != null ? marked.what() : null;
   }

   private void addMarkedReserve() {
      AEKey key = this.markedReserveKey();
      long value = this.parsedAddAmount();
      if (key != null && value != Long.MIN_VALUE && ((TianshuPatternEncodingTermMenu)this.f_97732_).maintenanceAvailable) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).sendGlobalReserve(key, value, ReservedStockMatchMode.EXACT);
         ((TianshuPatternEncodingTermMenu)this.f_97732_).getGlobalReserveMarkSlot().setFilterTo(ItemStack.f_41583_);
         this.reserveAmount.m_94144_("1");
      }
   }

   private List<TianshuGlobalReserveScreen.OverviewEntry> entries() {
      String needle = this.search.m_94155_().strip().toLowerCase(Locale.ROOT);
      Map<AEKey, MaintenanceSummarySyncPacket.Entry> summaries = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummary();
      LinkedHashMap<AEKey, MaintenanceSummarySyncPacket.Entry> merged = new LinkedHashMap<>();
      if (this.view == TianshuGlobalReserveScreen.View.RULES) {
         summaries.values().stream().filter(MaintenanceSummarySyncPacket.Entry::ruleConfigured).forEach(entryx -> merged.put(entryx.key(), entryx));
      } else {
         summaries.values()
            .stream()
            .filter(entryx -> entryx.globalReserveConfigured() || !needle.isEmpty())
            .forEach(entryx -> merged.put(entryx.key(), entryx));
         if (!needle.isEmpty()) {
            for (GridInventoryEntry networkEntry : ((TianshuPatternEncodingTermScreen)this.getParent()).getNetworkEntriesForMaintenance()) {
               AEKey key = networkEntry.getWhat();
               if (key != null && !merged.containsKey(key)) {
                  merged.put(
                     key,
                     new MaintenanceSummarySyncPacket.Entry(
                        key,
                        false,
                        InventoryMaintenanceStatus.IDLE,
                        Math.max(0L, networkEntry.getStoredAmount()),
                        0L,
                        0L,
                        0L,
                        0L,
                        ReservedStockMatchMode.EXACT,
                        false,
                        networkEntry.isCraftable(),
                        false
                     )
                  );
               }
            }
         }
      }

      ArrayList<TianshuGlobalReserveScreen.OverviewEntry> result = new ArrayList<>();

      for (MaintenanceSummarySyncPacket.Entry entry : merged.values()) {
         String displayName = entry.key().getDisplayName().getString();
         if (needle.isEmpty()
            || displayName.toLowerCase(Locale.ROOT).contains(needle)
            || entry.key().getId().toString().toLowerCase(Locale.ROOT).contains(needle)) {
            result.add(new TianshuGlobalReserveScreen.OverviewEntry(entry, displayName));
         }
      }

      result.sort(
         Comparator.comparing(TianshuGlobalReserveScreen.OverviewEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entryx -> entryx.summary().key().getId().toString())
      );
      return List.copyOf(result);
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.overview_title"), 10, 9, 3159099, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.column.item"), 37, 53, 6120557, false);
      this.drawRightAligned(graphics, Component.m_237115_("ae2lt.tianshu.maintenance.column.stock"), 143, 53, 6120557);
      this.drawRightAligned(
         graphics,
         Component.m_237115_(
            this.view == TianshuGlobalReserveScreen.View.RULES ? "ae2lt.tianshu.maintenance.column.target" : "ae2lt.tianshu.maintenance.column.reserve"
         ),
         184,
         53,
         6120557
      );
      graphics.m_280509_(10, 63, 186, 64, 861758573);
      int localMouseX = mouseX - this.f_97735_;
      int localMouseY = mouseY - this.f_97736_;
      List<TianshuGlobalReserveScreen.OverviewEntry> entries = this.entries();
      int start = this.scrollbar.getCurrentScroll();
      int end = Math.min(entries.size(), start + 6);

      for (int index = start; index < end; index++) {
         int row = index - start;
         int y = 64 + row * 20;
         MaintenanceSummarySyncPacket.Entry summary = entries.get(index).summary();
         boolean hovered = localMouseX >= 9 && localMouseX < 187 && localMouseY >= y && localMouseY < y + 20 - 1;
         graphics.m_280509_(9, y, 187, y + 20 - 1, hovered ? 1429959071 : ((row & 1) == 0 ? 520093695 : 301989888));
         graphics.m_280509_(12, y + 8, 16, y + 12, badgeColor(summary));
         graphics.m_280480_(summary.key().wrapForDisplayOrFilter(), 18, y + 2);
         graphics.m_280056_(
            this.f_96547_,
            this.f_96547_.m_92834_(entries.get(index).displayName(), 70),
            37,
            y + 6,
            summary.ruleConfigured() && !summary.craftable() ? 10958133 : 3159099,
            false
         );
         this.drawRightAligned(graphics, compactAmount(summary.key(), summary.storedAmount()), 143, y + 6, 4015696);
         String finalValue = this.view == TianshuGlobalReserveScreen.View.RULES
            ? compactAmount(summary.key(), summary.upperThreshold())
            : formatReserve(summary.globalReserve());
         this.drawRightAligned(
            graphics, finalValue, 184, y + 6, this.view == TianshuGlobalReserveScreen.View.RESERVES && summary.globalReserve() != 0L ? 2383505 : 4015696
         );
      }

      if (!((TianshuPatternEncodingTermMenu)this.f_97732_).maintenanceAvailable) {
         graphics.m_280653_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.unavailable"), 98, 121, 10958133);
      } else if (entries.isEmpty()) {
         graphics.m_280653_(
            this.f_96547_,
            Component.m_237115_(
               this.view == TianshuGlobalReserveScreen.View.RULES
                  ? "ae2lt.tianshu.maintenance.empty"
                  : (this.search.m_94155_().isBlank() ? "ae2lt.tianshu.reserve.empty" : "ae2lt.tianshu.reserve.no_match")
            ),
            98,
            121,
            5593956
         );
      }

      if (this.view == TianshuGlobalReserveScreen.View.RESERVES && ((TianshuPatternEncodingTermMenu)this.f_97732_).maintenanceAvailable) {
         graphics.m_280056_(
            this.f_96547_, this.f_96547_.m_92834_(Component.m_237115_("ae2lt.tianshu.reserve.add_hint").getString(), 188), 10, 190, 6712693, false
         );
      }

      if (((TianshuPatternEncodingTermMenu)this.f_97732_).isMaintenanceSummaryOverflow()) {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.summary_too_large"), 10, 232, 10958133, false);
      }
   }

   private void drawRightAligned(GuiGraphics graphics, String text, int right, int y, int color) {
      graphics.m_280056_(this.f_96547_, text, right - this.f_96547_.m_92895_(text), y, color, false);
   }

   private void drawRightAligned(GuiGraphics graphics, Component text, int right, int y, int color) {
      graphics.m_280614_(this.f_96547_, text, right - this.f_96547_.m_92852_(text), y, color, false);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0 || button == 2) {
         int row = (int)((mouseY - (double)this.f_97736_ - 64.0) / 20.0);
         List<TianshuGlobalReserveScreen.OverviewEntry> entries = this.entries();
         int index = this.scrollbar.getCurrentScroll() + row;
         if (mouseY >= (double)(this.f_97736_ + 64)
            && mouseY < (double)(this.f_97736_ + 64 + 120)
            && row >= 0
            && row < 6
            && index >= 0
            && index < entries.size()
            && mouseX >= (double)(this.f_97735_ + 9)
            && mouseX < (double)(this.f_97735_ + 187)) {
            MaintenanceSummarySyncPacket.Entry summary = entries.get(index).summary();
            if (this.view == TianshuGlobalReserveScreen.View.RULES) {
               this.requestRuleEditor(summary.key());
            } else {
               this.switchToScreen(new TianshuGlobalReserveScreen.GlobalReserveEditScreen<M>(this, summary, this.variantsFor(summary.key())));
            }

            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   private void requestRuleEditor(AEKey key) {
      if (key != null && !this.awaitingRuleEditor) {
         this.requestedRuleEditorRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceEditorRevision();
         this.awaitingRuleEditor = true;
         ((TianshuPatternEncodingTermMenu)this.f_97732_).requestMaintenanceEditor(key);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (mouseX >= (double)(this.f_97735_ + 9)
         && mouseX < (double)(this.f_97735_ + 187)
         && mouseY >= (double)(this.f_97736_ + 64)
         && mouseY < (double)(this.f_97736_ + 64 + 120)) {
         this.scrollbar.setCurrentScroll(this.scrollbar.getCurrentScroll() - (int)Math.signum(delta));
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   protected void m_280072_(GuiGraphics graphics, int x, int y) {
      int row = (y - this.f_97736_ - 64) / 20;
      List<TianshuGlobalReserveScreen.OverviewEntry> entries = this.entries();
      int index = this.scrollbar.getCurrentScroll() + row;
      if (y >= this.f_97736_ + 64
         && y < this.f_97736_ + 64 + 120
         && row >= 0
         && row < 6
         && index >= 0
         && index < entries.size()
         && x >= this.f_97735_ + 9
         && x < this.f_97735_ + 187) {
         MaintenanceSummarySyncPacket.Entry summary = entries.get(index).summary();
         ArrayList<Component> lines = new ArrayList<>();
         lines.add(summary.key().getDisplayName());
         lines.add(
            Component.m_237110_("ae2lt.tianshu.maintenance.tooltip.stock", new Object[]{compactAmount(summary.key(), summary.storedAmount())})
               .m_130940_(ChatFormatting.GRAY)
         );
         if (summary.ruleConfigured()) {
            lines.add(
               Component.m_237110_("ae2lt.tianshu.maintenance.tooltip.thresholds", new Object[]{summary.lowerThreshold(), summary.upperThreshold()})
                  .m_130940_(ChatFormatting.GRAY)
            );
            lines.add(Component.m_237110_("ae2lt.tianshu.maintenance.tooltip.batch", new Object[]{summary.amountPerJob()}).m_130940_(ChatFormatting.GRAY));
            lines.add(
               Component.m_237115_("ae2lt.tianshu.maintenance.status." + summary.status().name().toLowerCase(Locale.ROOT))
                  .m_130940_(statusFormatting(summary.status()))
            );
         }

         if (summary.globalReserve() != 0L) {
            lines.add(
               Component.m_237110_(
                     "ae2lt.tianshu.maintenance.tooltip.reserve",
                     new Object[]{
                        formatReserve(summary.globalReserve()),
                        Component.m_237115_(
                           summary.globalMode() == ReservedStockMatchMode.EXACT ? "ae2lt.tianshu.reserve.exact" : "ae2lt.tianshu.reserve.ignore_nbt"
                        )
                     }
                  )
                  .m_130940_(ChatFormatting.DARK_AQUA)
            );
         }

         lines.add(
            Component.m_237115_(
                  this.view == TianshuGlobalReserveScreen.View.RULES
                     ? "ae2lt.tianshu.maintenance.click_edit_rule"
                     : "ae2lt.tianshu.maintenance.click_edit_reserve"
               )
               .m_130940_(ChatFormatting.DARK_GRAY)
         );
         this.drawTooltip(graphics, x, y, lines);
      } else {
         super.m_280072_(graphics, x, y);
      }
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.returnToParent();
         return true;
      } else {
         return super.m_7933_(keyCode, scanCode, modifiers);
      }
   }

   private static int badgeColor(MaintenanceSummarySyncPacket.Entry entry) {
      if (!entry.ruleConfigured()) {
         return entry.globalReserve() == 0L ? -7695975 : -12681291;
      } else {
         return switch (InventoryMaintenanceBadge.from(entry.status())) {
            case GREEN -> -13256362;
            case YELLOW -> -1987285;
            case RED -> -2930102;
            case GRAY -> -8025199;
         };
      }
   }

   private static ChatFormatting statusFormatting(InventoryMaintenanceStatus status) {
      return switch (InventoryMaintenanceBadge.from(status)) {
         case GREEN -> ChatFormatting.GREEN;
         case YELLOW -> ChatFormatting.GOLD;
         case RED -> ChatFormatting.RED;
         case GRAY -> ChatFormatting.GRAY;
      };
   }

   private static String compactAmount(AEKey key, long amount) {
      return key.formatAmount(Math.max(0L, amount), AmountFormat.SLOT);
   }

   private static String formatReserve(long amount) {
      return amount < 0L ? "∞" : Long.toString(amount);
   }

   private List<TianshuGlobalReserveScreen.ReserveVariant> variantsFor(AEKey selected) {
      AEKey identity = selected.dropSecondary();
      LinkedHashMap<AEKey, TianshuGlobalReserveScreen.ReserveVariant> variants = new LinkedHashMap<>();

      for (GridInventoryEntry networkEntry : ((TianshuPatternEncodingTermScreen)this.getParent()).getNetworkEntriesForMaintenance()) {
         AEKey candidate = networkEntry.getWhat();
         if (candidate != null && identity.equals(candidate.dropSecondary())) {
            MaintenanceSummarySyncPacket.Entry summary = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummaryEntry(candidate);
            boolean exactReserve = summary != null && summary.globalReserveConfigured() && summary.globalMode() == ReservedStockMatchMode.EXACT;
            TianshuGlobalReserveScreen.ReserveVariant variant = new TianshuGlobalReserveScreen.ReserveVariant(
               candidate, Math.max(0L, networkEntry.getStoredAmount()), networkEntry.isCraftable(), exactReserve
            );
            variants.merge(
               candidate,
               variant,
               (left, right) -> new TianshuGlobalReserveScreen.ReserveVariant(
                     candidate,
                     Math.max(left.storedAmount(), right.storedAmount()),
                     left.craftable() || right.craftable(),
                     left.exactReserveConfigured() || right.exactReserveConfigured()
                  )
            );
         }
      }

      MaintenanceSummarySyncPacket.Entry selectedSummary = ((TianshuPatternEncodingTermMenu)this.f_97732_).getMaintenanceSummaryEntry(selected);
      variants.putIfAbsent(
         selected,
         new TianshuGlobalReserveScreen.ReserveVariant(
            selected,
            selectedSummary != null ? selectedSummary.storedAmount() : 0L,
            selectedSummary != null && selectedSummary.craftable(),
            selectedSummary != null && selectedSummary.globalReserveConfigured() && selectedSummary.globalMode() == ReservedStockMatchMode.EXACT
         )
      );
      return variants.values()
         .stream()
         .sorted(
            Comparator.<TianshuGlobalReserveScreen.ReserveVariant, String>comparing(
                  variantx -> variantx.key().getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER
               )
               .thenComparing(variantx -> variantx.key().getId().toString())
         )
         .toList();
   }

   private static final class GlobalReserveEditScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuGlobalReserveScreen<M>> {
      private static final int VARIANT_FIRST_ROW = 134;
      private static final int VARIANT_ROW_HEIGHT = 17;
      private static final int VISIBLE_VARIANTS = 3;
      private final MaintenanceSummarySyncPacket.Entry entry;
      private final List<TianshuGlobalReserveScreen.ReserveVariant> variants;
      private final AETextField amount;
      private final Button modeButton;
      private final Button deleteButton;
      private final Button saveButton;
      private final Scrollbar scrollbar;
      private ReservedStockMatchMode mode;

      GlobalReserveEditScreen(
         TianshuGlobalReserveScreen<M> parent, MaintenanceSummarySyncPacket.Entry entry, List<TianshuGlobalReserveScreen.ReserveVariant> variants
      ) {
         this(parent, entry, variants, new TianshuGlobalReserveScreen.GlobalReserveEditScreen.EditorState(entry.globalReserve(), entry.globalMode()));
      }

      private GlobalReserveEditScreen(
         TianshuGlobalReserveScreen<M> parent,
         MaintenanceSummarySyncPacket.Entry entry,
         List<TianshuGlobalReserveScreen.ReserveVariant> variants,
         TianshuGlobalReserveScreen.GlobalReserveEditScreen.EditorState state
      ) {
         super(
            parent,
            state.mode == ReservedStockMatchMode.IGNORE_SECONDARY ? "/screens/tianshu_reserve_edit_expanded.json" : "/screens/tianshu_reserve_edit.json"
         );
         this.entry = entry;
         this.variants = List.copyOf(variants);
         this.mode = state.mode;
         this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
         this.scrollbar.setCaptureMouseWheel(false);
         this.amount = this.widgets.addTextField("amount");
         this.amount.m_94199_(19);
         this.amount.m_94153_(TianshuGlobalReserveScreen.GlobalReserveEditScreen::validDraft);
         this.amount.m_94144_(Long.toString(state.amount));
         Button scopeButton = this.widgets.addButton("scope", Component.m_237115_("ae2lt.tianshu.reserve.global"), () -> {
         });
         scopeButton.f_93623_ = false;
         this.modeButton = this.widgets.addButton("mode", this.modeLabel(), this::toggleMode);
         this.widgets.addButton("zero", Component.m_237113_("0"), () -> this.amount.m_94144_("0"));
         this.widgets.addButton("stack", Component.m_237113_("64"), () -> this.amount.m_94144_("64"));
         this.widgets.addButton("infinite", Component.m_237113_("∞"), () -> this.amount.m_94144_("-1"));
         this.deleteButton = this.widgets.addButton("delete", Component.m_237115_("ae2lt.tianshu.reserve.delete"), this::delete);
         this.saveButton = this.widgets.addButton("save", Component.m_237115_("gui.done"), this::save);
         this.widgets.addButton("cancel", Component.m_237115_("gui.cancel"), () -> this.returnToParent());
         this.widgets.add("back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
      }

      private static boolean validDraft(String value) {
         if (!value.isEmpty() && !value.equals("-1")) {
            try {
               return Long.parseLong(value) >= 0L;
            } catch (NumberFormatException var2) {
               return false;
            }
         } else {
            return true;
         }
      }

      private long parsedAmount() {
         try {
            long value = Long.parseLong(this.amount.m_94155_());
            return value >= -1L ? value : Long.MIN_VALUE;
         } catch (NumberFormatException var3) {
            return Long.MIN_VALUE;
         }
      }

      private void toggleMode() {
         long parsed = this.parsedAmount();
         if (parsed != Long.MIN_VALUE) {
            this.mode = this.mode == ReservedStockMatchMode.EXACT ? ReservedStockMatchMode.IGNORE_SECONDARY : ReservedStockMatchMode.EXACT;
            this.switchToScreen(
               new TianshuGlobalReserveScreen.GlobalReserveEditScreen(
                  (TianshuGlobalReserveScreen<M>)this.getParent(),
                  this.entry,
                  this.variants,
                  new TianshuGlobalReserveScreen.GlobalReserveEditScreen.EditorState(parsed, this.mode)
               )
            );
         }
      }

      private Component modeLabel() {
         return Component.m_237115_(this.mode == ReservedStockMatchMode.EXACT ? "ae2lt.tianshu.reserve.exact" : "ae2lt.tianshu.reserve.ignore_nbt");
      }

      private void save() {
         long parsed = this.parsedAmount();
         if (parsed != Long.MIN_VALUE) {
            ((TianshuPatternEncodingTermMenu)this.f_97732_).sendGlobalReserve(this.entry.key(), parsed, this.mode);
            this.returnToParent();
         }
      }

      private void delete() {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).sendGlobalReserve(this.entry.key(), 0L, this.entry.globalMode());
         this.returnToParent();
      }

      protected void updateBeforeRender() {
         super.updateBeforeRender();
         this.deleteButton.f_93623_ = this.entry.globalReserveConfigured();
         this.saveButton.f_93623_ = this.parsedAmount() != Long.MIN_VALUE;
         this.modeButton.m_93666_(this.modeLabel());
         boolean showVariants = this.mode == ReservedStockMatchMode.IGNORE_SECONDARY && !this.variants.isEmpty();
         this.scrollbar.setVisible(showVariants);
         this.scrollbar.setRange(0, Math.max(0, this.variants.size() - 3), 1);
      }

      public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
         super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
         graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.reserve.title"), 10, 9, 3159099, false);
         graphics.m_280480_(this.entry.key().wrapForDisplayOrFilter(), 16, 32);
         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(this.entry.key().getDisplayName().getString(), 147), 39, 35, 3159099, false);
         MutableComponent currentProtected = Component.m_237110_(
            "ae2lt.tianshu.reserve.current_protected",
            new Object[]{
               TianshuGlobalReserveScreen.compactAmount(this.entry.key(), this.visibleStock()),
               TianshuGlobalReserveScreen.compactAmount(this.entry.key(), this.protectedStock())
            }
         );
         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(currentProtected.getString(), 187), 10, 83, 5857643, false);
         MutableComponent amountLabel = Component.m_237115_("ae2lt.tianshu.reserve.amount");
         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(amountLabel.getString(), 64), 10, 99, 4212302, false);
         if (this.mode == ReservedStockMatchMode.IGNORE_SECONDARY) {
            if (!this.variants.isEmpty()) {
               graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.reserve.variant_title"), 10, 116, 5594470, false);
               int start = this.scrollbar.getCurrentScroll();
               int end = Math.min(this.variants.size(), start + 3);

               for (int index = start; index < end; index++) {
                  int y = 134 + (index - start) * 17;
                  TianshuGlobalReserveScreen.ReserveVariant variant = this.variants.get(index);
                  graphics.m_280480_(variant.key().wrapForDisplayOrFilter(), 12, y);
                  graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(variant.key().getDisplayName().getString(), 112), 33, y + 4, 3949387, false);
                  String stock = TianshuGlobalReserveScreen.compactAmount(variant.key(), variant.storedAmount());
                  graphics.m_280056_(this.f_96547_, stock, 185 - this.f_96547_.m_92895_(stock), y + 4, variant.craftable() ? 3108156 : 6054763, false);
               }
            } else {
               MutableComponent emptyText = Component.m_237115_("ae2lt.tianshu.reserve.variant_empty");
               graphics.m_280614_(this.f_96547_, emptyText, 98 - this.f_96547_.m_92852_(emptyText) / 2, 156, 6120557, false);
            }
         }
      }

      private long visibleStock() {
         if (this.mode == ReservedStockMatchMode.EXACT) {
            return this.entry.storedAmount();
         } else {
            long total = 0L;

            for (TianshuGlobalReserveScreen.ReserveVariant variant : this.variants) {
               if (!variant.exactReserveConfigured()) {
                  if (Long.MAX_VALUE - total < variant.storedAmount()) {
                     return Long.MAX_VALUE;
                  }

                  total += variant.storedAmount();
               }
            }

            return total;
         }
      }

      private long protectedStock() {
         long stock = this.visibleStock();
         long configured = this.parsedAmount();
         if (configured != Long.MIN_VALUE && configured != 0L) {
            return configured < 0L ? stock : Math.min(stock, configured);
         } else {
            return 0L;
         }
      }

      public boolean m_6050_(double mouseX, double mouseY, double delta) {
         if (this.mode == ReservedStockMatchMode.IGNORE_SECONDARY
            && !this.variants.isEmpty()
            && mouseY >= (double)(this.f_97736_ + 134)
            && mouseY < (double)(this.f_97736_ + 134 + 51)) {
            this.scrollbar.setCurrentScroll(this.scrollbar.getCurrentScroll() - (int)Math.signum(delta));
            return true;
         } else {
            return super.m_6050_(mouseX, mouseY, delta);
         }
      }

      protected void m_280072_(GuiGraphics graphics, int x, int y) {
         int row = (y - this.f_97736_ - 134) / 17;
         int index = this.scrollbar.getCurrentScroll() + row;
         if (this.mode == ReservedStockMatchMode.IGNORE_SECONDARY
            && y >= this.f_97736_ + 134
            && y < this.f_97736_ + 134 + 51
            && row >= 0
            && row < 3
            && index >= 0
            && index < this.variants.size()
            && x >= this.f_97735_ + 9
            && x < this.f_97735_ + 187) {
            TianshuGlobalReserveScreen.ReserveVariant variant = this.variants.get(index);
            ArrayList<Component> lines = new ArrayList<>(AEKeyRendering.getTooltip(variant.key()));
            lines.add(
               Component.m_237110_(
                     "ae2lt.tianshu.maintenance.tooltip.stock", new Object[]{TianshuGlobalReserveScreen.compactAmount(variant.key(), variant.storedAmount())}
                  )
                  .m_130940_(ChatFormatting.GRAY)
            );
            if (variant.craftable()) {
               lines.add(ButtonToolTips.Craftable.text().m_130940_(ChatFormatting.GREEN));
            }

            if (variant.exactReserveConfigured()) {
               lines.add(Component.m_237115_("ae2lt.tianshu.reserve.exact_override").m_130940_(ChatFormatting.DARK_AQUA));
            }

            this.drawTooltip(graphics, x, y, lines);
         } else {
            super.m_280072_(graphics, x, y);
         }
      }

      public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
         if (keyCode == 256) {
            this.returnToParent();
            return true;
         } else {
            return super.m_7933_(keyCode, scanCode, modifiers);
         }
      }

      private static record EditorState(long amount, ReservedStockMatchMode mode) {
      }
   }

   private static record OverviewEntry(MaintenanceSummarySyncPacket.Entry summary, String displayName) {
   }

   private static record ReserveVariant(AEKey key, long storedAmount, boolean craftable, boolean exactReserveConfigured) {
   }

   private static enum View {
      RULES("ae2lt.tianshu.maintenance.rules_tab"),
      RESERVES("ae2lt.tianshu.maintenance.reserves_tab");

      private final String translationKey;

      private View(String translationKey) {
         this.translationKey = translationKey;
      }
   }
}
