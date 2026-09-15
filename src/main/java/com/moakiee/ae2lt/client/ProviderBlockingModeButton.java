package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class ProviderBlockingModeButton extends IconButton {
   private static final int STATE_OFF = 0;
   private static final int STATE_NORMAL = 1;
   private static final int STATE_SAME_PATTERN = 2;
   private static final ResourceLocation SAME_PATTERN_TEXTURE = new ResourceLocation("ae2lt", "textures/gui/buttons/same_pattern_blocking_on.png");
   private int state;

   ProviderBlockingModeButton(OnPress onPress) {
      super(onPress);
   }

   void setState(int state) {
      this.state = Math.max(0, Math.min(2, state));
   }

   protected Icon getIcon() {
      return switch (this.state) {
         case 1 -> Icon.BLOCKING_MODE_YES;
         case 2 -> Icon.TOOLBAR_BUTTON_BACKGROUND;
         default -> Icon.BLOCKING_MODE_NO;
      };
   }

   public void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      boolean customIcon = this.state == 2;
      this.setDisableBackground(customIcon);
      boolean wasActive = this.f_93623_;
      if (customIcon) {
         this.f_93623_ = true;
      }

      super.m_87963_(guiGraphics, mouseX, mouseY, partialTick);
      this.f_93623_ = wasActive;
      if (customIcon && this.f_93624_) {
         Blitter blitter = Blitter.texture(SAME_PATTERN_TEXTURE, 16, 16).src(0, 0, 16, 16);
         if (!wasActive) {
            blitter.opacity(0.5F);
         }

         blitter.dest(this.m_252754_(), this.m_252907_()).blit(guiGraphics);
      }
   }

   public List<Component> getTooltipMessage() {
      return List.of(Component.m_237115_(switch (this.state) {
         case 1 -> "ae2lt.gui.blocking_mode.normal";
         case 2 -> "ae2lt.gui.blocking_mode.same_pattern";
         default -> "ae2lt.gui.blocking_mode.off";
      }));
   }
}
