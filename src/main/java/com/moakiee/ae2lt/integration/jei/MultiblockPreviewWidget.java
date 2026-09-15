package com.moakiee.ae2lt.integration.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class MultiblockPreviewWidget implements IRecipeWidget {
   private static final float X_ROTATION_DEG = 30.0F;
   private static final float SCALE_MARGIN = 0.92F;
   private static final float ROTATION_PER_FRAME = 0.4F;
   private final ScreenPosition position;
   private final int width;
   private final int height;
   private final List<MultiblockPreviewWidget.Entry> blocks;
   private final float centerX;
   private final float centerY;
   private final float centerZ;
   private final float scale;
   private float rotation;

   private MultiblockPreviewWidget(int x, int y, int width, int height, List<MultiblockPreviewWidget.Entry> blocks) {
      this.position = new ScreenPosition(x, y);
      this.width = width;
      this.height = height;
      this.blocks = blocks;
      if (blocks.isEmpty()) {
         this.centerX = 0.0F;
         this.centerY = 0.0F;
         this.centerZ = 0.0F;
         this.scale = 0.0F;
      } else {
         float minX = Float.POSITIVE_INFINITY;
         float maxX = Float.NEGATIVE_INFINITY;
         float minY = Float.POSITIVE_INFINITY;
         float maxY = Float.NEGATIVE_INFINITY;
         float minZ = Float.POSITIVE_INFINITY;
         float maxZ = Float.NEGATIVE_INFINITY;

         for (MultiblockPreviewWidget.Entry entry : blocks) {
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
         float[] xs = new float[]{minX - this.centerX, maxX - this.centerX};
         float[] ys = new float[]{minY - this.centerY, maxY - this.centerY};
         float[] zs = new float[]{minZ - this.centerZ, maxZ - this.centerZ};

         for (float xv : xs) {
            for (float yv : ys) {
               for (float zv : zs) {
                  float horizontal = (float)Math.hypot((double)xv, (double)zv);
                  horizontalRadius = Math.max(horizontalRadius, horizontal);
                  verticalRadius = Math.max(verticalRadius, Math.abs(yv) * xRotCos + horizontal * xRotSin);
               }
            }
         }

         if (!(horizontalRadius <= 0.0F) && !(verticalRadius <= 0.0F)) {
            float widthScale = (float)width * 0.5F / horizontalRadius;
            float heightScale = (float)height * 0.5F / verticalRadius;
            this.scale = Math.min(widthScale, heightScale) * 0.92F;
         } else {
            this.scale = 0.0F;
         }
      }
   }

   public ScreenPosition getPosition() {
      return this.position;
   }

   public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
      if (!this.blocks.isEmpty() && !(this.scale <= 0.0F)) {
         Minecraft client = Minecraft.m_91087_();
         BufferSource bufferSource = client.m_91269_().m_110104_();
         BlockRenderDispatcher blockRenderer = client.m_91289_();
         PoseStack pose = guiGraphics.m_280168_();
         float cx = (float)this.width / 2.0F;
         float cy = (float)this.height / 2.0F;

         for (MultiblockPreviewWidget.Entry entry : this.blocks) {
            pose.m_85836_();
            pose.m_252880_(cx, cy, 400.0F);
            pose.m_85841_(this.scale, -this.scale, this.scale);
            pose.m_252781_(Axis.f_252529_.m_252977_(30.0F));
            pose.m_252781_(Axis.f_252436_.m_252977_(225.0F + this.rotation));
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

   public void tick() {
      this.rotation += 0.4F;
      if (this.rotation >= 360.0F) {
         this.rotation -= 360.0F;
      }
   }

   public static MultiblockPreviewWidget.Builder builder(int x, int y, int width, int height) {
      return new MultiblockPreviewWidget.Builder(x, y, width, height);
   }

   public static final class Builder {
      private final int x;
      private final int y;
      private final int width;
      private final int height;
      private final List<MultiblockPreviewWidget.Entry> blocks = new ArrayList<>();

      private Builder(int x, int y, int width, int height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      public MultiblockPreviewWidget.Builder addBlock(Block block, BlockPos offset) {
         if (block == null) {
            return this;
         } else {
            this.blocks.add(new MultiblockPreviewWidget.Entry(block.m_49966_(), offset));
            return this;
         }
      }

      public MultiblockPreviewWidget build() {
         return new MultiblockPreviewWidget(this.x, this.y, this.width, this.height, List.copyOf(this.blocks));
      }
   }

   private static record Entry(BlockState state, BlockPos offset) {
   }
}
