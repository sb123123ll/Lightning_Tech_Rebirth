package com.moakiee.ae2lt.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import com.moakiee.ae2lt.client.gui.LightningStatusIconWidget;
import com.moakiee.ae2lt.client.gui.LightningStatusLines;
import com.moakiee.ae2lt.menu.LightningCollectorMenu;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LightningCollectorScreen extends AEBaseScreen<LightningCollectorMenu> {
   private final ProgressBar progressBar;

   public LightningCollectorScreen(LightningCollectorMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
      super(menu, playerInventory, title, style);
      this.addToLeftToolbar(FrequencyBindingClient.createToolbarButton(menu));
      this.progressBar = new ProgressBar(menu, style.getImage("progressBar"), Direction.VERTICAL);
      this.widgets.add("progressBar", this.progressBar);
      this.widgets
         .add(
            "lightningStatus",
            new LightningStatusIconWidget(
               () -> List.of(
                     LightningStatusLines.title(),
                     Component.m_237110_("gui.ae2lt.lightning_collector.high_output", new Object[]{formatRange(menu.previewHighMin, menu.previewHighMax)}),
                     Component.m_237110_(
                        "gui.ae2lt.lightning_collector.extreme_output.simple", new Object[]{formatRange(menu.previewExtremeMin, menu.previewExtremeMax)}
                     )
                  )
            )
         );
   }

   protected void updateBeforeRender() {
      super.updateBeforeRender();
      int progress = ((LightningCollectorMenu)this.f_97732_).getCurrentProgress() * 100 / ((LightningCollectorMenu)this.f_97732_).getMaxProgress();
      this.progressBar.setFullMsg(Component.m_237113_(progress + "%"));
   }

   private static String formatRange(int min, int max) {
      return min == max ? Integer.toString(min) : min + "~" + max;
   }
}
