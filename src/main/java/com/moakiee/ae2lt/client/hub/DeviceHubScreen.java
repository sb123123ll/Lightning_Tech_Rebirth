package com.moakiee.ae2lt.client.hub;

import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.menu.hub.DeviceHubDisplayRules;
import com.moakiee.ae2lt.menu.hub.DeviceHubMenu;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.hub.DeviceHubActionPacket;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class DeviceHubScreen extends AbstractContainerScreen<DeviceHubMenu> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/armor_settings_gui.png");
   private static final ResourceLocation CHECKBOX_TEXTURE = new ResourceLocation("ae2", "textures/guis/checkbox.png");
   private static final int TEXTURE_SIZE = 256;
   private static final int CHECKBOX_TEXTURE_SIZE = 64;
   private static final int GUI_WIDTH = 176;
   private static final int GUI_HEIGHT = 223;
   private static final int TEXT_ON_LIGHT_BG = -9538420;
   private static final int TEXT_ON_DARK_BG = -1;
   private static final int ROW_HOVER = 810372455;
   private static final int ROW_SELECTED = 1078807911;
   private static final int ROW_DIVIDER = -7893596;
   private static final int BUTTON_BORDER = -11709848;
   private static final int BUTTON_BORDER_HOVER = -7563597;
   private static final int BUTTON_DISABLED = -9736058;
   private static final int BUTTON_FILL = -9867126;
   private static final int BUTTON_FILL_HOVER = -8682337;
   private static final int BUTTON_FILL_DISABLED = -8551525;
   private static final int BUTTON_TEXT = -1;
   private static final int TAB_COUNT = 5;
   private static final int TAB_Y = 0;
   private static final int TAB_WIDTH = 31;
   private static final int TAB_HEIGHT = 25;
   private static final int TAB_ICON_SIZE = 16;
   private static final int TAB_ACTIVE_SRC_Y = 225;
   private static final int TAB_ACTIVE_H = 26;
   private static final int[] TAB_X = new int[]{0, 31, 62, 93, 145};
   private static final int[] TAB_ACTIVE_SRC_X = new int[]{0, 31, 62, 93, 145};
   private static final int STATUS_X = 12;
   private static final int STATUS_Y = 36;
   private static final int STATUS_ICON_X = 13;
   private static final int STATUS_ICON_Y = 42;
   private static final int STATUS_TEXT_X = 34;
   private static final int STATUS_NAME_Y = 40;
   private static final int STATUS_LINE_Y = 53;
   private static final int STATUS_RIGHT = 166;
   private static final int MODULE_HEADER_X = 12;
   private static final int MODULE_HEADER_Y = 69;
   private static final int MODULE_LIST_X = 19;
   private static final int MODULE_LIST_Y = 83;
   private static final int MODULE_LIST_RIGHT = 166;
   private static final int MODULE_ROW_H = 14;
   private static final int MODULE_VISIBLE_ROWS = 4;
   private static final int MODULE_CHECKBOX_X = 142;
   private static final int SCROLL_X = 10;
   private static final int SCROLL_Y = 83;
   private static final int SCROLL_H = 56;
   private static final int SCROLL_SRC_X = 180;
   private static final int SCROLL_SRC_Y = 0;
   private static final int SCROLL_HOVER_SRC_Y = 17;
   private static final int SCROLL_SRC_W = 7;
   private static final int SCROLL_SRC_H = 15;
   private static final int SCROLL_HOVER_SRC_H = 14;
   private static final int CONFIG_X = 12;
   private static final int CONFIG_ROW_X = 19;
   private static final int CONFIG_HEADER_Y = 144;
   private static final int CONFIG_Y = 160;
   private static final int CONFIG_BUTTON_X = 124;
   private static final int CONFIG_BUTTON_W = 40;
   private static final int CONFIG_BUTTON_H = 12;
   private static final int CONFIG_ROW_H = 16;
   private static final int CONFIG_VISIBLE_ROWS = 3;
   private static final int CONFIG_SCROLL_Y = 160;
   private static final int CONFIG_SCROLL_H = 46;
   private static final int RAILGUN_SETTING_COUNT = 6;
   private static final int RAILGUN_SETTING_TERRAIN = 0;
   private static final int RAILGUN_SETTING_PVP = 1;
   private static final int RAILGUN_SETTING_SOUND = 2;
   private static final int RAILGUN_SETTING_CHAIN_DAMAGE = 3;
   private static final int RAILGUN_SETTING_CHARGED_SPLASH = 4;
   private static final int RAILGUN_SETTING_EXECUTION_MODE = 5;
   private static final int CHECKBOX_WIDTH = 14;
   private static final int CHECKBOX_HEIGHT = 14;
   private static final int CHECKBOX_OFF_SRC_Y = 0;
   private static final int CHECKBOX_ON_SRC_Y = 14;
   private static final String[] TAB_LABEL_KEYS = new String[]{
      "ae2lt.device_hub.tab.helmet",
      "ae2lt.device_hub.tab.chestplate",
      "ae2lt.device_hub.tab.leggings",
      "ae2lt.device_hub.tab.boots",
      "ae2lt.device_hub.tab.railgun"
   };
   private static final String[] TAB_REQUIRED_KEYS = new String[]{
      "ae2lt.device_hub.tab.required.helmet",
      "ae2lt.device_hub.tab.required.chestplate",
      "ae2lt.device_hub.tab.required.leggings",
      "ae2lt.device_hub.tab.required.boots",
      "ae2lt.device_hub.tab.required.railgun"
   };
   private int scrollOffset = 0;
   private int configScrollOffset = 0;
   private int lastConfigTab = -1;
   private int lastConfigModule = -1;

   public DeviceHubScreen(DeviceHubMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.f_97726_ = 176;
      this.f_97727_ = 223;
      this.f_97731_ = this.f_97727_ + 100;
   }

   protected void m_7856_() {
      super.m_7856_();
      this.f_97728_ = 0;
      this.f_97729_ = -100;
   }

   protected void m_7286_(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
      gfx.m_280163_(TEXTURE, this.f_97735_, this.f_97736_, 0.0F, 0.0F, 176, 223, 256, 256);
      this.renderSelectedTabTexture(gfx, ((DeviceHubMenu)this.f_97732_).getSelectedTab());
   }

   public void m_88315_(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(gfx);
      super.m_88315_(gfx, mouseX, mouseY, partialTick);
      int selectedTab = ((DeviceHubMenu)this.f_97732_).getSelectedTab();
      int tabMask = ((DeviceHubMenu)this.f_97732_).getTabAvailability();
      boolean railgunTab = selectedTab == 4;
      this.resetConfigScrollWhenSelectionChanges(selectedTab, railgunTab ? -1 : ((DeviceHubMenu)this.f_97732_).getSelectedModuleIndex());
      this.renderTabIcons(gfx);
      boolean hasDevice = (tabMask & 1 << selectedTab) != 0;
      if (!hasDevice) {
         gfx.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.device_hub.no_device"), this.f_97735_ + 12, this.f_97736_ + 36 + 9, -1, false);
         this.renderTabTooltips(gfx, mouseX, mouseY, tabMask);
         this.m_280072_(gfx, mouseX, mouseY);
      } else {
         this.renderStatusPanel(gfx, railgunTab);
         this.renderModuleList(gfx, mouseX, mouseY, railgunTab);
         if (railgunTab) {
            this.renderRailgunSettings(gfx, mouseX, mouseY);
         } else {
            this.renderModuleConfig(gfx, mouseX, mouseY);
         }

         this.renderTabTooltips(gfx, mouseX, mouseY, tabMask);
         this.m_280072_(gfx, mouseX, mouseY);
      }
   }

   protected void m_280003_(GuiGraphics gfx, int mouseX, int mouseY) {
   }

   private void renderSelectedTabTexture(GuiGraphics gfx, int selectedTab) {
      if (selectedTab >= 0 && selectedTab < 5) {
         gfx.m_280163_(TEXTURE, this.f_97735_ + TAB_X[selectedTab], this.f_97736_ + 0, (float)TAB_ACTIVE_SRC_X[selectedTab], 225.0F, 31, 26, 256, 256);
      }
   }

   private void renderTabIcons(GuiGraphics gfx) {
      for (int i = 0; i < 5; i++) {
         int x = this.f_97735_ + TAB_X[i];
         ItemStack stack = this.tabDisplayStack(i);
         if (!stack.m_41619_()) {
            int iconX = x + 7;
            gfx.m_280480_(stack, iconX, this.f_97736_ + 5);
         }
      }
   }

   private void renderStatusPanel(GuiGraphics gfx, boolean railgunTab) {
      int x = this.f_97735_ + 34;
      ItemStack stack = this.selectedDeviceStack();
      if (!stack.m_41619_()) {
         gfx.m_280480_(stack, this.f_97735_ + 13, this.f_97736_ + 42);
      }

      String deviceName = ((DeviceHubMenu)this.f_97732_).getDeviceName();
      if (!deviceName.isEmpty()) {
         gfx.m_280614_(this.f_96547_, Component.m_237113_(truncate(this.f_96547_, deviceName, 132)), x, this.f_97736_ + 40, -1, false);
      }

      Component statusLine = Component.m_237110_("ae2lt.device_hub.status.line", new Object[]{this.statusText(railgunTab)});
      gfx.m_280614_(this.f_96547_, statusLine, x, this.f_97736_ + 53, -1, false);
   }

   private Component statusText(boolean railgunTab) {
      return !railgunTab
         ? Component.m_237115_(DeviceHubDisplayRules.armorStatusKey(((DeviceHubMenu)this.f_97732_).hasCore(), ((DeviceHubMenu)this.f_97732_).isPowered()))
         : Component.m_237115_(((DeviceHubMenu)this.f_97732_).isPowered() ? "ae2lt.device_hub.status.normal" : "ae2lt.device_hub.status.unpowered");
   }

   private void renderModuleList(GuiGraphics gfx, int mouseX, int mouseY, boolean railgunTab) {
      List<String> moduleNameKeys = ((DeviceHubMenu)this.f_97732_).getModuleNameKeys();
      List<Integer> moduleCounts = ((DeviceHubMenu)this.f_97732_).getModuleCounts();
      List<Boolean> moduleEnabled = ((DeviceHubMenu)this.f_97732_).getModuleEnabled();
      gfx.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.device_hub.modules"), this.f_97735_ + 12, this.f_97736_ + 69, -9538420, false);
      this.scrollOffset = DeviceHubDisplayRules.clampScrollOffset(this.scrollOffset, moduleNameKeys.size(), 4);
      int selectedModuleIndex = ((DeviceHubMenu)this.f_97732_).getSelectedModuleIndex();

      for (int i = 0; i < Math.min(moduleNameKeys.size(), 4); i++) {
         int idx = i + this.scrollOffset;
         if (idx >= moduleNameKeys.size()) {
            break;
         }

         int rowY = this.f_97736_ + 83 + i * 14;
         boolean hovered = mouseX >= this.f_97735_ + 19 && mouseX < this.f_97735_ + 166 && mouseY >= rowY && mouseY < rowY + 14;
         if (hovered || idx == selectedModuleIndex) {
            gfx.m_280509_(this.f_97735_ + 19 - 1, rowY - 1, this.f_97735_ + 166, rowY + 14 - 1, idx == selectedModuleIndex ? 1078807911 : 810372455);
         }

         int count = idx < moduleCounts.size() ? moduleCounts.get(idx) : 1;
         int nameMaxWidth = railgunTab ? 147 : 117;
         String name = truncate(this.f_96547_, moduleName(moduleNameKeys.get(idx), count).getString(), nameMaxWidth);
         gfx.m_280614_(this.f_96547_, Component.m_237113_(name), this.f_97735_ + 19, rowY + 2, -1, false);
         if (!railgunTab) {
            boolean enabled = idx < moduleEnabled.size() && moduleEnabled.get(idx);
            this.drawCheckbox(gfx, this.f_97735_ + 142, rowY + 1, enabled);
         }

         if (i < Math.min(moduleNameKeys.size(), 4) - 1) {
            gfx.m_280509_(this.f_97735_ + 19, rowY + 14 - 1, this.f_97735_ + 166, rowY + 14, -7893596);
         }
      }

      if (moduleNameKeys.size() > 4) {
         this.renderScrollBar(gfx, moduleNameKeys.size(), mouseX, mouseY);
      }
   }

   private void renderScrollBar(GuiGraphics gfx, int moduleCount, int mouseX, int mouseY) {
      this.renderScrollBar(gfx, moduleCount, 4, this.scrollOffset, 10, 83, 56, mouseX, mouseY);
   }

   private void renderModuleConfig(GuiGraphics gfx, int mouseX, int mouseY) {
      int x = this.f_97735_ + 19;
      int y = this.f_97736_ + 160;
      gfx.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.celestweave.screen.module_options"), this.f_97735_ + 12, this.f_97736_ + 144, -9538420, false);
      int count = this.moduleConfigCount();
      if (count > 0) {
         this.configScrollOffset = DeviceHubDisplayRules.clampScrollOffset(this.configScrollOffset, count, 3);
         int rowY = y;

         for (int i = 0; i < Math.min(count, 3); i++) {
            int configIndex = i + this.configScrollOffset;
            if (configIndex >= count) {
               break;
            }

            String value = ((DeviceHubMenu)this.f_97732_).getModuleConfigValues().get(configIndex);
            boolean editable = ((DeviceHubMenu)this.f_97732_).getModuleConfigEditable().get(configIndex);
            gfx.m_280614_(this.f_96547_, this.moduleConfigLabel(configIndex), x, rowY + 1, -1, false);
            this.drawConfigValueButton(gfx, this.f_97735_ + 124, rowY - 1, value, editable, mouseX, mouseY);
            rowY += 16;
         }

         if (count > 3) {
            this.renderConfigScrollBar(gfx, count, mouseX, mouseY);
         }
      }
   }

   private void renderRailgunSettings(GuiGraphics gfx, int mouseX, int mouseY) {
      int x = this.f_97735_ + 19;
      int y = this.f_97736_ + 160;
      gfx.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.device_hub.settings"), this.f_97735_ + 12, this.f_97736_ + 144, -9538420, false);
      this.configScrollOffset = DeviceHubDisplayRules.clampScrollOffset(this.configScrollOffset, 6, 3);
      int rowY = y;

      for (int i = 0; i < 3; i++) {
         int settingIndex = i + this.configScrollOffset;
         if (settingIndex >= 6) {
            break;
         }

         Component label = this.railgunSettingLabel(settingIndex);
         if (settingIndex <= 4) {
            this.drawSettingRow(gfx, x, rowY, label, this.railgunSettingEnabled(settingIndex));
         } else {
            gfx.m_280614_(this.f_96547_, label, x, rowY + 1, -1, false);
            this.drawConfigValueButton(gfx, this.f_97735_ + 124, rowY - 1, this.railgunSettingValue(settingIndex), true, mouseX, mouseY);
         }

         rowY += 16;
      }

      this.renderConfigScrollBar(gfx, 6, mouseX, mouseY);
   }

   private Component railgunSettingLabel(int settingIndex) {
      return switch (settingIndex) {
         case 0 -> Component.m_237115_("ae2lt.device_hub.setting.terrain");
         case 1 -> Component.m_237115_("ae2lt.device_hub.setting.pvp");
         case 2 -> Component.m_237115_("ae2lt.device_hub.setting.sound");
         case 3 -> Component.m_237115_("ae2lt.device_hub.setting.chain_damage");
         case 4 -> Component.m_237115_("ae2lt.railgun.config.charged_splash");
         case 5 -> Component.m_237115_("ae2lt.railgun.config.overload_execution_mode");
         default -> Component.m_237119_();
      };
   }

   private boolean railgunSettingEnabled(int settingIndex) {
      return switch (settingIndex) {
         case 0 -> ((DeviceHubMenu)this.f_97732_).isTerrainDestruction();
         case 1 -> ((DeviceHubMenu)this.f_97732_).isPvp();
         case 2 -> ((DeviceHubMenu)this.f_97732_).isSoundEnabled();
         case 3 -> ((DeviceHubMenu)this.f_97732_).isChainDamage();
         case 4 -> ((DeviceHubMenu)this.f_97732_).isChargedSplash();
         default -> false;
      };
   }

   private String railgunSettingValue(int settingIndex) {
      String key = switch (settingIndex) {
         case 5 -> ((DeviceHubMenu)this.f_97732_).getExecutionMode().translationKey();
         default -> "";
      };
      return key.isEmpty() ? "" : Component.m_237115_(key).getString();
   }

   private void drawSettingRow(GuiGraphics gfx, int x, int y, Component label, boolean on) {
      gfx.m_280614_(this.f_96547_, label, x, y + 1, -1, false);
      this.drawCheckbox(gfx, this.f_97735_ + 142, y, on);
   }

   private void drawCheckbox(GuiGraphics gfx, int x, int y, boolean checked) {
      gfx.m_280163_(CHECKBOX_TEXTURE, x, y, 0.0F, checked ? 14.0F : 0.0F, 14, 14, 64, 64);
   }

   private void drawConfigValueButton(GuiGraphics gfx, int x, int y, String value, boolean editable, int mouseX, int mouseY) {
      boolean hovered = editable && mouseX >= x && mouseX <= x + 40 && mouseY >= y && mouseY <= y + 12;
      int borderColor = editable ? (hovered ? -7563597 : -11709848) : -9736058;
      int fillColor = editable ? (hovered ? -8682337 : -9867126) : -8551525;
      gfx.m_280509_(x - 1, y - 1, x + 40 + 1, y + 12 + 1, borderColor);
      gfx.m_280509_(x, y, x + 40, y + 12, fillColor);
      String text = truncate(this.f_96547_, value, 36);
      int textColor = editable ? -1 : -1;
      gfx.m_280614_(this.f_96547_, Component.m_237113_(text), x + (40 - this.f_96547_.m_92895_(text)) / 2, y + 2, textColor, false);
   }

   private boolean mouseClickedModuleConfig(double mouseX, double mouseY) {
      int count = this.moduleConfigCount();
      if (count <= 0) {
         return false;
      } else {
         int rowY = this.f_97736_ + 160;
         int buttonX = this.f_97735_ + 124;

         for (int i = 0; i < Math.min(count, 3); i++) {
            int configIndex = i + this.configScrollOffset;
            if (configIndex >= count) {
               break;
            }

            boolean editable = ((DeviceHubMenu)this.f_97732_).getModuleConfigEditable().get(configIndex);
            if (editable && mouseX >= (double)buttonX && mouseX <= (double)(buttonX + 40) && mouseY >= (double)(rowY - 1) && mouseY <= (double)(rowY - 1 + 12)) {
               NetworkInit.sendToServer(new DeviceHubActionPacket(5, configIndex));
               return true;
            }

            rowY += 16;
         }

         return false;
      }
   }

   private int moduleConfigCount() {
      return Math.min(
         Math.min(
            Math.min(((DeviceHubMenu)this.f_97732_).getModuleConfigKeys().size(), ((DeviceHubMenu)this.f_97732_).getModuleConfigLabels().size()),
            ((DeviceHubMenu)this.f_97732_).getModuleConfigValues().size()
         ),
         ((DeviceHubMenu)this.f_97732_).getModuleConfigEditable().size()
      );
   }

   private Component moduleConfigLabel(int index) {
      String key = ((DeviceHubMenu)this.f_97732_).getModuleConfigKeys().get(index);
      return key != null && !key.isBlank()
         ? Component.m_237115_("ae2lt.celestweave.config." + key)
         : Component.m_237113_(((DeviceHubMenu)this.f_97732_).getModuleConfigLabels().get(index));
   }

   private static Component moduleName(String nameKey, int count) {
      Component name = Component.m_237115_(nameKey);
      return (Component)(count > 1 ? Component.m_237110_("ae2lt.device_hub.module.counted", new Object[]{name, count}) : name);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.m_6375_(mouseX, mouseY, button);
      } else {
         int tabMask = ((DeviceHubMenu)this.f_97732_).getTabAvailability();
         int selectedTab = ((DeviceHubMenu)this.f_97732_).getSelectedTab();

         for (int i = 0; i < 5; i++) {
            int tx = this.f_97735_ + TAB_X[i];
            int ty = this.f_97736_ + 0;
            if (mouseX >= (double)tx && mouseX <= (double)(tx + 31) && mouseY >= (double)ty && mouseY <= (double)(ty + 25)) {
               if ((tabMask & 1 << i) != 0 && i != selectedTab) {
                  NetworkInit.sendToServer(new DeviceHubActionPacket(0, i));
                  this.playClick();
               }

               return true;
            }
         }

         boolean railgunTab = selectedTab == 4;
         if (!railgunTab) {
            if (this.mouseClickedModuleConfig(mouseX, mouseY)) {
               this.playClick();
               return true;
            }

            if (this.mouseClickedModule(mouseX, mouseY, false)) {
               this.playClick();
               return true;
            }
         } else if (this.mouseClickedRailgunSettings(mouseX, mouseY)) {
            this.playClick();
            return true;
         }

         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   private boolean mouseClickedModule(double mouseX, double mouseY, boolean railgunTab) {
      List<String> moduleNames = ((DeviceHubMenu)this.f_97732_).getModuleNameKeys();
      this.scrollOffset = DeviceHubDisplayRules.clampScrollOffset(this.scrollOffset, moduleNames.size(), 4);

      for (int i = 0; i < Math.min(moduleNames.size(), 4); i++) {
         int idx = i + this.scrollOffset;
         int rowY = this.f_97736_ + 83 + i * 14;
         int checkboxX = this.f_97735_ + 142;
         if (!railgunTab
            && mouseX >= (double)checkboxX
            && mouseX <= (double)(checkboxX + 14)
            && mouseY >= (double)(rowY + 1)
            && mouseY <= (double)(rowY + 1 + 14)) {
            NetworkInit.sendToServer(new DeviceHubActionPacket(1, idx));
            return true;
         }

         if (mouseX >= (double)(this.f_97735_ + 19) && mouseX <= (double)(this.f_97735_ + 166) && mouseY >= (double)rowY && mouseY <= (double)(rowY + 14)) {
            NetworkInit.sendToServer(new DeviceHubActionPacket(4, idx));
            return true;
         }
      }

      return false;
   }

   private boolean mouseClickedRailgunSettings(double mouseX, double mouseY) {
      int checkboxX = this.f_97735_ + 142;
      int buttonX = this.f_97735_ + 124;
      int rowY = this.f_97736_ + 160;

      for (int i = 0; i < 3; i++) {
         int settingIndex = i + this.configScrollOffset;
         if (settingIndex >= 6) {
            break;
         }

         boolean checkboxSetting = settingIndex <= 4;
         int controlX = checkboxSetting ? checkboxX : buttonX;
         int controlWidth = checkboxSetting ? 14 : 40;
         if (mouseX >= (double)controlX
            && mouseX <= (double)(controlX + controlWidth)
            && mouseY >= (double)(rowY - (checkboxSetting ? 0 : 1))
            && mouseY <= (double)(rowY - (checkboxSetting ? 0 : 1) + (checkboxSetting ? 14 : 12))) {
            int action = switch (settingIndex) {
               case 0 -> 2;
               case 1 -> 3;
               case 2 -> 6;
               case 3 -> 7;
               case 4 -> 9;
               case 5 -> 8;
               default -> -1;
            };
            if (action >= 0) {
               NetworkInit.sendToServer(new DeviceHubActionPacket(action, 0));
               return true;
            }
         }

         rowY += 16;
      }

      return false;
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 258) {
         this.cycleTab((modifiers & 1) != 0 ? -1 : 1);
         return true;
      } else if (keyCode == 263) {
         this.cycleTab(-1);
         return true;
      } else if (keyCode == 262) {
         this.cycleTab(1);
         return true;
      } else {
         return super.m_7933_(keyCode, scanCode, modifiers);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (mouseX >= (double)(this.f_97735_ + 8)
         && mouseX <= (double)(this.f_97735_ + 168)
         && mouseY >= (double)(this.f_97736_ + 78)
         && mouseY <= (double)(this.f_97736_ + 142)) {
         List<String> moduleNames = ((DeviceHubMenu)this.f_97732_).getModuleNameKeys();
         this.scrollOffset = scroll(this.scrollOffset, delta);
         this.scrollOffset = DeviceHubDisplayRules.clampScrollOffset(this.scrollOffset, moduleNames.size(), 4);
         return true;
      } else {
         int configCount = ((DeviceHubMenu)this.f_97732_).getSelectedTab() == 4 ? 6 : this.moduleConfigCount();
         if (configCount > 0
            && mouseX >= (double)(this.f_97735_ + 8)
            && mouseX <= (double)(this.f_97735_ + 175)
            && mouseY >= (double)(this.f_97736_ + 144)
            && mouseY <= (double)(this.f_97736_ + 223)) {
            this.configScrollOffset = scroll(this.configScrollOffset, delta);
            this.configScrollOffset = DeviceHubDisplayRules.clampScrollOffset(this.configScrollOffset, configCount, 3);
            return true;
         } else {
            return super.m_6050_(mouseX, mouseY, delta);
         }
      }
   }

   private void renderConfigScrollBar(GuiGraphics gfx, int configCount, int mouseX, int mouseY) {
      this.renderScrollBar(gfx, configCount, 3, this.configScrollOffset, 10, 160, 46, mouseX, mouseY);
   }

   private void renderScrollBar(GuiGraphics gfx, int itemCount, int visibleRows, int offset, int relativeX, int relativeY, int height, int mouseX, int mouseY) {
      int thumbRange = height - 15;
      int thumbY = this.f_97736_ + relativeY + thumbRange * offset / Math.max(1, itemCount - visibleRows);
      int thumbX = this.f_97735_ + relativeX;
      boolean hovered = mouseX >= thumbX && mouseX < thumbX + 7 && mouseY >= thumbY && mouseY < thumbY + 15;
      gfx.m_280163_(TEXTURE, thumbX, thumbY, 180.0F, hovered ? 17.0F : 0.0F, 7, hovered ? 14 : 15, 256, 256);
   }

   private void resetConfigScrollWhenSelectionChanges(int selectedTab, int selectedModule) {
      if (selectedTab != this.lastConfigTab || selectedModule != this.lastConfigModule) {
         this.lastConfigTab = selectedTab;
         this.lastConfigModule = selectedModule;
         this.configScrollOffset = 0;
      }
   }

   private static int scroll(int offset, double scrollY) {
      if (scrollY > 0.0 && offset > 0) {
         return offset - 1;
      } else {
         return scrollY < 0.0 ? offset + 1 : offset;
      }
   }

   private void renderTabTooltips(GuiGraphics gfx, int mouseX, int mouseY, int tabMask) {
      for (int i = 0; i < 5; i++) {
         int tx = this.f_97735_ + TAB_X[i];
         boolean available = (tabMask & 1 << i) != 0;
         if (mouseX >= tx && mouseX <= tx + 31 && mouseY >= this.f_97736_ + 0 && mouseY <= this.f_97736_ + 0 + 25) {
            if (available) {
               ItemStack stack = this.tabStack(i);
               if (!stack.m_41619_()) {
                  gfx.m_280153_(this.f_96547_, stack, mouseX, mouseY);
               } else {
                  gfx.m_280557_(this.f_96547_, Component.m_237115_(TAB_LABEL_KEYS[i]), mouseX, mouseY);
               }
            } else {
               gfx.m_280557_(this.f_96547_, Component.m_237115_(TAB_REQUIRED_KEYS[i]), mouseX, mouseY);
            }

            return;
         }
      }
   }

   private void cycleTab(int dir) {
      int current = ((DeviceHubMenu)this.f_97732_).getSelectedTab();
      int tabMask = ((DeviceHubMenu)this.f_97732_).getTabAvailability();

      for (int attempt = 0; attempt < 5; attempt++) {
         current = (current + dir + 5) % 5;
         if ((tabMask & 1 << current) != 0) {
            NetworkInit.sendToServer(new DeviceHubActionPacket(0, current));
            this.playClick();
            return;
         }
      }
   }

   private void playClick() {
      if (this.f_96541_ != null && this.f_96541_.m_91106_() != null) {
         this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_119752_((SoundEvent)SoundEvents.f_12490_.m_203334_(), 1.0F));
      }
   }

   private static String truncate(Font font, String text, int maxWidth) {
      if (maxWidth <= 0) {
         return "";
      } else if (font.m_92895_(text) <= maxWidth) {
         return text;
      } else {
         int ellipsisWidth = font.m_92895_("...");
         return font.m_92834_(text, Math.max(0, maxWidth - ellipsisWidth)) + "...";
      }
   }

   private ItemStack selectedDeviceStack() {
      return this.tabStack(((DeviceHubMenu)this.f_97732_).getSelectedTab());
   }

   private ItemStack tabDisplayStack(int tab) {
      ItemStack equipped = this.tabStack(tab);
      return equipped.m_41619_() ? defaultTabStack(tab) : equipped;
   }

   private static ItemStack defaultTabStack(int tab) {
      return switch (tab) {
         case 0 -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_OCULUS.get());
         case 1 -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_CORE.get());
         case 2 -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_CONDUIT.get());
         case 3 -> new ItemStack((ItemLike)ModItems.CELESTWEAVE_STRIDE.get());
         case 4 -> new ItemStack((ItemLike)ModItems.ELECTROMAGNETIC_RAILGUN.get());
         default -> ItemStack.f_41583_;
      };
   }

   private ItemStack tabStack(int tab) {
      if (this.f_96541_ != null && this.f_96541_.f_91074_ != null) {
         Player player = this.f_96541_.f_91074_;

         return switch (tab) {
            case 0 -> armorStack(player, EquipmentSlot.HEAD);
            case 1 -> armorStack(player, EquipmentSlot.CHEST);
            case 2 -> armorStack(player, EquipmentSlot.LEGS);
            case 3 -> armorStack(player, EquipmentSlot.FEET);
            case 4 -> railgunStack(player);
            default -> ItemStack.f_41583_;
         };
      } else {
         return ItemStack.f_41583_;
      }
   }

   private static ItemStack armorStack(Player player, EquipmentSlot slot) {
      ItemStack stack = player.m_6844_(slot);
      return stack.m_41720_() instanceof BaseCelestweaveArmorItem ? stack : ItemStack.f_41583_;
   }

   private static ItemStack railgunStack(Player player) {
      ItemStack main = player.m_21205_();
      if (main.m_41720_() instanceof ElectromagneticRailgunItem) {
         return main;
      } else {
         ItemStack offhand = player.m_21206_();
         return offhand.m_41720_() instanceof ElectromagneticRailgunItem ? offhand : ItemStack.f_41583_;
      }
   }
}
