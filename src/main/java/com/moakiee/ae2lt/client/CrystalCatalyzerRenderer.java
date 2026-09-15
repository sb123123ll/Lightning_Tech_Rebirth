package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.blockentity.CrystalCatalyzerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CrystalCatalyzerRenderer implements BlockEntityRenderer<CrystalCatalyzerBlockEntity> {
   private static final double CAVITY_CENTER_Y = 0.5;
   private static final float ITEM_SCALE = 0.5F;
   private static final float ROTATION_SPEED = 2.0F;

   public CrystalCatalyzerRenderer(Context context) {
   }

   public void render(
      CrystalCatalyzerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay
   ) {
      ItemStack stack = getDisplayStack(blockEntity);
      if (!stack.m_41619_()) {
         Level level = blockEntity.m_58904_();
         ItemRenderer itemRenderer = Minecraft.m_91087_().m_91291_();
         poseStack.m_85836_();
         poseStack.m_85837_(0.5, 0.5, 0.5);
         if (level != null) {
            float rotation = ((float)level.m_46467_() + partialTick) * 2.0F;
            poseStack.m_252781_(Axis.f_252436_.m_252977_(rotation));
         }

         poseStack.m_85841_(0.5F, 0.5F, 0.5F);
         itemRenderer.m_269128_(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.f_118083_, poseStack, buffer, level, 0);
         poseStack.m_85849_();
      }
   }

   private static ItemStack getDisplayStack(CrystalCatalyzerBlockEntity blockEntity) {
      return blockEntity.getInventory().getStackInSlot(0);
   }
}
