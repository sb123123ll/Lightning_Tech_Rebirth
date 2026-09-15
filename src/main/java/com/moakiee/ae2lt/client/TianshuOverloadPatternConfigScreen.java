package com.moakiee.ae2lt.client;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import com.moakiee.ae2lt.client.gui.AE2Button;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternEncodingType;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

final class TianshuOverloadPatternConfigScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuPatternEncodingTermScreen<M>> {
   private static final int ROW_ICON_Y_OFFSET = 4;
   private static final int ROW_TEXT_Y_OFFSET = 8;
   private static final int TOGGLE_LEFT = 120;
   private static final int TOGGLE_Y_OFFSET = 4;
   private static final int TOGGLE_WIDTH = 46;
   private static final int TOGGLE_HEIGHT = 16;
   private final Scrollbar scrollbar;
   private final List<TianshuOverloadPatternConfigScreen.Row> rows = new ArrayList<>();
   private final AE2Button[] toggleButtons = new AE2Button[5];

   TianshuOverloadPatternConfigScreen(TianshuPatternEncodingTermScreen<M> parent) {
      super(parent, "/screens/tianshu_overload_pattern_config.json");
      this.f_97726_ = 190;
      this.f_97727_ = 187;
      this.widgets.add("button_back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
      this.widgets.addButton("save", Component.m_237115_("ae2lt.tianshu.pattern_config.done"), this::confirm);
      this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
      this.collectRows();
   }

   private void collectRows() {
      ProcessingPatternEncodingType.OverloadConfig config = ((TianshuPatternEncodingTermMenu)this.f_97732_).getOverloadEncodingConfig();
      ArrayList<TianshuOverloadPatternConfigScreen.Row> inputRows = new ArrayList<>();
      FakeSlot[] inputs = ((TianshuPatternEncodingTermMenu)this.f_97732_).getProcessingInputSlots();

      for (int i = 0; i < inputs.length; i++) {
         GenericStack stack = GenericStack.fromItemStack(inputs[i].m_7993_());
         if (stack != null && stack.what() != null) {
            inputRows.add(TianshuOverloadPatternConfigScreen.Row.slot(false, i, stack, config != null && config.isInputIdOnly(i)));
         }
      }

      ArrayList<TianshuOverloadPatternConfigScreen.Row> outputRows = new ArrayList<>();
      FakeSlot[] outputs = ((TianshuPatternEncodingTermMenu)this.f_97732_).getProcessingOutputSlots();

      for (int ix = 0; ix < outputs.length; ix++) {
         GenericStack stack = GenericStack.fromItemStack(outputs[ix].m_7993_());
         if (stack != null && stack.what() != null) {
            outputRows.add(TianshuOverloadPatternConfigScreen.Row.slot(true, ix, stack, config != null && config.isOutputIdOnly(ix)));
         }
      }

      if (!inputRows.isEmpty()) {
         this.rows.add(TianshuOverloadPatternConfigScreen.Row.header(Component.m_237115_("ae2lt.tianshu.overload_config.inputs")));
         this.rows.addAll(inputRows);
      }

      if (!outputRows.isEmpty()) {
         this.rows.add(TianshuOverloadPatternConfigScreen.Row.header(Component.m_237115_("ae2lt.tianshu.overload_config.outputs")));
         this.rows.addAll(outputRows);
      }
   }

   protected void m_7856_() {
      super.m_7856_();
      Tooltip modeTooltip = Tooltip.m_257550_(Component.m_237115_("ae2lt.tianshu.overload_config.mode.tooltip"));

      for (int i = 0; i < 5; i++) {
         int visibleIndex = i;
         AE2Button button = new AE2Button(0, 0, 46, 16, Component.m_237119_(), ignored -> this.toggleRow(visibleIndex));
         button.m_257544_(modeTooltip);
         button.f_93624_ = false;
         this.toggleButtons[i] = (AE2Button)this.m_142416_(button);
      }

      this.scrollbar.setHeight(129);
      this.scrollbar.setRange(0, Math.max(0, this.rows.size() - 5), 2);
      this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
   }

   private void toggleRow(int visibleIndex) {
      int index = this.scrollbar.getCurrentScroll() + visibleIndex;
      if (index >= 0 && index < this.rows.size()) {
         TianshuOverloadPatternConfigScreen.Row row = this.rows.get(index);
         if (row.header == null) {
            row.idOnly = !row.idOnly;
         }
      }
   }

   private void confirm() {
      ArrayList<Integer> inputIdOnly = new ArrayList<>();
      ArrayList<Integer> outputIdOnly = new ArrayList<>();
      boolean hasSlots = false;

      for (TianshuOverloadPatternConfigScreen.Row row : this.rows) {
         if (row.header == null) {
            hasSlots = true;
            if (row.idOnly) {
               (row.output ? outputIdOnly : inputIdOnly).add(row.slotIndex);
            }
         }
      }

      if (hasSlots) {
         ((TianshuPatternEncodingTermMenu)this.f_97732_)
            .armOverloadEncoding(new ProcessingPatternEncodingType.OverloadConfig(toArray(inputIdOnly), toArray(outputIdOnly)));
      }

      this.returnToParent();
   }

   private static int[] toArray(List<Integer> values) {
      int[] result = new int[values.size()];

      for (int i = 0; i < result.length; i++) {
         result[i] = values.get(i);
      }

      return result;
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      for (AE2Button button : this.toggleButtons) {
         button.f_93624_ = false;
      }

      int textColor = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
      int scroll = this.scrollbar.getCurrentScroll();

      for (int visible = 0; visible < 5; visible++) {
         int index = scroll + visible;
         if (index >= this.rows.size()) {
            break;
         }

         TianshuOverloadPatternConfigScreen.Row row = this.rows.get(index);
         int rowY = 32 + visible * 25;
         if (row.header != null) {
            graphics.m_280614_(this.f_96547_, row.header, 16, rowY + 8, textColor, false);
         } else {
            graphics.m_280480_(row.stack.what().wrapForDisplayOrFilter(), 14, rowY + 4);
            Component name = row.stack.what().getDisplayName();
            graphics.m_280649_(this.f_96547_, Language.m_128107_().m_5536_(this.f_96547_.m_92854_(name, 84)), 34, rowY + 8, textColor, false);
            AE2Button button = this.toggleButtons[visible];
            button.m_93666_(Component.m_237115_(row.idOnly ? "ae2lt.tianshu.overload_config.id_only" : "ae2lt.tianshu.overload_config.strict"));
            button.m_264152_(this.f_97735_ + 120, this.f_97736_ + rowY + 4);
            button.f_93624_ = true;
         }
      }

      if (this.rows.isEmpty()) {
         MutableComponent text = Component.m_237115_("ae2lt.tianshu.pattern_config.empty");
         graphics.m_280614_(this.f_96547_, text, (190 - this.f_96547_.m_92852_(text)) / 2, 90, -8947849, false);
      }
   }

   public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
      TianshuPatternConfigLayout.drawBackground(graphics, offsetX, offsetY);
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (delta != 0.0 && this.rows.size() > 5) {
         this.scrollbar.setCurrentScroll(this.scrollbar.getCurrentScroll() + (delta > 0.0 ? -1 : 1));
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public void m_7379_() {
      this.returnToParent();
   }

   private static final class Row {
      @Nullable
      final Component header;
      final boolean output;
      final int slotIndex;
      final GenericStack stack;
      boolean idOnly;

      private Row(@Nullable Component header, boolean output, int slotIndex, GenericStack stack, boolean idOnly) {
         this.header = header;
         this.output = output;
         this.slotIndex = slotIndex;
         this.stack = stack;
         this.idOnly = idOnly;
      }

      static TianshuOverloadPatternConfigScreen.Row header(Component text) {
         return new TianshuOverloadPatternConfigScreen.Row(text, false, -1, null, false);
      }

      static TianshuOverloadPatternConfigScreen.Row slot(boolean output, int slotIndex, GenericStack stack, boolean idOnly) {
         return new TianshuOverloadPatternConfigScreen.Row(null, output, slotIndex, stack, idOnly);
      }
   }
}
