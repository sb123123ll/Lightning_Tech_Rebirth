package com.moakiee.ae2lt.client;

import appeng.api.config.ActionItems;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.slot.AppEngSlot;
import com.moakiee.ae2lt.client.gui.AE2Button;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.logic.tianshu.terminal.SeedRefillSync;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.util.SlotPositionAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class TianshuClosedLoopEncodingPanel implements ICompositeWidget {
   private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(0, 70, 124, 66);
   private static final Blitter ARROW_COVER = Blitter.texture("guis/pattern_modes.png").src(73, 77, 23, 16);
   private static final int ARROW_X = 73;
   private static final int ARROW_Y = 25;
   private static final int PANEL_WIDTH = 124;
   private static final int PANEL_HEIGHT = 66;
   private static final int MEMBER_X = 16;
   private static final int SLOT_Y = 9;
   private static final int OUTPUT_X = 100;
   private static final int CONTROL_X = 70;
   private static final int CONTROL_WIDTH = 26;
   private static final int EXEC_LABEL_Y = 9;
   private static final int STORED_LABEL_Y = 34;
   private static final int STATUS_Y = 58;
   private static final int STATUS_HEIGHT = 7;
   private static final int LABEL_HEIGHT = 7;
   private static final float LABEL_SCALE = 0.6F;
   private static final int VISIBLE_ROWS = 3;
   private final TianshuPatternEncodingTermScreen<?> screen;
   private final TianshuPatternEncodingTermMenu menu;
   private final Scrollbar scrollbar;
   private final AE2Button detailButton;
   private final AE2Button autoFillButton;
   private final ActionButton clearButton;
   private final ActionButton cycleOutputButton;
   private final IconButton refillButton;
   private final AETextField executionField;
   private final AETextField storedField;
   private boolean visible;
   private boolean syncing;
   private int x;
   private int y;

   TianshuClosedLoopEncodingPanel(TianshuPatternEncodingTermScreen<?> screen, WidgetContainer widgets, Runnable openDetail) {
      this.screen = screen;
      this.menu = (TianshuPatternEncodingTermMenu)screen.m_6262_();
      this.scrollbar = widgets.addScrollBar("closedLoopScrollbar", Scrollbar.SMALL);
      this.scrollbar.setRange(0, 6, 3);
      this.scrollbar.setCaptureMouseWheel(false);
      this.detailButton = new AE2Button(Component.m_237115_("ae2lt.tianshu.closed_loop.detail"), btn -> openDetail.run());
      widgets.add("closedLoopDetail", this.detailButton);
      this.detailButton.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.tianshu.closed_loop.detail.tooltip")));
      this.autoFillButton = new AE2Button(Component.m_237115_("ae2lt.tianshu.closed_loop.auto_fill"), btn -> this.menu.autoFillClosedLoop());
      widgets.add("closedLoopAutoFill", this.autoFillButton);
      this.clearButton = new ActionButton(ActionItems.CLOSE, this.menu::clearClosedLoopDraft);
      this.clearButton.setHalfSize(true);
      this.clearButton.setDisableBackground(true);
      widgets.add("closedLoopClear", this.clearButton);
      this.cycleOutputButton = new ActionButton(ActionItems.CYCLE_PROCESSING_OUTPUT, ignored -> this.menu.cycleClosedLoopOutput());
      this.cycleOutputButton.setHalfSize(true);
      this.cycleOutputButton.setDisableBackground(true);
      widgets.add("closedLoopCycleOutput", this.cycleOutputButton);
      this.refillButton = new IconButton(btn -> this.menu.refillClosedLoopSeeds()) {
         protected Icon getIcon() {
            return Icon.ARROW_DOWN;
         }

         public List<Component> getTooltipMessage() {
            return List.of(
               Component.m_237115_("ae2lt.tianshu.closed_loop.refill_seeds"),
               Component.m_237115_("ae2lt.tianshu.closed_loop.refill_seeds.tooltip").m_130940_(ChatFormatting.GRAY)
            );
         }
      };
      this.refillButton.setHalfSize(true);
      this.refillButton.setDisableBackground(true);
      widgets.add("closedLoopSeedRefill", this.refillButton);
      this.executionField = widgets.addTextField("closedLoopExecMultiplier");
      this.storedField = widgets.addTextField("closedLoopStoredMultiplier");

      for (AETextField field : List.of(this.executionField, this.storedField)) {
         field.m_94199_(9);
         field.m_94153_(TianshuClosedLoopEncodingPanel::isPositiveIntDraft);
         field.m_94151_(ignored -> this.submitMultipliers());
      }

      this.updateMultiplierTooltips();
   }

   public void setPosition(Point position) {
      this.x = position.getX();
      this.y = position.getY();
   }

   public void setSize(int width, int height) {
   }

   public Rect2i getBounds() {
      return new Rect2i(this.x, this.y, 124, 66);
   }

   public boolean isVisible() {
      return this.visible;
   }

   void setVisible(boolean visible) {
      this.visible = visible;
      this.scrollbar.setVisible(visible);
      this.detailButton.f_93624_ = visible;
      this.autoFillButton.f_93624_ = visible;
      this.clearButton.setVisibility(visible);
      this.cycleOutputButton.setVisibility(visible && this.menu.canCycleClosedLoopOutputs());
      this.refillButton.setVisibility(visible);
      this.executionField.m_94194_(visible);
      this.storedField.m_94194_(visible);
      this.screen.setSlotsHidden(Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_MEMBER, !visible);
      this.screen.setSlotsHidden(Ae2ltSlotSemantics.TIANSHU_CLOSED_LOOP_OUTPUT_MARK, !visible);
   }

   public void updateBeforeRender() {
      if (this.visible) {
         int scroll = this.scrollbar.getCurrentScroll();
         List<AppEngSlot> memberSlots = this.menu.getClosedLoopMemberSlots();

         for (int i = 0; i < memberSlots.size(); i++) {
            AppEngSlot slot = memberSlots.get(i);
            int effectiveRow = i / 3 - scroll;
            boolean active = effectiveRow >= 0 && effectiveRow < 3;
            slot.setActive(active);
            SlotPositionAccess.set(slot, this.x + 16 + i % 3 * 18, this.y + 9 + effectiveRow * 18);
         }

         List<AppEngSlot> outputSlots = this.menu.getClosedLoopOutputSlots();

         for (int i = 0; i < outputSlots.size(); i++) {
            AppEngSlot slot = outputSlots.get(i);
            int effectiveRow = i - scroll;
            boolean active = effectiveRow >= 0 && effectiveRow < 3;
            slot.setActive(active);
            SlotPositionAccess.set(slot, this.x + 100, this.y + 9 + effectiveRow * 18);
         }

         this.autoFillButton.f_93623_ = this.menu.closedLoopCandidateCount > 0 || this.menu.hasClosedLoopPrimaryOutputMark();
         this.cycleOutputButton.setVisibility(this.menu.canCycleClosedLoopOutputs());
         this.autoFillButton
            .m_257544_(
               Tooltip.m_257550_(
                  Component.m_237110_(
                     "ae2lt.tianshu.closed_loop.auto_fill.tooltip",
                     new Object[]{this.menu.closedLoopCandidateCount == 0 ? 0 : this.menu.closedLoopCandidateIndex + 1, this.menu.closedLoopCandidateCount}
                  )
               )
            );
         this.refillButton.f_93623_ = this.menu.seedRefillAvailable;
         this.syncFields();
         this.updateMultiplierTooltips();
      }
   }

   public void drawBackgroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      int panelX = bounds.m_110085_() + this.x - 1;
      int panelY = bounds.m_110086_() + this.y + 1;
      BG.dest(panelX, panelY).blit(graphics);
      ARROW_COVER.dest(panelX + 73, panelY + 25).blit(graphics);
   }

   public void drawForegroundLayer(GuiGraphics graphics, Rect2i bounds, Point mouse) {
      this.drawScaledLabel(graphics, Component.m_237115_("ae2lt.tianshu.closed_loop.execution_multiplier.short"), 9);
      this.drawScaledLabel(graphics, Component.m_237115_("ae2lt.tianshu.closed_loop.stored_multiplier.short"), 34);
      this.drawStatus(graphics);
   }

   private void drawScaledLabel(GuiGraphics graphics, Component text, int labelY) {
      Font font = Minecraft.m_91087_().f_91062_;
      PoseStack pose = graphics.m_280168_();
      pose.m_85836_();
      pose.m_252880_((float)(this.x + 70), (float)(this.y + labelY), 0.0F);
      pose.m_85841_(0.6F, 0.6F, 1.0F);
      graphics.m_280056_(font, font.m_92834_(text.getString(), 43), 0, 0, 4210752, false);
      pose.m_85849_();
   }

   private void drawStatus(GuiGraphics graphics) {
      Font font = Minecraft.m_91087_().f_91062_;
      String text = font.m_92834_(this.statusText().getString(), 43);
      float textWidth = (float)font.m_92895_(text) * 0.6F;
      PoseStack pose = graphics.m_280168_();
      pose.m_85836_();
      pose.m_252880_((float)(this.x + 70) + (26.0F - textWidth) / 2.0F, (float)(this.y + 58), 0.0F);
      pose.m_85841_(0.6F, 0.6F, 1.0F);
      graphics.m_280056_(font, text, 0, 0, this.statusColor(), false);
      pose.m_85849_();
   }

   public boolean onMouseWheel(Point mousePosition, double delta) {
      return this.scrollbar.onMouseWheel(mousePosition, delta);
   }

   boolean isMouseOverStatus(int mouseX, int mouseY) {
      return this.visible && mouseX >= this.x + 70 && mouseX < this.x + 70 + 26 && mouseY >= this.y + 58 && mouseY < this.y + 58 + 7;
   }

   @Nullable
   List<Component> getMultiplierTooltipAt(int mouseX, int mouseY) {
      if (!this.visible || mouseX < this.x + 70 || mouseX >= this.x + 70 + 26) {
         return null;
      } else if (mouseY >= this.y + 9 && mouseY < this.y + 9 + 7) {
         return this.executionField.getTooltipMessage();
      } else {
         return mouseY >= this.y + 34 && mouseY < this.y + 34 + 7 ? this.storedField.getTooltipMessage() : null;
      }
   }

   List<Component> buildStatusTooltip() {
      ArrayList<Component> lines = new ArrayList<>();
      lines.add(this.statusText());
      if (this.menu.uploadState == 0 && isRefillProblem(this.menu.seedRefillSync.state())) {
         for (SeedRefillSync.Entry entry : this.menu.seedRefillSync.problems()) {
            if (entry.networkMissing() > 0L) {
               lines.add(
                  Component.m_237110_(
                        "ae2lt.tianshu.closed_loop.refill.network_missing_entry", new Object[]{entry.what().getDisplayName(), entry.networkMissing()}
                     )
                     .m_130940_(ChatFormatting.GOLD)
               );
            }

            if (entry.storageBlocked() > 0L) {
               lines.add(
                  Component.m_237110_(
                        "ae2lt.tianshu.closed_loop.refill.storage_blocked_entry", new Object[]{entry.what().getDisplayName(), entry.storageBlocked()}
                     )
                     .m_130940_(ChatFormatting.RED)
               );
            }
         }

         if (this.menu.seedRefillSync.state() == 2 || this.menu.seedRefillSync.state() == 4) {
            lines.add(Component.m_237115_("ae2lt.tianshu.closed_loop.refill.network_missing_hint").m_130940_(ChatFormatting.GRAY));
         }

         if (this.menu.seedRefillSync.state() == 3 || this.menu.seedRefillSync.state() == 4) {
            lines.add(Component.m_237115_("ae2lt.tianshu.closed_loop.refill.storage_blocked_hint").m_130940_(ChatFormatting.GRAY));
         }
      }

      lines.add(
         Component.m_237110_(
               "ae2lt.tianshu.terminal.closed_loop.candidate",
               new Object[]{this.menu.closedLoopCandidateCount == 0 ? 0 : this.menu.closedLoopCandidateIndex + 1, this.menu.closedLoopCandidateCount}
            )
            .m_130940_(ChatFormatting.GRAY)
      );
      return lines;
   }

   private Component statusText() {
      if (this.menu.uploadState != 0) {
         return Component.m_237115_(this.menu.uploadState == 1 ? "ae2lt.tianshu.terminal.upload.success" : "ae2lt.tianshu.terminal.upload.failed");
      } else {
         return switch (this.menu.seedRefillSync.state()) {
            case 1 -> Component.m_237115_("ae2lt.tianshu.closed_loop.refill.status.complete");
            case 2 -> Component.m_237115_("ae2lt.tianshu.closed_loop.refill.status.network_missing");
            case 3 -> Component.m_237115_("ae2lt.tianshu.closed_loop.refill.status.storage_blocked");
            case 4 -> Component.m_237115_("ae2lt.tianshu.closed_loop.refill.status.mixed");
            case 5 -> Component.m_237115_("ae2lt.tianshu.closed_loop.refill.status.unavailable");
            default -> Component.m_237115_("ae2lt.tianshu.closed_loop.status." + this.menu.closedLoopDraftStatus.name().toLowerCase(Locale.ROOT));
         };
      }
   }

   private int statusColor() {
      if (this.menu.uploadState != 0) {
         return this.menu.uploadState == 1 ? 2263074 : 11149858;
      } else {
         return switch (this.menu.seedRefillSync.state()) {
            case 1 -> 2263074;
            case 2 -> 11171584;
            case 3, 4 -> 11149858;
            case 5 -> 11149858;
            default -> {
               switch (this.menu.closedLoopDraftStatus) {
                  case VALID:
                  case ENCODED:
                     yield 2263074;
                     break;
                  case EMPTY:
                  case NO_CANDIDATE:
                     yield 6710886;
                     break;
                  case MISSING_PRIMARY_OUTPUT:
                     yield 11171584;
                     break;
                  default:
                     yield 11149858;
               }
            }
         };
      }
   }

   private static boolean isRefillProblem(int state) {
      return state == 2 || state == 3 || state == 4;
   }

   private void syncFields() {
      this.syncing = true;

      try {
         if (!this.executionField.m_93696_()) {
            String value = Integer.toString(this.menu.closedLoopExecutionSeedMultiplier);
            if (!this.executionField.m_94155_().equals(value)) {
               this.executionField.m_94144_(value);
            }
         }

         if (!this.storedField.m_93696_()) {
            String value = Integer.toString(this.menu.closedLoopStoredTaskMultiplier);
            if (!this.storedField.m_94155_().equals(value)) {
               this.storedField.m_94144_(value);
            }
         }
      } finally {
         this.syncing = false;
      }
   }

   private void submitMultipliers() {
      if (!this.syncing && !this.executionField.m_94155_().isEmpty() && !this.storedField.m_94155_().isEmpty()) {
         try {
            int execution = Integer.parseInt(this.executionField.m_94155_());
            int stored = Integer.parseInt(this.storedField.m_94155_());
            if (execution >= 1 && stored >= 1) {
               this.menu.setClosedLoopMultipliers(execution, stored);
            }
         } catch (NumberFormatException var3) {
         }
      }
   }

   private void updateMultiplierTooltips() {
      this.executionField
         .setTooltipMessage(
            multiplierTooltip(
               this.executionField, "ae2lt.tianshu.closed_loop.execution_multiplier", "ae2lt.tianshu.terminal.closed_loop.execution_seed_multiplier.tooltip"
            )
         );
      this.storedField
         .setTooltipMessage(
            multiplierTooltip(
               this.storedField, "ae2lt.tianshu.closed_loop.stored_multiplier", "ae2lt.tianshu.terminal.closed_loop.stored_task_multiplier.tooltip"
            )
         );
   }

   private static List<Component> multiplierTooltip(AETextField field, String titleKey, String descriptionKey) {
      return List.of(
         Component.m_237115_(titleKey),
         Component.m_237110_("ae2lt.tianshu.terminal.closed_loop.multiplier.current_value", new Object[]{field.m_94155_()}).m_130940_(ChatFormatting.AQUA),
         Component.m_237115_(descriptionKey).m_130940_(ChatFormatting.GRAY)
      );
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

   static void appendMemberTooltip(List<Component> lines, TianshuPatternEncodingTermMenu menu, int index, ItemStack stack, Level level) {
      boolean var10000;
      label64: {
         lines.add(Component.m_237110_("ae2lt.tianshu.closed_loop.tooltip.member", new Object[]{index + 1}).m_130940_(ChatFormatting.GRAY));
         lines.add(
            Component.m_237110_("ae2lt.tianshu.closed_loop.tooltip.copies", new Object[]{menu.closedLoopDraftSync.copies(index)})
               .m_130940_(ChatFormatting.GRAY)
         );
         if (stack.m_41720_() instanceof ClosedLoopPatternItem item && item.hasPayload(stack) && item.readExecutionMember(stack) < 0) {
            var10000 = true;
            break label64;
         }

         var10000 = false;
      }

      boolean macro = var10000;
      lines.add(
         Component.m_237110_("ae2lt.tianshu.closed_loop.tooltip.macro", new Object[]{Component.m_237115_(macro ? "gui.yes" : "gui.no")})
            .m_130940_(ChatFormatting.GRAY)
      );
      if (level != null) {
         IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
         if (details != null) {
            lines.add(Component.m_237115_("ae2lt.tianshu.closed_loop.tooltip.inputs").m_130940_(ChatFormatting.DARK_GRAY));

            for (IInput input : details.getInputs()) {
               GenericStack[] possible = input.getPossibleInputs();
               if (possible.length != 0 && possible[0] != null && possible[0].what() != null) {
                  long amount;
                  try {
                     amount = Math.multiplyExact(possible[0].amount(), input.getMultiplier());
                  } catch (ArithmeticException var15) {
                     amount = Long.MAX_VALUE;
                  }

                  lines.add(
                     Component.m_237110_("ae2lt.tianshu.closed_loop.tooltip.stack", new Object[]{possible[0].what().getDisplayName(), amount})
                        .m_130940_(ChatFormatting.GRAY)
                  );
               }
            }

            lines.add(Component.m_237115_("ae2lt.tianshu.closed_loop.tooltip.outputs").m_130940_(ChatFormatting.DARK_GRAY));

            for (GenericStack output : details.getOutputs()) {
               if (output != null && output.what() != null) {
                  lines.add(
                     Component.m_237110_("ae2lt.tianshu.closed_loop.tooltip.stack", new Object[]{output.what().getDisplayName(), output.amount()})
                        .m_130940_(ChatFormatting.GRAY)
                  );
               }
            }
         }
      }
   }
}
