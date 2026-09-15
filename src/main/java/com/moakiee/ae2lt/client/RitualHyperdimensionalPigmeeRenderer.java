package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.entity.RitualHyperdimensionalPigmeeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.item.ItemEntity;

public final class RitualHyperdimensionalPigmeeRenderer extends ItemEntityRenderer {
   public RitualHyperdimensionalPigmeeRenderer(Context context) {
      super(context);
   }

   public void m_7392_(ItemEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.m_85836_();
      if (entity instanceof RitualHyperdimensionalPigmeeEntity ritualEntity) {
         float scale = ritualEntity.getCeremonyScale(partialTick);
         poseStack.m_85841_(scale, scale, scale);
      }

      super.m_7392_(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      poseStack.m_85849_();
   }
}
