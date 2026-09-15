package com.moakiee.ae2lt.client;

import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.client.gui.AE2Button;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopDraftStatus;
import com.moakiee.ae2lt.logic.tianshu.terminal.ClosedLoopResultPage;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class TianshuClosedLoopPatternConfigScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuPatternEncodingTermScreen<M>> {
   private static final int SLOT_X = 13;
   private static final int ROW_SLOT_Y_OFFSET = 4;
   private static final int HIDDEN_SLOT = -10000;
   private static final int NAME_X = 34;
   private static final int MEMBER_NAME_WIDTH = 45;
   private static final int RESULT_NAME_WIDTH = 130;
   private static final int MEMBER_AMOUNT_X = 96;
   private static final int MEMBER_AMOUNT_WIDTH = 36;
   private static final int MEMBER_AMOUNT_Y_OFFSET = 6;
   private static final int NUMBER_FIELD_HEIGHT = 12;
   private static final int MEMBER_UP_X = 134;
   private static final int MEMBER_DOWN_X = 150;
   private static final int ROLE_X = 111;
   private static final int ROLE_WIDTH = 54;
   private static final int FOOTER_TEXT_WIDTH = 168;
   private static final int SETTINGS_FIELD_X = 96;
   private static final int SETTINGS_FIELD_WIDTH = 69;
   private static final int SETTINGS_EXECUTION_ROW_Y = 64;
   private static final int SETTINGS_STORED_ROW_Y = 89;
   private final Scrollbar scrollbar;
   private final List<Button> pageButtons;
   private final AETextField[] memberAmounts = new AETextField[5];
   private final int[] memberRows = new int[5];
   private final TianshuClosedLoopPatternConfigScreen.ArrowButton[] memberUp = new TianshuClosedLoopPatternConfigScreen.ArrowButton[5];
   private final TianshuClosedLoopPatternConfigScreen.ArrowButton[] memberDown = new TianshuClosedLoopPatternConfigScreen.ArrowButton[5];
   private final Button[] outputRoles = new Button[5];
   private TianshuClosedLoopPatternConfigScreen.Page page = TianshuClosedLoopPatternConfigScreen.Page.MEMBERS;
   private AETextField executionMultiplier;
   private AETextField storedMultiplier;
   private TianshuClosedLoopPatternConfigScreen.ArrowButton previousCandidate;
   private TianshuClosedLoopPatternConfigScreen.ArrowButton nextCandidate;
   private boolean syncingFields;
   @Nullable
   private ClosedLoopResultPage.Kind requestedResultKind;
   private int requestedResultRevision = Integer.MIN_VALUE;
   private int requestedResultOffset = -1;

   TianshuClosedLoopPatternConfigScreen(TianshuPatternEncodingTermScreen<M> parent) {
      super(parent, "/screens/tianshu_closed_loop_pattern_config.json");
      this.f_97726_ = 190;
      this.f_97727_ = 187;
      this.widgets.add("button_back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.closeEditor()));
      this.pageButtons = List.of(
         this.widgets
            .addButton(
               "page_members",
               pageLabel(TianshuClosedLoopPatternConfigScreen.Page.MEMBERS),
               () -> this.setPage(TianshuClosedLoopPatternConfigScreen.Page.MEMBERS)
            ),
         this.widgets
            .addButton(
               "page_outputs",
               pageLabel(TianshuClosedLoopPatternConfigScreen.Page.OUTPUTS),
               () -> this.setPage(TianshuClosedLoopPatternConfigScreen.Page.OUTPUTS)
            ),
         this.widgets
            .addButton(
               "page_inputs",
               pageLabel(TianshuClosedLoopPatternConfigScreen.Page.EXTERNAL_INPUTS),
               () -> this.setPage(TianshuClosedLoopPatternConfigScreen.Page.EXTERNAL_INPUTS)
            ),
         this.widgets
            .addButton(
               "page_seeds", pageLabel(TianshuClosedLoopPatternConfigScreen.Page.SEEDS), () -> this.setPage(TianshuClosedLoopPatternConfigScreen.Page.SEEDS)
            ),
         this.widgets
            .addButton(
               "page_settings",
               pageLabel(TianshuClosedLoopPatternConfigScreen.Page.SETTINGS),
               () -> this.setPage(TianshuClosedLoopPatternConfigScreen.Page.SETTINGS)
            )
      );
      this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
   }

   protected void m_7856_() {
      super.m_7856_();
      this.scrollbar.setHeight(129);

      for (int row = 0; row < 5; row++) {
         int visibleRow = row;
         int rowY = 32 + row * 25;
         AETextField amount = this.numberField(96, rowY + 6, 36);
         amount.m_94199_(19);
         amount.m_94153_(TianshuClosedLoopPatternConfigScreen::isPositiveLongDraft);
         amount.m_94151_(value -> this.submitMemberAmount(visibleRow, value));
         this.memberAmounts[row] = amount;
         this.memberUp[row] = (TianshuClosedLoopPatternConfigScreen.ArrowButton)this.m_142416_(
            new TianshuClosedLoopPatternConfigScreen.ArrowButton(
               Icon.ARROW_UP, Component.m_237115_("ae2lt.tianshu.closed_loop.move_up"), ignored -> this.moveMember(visibleRow, -1)
            )
         );
         this.memberUp[row].m_264152_(this.f_97735_ + 134, this.f_97736_ + rowY + 4);
         this.memberDown[row] = (TianshuClosedLoopPatternConfigScreen.ArrowButton)this.m_142416_(
            new TianshuClosedLoopPatternConfigScreen.ArrowButton(
               Icon.ARROW_DOWN, Component.m_237115_("ae2lt.tianshu.closed_loop.move_down"), ignored -> this.moveMember(visibleRow, 1)
            )
         );
         this.memberDown[row].m_264152_(this.f_97735_ + 150, this.f_97736_ + rowY + 4);
         AE2Button role = new AE2Button(this.f_97735_ + 111, this.f_97736_ + rowY + 4, 54, 16, Component.m_237119_(), ignored -> {
         });
         this.outputRoles[row] = (Button)this.m_142416_(role);
      }

      this.executionMultiplier = this.numberBox(96, 64, ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopExecutionSeedMultiplier);
      this.storedMultiplier = this.numberBox(96, 89, ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopStoredTaskMultiplier);
      this.executionMultiplier.m_94151_(ignored -> this.submitMultipliers());
      this.storedMultiplier.m_94151_(ignored -> this.submitMultipliers());
      this.executionMultiplier.setTooltipMessage(List.of(Component.m_237115_("ae2lt.tianshu.terminal.closed_loop.execution_seed_multiplier.tooltip")));
      this.storedMultiplier.setTooltipMessage(List.of(Component.m_237115_("ae2lt.tianshu.terminal.closed_loop.stored_task_multiplier.tooltip")));
      this.previousCandidate = (TianshuClosedLoopPatternConfigScreen.ArrowButton)this.m_142416_(
         new TianshuClosedLoopPatternConfigScreen.ArrowButton(
            Icon.ARROW_LEFT,
            Component.m_237115_("ae2lt.tianshu.closed_loop.previous_candidate"),
            ignored -> ((TianshuPatternEncodingTermMenu)this.f_97732_).selectClosedLoopCandidate(-1)
         )
      );
      this.previousCandidate.m_264152_(this.f_97735_ + 116, this.f_97736_ + 32 + 4);
      this.nextCandidate = (TianshuClosedLoopPatternConfigScreen.ArrowButton)this.m_142416_(
         new TianshuClosedLoopPatternConfigScreen.ArrowButton(
            Icon.ARROW_RIGHT,
            Component.m_237115_("ae2lt.tianshu.closed_loop.next_candidate"),
            ignored -> ((TianshuPatternEncodingTermMenu)this.f_97732_).selectClosedLoopCandidate(1)
         )
      );
      this.nextCandidate.m_264152_(this.f_97735_ + 145, this.f_97736_ + 32 + 4);
      this.hideParentSlots();
      this.configurePage();
   }

   private AETextField numberBox(int x, int y, int value) {
      AETextField field = this.numberField(x, y, 69);
      field.m_94199_(9);
      field.m_94153_(TianshuClosedLoopPatternConfigScreen::isPositiveIntDraft);
      this.syncingFields = true;
      field.m_94144_(Integer.toString(value));
      this.syncingFields = false;
      return field;
   }

   private AETextField numberField(int x, int y, int width) {
      AETextField field = new AETextField(this.style, this.f_96547_, this.f_97735_ + x, this.f_97736_ + y, width, 12);
      field.m_94182_(false);
      return (AETextField)this.m_142416_(field);
   }

   private void setPage(TianshuClosedLoopPatternConfigScreen.Page newPage) {
      if (this.page != newPage) {
         this.page = newPage;
         this.scrollbar.setCurrentScroll(0);
         this.configurePage();
      }
   }

   private void configurePage() {
      int rows = switch (this.page) {
         case MEMBERS -> 27;
         case OUTPUTS -> 9;
         case EXTERNAL_INPUTS -> Math.min(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopExternalInputCount, 243);
         case SEEDS -> Math.min(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopSeedInputCount, 243);
         case SETTINGS -> 0;
      };
      this.scrollbar.setRange(0, Math.max(0, rows - 5), 1);
      this.scrollbar.setVisible(this.page != TianshuClosedLoopPatternConfigScreen.Page.SETTINGS && rows > 5);

      for (int i = 0; i < this.pageButtons.size(); i++) {
         TianshuClosedLoopPatternConfigScreen.Page value = TianshuClosedLoopPatternConfigScreen.Page.values()[i];
         this.pageButtons.get(i).m_93666_(pageLabel(value).m_6881_().m_130940_(value == this.page ? ChatFormatting.GREEN : ChatFormatting.WHITE));
      }

      this.updateVisibleSlots();
      this.updateControls();
      this.requestVisibleResultPage();
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.updateVisibleSlots();
      this.updateControls();
      this.requestVisibleResultPage();
   }

   public void m_181908_() {
      super.m_181908_();
      this.syncFields();
   }

   private void updateVisibleSlots() {
      hideSlots(((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots());
      hideSlots(((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopOutputSlots());
      this.setSlotsHidden(Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_MEMBER, this.page != TianshuClosedLoopPatternConfigScreen.Page.MEMBERS);
      this.setSlotsHidden(Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_OUTPUT_MARK, this.page != TianshuClosedLoopPatternConfigScreen.Page.OUTPUTS);

      List<AppEngSlot> slots = switch (this.page) {
         case MEMBERS -> ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots();
         case OUTPUTS -> ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopOutputSlots();
         case EXTERNAL_INPUTS, SEEDS, SETTINGS -> List.of();
      };
      int scroll = this.scrollbar.getCurrentScroll();

      for (int visible = 0; visible < 5; visible++) {
         int index = scroll + visible;
         if (index >= slots.size()) {
            break;
         }

         AppEngSlot slot = slots.get(index);
         SlotPositionAccess.set(slot, 13, 32 + visible * 25 + 4);
         slot.setActive(true);
      }
   }

   private void updateControls() {
      int scroll = this.scrollbar.getCurrentScroll();

      for (int visible = 0; visible < 5; visible++) {
         int memberIndex = scroll + visible;
         this.memberRows[visible] = memberIndex;
         boolean memberPage = this.page == TianshuClosedLoopPatternConfigScreen.Page.MEMBERS && memberIndex < 27;
         AppEngSlot memberSlot = memberPage ? ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots().get(memberIndex) : null;
         boolean memberPresent = memberSlot != null && !memberSlot.m_7993_().m_41619_();
         this.memberAmounts[visible].m_94194_(memberPage);
         this.memberAmounts[visible].f_93623_ = memberPresent;
         this.memberAmounts[visible].m_94186_(memberPresent);
         this.memberUp[visible].f_93624_ = memberPage;
         this.memberDown[visible].f_93624_ = memberPage;
         this.memberUp[visible].f_93623_ = memberPresent && memberIndex > 0;
         this.memberDown[visible].f_93623_ = memberPresent && memberIndex + 1 < 27;
         int outputIndex = scroll + visible;
         boolean outputPage = this.page == TianshuClosedLoopPatternConfigScreen.Page.OUTPUTS && outputIndex < 9;
         boolean outputPresent = outputPage
            && !((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopOutputSlots().get(outputIndex).m_7993_().m_41619_();
         this.outputRoles[visible].f_93624_ = outputPresent;
         this.outputRoles[visible].f_93623_ = false;
         if (outputPage) {
            this.outputRoles[visible].m_93666_(roleLabel(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopDraftSync.outputRole(outputIndex)));
         }
      }

      boolean settings = this.page == TianshuClosedLoopPatternConfigScreen.Page.SETTINGS;
      this.executionMultiplier.m_94194_(settings);
      this.storedMultiplier.m_94194_(settings);
      this.previousCandidate.f_93624_ = settings;
      this.nextCandidate.f_93624_ = settings;
      this.previousCandidate.f_93623_ = ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopCandidateCount > 1;
      this.nextCandidate.f_93623_ = ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopCandidateCount > 1;
      this.syncFields();
   }

   private void syncFields() {
      this.syncingFields = true;

      try {
         if (this.page == TianshuClosedLoopPatternConfigScreen.Page.MEMBERS) {
            for (int visible = 0; visible < this.memberAmounts.length; visible++) {
               AETextField field = this.memberAmounts[visible];
               int index = this.memberRows[visible];
               if (!field.m_93696_() && index >= 0 && index < 27) {
                  boolean present = !((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots().get(index).m_7993_().m_41619_();
                  String value = present ? Long.toString(Math.max(1L, ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopDraftSync.copies(index))) : "";
                  if (!field.m_94155_().equals(value)) {
                     field.m_94144_(value);
                  }
               }
            }
         }

         if (this.executionMultiplier != null && !this.executionMultiplier.m_93696_()) {
            String value = Integer.toString(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopExecutionSeedMultiplier);
            if (!this.executionMultiplier.m_94155_().equals(value)) {
               this.executionMultiplier.m_94144_(value);
            }
         }

         if (this.storedMultiplier != null && !this.storedMultiplier.m_93696_()) {
            String value = Integer.toString(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopStoredTaskMultiplier);
            if (!this.storedMultiplier.m_94155_().equals(value)) {
               this.storedMultiplier.m_94144_(value);
            }
         }
      } finally {
         this.syncingFields = false;
      }
   }

   private void submitMemberAmount(int visibleRow, String value) {
      if (!this.syncingFields && this.page == TianshuClosedLoopPatternConfigScreen.Page.MEMBERS && !value.isEmpty()) {
         try {
            long parsed = Long.parseLong(value);
            if (parsed >= 1L) {
               ((TianshuPatternEncodingTermMenu)this.f_97732_).setClosedLoopMemberCopies(this.memberRows[visibleRow], parsed);
            }
         } catch (NumberFormatException var5) {
         }
      }
   }

   private void moveMember(int visibleRow, int direction) {
      if (this.page == TianshuClosedLoopPatternConfigScreen.Page.MEMBERS) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_).moveClosedLoopMember(this.memberRows[visibleRow], direction);
      }
   }

   private void submitMultipliers() {
      if (!this.syncingFields
         && this.executionMultiplier != null
         && this.storedMultiplier != null
         && !this.executionMultiplier.m_94155_().isEmpty()
         && !this.storedMultiplier.m_94155_().isEmpty()) {
         try {
            int execution = Integer.parseInt(this.executionMultiplier.m_94155_());
            int stored = Integer.parseInt(this.storedMultiplier.m_94155_());
            if (execution >= 1 && stored >= 1) {
               ((TianshuPatternEncodingTermMenu)this.f_97732_).setClosedLoopMultipliers(execution, stored);
            }
         } catch (NumberFormatException var3) {
         }
      }
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      switch (this.page) {
         case MEMBERS:
            this.drawMemberRows(graphics);
            break;
         case OUTPUTS:
            this.drawOutputRows(graphics, ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopOutputSlots(), 73);
            break;
         case EXTERNAL_INPUTS:
            this.drawResultRows(graphics, ClosedLoopResultPage.Kind.EXTERNAL_INPUTS);
            break;
         case SEEDS:
            this.drawResultRows(graphics, ClosedLoopResultPage.Kind.SEEDS);
            break;
         case SETTINGS:
            this.drawSettings(graphics);
      }

      MutableComponent status = Component.m_237115_(
         "ae2lt.tianshu.closed_loop.status." + ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopDraftStatus.name().toLowerCase(Locale.ROOT)
      );
      int color = statusColor(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopDraftStatus);
      graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(status.getString(), 168), 8, 161, color, false);
      if (this.page != TianshuClosedLoopPatternConfigScreen.Page.SETTINGS) {
         int rows = this.currentRowCount();
         graphics.m_280614_(
            this.f_96547_,
            Component.m_237110_("ae2lt.tianshu.closed_loop.page_count", new Object[]{rows == 0 ? 0 : this.scrollbar.getCurrentScroll() + 1, rows}),
            8,
            173,
            6710886,
            false
         );
      }
   }

   private void drawMemberRows(GuiGraphics graphics) {
      int scroll = this.scrollbar.getCurrentScroll();

      for (int visible = 0; visible < 5; visible++) {
         int index = scroll + visible;
         if (index >= ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots().size()) {
            break;
         }

         int y = 32 + visible * 25 + 8;
         ItemStack stack = ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots().get(index).m_7993_();
         graphics.m_280056_(this.f_96547_, Integer.toString(index + 1), 34, y, 7829367, false);
         if (!stack.m_41619_()) {
            graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(stack.m_41786_().getString(), 45), 47, y, 4210752, false);
         }
      }
   }

   private void drawOutputRows(GuiGraphics graphics, List<AppEngSlot> slots, int width) {
      int scroll = this.scrollbar.getCurrentScroll();

      for (int visible = 0; visible < 5; visible++) {
         int index = scroll + visible;
         if (index >= slots.size()) {
            break;
         }

         ItemStack stack = slots.get(index).m_7993_();
         if (!stack.m_41619_()) {
            int y = 32 + visible * 25 + 8;
            GenericStack generic = GenericStack.fromItemStack(stack);
            String name = generic != null && generic.what() != null ? generic.what().getDisplayName().getString() : stack.m_41786_().getString();
            graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(name, width), 34, y, 4210752, false);
         }
      }
   }

   private void drawResultRows(GuiGraphics graphics, ClosedLoopResultPage.Kind kind) {
      ClosedLoopResultPage resultPage = ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopResultPage(kind, this.scrollbar.getCurrentScroll());
      if (resultPage != null) {
         for (int visible = 0; visible < resultPage.entries().size(); visible++) {
            GenericStack entry = resultPage.entries().get(visible);
            int rowY = 32 + visible * 25;
            graphics.m_280480_(GenericStack.wrapInItemStack(entry), 13, rowY + 4);
            graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(entry.what().getDisplayName().getString(), 130), 34, rowY + 8, 4210752, false);
            PoseStack poseStack = graphics.m_280168_();
            poseStack.m_85836_();
            poseStack.m_252880_(0.0F, 0.0F, 100.0F);
            StackSizeRenderer.renderSizeLabel(
               graphics, this.f_96547_, 13.0F, (float)(rowY + 4), entry.what().formatAmount(entry.amount(), AmountFormat.SLOT), false
            );
            poseStack.m_85849_();
         }
      }
   }

   private void requestVisibleResultPage() {
      ClosedLoopResultPage.Kind kind = switch (this.page) {
         case EXTERNAL_INPUTS -> ClosedLoopResultPage.Kind.EXTERNAL_INPUTS;
         case SEEDS -> ClosedLoopResultPage.Kind.SEEDS;
         default -> null;
      };
      if (kind != null) {
         int offset = this.scrollbar.getCurrentScroll();
         if (((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopResultPage(kind, offset) == null) {
            if (this.requestedResultKind != kind
               || this.requestedResultRevision != ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopResultRevision
               || this.requestedResultOffset != offset) {
               this.requestedResultKind = kind;
               this.requestedResultRevision = ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopResultRevision;
               this.requestedResultOffset = offset;
               ((TianshuPatternEncodingTermMenu)this.f_97732_).requestClosedLoopResultPage(kind, offset);
            }
         }
      }
   }

   private void drawSettings(GuiGraphics graphics) {
      int top = 32;
      graphics.m_280614_(
         this.f_96547_,
         Component.m_237110_(
            "ae2lt.tianshu.closed_loop.candidate",
            new Object[]{
               ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopCandidateCount == 0
                  ? 0
                  : ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopCandidateIndex + 1,
               ((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopCandidateCount
            }
         ),
         12,
         top + 8,
         4210752,
         false
      );
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.closed_loop.execution_multiplier"), 12, 66, 4210752, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.closed_loop.stored_multiplier"), 12, 91, 4210752, false);
   }

   public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
      TianshuPatternConfigLayout.drawBackground(graphics, offsetX, offsetY);
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (delta != 0.0 && this.page != TianshuClosedLoopPatternConfigScreen.Page.SETTINGS) {
         this.scrollbar.setCurrentScroll(this.scrollbar.getCurrentScroll() + (delta > 0.0 ? -1 : 1));
         this.updateVisibleSlots();
         this.updateControls();
         this.requestVisibleResultPage();
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.closeEditor();
         return true;
      } else {
         return super.m_7933_(keyCode, scanCode, modifiers);
      }
   }

   public void m_7379_() {
      this.closeEditor();
   }

   private void closeEditor() {
      hideSlots(((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots());
      hideSlots(((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopOutputSlots());
      this.returnToParent();
   }

   protected void m_280072_(GuiGraphics graphics, int x, int y) {
      ClosedLoopResultPage.Kind kind = switch (this.page) {
         case EXTERNAL_INPUTS -> ClosedLoopResultPage.Kind.EXTERNAL_INPUTS;
         case SEEDS -> ClosedLoopResultPage.Kind.SEEDS;
         default -> null;
      };
      int localX = x - this.f_97735_;
      int localY = y - this.f_97736_;
      int resultTop = 36;
      int visible = (localY - resultTop) / 25;
      if (kind != null && localX >= 13 && localX < 29 && localY >= resultTop && visible >= 0 && visible < 5) {
         ClosedLoopResultPage resultPage = ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopResultPage(kind, this.scrollbar.getCurrentScroll());
         if (resultPage != null && visible < resultPage.entries().size() && localY < resultTop + visible * 25 + 16) {
            graphics.m_280153_(this.f_96547_, GenericStack.wrapInItemStack(resultPage.entries().get(visible)), x, y);
            return;
         }
      }

      super.m_280072_(graphics, x, y);
   }

   protected List<Component> m_280553_(ItemStack stack) {
      ArrayList<Component> lines = new ArrayList<>(super.m_280553_(stack));
      int index = this.f_97734_ == null ? -1 : ((TianshuPatternEncodingTermMenu)this.f_97732_).getClosedLoopMemberSlots().indexOf(this.f_97734_);
      if (index >= 0 && !stack.m_41619_()) {
         TianshuClosedLoopEncodingPanel.appendMemberTooltip(lines, (TianshuPatternEncodingTermMenu)this.f_97732_, index, stack, this.f_96541_.f_91073_);
         return lines;
      } else {
         return lines;
      }
   }

   private void hideParentSlots() {
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
         SlotSemantics.PLAYER_HOTBAR,
         SlotSemantics.TOOLBOX
      )) {
         this.setSlotsHidden(semantic, true);
      }
   }

   private static void hideSlots(List<? extends AppEngSlot> slots) {
      for (AppEngSlot slot : slots) {
         slot.setActive(false);
         SlotPositionAccess.set(slot, -10000, -10000);
      }
   }

   private int currentRowCount() {
      return switch (this.page) {
         case MEMBERS -> 27;
         case OUTPUTS -> 9;
         case EXTERNAL_INPUTS -> Math.min(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopExternalInputCount, 243);
         case SEEDS -> Math.min(((TianshuPatternEncodingTermMenu)this.f_97732_).closedLoopSeedInputCount, 243);
         case SETTINGS -> 1;
      };
   }

   private static boolean isPositiveLongDraft(String value) {
      if (value.isEmpty()) {
         return true;
      } else {
         try {
            return Long.parseLong(value) >= 1L;
         } catch (NumberFormatException var2) {
            return false;
         }
      }
   }

   private static boolean isPositiveIntDraft(String value) {
      if (value.isEmpty()) {
         return true;
      } else {
         try {
            return Integer.parseInt(value) >= 1;
         } catch (NumberFormatException var2) {
            return false;
         }
      }
   }

   private static Component pageLabel(TianshuClosedLoopPatternConfigScreen.Page page) {
      return Component.m_237115_("ae2lt.tianshu.closed_loop.page." + page.name().toLowerCase(Locale.ROOT));
   }

   private static Component roleLabel(int role) {
      return switch (role) {
         case 1 -> Component.m_237115_("ae2lt.tianshu.closed_loop.role.primary").m_130940_(ChatFormatting.GREEN);
         case 2 -> Component.m_237115_("ae2lt.tianshu.closed_loop.role.secondary");
         default -> Component.m_237115_("ae2lt.tianshu.closed_loop.role.none").m_130940_(ChatFormatting.GRAY);
      };
   }

   private static int statusColor(ClosedLoopDraftStatus status) {
      return switch (status) {
         case VALID, ENCODED -> 2263074;
         case EMPTY, NO_CANDIDATE -> 6710886;
         case MISSING_PRIMARY_OUTPUT -> 11171584;
         default -> 11154227;
      };
   }

   private static final class ArrowButton extends IconButton {
      private final Icon icon;
      private final Component tooltip;

      private ArrowButton(Icon icon, Component tooltip, OnPress onPress) {
         super(onPress);
         this.icon = icon;
         this.tooltip = tooltip;
      }

      protected Icon getIcon() {
         return this.icon;
      }

      public List<Component> getTooltipMessage() {
         return List.of(this.tooltip);
      }
   }

   private static enum Page {
      MEMBERS,
      OUTPUTS,
      EXTERNAL_INPUTS,
      SEEDS,
      SETTINGS;
   }
}
