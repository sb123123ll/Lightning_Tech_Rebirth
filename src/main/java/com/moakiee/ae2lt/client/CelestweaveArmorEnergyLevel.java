package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.celestweave.ArmorEnergyBuffer;
import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class CelestweaveArmorEnergyLevel implements IGuiOverlay {
   public static final CelestweaveArmorEnergyLevel INSTANCE = new CelestweaveArmorEnergyLevel();
   private static final int BAR_WIDTH = 81;
   private static final int BAR_HEIGHT = 6;
   private static final int INNER_WIDTH = 79;
   private static final int INNER_HEIGHT = 4;
   private static final ResourceLocation BAR_BASE = new ResourceLocation("ae2lt", "textures/gui/hud/hud_bar.png");
   private static final ResourceLocation BAR_OVERLAY = new ResourceLocation("ae2lt", "textures/gui/hud/hud_bar_overlay.png");

   private CelestweaveArmorEnergyLevel() {
   }

   public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
      Minecraft minecraft = Minecraft.m_91087_();
      if (minecraft.f_91074_ != null && minecraft.f_91072_ != null && minecraft.f_91072_.m_105205_() && !minecraft.f_91066_.f_92062_) {
         long capacity = 0L;
         long stored = 0L;

         for (ItemStack stack : minecraft.f_91074_.m_6168_()) {
            if (stack.m_41720_() instanceof BaseCelestweaveArmorItem) {
               capacity = addClamped(capacity, ArmorEnergyBuffer.capacity(stack));
               stored = addClamped(stored, ArmorEnergyBuffer.read(stack));
            }
         }

         if (capacity > 0L) {
            int x = width / 2 - 91;
            int y = height - gui.leftHeight + 2;
            int length = Mth.m_14045_((int)Math.round((double)Math.min(stored, capacity) / (double)capacity * 79.0), 0, 79);
            guiGraphics.m_280411_(BAR_BASE, x, y, 81, 6, 0.0F, 0.0F, 81, 6, 81, 6);
            if (length > 0) {
               guiGraphics.m_280411_(BAR_OVERLAY, x + 1, y + 1, length, 4, 1.0F, 1.0F, length, 4, 81, 6);
            }

            gui.leftHeight += 8;
         }
      }
   }

   private static long addClamped(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
      }
   }
}
