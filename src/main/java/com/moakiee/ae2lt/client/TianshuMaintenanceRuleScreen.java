package com.moakiee.ae2lt.client;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.ButtonToolTips;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.client.gui.AE2Button;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceBadge;
import com.moakiee.ae2lt.logic.tianshu.maintenance.InventoryMaintenanceStatus;
import com.moakiee.ae2lt.logic.tianshu.maintenance.ReservedStockMatchMode;
import com.moakiee.ae2lt.logic.tianshu.terminal.MaintenanceEditorData;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.network.tianshu.SaveMaintenanceRulePacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.Slot;

public final class TianshuMaintenanceRuleScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, AEBaseScreen<M>> {
   private static final int LIST_LEFT = 9;
   private static final int LIST_RIGHT = 187;
   private static final int FIRST_ROW_Y = 134;
   private static final int ROW_HEIGHT = 17;
   private static final int VISIBLE_ROWS = 3;
   private final TianshuMaintenanceRuleScreen.Draft draft;
   private final AETextField lower;
   private final AETextField upper;
   private final AETextField perJob;
   private final AECheckbox enabled;
   private final Scrollbar scrollbar;
   private final AE2Button saveButton;
   private final AE2Button deleteButton;
   private final AE2Button checkButton;
   private final AE2Button cancelJobButton;
   private boolean deleteArmed;

   public TianshuMaintenanceRuleScreen(AEBaseScreen<M> parent, MaintenanceEditorData data) {
      super(parent, "/screens/tianshu_maintenance_rule.json");
      this.draft = new TianshuMaintenanceRuleScreen.Draft(data);
      this.lower = this.numberField("lower", data.lowerThreshold());
      this.upper = this.numberField("upper", data.upperThreshold());
      this.perJob = this.numberField("perJob", data.amountPerJob());
      this.enabled = this.widgets.addCheckbox("enabled", Component.m_237115_("ae2lt.tianshu.maintenance.enabled"), () -> {
      });
      this.enabled.setSelected(data.enabled());
      this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
      this.scrollbar.setCaptureMouseWheel(false);
      this.checkButton = new AE2Button(Component.m_237115_("ae2lt.tianshu.maintenance.check_now"), btn -> this.checkNow());
      this.widgets.add("check", this.checkButton);
      this.cancelJobButton = new AE2Button(Component.m_237115_("ae2lt.tianshu.maintenance.cancel_job"), btn -> this.cancelJob());
      this.widgets.add("cancelJob", this.cancelJobButton);
      this.deleteButton = new AE2Button(Component.m_237115_("ae2lt.tianshu.maintenance.delete"), btn -> this.delete());
      this.widgets.add("delete", this.deleteButton);
      this.widgets.addButton("cancel", Component.m_237115_("gui.cancel"), () -> this.returnToParent());
      this.saveButton = new AE2Button(Component.m_237115_("gui.done"), btn -> this.save());
      this.widgets.add("save", this.saveButton);
      this.widgets.add("back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
   }

   public void m_7856_() {
      this.hideTerminalSlots();
      super.m_7856_();
   }

   private AETextField numberField(String id, long initialValue) {
      AETextField field = this.widgets.addTextField(id);
      field.m_94199_(19);
      field.m_94153_(TianshuMaintenanceRuleScreen::isNonNegativeIntegerDraft);
      field.m_94144_(Long.toString(initialValue));
      return field;
   }

   private static boolean isNonNegativeIntegerDraft(String value) {
      if (value.isEmpty()) {
         return true;
      } else {
         try {
            return Long.parseLong(value) >= 0L;
         } catch (NumberFormatException var2) {
            return false;
         }
      }
   }

   private void hideTerminalSlots() {
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

   public void m_280092_(GuiGraphics graphics, Slot slot) {
   }

   protected boolean m_97774_(Slot slot, double mouseX, double mouseY) {
      return false;
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.scrollbar.setRange(0, Math.max(0, this.draft.reserves.size() - 3), Math.max(1, 2));
      this.saveButton.f_93623_ = this.validationError() == null;
      this.deleteButton.f_93624_ = this.draft.data.ruleId() != null;
      this.deleteButton.f_93623_ = this.draft.data.ruleId() != null;
      this.deleteButton.m_93666_(Component.m_237115_(this.deleteArmed ? "ae2lt.tianshu.maintenance.confirm_delete" : "ae2lt.tianshu.maintenance.delete"));
      boolean existing = this.draft.data.ruleId() != null;
      this.checkButton.f_93624_ = existing;
      this.checkButton.f_93623_ = existing
         && this.draft.data.status() != InventoryMaintenanceStatus.CRAFTING
         && this.draft.data.status() != InventoryMaintenanceStatus.CANCELLING;
      this.cancelJobButton.f_93624_ = existing;
      this.cancelJobButton.f_93623_ = existing
         && (this.draft.data.status() == InventoryMaintenanceStatus.CRAFTING || this.draft.data.status() == InventoryMaintenanceStatus.CANCELLING);
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
      graphics.m_280614_(
         this.f_96547_,
         Component.m_237115_(this.draft.data.ruleId() == null ? "ae2lt.tianshu.maintenance.create_title" : "ae2lt.tianshu.maintenance.edit_title"),
         10,
         9,
         3159099,
         false
      );
      graphics.m_280480_(this.draft.data.target().wrapForDisplayOrFilter(), 16, 32);
      graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(this.draft.data.target().getDisplayName().getString(), 147), 39, 29, 3159099, false);
      MutableComponent stockAndStatus = Component.m_237110_(
         "ae2lt.tianshu.maintenance.stock_and_status",
         new Object[]{compactAmount(this.draft.data.target(), this.draft.data.currentStock()), Component.m_237115_(statusKey(this.draft.data.status()))}
      );
      graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(stockAndStatus.getString(), 147), 39, 41, statusColor(this.draft.data.status()), false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.lower"), 10, 57, 5265248, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.upper"), 73, 57, 5265248, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.batch"), 137, 57, 5265248, false);
      Component validationError = this.validationError();
      if (validationError != null) {
         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(validationError.getString(), 181), 10, 116, 11677494, false);
      } else if (this.draft.data.recoveryPage()) {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.recovery_page"), 10, 116, 10958133, false);
      } else {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.maintenance.topology"), 10, 116, 6120557, false);
         this.drawCentered(graphics, Component.m_237115_("ae2lt.tianshu.maintenance.column.stock"), 117, 116, 6120557);
         this.drawCentered(graphics, Component.m_237115_("ae2lt.tianshu.maintenance.column.global_short"), 151, 116, 6120557);
         this.drawCentered(graphics, Component.m_237115_("ae2lt.tianshu.maintenance.column.rule_short"), 179, 116, 6120557);
      }

      this.drawTopology(graphics, mouseX - this.f_97735_, mouseY - this.f_97736_);
   }

   private void drawTopology(GuiGraphics graphics, int mouseX, int mouseY) {
      int start = this.scrollbar.getCurrentScroll();
      int end = Math.min(this.draft.reserves.size(), start + 3);

      for (int index = start; index < end; index++) {
         int row = index - start;
         int y = 134 + row * 17;
         TianshuMaintenanceRuleScreen.ReserveDraft entry = this.draft.reserves.get(index);
         boolean hovered = mouseY >= y && mouseY < y + 17 - 1 && mouseX >= 9 && mouseX < 187;
         graphics.m_280509_(9, y, 187, y + 17 - 1, hovered ? 1429959071 : ((row & 1) == 0 ? 520093695 : 301989888));
         int indent = Math.min(4, entry.depth) * 4;
         graphics.m_280480_(entry.key.wrapForDisplayOrFilter(), 11 + indent, y + 1);
         graphics.m_280056_(
            this.f_96547_,
            this.f_96547_.m_92834_(entry.key.getDisplayName().getString(), 69 - indent),
            31 + indent,
            y + 4,
            entry.craftable ? 3159099 : 10958133,
            false
         );
         this.drawRightAligned(graphics, Component.m_237113_(compactAmount(entry.key, entry.storedAmount)), 125, y + 4, 4475731);
         this.drawRightAligned(graphics, reserveText(entry.globalAmount, entry.globalMode), 158, y + 4, entry.globalAmount == 0L ? 8028295 : 2383505);
         this.drawRightAligned(graphics, reserveText(entry.ruleAmount, entry.ruleMode), 185, y + 4, entry.ruleAmount == 0L ? 8028295 : 7949713);
      }
   }

   private void drawRightAligned(GuiGraphics graphics, Component text, int right, int y, int color) {
      graphics.m_280614_(this.f_96547_, text, right - this.f_96547_.m_92852_(text), y, color, false);
   }

   private void drawCentered(GuiGraphics graphics, Component text, int center, int y, int color) {
      graphics.m_280614_(this.f_96547_, text, center - this.f_96547_.m_92852_(text) / 2, y, color, false);
   }

   private static Component reserveText(long amount, ReservedStockMatchMode mode) {
      String value = amount < 0L ? "∞" : Long.toString(amount);
      return Component.m_237113_(value + (mode == ReservedStockMatchMode.IGNORE_SECONDARY ? "*" : ""));
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 2 || button == 0) {
         int row = (int)((mouseY - (double)this.f_97736_ - 134.0) / 17.0);
         int index = this.scrollbar.getCurrentScroll() + row;
         if (mouseY >= (double)(this.f_97736_ + 134)
            && mouseY < (double)(this.f_97736_ + 134 + 51)
            && row >= 0
            && row < 3
            && index < this.draft.reserves.size()
            && mouseX >= (double)(this.f_97735_ + 9)
            && mouseX < (double)(this.f_97735_ + 187)) {
            TianshuMaintenanceRuleScreen.ReserveDraft reserve = this.draft.reserves.get(index);
            this.switchToScreen(
               new TianshuMaintenanceRuleScreen.ReserveEditorScreen<M>(
                  this, reserve, this.draft.data.target().equals(reserve.key) ? this.draft.data.variants() : List.of()
               )
            );
            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (mouseX >= (double)(this.f_97735_ + 9)
         && mouseX < (double)(this.f_97735_ + 187)
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
      if (y >= this.f_97736_ + 134
         && y < this.f_97736_ + 134 + 51
         && row >= 0
         && row < 3
         && index >= 0
         && index < this.draft.reserves.size()
         && x >= this.f_97735_ + 9
         && x < this.f_97735_ + 187) {
         TianshuMaintenanceRuleScreen.ReserveDraft reserve = this.draft.reserves.get(index);
         ArrayList<Component> lines = new ArrayList<>();
         lines.add(reserve.key.getDisplayName());
         lines.add(
            Component.m_237110_("ae2lt.tianshu.maintenance.tooltip.stock", new Object[]{compactAmount(reserve.key, reserve.storedAmount)})
               .m_130940_(ChatFormatting.GRAY)
         );
         lines.add(
            Component.m_237110_("ae2lt.tianshu.reserve.global_value", new Object[]{formatReserve(reserve.globalAmount), modeLabel(reserve.globalMode)})
               .m_130940_(ChatFormatting.BLUE)
         );
         lines.add(
            Component.m_237110_("ae2lt.tianshu.reserve.rule_value", new Object[]{formatReserve(reserve.ruleAmount), modeLabel(reserve.ruleMode)})
               .m_130940_(ChatFormatting.DARK_PURPLE)
         );
         lines.add(Component.m_237115_("ae2lt.tianshu.maintenance.click_edit_reserve").m_130940_(ChatFormatting.DARK_GRAY));
         this.drawTooltip(graphics, x, y, lines);
      } else {
         super.m_280072_(graphics, x, y);
      }
   }

   private Component validationError() {
      Long parsedLower = parse(this.lower);
      Long parsedUpper = parse(this.upper);
      Long parsedPerJob = parse(this.perJob);
      if (parsedLower != null && parsedUpper != null && parsedPerJob != null) {
         if (parsedUpper < parsedLower) {
            return Component.m_237115_("ae2lt.tianshu.maintenance.error.thresholds");
         } else {
            return parsedPerJob <= 0L ? Component.m_237115_("ae2lt.tianshu.maintenance.error.batch") : null;
         }
      } else {
         return Component.m_237115_("ae2lt.tianshu.maintenance.error.number");
      }
   }

   private void save() {
      if (this.validationError() == null) {
         long parsedLower = parse(this.lower);
         long parsedUpper = parse(this.upper);
         long parsedPerJob = parse(this.perJob);
         List<SaveMaintenanceRulePacket.ReserveEdit> edits = this.draft
            .reserves
            .stream()
            .map(entry -> new SaveMaintenanceRulePacket.ReserveEdit(entry.key, entry.globalAmount, entry.globalMode, entry.ruleAmount, entry.ruleMode))
            .toList();
         ((TianshuPatternEncodingTermMenu)this.f_97732_)
            .sendMaintenanceSave(
               new SaveMaintenanceRulePacket(
                  ((TianshuPatternEncodingTermMenu)this.f_97732_).f_38840_,
                  ((TianshuPatternEncodingTermMenu)this.f_97732_).tianshuSelectionRevision,
                  this.draft.data.target(),
                  this.draft.data.ruleId(),
                  false,
                  parsedLower,
                  parsedUpper,
                  parsedPerJob,
                  this.enabled.isSelected(),
                  edits
               )
            );
         this.returnToParent();
      }
   }

   private void delete() {
      if (this.draft.data.ruleId() != null) {
         if (!this.deleteArmed) {
            this.deleteArmed = true;
         } else {
            ((TianshuPatternEncodingTermMenu)this.f_97732_)
               .sendMaintenanceSave(
                  new SaveMaintenanceRulePacket(
                     ((TianshuPatternEncodingTermMenu)this.f_97732_).f_38840_,
                     ((TianshuPatternEncodingTermMenu)this.f_97732_).tianshuSelectionRevision,
                     this.draft.data.target(),
                     this.draft.data.ruleId(),
                     true,
                     0L,
                     1L,
                     1L,
                     false,
                     List.of()
                  )
               );
            this.returnToParent();
         }
      }
   }

   private void checkNow() {
      if (this.draft.data.ruleId() != null) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).runMaintenanceAction(this.draft.data.ruleId(), false);
         this.returnToParent();
      }
   }

   private void cancelJob() {
      if (this.draft.data.ruleId() != null) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).runMaintenanceAction(this.draft.data.ruleId(), true);
         this.returnToParent();
      }
   }

   private static Long parse(AETextField field) {
      try {
         return Long.parseLong(field.m_94155_());
      } catch (NumberFormatException var2) {
         return null;
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

   private static String compactAmount(AEKey key, long amount) {
      return key.formatAmount(Math.max(0L, amount), AmountFormat.SLOT);
   }

   private static String formatReserve(long amount) {
      return amount < 0L ? "∞" : Long.toString(amount);
   }

   private static Component modeLabel(ReservedStockMatchMode mode) {
      return Component.m_237115_(mode == ReservedStockMatchMode.EXACT ? "ae2lt.tianshu.reserve.exact" : "ae2lt.tianshu.reserve.ignore_nbt");
   }

   private static String statusKey(InventoryMaintenanceStatus status) {
      return "ae2lt.tianshu.maintenance.status." + status.name().toLowerCase(Locale.ROOT);
   }

   private static int statusColor(InventoryMaintenanceStatus status) {
      return switch (InventoryMaintenanceBadge.from(status)) {
         case GREEN -> 2854467;
         case YELLOW -> 10185238;
         case RED -> 11679035;
         case GRAY -> 7238521;
      };
   }

   private static final class Draft {
      final MaintenanceEditorData data;
      final List<TianshuMaintenanceRuleScreen.ReserveDraft> reserves = new ArrayList<>();

      Draft(MaintenanceEditorData data) {
         this.data = data;

         for (MaintenanceEditorData.TopologyEntry entry : data.topology()) {
            this.reserves.add(new TianshuMaintenanceRuleScreen.ReserveDraft(entry));
         }
      }
   }

   private static final class ReserveDraft {
      final AEKey key;
      final int depth;
      final boolean craftable;
      final long storedAmount;
      long globalAmount;
      ReservedStockMatchMode globalMode;
      long ruleAmount;
      ReservedStockMatchMode ruleMode;

      ReserveDraft(MaintenanceEditorData.TopologyEntry entry) {
         this.key = entry.key();
         this.depth = entry.depth();
         this.craftable = entry.craftable();
         this.storedAmount = entry.storedAmount();
         this.globalAmount = entry.globalReserve();
         this.globalMode = entry.globalMode();
         this.ruleAmount = entry.ruleReserve();
         this.ruleMode = entry.ruleMode();
      }
   }

   private static final class ReserveEditorScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuMaintenanceRuleScreen<M>> {
      private static final int VARIANT_FIRST_ROW = 134;
      private static final int VARIANT_ROW_HEIGHT = 17;
      private static final int VISIBLE_VARIANTS = 3;
      private final TianshuMaintenanceRuleScreen.ReserveDraft reserve;
      private final List<MaintenanceEditorData.VariantEntry> variants;
      private final AETextField amount;
      private final AE2Button scopeButton;
      private final AE2Button modeButton;
      private final AE2Button deleteButton;
      private final AE2Button saveButton;
      private final Scrollbar scrollbar;
      private boolean global = true;
      private long globalAmount;
      private ReservedStockMatchMode globalMode;
      private long ruleAmount;
      private ReservedStockMatchMode ruleMode;

      ReserveEditorScreen(
         TianshuMaintenanceRuleScreen<M> parent, TianshuMaintenanceRuleScreen.ReserveDraft reserve, List<MaintenanceEditorData.VariantEntry> variants
      ) {
         this(
            parent,
            reserve,
            variants,
            new TianshuMaintenanceRuleScreen.ReserveEditorScreen.EditorState(
               true, reserve.globalAmount, reserve.globalMode, reserve.ruleAmount, reserve.ruleMode
            )
         );
      }

      private ReserveEditorScreen(
         TianshuMaintenanceRuleScreen<M> parent,
         TianshuMaintenanceRuleScreen.ReserveDraft reserve,
         List<MaintenanceEditorData.VariantEntry> variants,
         TianshuMaintenanceRuleScreen.ReserveEditorScreen.EditorState state
      ) {
         super(
            parent,
            state.selectedMode() == ReservedStockMatchMode.IGNORE_SECONDARY
               ? "/screens/tianshu_reserve_edit_expanded.json"
               : "/screens/tianshu_reserve_edit.json"
         );
         this.reserve = reserve;
         this.variants = List.copyOf(variants);
         this.global = state.global;
         this.globalAmount = state.globalAmount;
         this.globalMode = state.globalMode;
         this.ruleAmount = state.ruleAmount;
         this.ruleMode = state.ruleMode;
         this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
         this.scrollbar.setCaptureMouseWheel(false);
         this.amount = this.widgets.addTextField("amount");
         this.amount.m_94199_(19);
         this.amount.m_94153_(TianshuMaintenanceRuleScreen.ReserveEditorScreen::validDraft);
         this.amount.m_94144_(TianshuMaintenanceRuleScreen.formatReserve(this.globalAmount).replace("∞", "-1"));
         this.scopeButton = new AE2Button(this.scopeLabel(), btn -> this.toggleScope());
         this.widgets.add("scope", this.scopeButton);
         this.modeButton = new AE2Button(this.modeLabel(), btn -> this.toggleMode());
         this.widgets.add("mode", this.modeButton);
         this.widgets.addButton("zero", Component.m_237113_("0"), () -> this.amount.m_94144_("0"));
         this.widgets.addButton("stack", Component.m_237113_("64"), () -> this.amount.m_94144_("64"));
         this.widgets.addButton("infinite", Component.m_237113_("∞"), () -> this.amount.m_94144_("-1"));
         this.deleteButton = new AE2Button(Component.m_237115_("ae2lt.tianshu.reserve.delete"), btn -> this.delete());
         this.widgets.add("delete", this.deleteButton);
         this.widgets.addButton("cancel", Component.m_237115_("gui.cancel"), () -> this.returnToParent());
         this.saveButton = new AE2Button(Component.m_237115_("gui.done"), btn -> this.save());
         this.widgets.add("save", this.saveButton);
         this.widgets.add("back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
      }

      public void m_280092_(GuiGraphics graphics, Slot slot) {
      }

      protected boolean m_97774_(Slot slot, double mouseX, double mouseY) {
         return false;
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

      private void storeVisibleDraft() {
         long parsed = this.parsedAmount();
         if (parsed != Long.MIN_VALUE) {
            if (this.global) {
               this.globalAmount = parsed;
            } else {
               this.ruleAmount = parsed;
            }
         }
      }

      private void toggleScope() {
         if (this.parsedAmount() != Long.MIN_VALUE) {
            this.storeVisibleDraft();
            this.global = !this.global;
            this.reopen();
         }
      }

      private void toggleMode() {
         if (this.parsedAmount() != Long.MIN_VALUE) {
            this.storeVisibleDraft();
            if (this.global) {
               this.globalMode = this.globalMode == ReservedStockMatchMode.EXACT ? ReservedStockMatchMode.IGNORE_SECONDARY : ReservedStockMatchMode.EXACT;
            } else {
               this.ruleMode = this.ruleMode == ReservedStockMatchMode.EXACT ? ReservedStockMatchMode.IGNORE_SECONDARY : ReservedStockMatchMode.EXACT;
            }

            this.reopen();
         }
      }

      private void reopen() {
         this.switchToScreen(
            new TianshuMaintenanceRuleScreen.ReserveEditorScreen(
               (TianshuMaintenanceRuleScreen<M>)this.getParent(),
               this.reserve,
               this.variants,
               new TianshuMaintenanceRuleScreen.ReserveEditorScreen.EditorState(this.global, this.globalAmount, this.globalMode, this.ruleAmount, this.ruleMode)
            )
         );
      }

      private Component scopeLabel() {
         return Component.m_237115_(this.global ? "ae2lt.tianshu.reserve.global" : "ae2lt.tianshu.reserve.rule");
      }

      private Component modeLabel() {
         ReservedStockMatchMode mode = this.global ? this.globalMode : this.ruleMode;
         return TianshuMaintenanceRuleScreen.modeLabel(mode);
      }

      private void save() {
         if (this.parsedAmount() != Long.MIN_VALUE) {
            this.storeVisibleDraft();
            this.reserve.globalAmount = this.globalAmount;
            this.reserve.globalMode = this.globalMode;
            this.reserve.ruleAmount = this.ruleAmount;
            this.reserve.ruleMode = this.ruleMode;
            this.returnToParent();
         }
      }

      private void delete() {
         if (this.global) {
            this.globalAmount = 0L;
         } else {
            this.ruleAmount = 0L;
         }

         this.reserve.globalAmount = this.globalAmount;
         this.reserve.globalMode = this.globalMode;
         this.reserve.ruleAmount = this.ruleAmount;
         this.reserve.ruleMode = this.ruleMode;
         this.returnToParent();
      }

      protected void updateBeforeRender() {
         super.updateBeforeRender();
         this.deleteButton.f_93623_ = (this.global ? this.globalAmount : this.ruleAmount) != 0L;
         this.saveButton.f_93623_ = this.parsedAmount() != Long.MIN_VALUE;
         this.scopeButton.m_93666_(this.scopeLabel());
         this.modeButton.m_93666_(this.modeLabel());
         boolean showVariants = (this.global ? this.globalMode : this.ruleMode) == ReservedStockMatchMode.IGNORE_SECONDARY && !this.variants.isEmpty();
         this.scrollbar.setVisible(showVariants);
         this.scrollbar.setRange(0, Math.max(0, this.variants.size() - 3), 1);
      }

      public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
         super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
         graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.reserve.title"), 10, 9, 3159099, false);
         graphics.m_280480_(this.reserve.key.wrapForDisplayOrFilter(), 16, 32);
         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(this.reserve.key.getDisplayName().getString(), 147), 39, 35, 3159099, false);
         graphics.m_280614_(
            this.f_96547_,
            Component.m_237110_(
               "ae2lt.tianshu.reserve.current_stock", new Object[]{TianshuMaintenanceRuleScreen.compactAmount(this.reserve.key, this.reserve.storedAmount)}
            ),
            10,
            83,
            5857643,
            false
         );
         MutableComponent amountLabel = Component.m_237115_("ae2lt.tianshu.reserve.amount");
         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(amountLabel.getString(), 64), 10, 99, 4212302, false);
         ReservedStockMatchMode selectedMode = this.global ? this.globalMode : this.ruleMode;
         if (selectedMode == ReservedStockMatchMode.IGNORE_SECONDARY) {
            if (!this.variants.isEmpty()) {
               graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.reserve.variant_title"), 10, 116, 5594470, false);
               int start = this.scrollbar.getCurrentScroll();
               int end = Math.min(this.variants.size(), start + 3);

               for (int index = start; index < end; index++) {
                  int y = 134 + (index - start) * 17;
                  MaintenanceEditorData.VariantEntry variant = this.variants.get(index);
                  graphics.m_280480_(variant.key().wrapForDisplayOrFilter(), 12, y);
                  graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(variant.key().getDisplayName().getString(), 112), 33, y + 4, 3949387, false);
                  String stock = TianshuMaintenanceRuleScreen.compactAmount(variant.key(), variant.storedAmount());
                  graphics.m_280056_(this.f_96547_, stock, 185 - this.f_96547_.m_92895_(stock), y + 4, variant.craftable() ? 3108156 : 6054763, false);
               }
            } else {
               MutableComponent emptyText = Component.m_237115_("ae2lt.tianshu.reserve.variant_empty");
               graphics.m_280614_(this.f_96547_, emptyText, 98 - this.f_96547_.m_92852_(emptyText) / 2, 156, 6120557, false);
            }
         }
      }

      public boolean m_6050_(double mouseX, double mouseY, double delta) {
         ReservedStockMatchMode selectedMode = this.global ? this.globalMode : this.ruleMode;
         if (selectedMode == ReservedStockMatchMode.IGNORE_SECONDARY
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
         ReservedStockMatchMode selectedMode = this.global ? this.globalMode : this.ruleMode;
         if (selectedMode == ReservedStockMatchMode.IGNORE_SECONDARY
            && y >= this.f_97736_ + 134
            && y < this.f_97736_ + 134 + 51
            && row >= 0
            && row < 3
            && index >= 0
            && index < this.variants.size()
            && x >= this.f_97735_ + 9
            && x < this.f_97735_ + 187) {
            MaintenanceEditorData.VariantEntry variant = this.variants.get(index);
            ArrayList<Component> lines = new ArrayList<>(AEKeyRendering.getTooltip(variant.key()));
            lines.add(
               Component.m_237110_(
                     "ae2lt.tianshu.maintenance.tooltip.stock", new Object[]{TianshuMaintenanceRuleScreen.compactAmount(variant.key(), variant.storedAmount())}
                  )
                  .m_130940_(ChatFormatting.GRAY)
            );
            if (variant.craftable()) {
               lines.add(ButtonToolTips.Craftable.text().m_130940_(ChatFormatting.GREEN));
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

      private static record EditorState(boolean global, long globalAmount, ReservedStockMatchMode globalMode, long ruleAmount, ReservedStockMatchMode ruleMode) {
         ReservedStockMatchMode selectedMode() {
            return this.global ? this.globalMode : this.ruleMode;
         }
      }
   }
}
