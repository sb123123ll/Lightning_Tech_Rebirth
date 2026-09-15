package com.moakiee.ae2lt.client.gui;

import com.moakiee.ae2lt.menu.LargeStackAppEngSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class LargeStackCountRenderer {
   private static final float SCALE = 0.75F;
   private static final int SHADOW_COLOR = 4276052;
   private static final int TEXT_COLOR = 16777215;

   private LargeStackCountRenderer() {
   }

   public static void renderSlotCount(GuiGraphics guiGraphics, Font font, Slot slot) {
      if (slot instanceof LargeStackAppEngSlot) {
         ItemStack stack = slot.m_7993_();
         long amount = ((LargeStackAppEngSlot)slot).getDisplayedAmount();
         if (!stack.m_41619_() && amount > 1L) {
            String text = formatCount(amount);
            renderLabel(guiGraphics, font, slot.f_40220_, slot.f_40221_, text);
         }
      }
   }

   public static void renderCountAt(GuiGraphics guiGraphics, Font font, int slotX, int slotY, long count) {
      if (count > 1L) {
         renderLabel(guiGraphics, font, slotX, slotY, formatCount(count));
      }
   }

   public static void appendCountTooltip(List<Component> lines, Slot slot) {
      if (slot instanceof LargeStackAppEngSlot && slot.m_6657_()) {
         appendCountTooltip(lines, ((LargeStackAppEngSlot)slot).getDisplayedAmount());
      }
   }

   public static void appendCountTooltip(List<Component> lines, long count) {
      if (count > 1L) {
         lines.add(Component.m_237110_("ae2lt.gui.slot_count", new Object[]{String.format("%,d", count)}).m_130940_(ChatFormatting.GRAY));
      }
   }

   public static void appendCountTooltip(ITooltipBuilder tooltip, long count) {
      if (count > 1L) {
         tooltip.add(Component.m_237110_("ae2lt.gui.slot_count", new Object[]{String.format("%,d", count)}).m_130940_(ChatFormatting.GRAY));
      }
   }

   public static String formatCount(long count) {
      if (count < 1000L) {
         return Long.toString(count);
      } else if (count < 1000000L) {
         return formatWithSuffix(count, 1000L, "K");
      } else {
         return count < 1000000000L ? formatWithSuffix(count, 1000000L, "M") : formatWithSuffix(count, 1000000000L, "B");
      }
   }

   private static String formatWithSuffix(long count, long divisor, String suffix) {
      double value = (double)count / (double)divisor;
      if (value < 10.0) {
         String formatted = String.format("%.1f", value);
         if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
         }

         return formatted + suffix;
      } else {
         return Math.round(value) + suffix;
      }
   }

   private static void renderLabel(GuiGraphics guiGraphics, Font font, int slotX, int slotY, String text) {
      float inverseScale = 1.3333334F;
      int drawX = (int)(((float)slotX + 18.0F - (float)font.m_92895_(text) * 0.75F) * inverseScale);
      int drawY = (int)(((float)slotY + 16.0F - 3.75F) * inverseScale);
      PoseStack pose = guiGraphics.m_280168_();
      pose.m_85836_();
      pose.m_252880_(0.0F, 0.0F, 300.0F);
      pose.m_85841_(0.75F, 0.75F, 0.75F);
      drawShadowedText(pose.m_85850_().m_252922_(), font, drawX, drawY, text);
      pose.m_85849_();
   }

   private static void drawShadowedText(Matrix4f matrix, Font font, int x, int y, String text) {
      BufferSource buffer = Minecraft.m_91087_().m_91269_().m_110104_();
      font.m_271703_(text, (float)(x + 1), (float)(y + 1), 4276052, false, matrix, buffer, DisplayMode.NORMAL, 0, 15728880);
      font.m_271703_(text, (float)x, (float)y, 16777215, false, matrix, buffer, DisplayMode.NORMAL, 0, 15728880);
      buffer.m_109911_();
   }
}
