package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.SwitchGuisPacket;
import appeng.menu.implementations.PriorityMenu;
import com.moakiee.ae2lt.logic.tianshu.CpuMainCoreTier;
import com.moakiee.ae2lt.logic.tianshu.TianshuMultiblockScanIssue;
import com.moakiee.ae2lt.menu.TianshuSupercomputerControllerMenu;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.TianshuControllerActionPacket;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

public class TianshuSupercomputerControllerScreen extends MultiblockControllerScreen<TianshuSupercomputerControllerMenu> {
   public TianshuSupercomputerControllerScreen(TianshuSupercomputerControllerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
   }

   protected void m_7856_() {
      super.m_7856_();
      int x = this.f_97735_ - 18;
      int y = this.f_97736_;
      TextureToggleButton build = new TextureToggleButton(
         TextureToggleButton.ButtonType.QUICK_BUILD, state -> this.sendAction(TianshuControllerActionPacket.Action.AUTO_BUILD)
      );
      build.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.tianshu.gui.build")));
      build.m_264152_(x, y);
      this.m_142416_(build);
      TextureToggleButton selection = new TextureToggleButton(
         TextureToggleButton.ButtonType.CPU_SELECTION, state -> this.sendAction(TianshuControllerActionPacket.Action.OPEN_ALGORITHM_SELECTION)
      );
      selection.m_257544_(Tooltip.m_257550_(Component.m_237115_("ae2lt.tianshu.gui.algorithm_selection")));
      selection.m_264152_(x, y + 22);
      this.m_142416_(selection);
      MutableComponent cpuPriorityLabel = Component.m_237115_("ae2lt.tianshu.gui.cpu_priority");
      TabButton priority = new TabButton(
         Icon.WRENCH, cpuPriorityLabel, ignored -> NetworkHandler.instance().sendToServer(SwitchGuisPacket.openSubMenu(PriorityMenu.TYPE))
      );
      priority.m_257544_(Tooltip.m_257550_(cpuPriorityLabel));
      priority.m_264152_(this.f_97735_ + 185, this.f_97736_ - 5);
      this.m_142416_(priority);
   }

   private void sendAction(TianshuControllerActionPacket.Action action) {
      NetworkInit.sendToServer(
         new TianshuControllerActionPacket(
            ((TianshuSupercomputerControllerMenu)this.f_97732_).token(), ((TianshuSupercomputerControllerMenu)this.f_97732_).getBlockPos(), action
         )
      );
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawTitle(graphics);
      if (!((TianshuSupercomputerControllerMenu)this.f_97732_).isFormed()) {
         this.drawStatus(graphics, Component.m_237115_("ae2lt.tianshu.gui.unformed"), 8019476);
         this.drawUnformed(graphics, this.issueText(), "ae2lt.matrix.gui.hint_unformed");
      } else {
         this.drawStatus(graphics, Component.m_237110_("ae2lt.tianshu.gui.formed", new Object[]{this.tierName()}), 1862452);
         int y = 56;
         this.drawRow(
            graphics,
            y,
            Component.m_237115_("ae2lt.tianshu.gui.label_storage"),
            formatStorage(((TianshuSupercomputerControllerMenu)this.f_97732_).getStorageBytes()),
            1316383
         );
         y += 13;
         boolean capped = ((TianshuSupercomputerControllerMenu)this.f_97732_).isCapped();
         this.drawRow(
            graphics,
            y,
            Component.m_237115_("ae2lt.tianshu.gui.label_dispatches"),
            formatCount((long)((TianshuSupercomputerControllerMenu)this.f_97732_).getSuccessfulDispatchesPerTick())
               + "/t"
               + (capped ? I18n.m_118938_("ae2lt.tianshu.gui.capped", new Object[0]) : ""),
            capped ? 8019476 : 1316383
         );
         y += 13;
         this.drawRow(
            graphics,
            y,
            Component.m_237115_("ae2lt.tianshu.gui.label_loop"),
            I18n.m_118938_(
               "ae2lt.tianshu.gui.value_loop",
               new Object[]{
                  formatCount((long)((TianshuSupercomputerControllerMenu)this.f_97732_).getClosedLoopPatternStorages()),
                  formatCount((long)((TianshuSupercomputerControllerMenu)this.f_97732_).getClosedLoopSeedStorages())
               }
            ),
            1316383
         );
      }
   }

   private Component issueText() {
      int ordinal = ((TianshuSupercomputerControllerMenu)this.f_97732_).getIssue();
      TianshuMultiblockScanIssue[] values = TianshuMultiblockScanIssue.values();
      String name = ordinal >= 0 && ordinal < values.length ? values[ordinal].name().toLowerCase(Locale.ROOT) : "unknown";
      return Component.m_237115_("ae2lt.tianshu.issue." + name);
   }

   private Component tierName() {
      CpuMainCoreTier tier = ((TianshuSupercomputerControllerMenu)this.f_97732_).getTier();
      return tier == null ? Component.m_237113_("—") : Component.m_237115_("ae2lt.tianshu.tier." + tier.name().toLowerCase(Locale.ROOT));
   }

   private static String formatStorage(long bytes) {
      if (bytes == Long.MAX_VALUE) {
         return "∞";
      } else {
         return bytes >= 1073741824L
            ? String.format(Locale.ROOT, "%.2f GiB", (double)bytes / 1.0737418E9F)
            : String.format(Locale.ROOT, "%.0f MiB", (double)bytes / 1048576.0);
      }
   }
}
