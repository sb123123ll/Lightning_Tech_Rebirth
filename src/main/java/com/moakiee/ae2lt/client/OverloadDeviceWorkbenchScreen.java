package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.menu.OverloadDeviceWorkbenchMenu;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class OverloadDeviceWorkbenchScreen extends AbstractContainerScreen<OverloadDeviceWorkbenchMenu> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("ae2lt", "textures/gui/overload_workplace_gui.png");
   private static final int TEXTURE_WIDTH = 320;
   private static final int TEXTURE_HEIGHT = 256;
   private static final int GUI_WIDTH = 176;
   private static final int GUI_HEIGHT = 245;
   private static final int TEXT_ON_LIGHT_BG = -9538420;
   private static final int TEXT_ON_DARK_BG = -1;
   private static final int STATUS_X = 44;
   private static final int STATUS_Y = 22;
   private static final int STATUS_SECOND_LINE_Y = 14;
   private static final int MODULE_HEADER_X = 42;
   private static final int MODULE_HEADER_Y = 49;
   private static final int MODULE_ROW_X = 42;
   private static final int MODULE_ROW_Y = 60;
   private static final int MODULE_ROW_WIDTH = 118;
   private static final int MODULE_ROW_HEIGHT = 17;
   private static final int MODULE_ICON_X = 44;
   private static final int MODULE_NAME_X = 67;
   private static final int MODULE_ITEM_Y_OFFSET = 1;
   private static final int MODULE_TEXT_Y_OFFSET = 5;
   private static final int VISIBLE_ROWS = 5;
   private static final int MODULE_ROW_SRC_X = 180;
   private static final int MODULE_ROW_SRC_Y = 90;
   private static final int MODULE_ROW_SELECTED_SRC_Y = 111;
   private static final int REMOVE_BUTTON_SIZE = 10;
   private static final int REMOVE_BUTTON_X = 148;
   private static final int REMOVE_BUTTON_Y_OFFSET = 3;
   private static final int REMOVE_BUTTON_SRC_X = 191;
   private static final int REMOVE_BUTTON_SRC_Y = 5;
   private static final int REMOVE_BUTTON_HOVER_SRC_X = 202;
   private static final int REMOVE_BUTTON_HOVER_SRC_Y = 6;
   private static final int REMOVE_BUTTON_WIDTH = 9;
   private static final int REMOVE_BUTTON_HEIGHT = 10;
   private static final int REMOVE_BUTTON_HOVER_HEIGHT = 9;
   private static final int SCROLLBAR_X = 164;
   private static final int SCROLLBAR_Y = 61;
   private static final int SCROLLBAR_WIDTH = 7;
   private static final int SCROLLBAR_HEIGHT = 82;
   private static final int SCROLLBAR_THUMB_SRC_X = 180;
   private static final int SCROLLBAR_THUMB_SRC_Y = 0;
   private static final int SCROLLBAR_THUMB_HOVER_SRC_Y = 17;
   private static final int SCROLLBAR_THUMB_HEIGHT = 15;
   private static final int SCROLLBAR_THUMB_HOVER_HEIGHT = 14;
   private static final int ARROW_PROGRESS_X = 8;
   private static final int ARROW_PROGRESS_Y = 97;
   private static final int ARROW_PROGRESS_WIDTH = 28;
   private static final int ARROW_PROGRESS_HEIGHT = 38;
   private static final int ARROW_PROGRESS_SRC_X = 180;
   private static final int ARROW_PROGRESS_SRC_Y = 48;
   private static final int ARROW_PROGRESS_VISIBLE_OFFSET_X = 9;
   private static final int ARROW_PROGRESS_VISIBLE_WIDTH = 16;
   private int scrollOffset = 0;

   public OverloadDeviceWorkbenchScreen(OverloadDeviceWorkbenchMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 176;
      this.f_97727_ = 245;
      this.f_97730_ = 8;
      this.f_97731_ = 151;
   }

   protected void m_7286_(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
      gfx.m_280163_(TEXTURE, this.f_97735_, this.f_97736_, 0.0F, 0.0F, 176, 245, 320, 256);
      this.renderStatusArea(gfx);
      this.renderInstallProgress(gfx);
      this.renderModuleList(gfx, mouseX, mouseY);
   }

   private void renderStatusArea(GuiGraphics gfx) {
      int x = this.f_97735_ + 44;
      int y = this.f_97736_ + 22;
      if (!((OverloadDeviceWorkbenchMenu)this.f_97732_).hasDeviceInserted()) {
         gfx.m_280614_(this.f_96547_, Component.m_237115_("ae2lt.overload_device_workbench.status.no_device"), x, y + 7, -1, false);
      } else {
         gfx.m_280614_(this.f_96547_, ((OverloadDeviceWorkbenchMenu)this.f_97732_).getStatusText(), x, y, -1, false);
         boolean grid = ((OverloadDeviceWorkbenchMenu)this.f_97732_).gridConnected != 0;
         Component gridText = grid
            ? Component.m_237115_("ae2lt.overload_device_workbench.screen.network.online")
            : Component.m_237115_("ae2lt.overload_device_workbench.screen.network.offline");
         gfx.m_280614_(this.f_96547_, gridText, x, y + 14, -1, false);
      }
   }

   private void renderInstallProgress(GuiGraphics gfx) {
      if (((OverloadDeviceWorkbenchMenu)this.f_97732_).installProgress > 0) {
         double ratio = (double)((OverloadDeviceWorkbenchMenu)this.f_97732_).installProgress / 20.0;
         int visible = Math.max(1, (int)Math.ceil(16.0 * ratio));
         int filled = Math.min(28, 9 + visible);
         gfx.m_280163_(TEXTURE, this.f_97735_ + 8, this.f_97736_ + 97, 180.0F, 48.0F, filled, 38, 320, 256);
      }
   }

   private void renderModuleList(GuiGraphics gfx, int mouseX, int mouseY) {
      List<ItemStack> modules = ((OverloadDeviceWorkbenchMenu)this.f_97732_).getInstalledModuleList();
      int listLeft = this.f_97735_ + 42;
      int listTop = this.f_97736_ + 60;
      int listRight = listLeft + 118;
      this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, Math.max(0, modules.size() - 5)));
      Component header = Component.m_237110_("ae2lt.overload_device_workbench.screen.module_types", new Object[]{modules.size()});
      gfx.m_280614_(this.f_96547_, header, this.f_97735_ + 42, this.f_97736_ + 49, -9538420, false);

      for (int row = 0; row < 5; row++) {
         int moduleIndex = this.scrollOffset + row;
         if (moduleIndex < modules.size()) {
            int rowY = listTop + row * 17;
            boolean hovered = mouseX >= listLeft && mouseX < listRight && mouseY >= rowY && mouseY < rowY + 17;
            this.renderModuleRowFrame(gfx, rowY, hovered);
            ItemStack stack = modules.get(moduleIndex);
            gfx.m_280480_(stack, this.f_97735_ + 44, rowY + 1);
            int cap = ((OverloadDeviceWorkbenchMenu)this.f_97732_).getModuleMaxInstallAmount(stack);
            String amount = cap > 0 ? "x" + stack.m_41613_() + "/" + cap : "x" + stack.m_41613_();
            int amountWidth = this.f_96547_.m_92895_(amount);
            int amountX = 148 - amountWidth - 4;
            int rowTextColor = hovered ? -9538420 : -1;
            gfx.m_280614_(this.f_96547_, Component.m_237113_(amount), this.f_97735_ + amountX, rowY + 5, rowTextColor, false);
            int nameX = this.f_97735_ + 67;
            int nameMaxWidth = this.f_97735_ + amountX - nameX - 3;
            gfx.m_280614_(
               this.f_96547_, Component.m_237113_(truncate(this.f_96547_, stack.m_41786_().getString(), nameMaxWidth)), nameX, rowY + 5, rowTextColor, false
            );
            this.renderRemoveButton(gfx, this.f_97735_ + 148, rowY + 3, mouseX, mouseY);
         }
      }

      if (modules.size() > 5) {
         this.renderScrollBar(gfx, modules.size(), mouseX, mouseY);
      }
   }

   private void renderModuleRowFrame(GuiGraphics gfx, int rowY, boolean selected) {
      gfx.m_280163_(TEXTURE, this.f_97735_ + 42, rowY, 180.0F, selected ? 111.0F : 90.0F, 118, 17, 320, 256);
   }

   private void renderRemoveButton(GuiGraphics gfx, int x, int y, int mouseX, int mouseY) {
      boolean hovered = mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 10;
      gfx.m_280163_(TEXTURE, x, y, hovered ? 202.0F : 191.0F, hovered ? 6.0F : 5.0F, 9, hovered ? 9 : 10, 320, 256);
   }

   private void renderScrollBar(GuiGraphics gfx, int moduleCount, int mouseX, int mouseY) {
      int barX = this.f_97735_ + 164;
      int barTop = this.f_97736_ + 61;
      int thumbSpace = 67;
      int thumbY = barTop + thumbSpace * this.scrollOffset / Math.max(1, moduleCount - 5);
      boolean hovered = mouseX >= barX && mouseX < barX + 7 && mouseY >= thumbY && mouseY < thumbY + 15;
      gfx.m_280163_(TEXTURE, barX, thumbY, 180.0F, hovered ? 17.0F : 0.0F, 7, hovered ? 14 : 15, 320, 256);
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

   public void m_88315_(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(gfx);
      super.m_88315_(gfx, mouseX, mouseY, partialTick);
      this.renderModuleRowTooltip(gfx, mouseX, mouseY);
      this.m_280072_(gfx, mouseX, mouseY);
   }

   protected void m_280003_(GuiGraphics gfx, int mouseX, int mouseY) {
      gfx.m_280614_(this.f_96547_, Component.m_237115_("block.ae2lt.overload_device_workbench"), 42, 6, -9538420, false);
      gfx.m_280614_(this.f_96547_, this.f_169604_, this.f_97730_, this.f_97731_, -9538420, false);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      return button == 0 && this.handleModuleListClick(mouseX, mouseY) ? true : super.m_6375_(mouseX, mouseY, button);
   }

   private boolean handleModuleListClick(double mouseX, double mouseY) {
      List<ItemStack> modules = ((OverloadDeviceWorkbenchMenu)this.f_97732_).getInstalledModuleList();

      for (int row = 0; row < 5; row++) {
         int moduleIndex = this.scrollOffset + row;
         if (moduleIndex >= modules.size()) {
            break;
         }

         int rowY = this.f_97736_ + 60 + row * 17 + 3;
         int buttonX = this.f_97735_ + 148;
         if (mouseX >= (double)buttonX && mouseX < (double)(buttonX + 10) && mouseY >= (double)rowY && mouseY < (double)(rowY + 10)) {
            ((OverloadDeviceWorkbenchMenu)this.f_97732_).requestUninstall(moduleIndex, m_96638_());
            return true;
         }
      }

      return false;
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      int listLeft = this.f_97735_ + 42;
      int listTop = this.f_97736_ + 60;
      int listRight = this.f_97735_ + 164 + 7;
      int listBottom = listTop + 85;
      if (mouseX >= (double)listLeft && mouseX < (double)listRight && mouseY >= (double)listTop && mouseY < (double)listBottom) {
         int max = Math.max(0, ((OverloadDeviceWorkbenchMenu)this.f_97732_).getInstalledModuleList().size() - 5);
         if (delta > 0.0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
         } else if (delta < 0.0) {
            this.scrollOffset = Math.min(max, this.scrollOffset + 1);
         }

         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   private void renderModuleRowTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
      List<ItemStack> modules = ((OverloadDeviceWorkbenchMenu)this.f_97732_).getInstalledModuleList();
      int listLeft = this.f_97735_ + 42;
      int listTop = this.f_97736_ + 60;
      int listRight = listLeft + 118;

      for (int row = 0; row < 5; row++) {
         int moduleIndex = this.scrollOffset + row;
         if (moduleIndex >= modules.size()) {
            break;
         }

         int rowY = listTop + row * 17;
         int buttonX = this.f_97735_ + 148;
         int buttonY = rowY + 3;
         if (mouseX >= buttonX && mouseX < buttonX + 10 && mouseY >= buttonY && mouseY < buttonY + 10) {
            gfx.m_280666_(
               this.f_96547_,
               List.of(
                  Component.m_237115_("ae2lt.overload_device_workbench.screen.uninstall_one"),
                  Component.m_237115_("ae2lt.overload_device_workbench.screen.uninstall_all")
               ),
               mouseX,
               mouseY
            );
            return;
         }

         if (mouseX >= listLeft && mouseX < listRight && mouseY >= rowY && mouseY < rowY + 17) {
            gfx.m_280153_(this.f_96547_, modules.get(moduleIndex), mouseX, mouseY);
            return;
         }
      }
   }
}
