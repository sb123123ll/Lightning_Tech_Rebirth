package com.moakiee.ae2lt.client;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToolboxPanel;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.client.gui.GuiTextLayout;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.menu.OverloadedInterfaceMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OverloadedInterfaceScreen extends AEBaseScreen<OverloadedInterfaceMenu> {
   private static final int SLOTS_PER_PAGE = 18;
   private static final int COLS = 9;
   private static final int SLOT_SPACING = 18;
   private static final int AMT_ROW1_Y = 35;
   private static final int AMT_ROW2_Y = 95;
   private static final int AMT_START_X = 8;
   private static final int PAGE_INDICATOR_Y = 18;
   private final SettingToggleButton<FuzzyMode> fuzzyMode;
   private final TextureToggleButton modeButton;
   private final TextureToggleButton exportModeButton;
   private final TextureToggleButton importModeButton;
   private final TextureToggleButton speedButton;
   private final OverloadedInterfaceScreen.PageButton prevPageButton;
   private final OverloadedInterfaceScreen.PageButton nextPageButton;
   private final List<OverloadedInterfaceScreen.SetAmountButton> amountButtons = new ArrayList<>();
   private final List<Slot> configSlots;
   private int lastKnownPage = -1;

   public OverloadedInterfaceScreen(OverloadedInterfaceMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));
      List<Slot> filterSlots = menu.getSlots(Ae2ltSlotSemantics.OVERLOADED_INTERFACE_FILTER);
      if (!filterSlots.isEmpty()) {
         this.widgets.add("overloadedFilter", new UpgradesPanel(filterSlots, () -> List.of(Component.m_237115_("item.ae2lt.overloaded_filter_component"))));
      }

      if (menu.getToolbox().isPresent()) {
         this.widgets.add("toolbox", new ToolboxPanel(style, menu.getToolbox().getName()));
      }

      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.fuzzyMode = new ServerSettingToggleButton(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
      this.addToLeftToolbar(this.fuzzyMode);
      this.nextPageButton = new OverloadedInterfaceScreen.PageButton(Icon.ARROW_RIGHT, btn -> menu.nextPage());
      this.nextPageButton.m_93666_(Component.m_237115_("ae2lt.gui.overloaded_interface.next_page"));
      this.addToLeftToolbar(this.nextPageButton);
      this.prevPageButton = new OverloadedInterfaceScreen.PageButton(Icon.ARROW_LEFT, btn -> menu.prevPage());
      this.prevPageButton.m_93666_(Component.m_237115_("ae2lt.gui.overloaded_interface.prev_page"));
      this.addToLeftToolbar(this.prevPageButton);
      this.modeButton = new TextureToggleButton(TextureToggleButton.ButtonType.MODE, btn -> menu.cycleInterfaceMode());
      this.modeButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.interface_mode.wireless")));
      this.modeButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.interface_mode.normal")));
      this.addToLeftToolbar(this.modeButton);
      this.exportModeButton = new TextureToggleButton(TextureToggleButton.ButtonType.AUTO_EXPORT, btn -> menu.cycleExportMode());
      this.exportModeButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.export_mode.auto")));
      this.exportModeButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.export_mode.off")));
      this.addToLeftToolbar(this.exportModeButton);
      this.importModeButton = new TextureToggleButton(TextureToggleButton.ButtonType.AUTO_IMPORT, btn -> menu.cycleImportMode());
      this.addToLeftToolbar(this.importModeButton);
      this.speedButton = new TextureToggleButton(TextureToggleButton.ButtonType.SPEED, btn -> menu.cycleIOSpeed());
      this.speedButton.setTooltipOn(List.of(Component.m_237115_("ae2lt.gui.io_speed.fast")));
      this.speedButton.setTooltipOff(List.of(Component.m_237115_("ae2lt.gui.io_speed.normal")));
      this.addToLeftToolbar(this.speedButton);
      this.widgets.addOpenPriorityButton();
      this.configSlots = menu.getAllConfigSlots();

      for (int i = 0; i < this.configSlots.size(); i++) {
         int slotIdx = i;
         OverloadedInterfaceScreen.SetAmountButton button = new OverloadedInterfaceScreen.SetAmountButton(btn -> {
            if (m_96638_()) {
               menu.toggleUnlimited(this.configSlots.get(slotIdx).m_150661_());
            } else {
               menu.openSetAmountMenu(this.configSlots.get(slotIdx).m_150661_());
            }
         });
         button.setDisableBackground(true);
         button.m_93666_(Component.m_237115_("ae2lt.gui.set_amount.message"));
         button.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.gui.set_amount.tooltip")));
         this.amountButtons.add(button);
      }
   }

   protected void m_7856_() {
      super.m_7856_();

      for (OverloadedInterfaceScreen.SetAmountButton btn : this.amountButtons) {
         this.m_142416_(btn);
      }
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      this.fuzzyMode.set(((OverloadedInterfaceMenu)this.f_97732_).getFuzzyMode());
      this.fuzzyMode.setVisibility(((OverloadedInterfaceMenu)this.f_97732_).hasUpgrade(AEItems.FUZZY_CARD));
      this.modeButton.setState(((OverloadedInterfaceMenu)this.f_97732_).interfaceMode == 1);
      this.exportModeButton.setState(((OverloadedInterfaceMenu)this.f_97732_).exportMode == OverloadedInterfaceBlockEntity.ExportMode.AUTO.ordinal());
      this.speedButton.setState(((OverloadedInterfaceMenu)this.f_97732_).ioSpeedMode == 1);
      OverloadedInterfaceBlockEntity.ImportMode[] impModes = OverloadedInterfaceBlockEntity.ImportMode.values();
      int importModeIndex = Math.max(0, Math.min(((OverloadedInterfaceMenu)this.f_97732_).importMode, impModes.length - 1));
      this.importModeButton.setTooltipAt(impModes[0].ordinal(), List.of(Component.m_237115_("ae2lt.gui.import_mode.off")));
      this.importModeButton.setTooltipAt(impModes[1].ordinal(), List.of(Component.m_237115_("ae2lt.gui.import_mode.auto")));
      this.importModeButton.setTooltipAt(impModes[2].ordinal(), List.of(Component.m_237115_("ae2lt.gui.import_mode.eject")));
      this.importModeButton.setStateIndex(importModeIndex);
      if (((OverloadedInterfaceMenu)this.f_97732_).currentPage != this.lastKnownPage) {
         this.lastKnownPage = ((OverloadedInterfaceMenu)this.f_97732_).currentPage;
         ((OverloadedInterfaceMenu)this.f_97732_).showPage(((OverloadedInterfaceMenu)this.f_97732_).currentPage);
      }

      int page = ((OverloadedInterfaceMenu)this.f_97732_).currentPage;
      int start = page * 18;
      int end = Math.min(start + 18, this.configSlots.size());

      for (int i = 0; i < this.amountButtons.size(); i++) {
         OverloadedInterfaceScreen.SetAmountButton button = this.amountButtons.get(i);
         if (i >= start && i < end) {
            int inPage = i - start;
            int col = inPage % 9;
            int row = inPage / 9;
            button.m_264152_(this.f_97735_ + 8 + col * 18, this.f_97736_ + (row == 0 ? 35 : 95));
            ItemStack item = this.configSlots.get(i).m_7993_();
            button.f_93624_ = !item.m_41619_();
         } else {
            button.f_93624_ = false;
         }
      }

      boolean hasMultiplePages = ((OverloadedInterfaceMenu)this.f_97732_).totalPages > 1;
      this.prevPageButton.setVisibility(hasMultiplePages && page > 0);
      this.nextPageButton.setVisibility(hasMultiplePages && page < ((OverloadedInterfaceMenu)this.f_97732_).totalPages - 1);
   }

   public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
      super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
      String pageText = ((OverloadedInterfaceMenu)this.f_97732_).currentPage + 1 + "/" + ((OverloadedInterfaceMenu)this.f_97732_).totalPages;
      int textWidth = this.f_96547_.m_92895_(pageText);
      guiGraphics.m_280056_(
         this.f_96547_, pageText, GuiTextLayout.centeredX(this.f_97726_, textWidth), 18, this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(), false
      );
      int page = ((OverloadedInterfaceMenu)this.f_97732_).currentPage;
      int start = page * 18;
      int end = Math.min(start + 18, this.configSlots.size());

      for (int i = start; i < end; i++) {
         if (((OverloadedInterfaceMenu)this.f_97732_).isSlotUnlimited(i)) {
            Slot slot = this.configSlots.get(i);
            if (!slot.m_7993_().m_41619_()) {
               guiGraphics.m_280056_(this.f_96547_, "∞", slot.f_40220_ + 10, slot.f_40221_ - 10, -16711936, true);
            }
         }
      }
   }

   private List<Component> getCompatibleUpgrades() {
      ArrayList<Component> list = new ArrayList<>();
      list.add(GuiText.CompatibleUpgrades.text());
      list.addAll(Upgrades.getTooltipLinesForMachine(((OverloadedInterfaceMenu)this.f_97732_).getUpgrades().getUpgradableItem()));
      return list;
   }

   static class PageButton extends IconButton {
      private final Icon icon;

      public PageButton(Icon icon, OnPress onPress) {
         super(onPress);
         this.icon = icon;
      }

      protected Icon getIcon() {
         return this.icon;
      }
   }

   static class SetAmountButton extends IconButton {
      public SetAmountButton(OnPress onPress) {
         super(onPress);
      }

      protected Icon getIcon() {
         return this.m_198029_() ? Icon.WRENCH : Icon.WRENCH_DISABLED;
      }
   }
}
