package com.moakiee.ae2lt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

final class HyperdimensionalPigmeePortalLayer {
   private static final long MODEL_SEED = 42L;
   private static final float SURFACE_OFFSET = 0.002F;

   private HyperdimensionalPigmeePortalLayer() {
   }

   static void renderBlock(BakedModel model, BlockState state, ModelData modelData, PoseStack poseStack, MultiBufferSource buffers) {
      VertexConsumer consumer = buffers.m_6299_(RenderType.m_173239_());
      RandomSource random = RandomSource.m_216327_();

      for (RenderType sourceType : model.getRenderTypes(state, random, modelData)) {
         for (Direction direction : Direction.values()) {
            random.m_188584_(42L);
            renderQuads(poseStack.m_85850_(), consumer, model.getQuads(state, direction, random, modelData, sourceType));
         }

         random.m_188584_(42L);
         renderQuads(poseStack.m_85850_(), consumer, model.getQuads(state, null, random, modelData, sourceType));
      }
   }

   static void renderItem(BakedModel model, PoseStack poseStack, MultiBufferSource buffers) {
      VertexConsumer consumer = buffers.m_6299_(RenderType.m_173239_());
      RandomSource random = RandomSource.m_216327_();

      for (Direction direction : Direction.values()) {
         random.m_188584_(42L);
         renderQuads(poseStack.m_85850_(), consumer, model.m_213637_(null, direction, random));
      }

      random.m_188584_(42L);
      renderQuads(poseStack.m_85850_(), consumer, model.m_213637_(null, null, random));
   }

   private static void renderQuads(Pose pose, VertexConsumer consumer, List<BakedQuad> quads) {
      for (BakedQuad quad : quads) {
         int[] vertices = quad.m_111303_();
         int stride = vertices.length / 4;
         Direction direction = quad.m_111306_();
         float offsetX = (float)direction.m_122429_() * 0.002F;
         float offsetY = (float)direction.m_122430_() * 0.002F;
         float offsetZ = (float)direction.m_122431_() * 0.002F;

         for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            consumer.m_252986_(
                  pose.m_252922_(),
                  Float.intBitsToFloat(vertices[offset]) + offsetX,
                  Float.intBitsToFloat(vertices[offset + 1]) + offsetY,
                  Float.intBitsToFloat(vertices[offset + 2]) + offsetZ
               )
               .m_5752_();
         }
      }
   }
}
