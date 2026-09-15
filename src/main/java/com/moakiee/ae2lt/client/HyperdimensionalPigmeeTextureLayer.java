package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.block.FumoBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

final class HyperdimensionalPigmeeTextureLayer {
   static final ResourceLocation MODEL = new ResourceLocation("ae2lt", "block/hyperdimensional_pigmee_fumo_overlay");
   private static final long MODEL_SEED = 42L;
   private static final float SURFACE_OFFSET = 0.004F;

   private HyperdimensionalPigmeeTextureLayer() {
   }

   static void renderBlock(BlockState state, PoseStack poseStack, MultiBufferSource buffers, int packedOverlay) {
      poseStack.m_85836_();
      poseStack.m_252880_(0.5F, 0.5F, 0.5F);
      poseStack.m_252781_(Axis.f_252436_.m_252977_(rotationFor(state)));
      poseStack.m_252880_(-0.5F, -0.5F, -0.5F);
      render(poseStack, buffers, packedOverlay);
      poseStack.m_85849_();
   }

   static void renderItem(PoseStack poseStack, MultiBufferSource buffers, int packedOverlay) {
      poseStack.m_85836_();
      render(poseStack, buffers, packedOverlay);
      poseStack.m_85849_();
   }

   private static void render(PoseStack poseStack, MultiBufferSource buffers, int packedOverlay) {
      Minecraft minecraft = Minecraft.m_91087_();
      BakedModel model = minecraft.m_91304_().getModel(MODEL);
      VertexConsumer consumer = buffers.m_6299_(RenderType.m_234338_(TextureAtlas.f_118259_));
      RandomSource random = RandomSource.m_216327_();

      for (Direction direction : Direction.values()) {
         random.m_188584_(42L);
         renderQuads(poseStack, consumer, model.m_213637_(null, direction, random), direction, packedOverlay);
      }

      random.m_188584_(42L);

      for (BakedQuad quad : model.m_213637_(null, null, random)) {
         renderQuads(poseStack, consumer, List.of(quad), quad.m_111306_(), packedOverlay);
      }
   }

   private static void renderQuads(PoseStack poseStack, VertexConsumer consumer, List<BakedQuad> quads, Direction direction, int packedOverlay) {
      if (!quads.isEmpty()) {
         poseStack.m_85836_();
         poseStack.m_252880_((float)direction.m_122429_() * 0.004F, (float)direction.m_122430_() * 0.004F, (float)direction.m_122431_() * 0.004F);

         for (BakedQuad quad : quads) {
            consumer.putBulkData(poseStack.m_85850_(), quad, 1.0F, 1.0F, 1.0F, 1.0F, 15728880, packedOverlay, true);
         }

         poseStack.m_85849_();
      }
   }

   private static float rotationFor(BlockState state) {
      if (!state.m_61138_(FumoBlock.FACING)) {
         return 0.0F;
      } else {
         Direction facing = (Direction)state.m_61143_(FumoBlock.FACING);

         return switch (facing) {
            case SOUTH -> -180.0F;
            case WEST -> -270.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
         };
      }
   }
}
