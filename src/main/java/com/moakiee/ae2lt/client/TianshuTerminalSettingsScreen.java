package com.moakiee.ae2lt.client;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.me.common.TerminalSettingsScreen;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.client.gui.AE2Button;
import com.moakiee.ae2lt.config.AE2LTClientConfig;
import com.moakiee.ae2lt.config.TianshuUploadTrigger;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TianshuTerminalSettingsScreen<M extends TianshuPatternEncodingTermMenu> extends AESubScreen<M, TerminalSettingsScreen<M>> {
   private AE2Button triggerButton;
   private AE2Button duplicateEncodingButton;

   public TianshuTerminalSettingsScreen(TerminalSettingsScreen<M> parent) {
      super(parent, "/screens/tianshu_terminal_settings.json");
      this.hideTerminalSlots();
      this.widgets.add("back", new TabButton(Icon.ARROW_LEFT, Component.m_237115_("gui.back"), ignored -> this.returnToParent()));
      this.triggerButton = new AE2Button(this.triggerLabel(), btn -> this.cycleTrigger());
      this.widgets.add("uploadTrigger", this.triggerButton);
      this.duplicateEncodingButton = new AE2Button(this.duplicateEncodingLabel(), btn -> this.toggleDuplicateEncoding());
      this.widgets.add("duplicateEncoding", this.duplicateEncodingButton);
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

   private void cycleTrigger() {
      TianshuUploadTrigger next = AE2LTClientConfig.uploadTrigger().next();
      AE2LTClientConfig.setUploadTrigger(next);
      this.triggerButton.m_93666_(this.triggerLabel());
   }

   private Component triggerLabel() {
      return Component.m_237115_("ae2lt.tianshu.settings.trigger." + AE2LTClientConfig.uploadTrigger().name().toLowerCase(Locale.ROOT));
   }

   private void toggleDuplicateEncoding() {
      AE2LTClientConfig.setInterceptDuplicatePatternEncoding(!AE2LTClientConfig.interceptDuplicatePatternEncoding());
      this.duplicateEncodingButton.m_93666_(this.duplicateEncodingLabel());
   }

   private Component duplicateEncodingLabel() {
      return Component.m_237115_(
         AE2LTClientConfig.interceptDuplicatePatternEncoding()
            ? "ae2lt.tianshu.settings.duplicate_encoding.on"
            : "ae2lt.tianshu.settings.duplicate_encoding.off"
      );
   }

   public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.settings.upload_trigger"), 10, 30, 4210752, false);
      graphics.m_280554_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.settings.upload_trigger.hint"), 10, 72, 180, 6710886);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.settings.duplicate_encoding"), 10, 118, 4210752, false);
      graphics.m_280554_(this.f_96547_, Component.m_237115_("ae2lt.tianshu.settings.duplicate_encoding.hint"), 10, 160, 180, 6710886);
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.returnToParent();
         return true;
      } else {
         return super.m_7933_(keyCode, scanCode, modifiers);
      }
   }
}
