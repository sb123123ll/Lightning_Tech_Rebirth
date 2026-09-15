package com.moakiee.ae2lt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

class SpinningFumoBakedModel extends BakedModelWrapper<BakedModel> {
   SpinningFumoBakedModel(BakedModel originalModel) {
      super(originalModel);
   }

   public BakedModel applyTransform(ItemDisplayContext displayContext, PoseStack poseStack, boolean leftHand) {
      this.originalModel.applyTransform(displayContext, poseStack, leftHand);
      if (displayContext == ItemDisplayContext.HEAD) {
         Minecraft minecraft = Minecraft.m_91087_();
         if (minecraft.f_91073_ != null) {
            float partialTick = minecraft.m_91296_();
            float angle = ((float)(minecraft.f_91073_.m_46467_() % 60L) + partialTick) * 6.0F;
            poseStack.m_252781_(Axis.f_252436_.m_252977_(angle));
         }
      }

      return this;
   }
}
