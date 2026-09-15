package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.blockentity.LightningAssemblyChamberBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LightningAssemblyChamberRenderer implements BlockEntityRenderer<LightningAssemblyChamberBlockEntity> {
   public LightningAssemblyChamberRenderer(Context context) {
   }

   public void render(
      LightningAssemblyChamberBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay
   ) {
      ItemStack stack = blockEntity.getClientRecipeResult();
      if (!stack.m_41619_()) {
         ItemRenderer itemRenderer = Minecraft.m_91087_().m_91291_();
         poseStack.m_85836_();
         poseStack.m_85837_(0.5, 0.5, 0.5);
         if (!(stack.m_41720_() instanceof BlockItem)) {
            poseStack.m_252880_(0.0F, -0.3F, 0.0F);
         } else {
            poseStack.m_252880_(0.0F, -0.2F, 0.0F);
         }

         itemRenderer.m_269128_(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.f_118083_, poseStack, buffer, blockEntity.m_58904_(), 0);
         poseStack.m_85849_();
      }
   }
}
