package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import com.moakiee.ae2lt.machine.teslacoil.TeslaCoilMode;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class TeslaCoilModeButton extends IconButton {
   private static final ResourceLocation HV_TEXTURE = new ResourceLocation("ae2lt", "textures/gui/buttons/lightning.png");
   private static final ResourceLocation EHV_TEXTURE = new ResourceLocation("ae2lt", "textures/gui/buttons/lightning_high_voltage.png");
   private TeslaCoilMode mode = TeslaCoilMode.HIGH_VOLTAGE;

   public TeslaCoilModeButton(OnPress onPress) {
      super(onPress);
      this.setDisableBackground(true);
   }

   public void setMode(TeslaCoilMode mode) {
      this.mode = mode;
   }

   protected Icon getIcon() {
      return Icon.TOOLBAR_BUTTON_BACKGROUND;
   }

   protected Item getItemOverlay() {
      return null;
   }

   public List<Component> getTooltipMessage() {
      MutableComponent current = Component.m_237115_(
         "ae2lt.gui.tesla_coil.mode." + (this.mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE ? "extreme_high_voltage" : "high_voltage")
      );
      MutableComponent next = Component.m_237115_(
         "ae2lt.gui.tesla_coil.mode." + (this.mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE ? "high_voltage" : "extreme_high_voltage")
      );
      return List.of(
         Component.m_237110_("ae2lt.gui.tesla_coil.mode.button", new Object[]{current}),
         Component.m_237110_("ae2lt.gui.tesla_coil.mode.click_to_switch", new Object[]{next})
      );
   }

   public void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
      if (this.f_93624_) {
         boolean wasActive = this.f_93623_;
         this.f_93623_ = true;
         super.m_87963_(guiGraphics, mouseX, mouseY, partial);
         this.f_93623_ = wasActive;
         ResourceLocation texture = this.mode == TeslaCoilMode.EXTREME_HIGH_VOLTAGE ? EHV_TEXTURE : HV_TEXTURE;
         Blitter blitter = Blitter.texture(texture, 16, 16).src(0, 0, 16, 16);
         if (!wasActive) {
            blitter.opacity(0.5F);
         }

         blitter.dest(this.m_252754_(), this.m_252907_(), 16, 16).blit(guiGraphics);
      }
   }
}
