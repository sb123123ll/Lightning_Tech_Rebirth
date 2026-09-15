package com.moakiee.ae2lt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class HyperdimensionalPigmeeItemRenderer extends BlockEntityWithoutLevelRenderer {
   HyperdimensionalPigmeeItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet entityModels) {
      super(dispatcher, entityModels);
   }

   public void m_108829_(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
      Minecraft minecraft = Minecraft.m_91087_();
      if (minecraft.m_91291_().m_174264_(stack, null, null, 0) instanceof HyperdimensionalPigmeeBakedModel hyperModel) {
         BakedModel model = hyperModel.baseModel();
         HyperdimensionalPigmeePortalLayer.renderItem(model, poseStack, buffers);
         HyperdimensionalPigmeeTextureLayer.renderItem(poseStack, buffers, packedOverlay);
      }
   }
}
