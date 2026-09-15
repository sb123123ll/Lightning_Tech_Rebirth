package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class RailgunHudRenderer {
   private static final ResourceLocation EMPTY_TEX = new ResourceLocation("ae2lt", "textures/gui/hud/lightning_charging_bar.png");
   private static final ResourceLocation FULL_TEX = new ResourceLocation("ae2lt", "textures/gui/hud/lightning_charging_bar_full.png");
   private static final int ICON_W = 13;
   private static final int ICON_H = 19;
   private static final int CROSSHAIR_OFFSET_X = 10;

   private RailgunHudRenderer() {
   }

   @SubscribeEvent
   public static void onRenderGui(Post e) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91074_ != null) {
         if (mc.f_91074_.m_6117_()) {
            ItemStack stack = mc.f_91074_.m_21211_();
            if (stack.m_41720_() instanceof ElectromagneticRailgunItem) {
               long ticks = ModDataComponents.RAILGUN_CHARGE_TICKS.getOrDefault(stack, 0L);
               int t3 = 40;
               float progress = Math.min(1.0F, (float)ticks / (float)t3);
               GuiGraphics gfx = e.getGuiGraphics();
               int w = mc.m_91268_().m_85445_();
               int h = mc.m_91268_().m_85446_();
               int x = w / 2 + 10;
               int y = (h - 19) / 2;
               gfx.m_280163_(EMPTY_TEX, x, y, 0.0F, 0.0F, 13, 19, 13, 19);
               int filledH = Math.round(19.0F * progress);
               if (filledH > 0) {
                  int emptyTop = 19 - filledH;
                  gfx.m_280163_(FULL_TEX, x, y + emptyTop, 0.0F, (float)emptyTop, 13, filledH, 13, 19);
               }
            }
         }
      }
   }
}
