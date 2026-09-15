package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import com.moakiee.ae2lt.me.cell.VoidCellMode;
import java.util.List;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

final class VoidCellModeButton extends IconButton {
   private final VoidCellMode mode;

   VoidCellModeButton(VoidCellMode mode, OnPress onPress) {
      super(onPress);
      this.mode = mode;
   }

   protected Icon getIcon() {
      return switch (this.mode) {
         case TRASH -> Icon.CONDENSER_OUTPUT_TRASH;
         case MATTER_BALLS -> Icon.CONDENSER_OUTPUT_MATTER_BALL;
         case SINGULARITY -> Icon.CONDENSER_OUTPUT_SINGULARITY;
      };
   }

   public List<Component> getTooltipMessage() {
      return List.of(Component.m_237115_("gui.ae2lt.void_cell.mode." + this.mode.ordinal()));
   }
}
