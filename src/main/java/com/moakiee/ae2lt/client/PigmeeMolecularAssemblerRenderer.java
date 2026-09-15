package com.moakiee.ae2lt.client;

import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.client.render.effects.ParticleTypes;
import appeng.core.AppEngClient;
import com.moakiee.ae2lt.blockentity.PigmeeMolecularAssemblerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

public final class PigmeeMolecularAssemblerRenderer implements BlockEntityRenderer<PigmeeMolecularAssemblerBlockEntity> {
   public static final ResourceLocation LIGHTS_MODEL = new ResourceLocation("ae2lt", "block/pigmee_molecular_assembler_lights");
   private final RandomSource particleRandom = RandomSource.m_216327_();

   public PigmeeMolecularAssemblerRenderer(Context context) {
   }

   public void render(
      PigmeeMolecularAssemblerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay
   ) {
      AssemblerAnimationStatus animationStatus = blockEntity.getAnimationStatus();
      if (animationStatus != null) {
         if (!Minecraft.m_91087_().m_91104_()) {
            if (animationStatus.isExpired()) {
               blockEntity.setAnimationStatus(null);
            }

            animationStatus.setAccumulatedTicks(animationStatus.getAccumulatedTicks() + partialTick);
            animationStatus.setTicksUntilParticles(animationStatus.getTicksUntilParticles() - partialTick);
         }

         this.renderAnimation(blockEntity, poseStack, buffer, packedLight, animationStatus);
      }

      if (blockEntity.isPowered()) {
         this.renderPowerLight(poseStack, buffer, packedLight, packedOverlay);
      }
   }

   private void renderPowerLight(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      Minecraft minecraft = Minecraft.m_91087_();
      BakedModel lightsModel = minecraft.m_91304_().getModel(LIGHTS_MODEL);
      VertexConsumer buffer = bufferSource.m_6299_(RenderType.m_110503_());
      minecraft.m_91289_()
         .m_110937_()
         .renderModel(poseStack.m_85850_(), buffer, null, lightsModel, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, ModelData.EMPTY, null);
   }

   private void renderAnimation(
      PigmeeMolecularAssemblerBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, AssemblerAnimationStatus status
   ) {
      double centerX = (double)blockEntity.m_58899_().m_123341_() + 0.5;
      double centerY = (double)blockEntity.m_58899_().m_123342_() + 0.5;
      double centerZ = (double)blockEntity.m_58899_().m_123343_() + 0.5;
      Minecraft minecraft = Minecraft.m_91087_();
      if (status.getTicksUntilParticles() <= 0.0F) {
         status.setTicksUntilParticles(4.0F);
         if (AppEngClient.instance().shouldAddParticles(this.particleRandom)) {
            for (int i = 0; i < (int)Math.ceil((double)status.getSpeed() / 5.0); i++) {
               minecraft.f_91061_.m_107370_(ParticleTypes.CRAFTING, centerX, centerY, centerZ, 0.0, 0.0, 0.0);
            }
         }
      }

      ItemStack stack = status.getIs();
      ItemRenderer itemRenderer = minecraft.m_91291_();
      poseStack.m_85836_();
      poseStack.m_85837_(0.5, 0.5, 0.5);
      poseStack.m_85837_(0.0, stack.m_41720_() instanceof BlockItem ? -0.2F : -0.3F, 0.0);
      itemRenderer.m_269128_(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.f_118083_, poseStack, buffer, blockEntity.m_58904_(), 0);
      poseStack.m_85849_();
   }
}
