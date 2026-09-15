package com.moakiee.ae2lt.client;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public class OverloadProcessingFactoryOutputButton extends IconButton {
   private final Component sideLabel;
   private ItemStack display = ItemStack.f_41583_;
   private boolean on;

   public OverloadProcessingFactoryOutputButton(Component sideLabel, OnPress onPress) {
      super(onPress);
      this.sideLabel = sideLabel;
   }

   public void setDisplay(@Nullable ItemLike itemLike) {
      this.display = itemLike == null ? ItemStack.f_41583_ : new ItemStack(itemLike);
   }

   public void setOn(boolean on) {
      this.on = on;
   }

   protected Icon getIcon() {
      return Icon.TOOLBAR_BUTTON_BACKGROUND;
   }

   protected Item getItemOverlay() {
      return this.display.m_41619_() ? null : this.display.m_41720_();
   }

   public List<Component> getTooltipMessage() {
      return List.of(
         this.sideLabel, Component.m_237115_(this.on ? "ae2lt.gui.overload_factory.output_side.enabled" : "ae2lt.gui.overload_factory.output_side.disabled")
      );
   }

   public void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
      if (this.f_93624_) {
         OutputSideButtonStyle.renderBackground(guiGraphics, this.m_252754_(), this.m_252907_(), this.on);
         if (!this.display.m_41619_()) {
            guiGraphics.m_280064_(this.display, this.m_252754_() + 1, this.m_252907_() + 1, 0, 3);
         }
      }
   }
}
