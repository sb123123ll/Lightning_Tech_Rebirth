package com.moakiee.ae2lt.client.render;

import com.moakiee.ae2lt.block.OverloadTntBlock;
import com.moakiee.ae2lt.entity.OverloadTntEntity;
import com.moakiee.ae2lt.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class OverloadTntRenderer extends EntityRenderer<OverloadTntEntity> {
   private final BlockRenderDispatcher blockRenderer;

   public OverloadTntRenderer(Context context) {
      super(context);
      this.f_114477_ = 0.5F;
      this.blockRenderer = context.m_234597_();
   }

   public void render(OverloadTntEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.m_85836_();
      poseStack.m_252880_(0.0F, 0.5F, 0.0F);
      int fuse = entity.m_32100_();
      if ((float)fuse - partialTick + 1.0F < 10.0F) {
         float scale = 1.0F - ((float)fuse - partialTick + 1.0F) / 10.0F;
         scale = Mth.m_14036_(scale, 0.0F, 1.0F);
         scale *= scale;
         scale *= scale;
         float inflatedScale = 1.0F + scale * 0.3F;
         poseStack.m_85841_(inflatedScale, inflatedScale, inflatedScale);
      }

      poseStack.m_252781_(Axis.f_252436_.m_252977_(-90.0F));
      poseStack.m_252880_(-0.5F, -0.5F, 0.5F);
      poseStack.m_252781_(Axis.f_252436_.m_252977_(90.0F));
      TntMinecartRenderer.m_234661_(
         this.blockRenderer, ((OverloadTntBlock)ModBlocks.OVERLOAD_TNT.get()).m_49966_(), poseStack, bufferSource, packedLight, fuse / 5 % 2 == 0
      );
      poseStack.m_85849_();
      super.m_7392_(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(OverloadTntEntity entity) {
      return TextureAtlas.f_118259_;
   }
}
