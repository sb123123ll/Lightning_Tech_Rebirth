package com.moakiee.ae2lt.client;

import appeng.api.client.AEKeyRenderHandler;
import appeng.client.gui.style.Blitter;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public final class LightningKeyRenderHandler implements AEKeyRenderHandler<LightningKey> {
   public static final LightningKeyRenderHandler INSTANCE = new LightningKeyRenderHandler();
   private static final ResourceLocation HIGH_VOLTAGE_SPRITE = new ResourceLocation("ae2lt", "item/high_voltage_lightning");
   private static final ResourceLocation EXTREME_HIGH_VOLTAGE_SPRITE = new ResourceLocation("ae2lt", "item/extreme_high_voltage_lightning");

   private LightningKeyRenderHandler() {
   }

   private static TextureAtlasSprite spriteFor(LightningKey stack) {
      ResourceLocation id = stack.tier() == LightningKey.Tier.EXTREME_HIGH_VOLTAGE ? EXTREME_HIGH_VOLTAGE_SPRITE : HIGH_VOLTAGE_SPRITE;
      return (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(InventoryMenu.f_39692_).apply(id);
   }

   public void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, LightningKey stack) {
      Blitter.sprite(spriteFor(stack)).dest(x, y, 16, 16).blit(guiGraphics);
   }

   public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, LightningKey what, float scale, int combinedLight, Level level) {
      TextureAtlasSprite sprite = spriteFor(what);
      poseStack.m_85836_();
      poseStack.m_252880_(0.0F, 0.0F, 0.01F);
      VertexConsumer buffer = buffers.m_6299_(RenderType.m_110463_());
      scale -= 0.05F;
      float x0 = -scale / 2.0F;
      float y0 = scale / 2.0F;
      float x1 = scale / 2.0F;
      float y1 = -scale / 2.0F;
      Matrix4f transform = poseStack.m_85850_().m_252922_();
      buffer.m_252986_(transform, x0, y1, 0.0F)
         .m_193479_(-1)
         .m_7421_(sprite.m_118409_(), sprite.m_118412_())
         .m_86008_(OverlayTexture.f_118083_)
         .m_85969_(combinedLight)
         .m_5601_(0.0F, 0.0F, 1.0F)
         .m_5752_();
      buffer.m_252986_(transform, x1, y1, 0.0F)
         .m_193479_(-1)
         .m_7421_(sprite.m_118410_(), sprite.m_118412_())
         .m_86008_(OverlayTexture.f_118083_)
         .m_85969_(combinedLight)
         .m_5601_(0.0F, 0.0F, 1.0F)
         .m_5752_();
      buffer.m_252986_(transform, x1, y0, 0.0F)
         .m_193479_(-1)
         .m_7421_(sprite.m_118410_(), sprite.m_118411_())
         .m_86008_(OverlayTexture.f_118083_)
         .m_85969_(combinedLight)
         .m_5601_(0.0F, 0.0F, 1.0F)
         .m_5752_();
      buffer.m_252986_(transform, x0, y0, 0.0F)
         .m_193479_(-1)
         .m_7421_(sprite.m_118409_(), sprite.m_118411_())
         .m_86008_(OverlayTexture.f_118083_)
         .m_85969_(combinedLight)
         .m_5601_(0.0F, 0.0F, 1.0F)
         .m_5752_();
      poseStack.m_85849_();
   }

   public Component getDisplayName(LightningKey stack) {
      return stack.getDisplayName();
   }
}
