package com.moakiee.ae2lt.integration.emi;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

final class EmiLightningStrikePreviewWidget extends Widget {
   private static final float X_ROTATION_DEG = 30.0F;
   private static final float SCALE_MARGIN = 0.92F;
   private static final float ROTATION_DEGREES_PER_MS = 0.024F;
   private final Bounds bounds;
   private final List<EmiLightningStrikePreviewWidget.Entry> blocks;
   private final float centerX;
   private final float centerY;
   private final float centerZ;
   private final float scale;

   private EmiLightningStrikePreviewWidget(int x, int y, int width, int height, List<EmiLightningStrikePreviewWidget.Entry> blocks) {
      this.bounds = new Bounds(x, y, width, height);
      this.blocks = blocks;
      float minX = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      float minZ = Float.POSITIVE_INFINITY;
      float maxZ = Float.NEGATIVE_INFINITY;

      for (EmiLightningStrikePreviewWidget.Entry entry : blocks) {
         minX = Math.min(minX, (float)entry.offset.m_123341_() - 0.5F);
         maxX = Math.max(maxX, (float)entry.offset.m_123341_() + 0.5F);
         minY = Math.min(minY, (float)entry.offset.m_123342_() - 0.5F);
         maxY = Math.max(maxY, (float)entry.offset.m_123342_() + 0.5F);
         minZ = Math.min(minZ, (float)entry.offset.m_123343_() - 0.5F);
         maxZ = Math.max(maxZ, (float)entry.offset.m_123343_() + 0.5F);
      }

      this.centerX = (minX + maxX) * 0.5F;
      this.centerY = (minY + maxY) * 0.5F;
      this.centerZ = (minZ + maxZ) * 0.5F;
      float xRotCos = (float)Math.cos(Math.toRadians(30.0));
      float xRotSin = (float)Math.sin(Math.toRadians(30.0));
      float horizontalRadius = 0.0F;
      float verticalRadius = 0.0F;

      for (float xv : new float[]{minX - this.centerX, maxX - this.centerX}) {
         for (float yv : new float[]{minY - this.centerY, maxY - this.centerY}) {
            for (float zv : new float[]{minZ - this.centerZ, maxZ - this.centerZ}) {
               float horizontal = (float)Math.hypot((double)xv, (double)zv);
               horizontalRadius = Math.max(horizontalRadius, horizontal);
               verticalRadius = Math.max(verticalRadius, Math.abs(yv) * xRotCos + horizontal * xRotSin);
            }
         }
      }

      this.scale = !(horizontalRadius <= 0.0F) && !(verticalRadius <= 0.0F)
         ? Math.min((float)width * 0.5F / horizontalRadius, (float)height * 0.5F / verticalRadius) * 0.92F
         : 0.0F;
   }

   public Bounds getBounds() {
      return this.bounds;
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      if (!this.blocks.isEmpty() && !(this.scale <= 0.0F)) {
         Minecraft client = Minecraft.m_91087_();
         BufferSource bufferSource = client.m_91269_().m_110104_();
         BlockRenderDispatcher blockRenderer = client.m_91289_();
         PoseStack pose = graphics.m_280168_();
         float rotation = (float)Util.m_137550_() * 0.024F % 360.0F;

         for (EmiLightningStrikePreviewWidget.Entry entry : this.blocks) {
            pose.m_85836_();
            pose.m_252880_((float)this.bounds.x() + (float)this.bounds.width() / 2.0F, (float)this.bounds.y() + (float)this.bounds.height() / 2.0F, 400.0F);
            pose.m_85841_(this.scale, -this.scale, this.scale);
            pose.m_252781_(Axis.f_252529_.m_252977_(30.0F));
            pose.m_252781_(Axis.f_252436_.m_252977_(225.0F + rotation));
            pose.m_252880_(
               -0.5F + (float)entry.offset.m_123341_() - this.centerX,
               -0.5F + (float)entry.offset.m_123342_() - this.centerY,
               -0.5F + (float)entry.offset.m_123343_() - this.centerZ
            );
            RenderSystem.runAsFancy(() -> {
               if (entry.state.m_60799_() != RenderShape.ENTITYBLOCK_ANIMATED) {
                  blockRenderer.m_110912_(entry.state, pose, bufferSource, 15728880, OverlayTexture.f_118083_);
               }
            });
            pose.m_85849_();
         }

         bufferSource.m_109911_();
         Lighting.m_84931_();
      }
   }

   static final class Builder {
      private final int x;
      private final int y;
      private final int width;
      private final int height;
      private final List<EmiLightningStrikePreviewWidget.Entry> blocks = new ArrayList<>();

      Builder(int x, int y, int width, int height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      EmiLightningStrikePreviewWidget.Builder addBlock(Block block, BlockPos offset) {
         if (block != null) {
            this.blocks.add(new EmiLightningStrikePreviewWidget.Entry(block.m_49966_(), offset));
         }

         return this;
      }

      EmiLightningStrikePreviewWidget build() {
         return new EmiLightningStrikePreviewWidget(this.x, this.y, this.width, this.height, List.copyOf(this.blocks));
      }
   }

   private static record Entry(BlockState state, BlockPos offset) {
   }
}
