package com.moakiee.ae2lt.client;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import com.moakiee.ae2lt.logic.tianshu.terminal.ProcessingPatternEncodingType;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.pedroksl.advanced_ae.AdvancedAE;
import net.pedroksl.advanced_ae.client.gui.widgets.DirectionInputButton;
import net.pedroksl.advanced_ae.common.definitions.AAEText;

final class TianshuAdvancedPatternConfigScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TianshuPatternEncodingTermScreen<M>> {
   private static final int ROW_CONTENT_Y_OFFSET = 4;
   private static final int BUTTONS_LEFT = 34;
   private static final int BUTTONS_Y_OFFSET = 5;
   private static final int DIR_BUTTON_WIDTH = 12;
   private static final int DIR_BUTTON_HEIGHT = 14;
   private static final int DIR_COUNT = 7;
   private final Scrollbar scrollbar;
   private final List<TianshuAdvancedPatternConfigScreen.Row> rows = new ArrayList<>();
   private final List<DirectionInputButton[]> rowButtons = new ArrayList<>();

   TianshuAdvancedPatternConfigScreen(TianshuPatternEncodingTermScreen<M> parent) {
      super(parent, "/screens/tianshu_advanced_pattern_config.json");
      this.f_97726_ = 190;
      this.f_97727_ = 187;
      this.widgets.add("button_back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
      this.widgets.addButton("save", Component.m_237115_("ae2lt.tianshu.pattern_config.done"), this::confirm);
      this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.SMALL);
      this.collectRows();
   }

   private void collectRows() {
      ProcessingPatternEncodingType.AdvancedConfig config = ((TianshuPatternEncodingTermMenu)this.f_97732_).getAdvancedEncodingConfig();
      LinkedHashMap<AEKey, Integer> pending = new LinkedHashMap<>();
      FakeSlot[] slots = ((TianshuPatternEncodingTermMenu)this.f_97732_).getProcessingInputSlots();

      for (int i = 0; i < slots.length; i++) {
         GenericStack stack = GenericStack.fromItemStack(slots[i].m_7993_());
         if (stack != null && stack.what() != null) {
            pending.putIfAbsent(stack.what(), Integer.valueOf(config != null ? config.direction(i) : 0));
         }
      }

      pending.forEach((key, direction) -> this.rows.add(new TianshuAdvancedPatternConfigScreen.Row(key, direction)));
   }

   protected void m_7856_() {
      super.m_7856_();
      this.rowButtons.clear();

      for (TianshuAdvancedPatternConfigScreen.Row row : this.rows) {
         DirectionInputButton[] buttons = new DirectionInputButton[7];

         for (int i = 0; i < 7; i++) {
            DirectionInputButton button = new DirectionInputButton(0, 0, 12, 14, directionTextures(i), this::directionPressed);
            button.m_257544_(Tooltip.m_257550_(directionLabel(i)));
            button.setKey(row.key);
            button.setIndex(i);
            button.f_93624_ = false;
            buttons[i] = (DirectionInputButton)this.m_142416_(button);
         }

         this.rowButtons.add(buttons);
      }

      this.scrollbar.setHeight(129);
      this.scrollbar.setRange(0, Math.max(0, this.rows.size() - 5), 2);
      this.setSlotsHidden(SlotSemantics.TOOLBOX, true);
   }

   private void directionPressed(Button pressed) {
      DirectionInputButton button = (DirectionInputButton)pressed;
      Direction direction = button.getDirection();
      int encoded = direction == null ? 0 : direction.ordinal() + 1;

      for (TianshuAdvancedPatternConfigScreen.Row row : this.rows) {
         if (row.key.equals(button.getKey())) {
            row.direction = encoded;
            break;
         }
      }
   }

   private void confirm() {
      if (this.rows.isEmpty()) {
         this.returnToParent();
      } else {
         FakeSlot[] slots = ((TianshuPatternEncodingTermMenu)this.f_97732_).getProcessingInputSlots();
         int[] directions = new int[slots.length];

         for (int i = 0; i < slots.length; i++) {
            GenericStack stack = GenericStack.fromItemStack(slots[i].m_7993_());
            if (stack != null && stack.what() != null) {
               for (TianshuAdvancedPatternConfigScreen.Row row : this.rows) {
                  if (row.key.equals(stack.what())) {
                     directions[i] = row.direction;
                     break;
                  }
               }
            }
         }

         ((TianshuPatternEncodingTermMenu)this.f_97732_).armAdvancedEncoding(new ProcessingPatternEncodingType.AdvancedConfig(directions));
         this.returnToParent();
      }
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      for (DirectionInputButton[] buttons : this.rowButtons) {
         for (DirectionInputButton button : buttons) {
            button.f_93624_ = false;
         }
      }

      int scroll = this.scrollbar.getCurrentScroll();

      for (int visible = 0; visible < 5; visible++) {
         int index = scroll + visible;
         if (index >= this.rows.size()) {
            break;
         }

         TianshuAdvancedPatternConfigScreen.Row row = this.rows.get(index);
         int rowY = 32 + visible * 25;
         graphics.m_280480_(row.key.wrapForDisplayOrFilter(), 14, rowY + 4);
         DirectionInputButton[] buttons = this.rowButtons.get(index);
         int highlighted = columnForDirection(row.direction);

         for (int col = 0; col < 7; col++) {
            DirectionInputButton button = buttons[col];
            button.m_264152_(this.f_97735_ + 34 + 4 + col * 13, this.f_97736_ + rowY + 5);
            button.setHighlighted(col == highlighted);
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

   private static int columnForDirection(int direction) {
      if (direction > 0 && direction <= 6) {
         return switch (Direction.values()[direction - 1]) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case UP -> 5;
            case DOWN -> 6;
            default -> throw new IncompatibleClassChangeError();
         };
      } else {
         return 0;
      }
   }

   private static Pair<ResourceLocation, ResourceLocation> directionTextures(int index) {
      String name = switch (index) {
         case 1 -> "north";
         case 2 -> "east";
         case 3 -> "south";
         case 4 -> "west";
         case 5 -> "up";
         case 6 -> "down";
         default -> "any";
      };
      return new Pair(AdvancedAE.makeId("textures/guis/" + name + "_button.png"), AdvancedAE.makeId("textures/guis/" + name + "_button_selected.png"));
   }

   private static Component directionLabel(int index) {
      return Component.m_237115_(switch (index) {
         case 1 -> AAEText.NorthButton.getTranslationKey();
         case 2 -> AAEText.EastButton.getTranslationKey();
         case 3 -> AAEText.SouthButton.getTranslationKey();
         case 4 -> AAEText.WestButton.getTranslationKey();
         case 5 -> AAEText.UpButton.getTranslationKey();
         case 6 -> AAEText.DownButton.getTranslationKey();
         default -> AAEText.AnyButton.getTranslationKey();
      });
   }

   private static final class Row {
      final AEKey key;
      int direction;

      Row(AEKey key, int direction) {
         this.key = key;
         this.direction = direction;
      }
   }
}
