package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.block.FumoBlock;
import com.moakiee.ae2lt.blockentity.FumoBlockEntity;
import com.moakiee.ae2lt.registry.ModFumos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class FumoBlockRenderer implements BlockEntityRenderer<FumoBlockEntity> {
   private static final RandomSource RAND = RandomSource.m_216327_();

   public FumoBlockRenderer(Context context) {
   }

   public void render(FumoBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
      BlockState state = blockEntity.m_58900_();
      boolean spinning = blockEntity.isSpinning();
      BlockState renderState = spinning && state.m_61138_(FumoBlock.FACING) ? (BlockState)state.m_61124_(FumoBlock.FACING, Direction.NORTH) : state;
      poseStack.m_85836_();
      if (spinning) {
         poseStack.m_85837_(0.5, 0.0, 0.5);
         poseStack.m_252781_(Axis.f_252436_.m_252977_(blockEntity.getRenderYRot(partialTick)));
         poseStack.m_85837_(-0.5, 0.0, -0.5);
      }

      BlockRenderDispatcher dispatcher = Minecraft.m_91087_().m_91289_();
      BakedModel model = dispatcher.m_110910_(renderState);
      ModelData modelData = ModelData.EMPTY;
      Level level = blockEntity.m_58904_();
      BlockPos pos = blockEntity.m_58899_();
      boolean hyperdimensional = state.m_60713_((Block)ModFumos.HYPERDIMENSIONAL_PIGMEE_FUMO.get());
      if (level != null) {
         if (!hyperdimensional) {
            ModelBlockRenderer modelRenderer = dispatcher.m_110937_();

            for (RenderType renderType : model.getRenderTypes(renderState, RAND, modelData)) {
               modelRenderer.tesselateBlock(
                  level, model, renderState, pos, poseStack, buffer.m_6299_(renderType), false, RAND, 42L, packedOverlay, modelData, renderType
               );
            }
         }

         if (hyperdimensional) {
            HyperdimensionalPigmeePortalLayer.renderBlock(model, renderState, modelData, poseStack, buffer);
            HyperdimensionalPigmeeTextureLayer.renderBlock(renderState, poseStack, buffer, packedOverlay);
         }
      } else {
         int color = Minecraft.m_91087_().m_91298_().m_92577_(renderState, null, null, 0);
         float r = (float)(color >> 16 & 0xFF) / 255.0F;
         float g = (float)(color >> 8 & 0xFF) / 255.0F;
         float b = (float)(color & 0xFF) / 255.0F;
         Pose pose = poseStack.m_85850_();
         if (!hyperdimensional) {
            for (RenderType renderType : model.getRenderTypes(renderState, RAND, modelData)) {
               VertexConsumer consumer = buffer.m_6299_(renderType);

               for (Direction dir : Direction.values()) {
                  RAND.m_188584_(42L);
                  renderQuads(pose, consumer, model.getQuads(renderState, dir, RAND, modelData, renderType), r, g, b, packedLight, packedOverlay);
               }

               RAND.m_188584_(42L);
               renderQuads(pose, consumer, model.getQuads(renderState, null, RAND, modelData, renderType), r, g, b, packedLight, packedOverlay);
            }
         }

         if (hyperdimensional) {
            HyperdimensionalPigmeePortalLayer.renderBlock(model, renderState, modelData, poseStack, buffer);
            HyperdimensionalPigmeeTextureLayer.renderBlock(renderState, poseStack, buffer, packedOverlay);
         }
      }

      poseStack.m_85849_();
   }

   private static void renderQuads(Pose pose, VertexConsumer consumer, List<BakedQuad> quads, float r, float g, float b, int packedLight, int packedOverlay) {
      for (BakedQuad quad : quads) {
         float shade = getShade(quad);
         float qr;
         float qg;
         float qb;
         if (quad.m_111304_()) {
            qr = Mth.m_14036_(r, 0.0F, 1.0F) * shade;
            qg = Mth.m_14036_(g, 0.0F, 1.0F) * shade;
            qb = Mth.m_14036_(b, 0.0F, 1.0F) * shade;
         } else {
            qr = shade;
            qg = shade;
            qb = shade;
         }

         consumer.m_85987_(pose, quad, qr, qg, qb, packedLight, packedOverlay);
      }
   }

   private static float getShade(BakedQuad quad) {
      if (!quad.m_111307_()) {
         return 1.0F;
      } else {
         Direction dir = quad.m_111306_();
         if (dir == null) {
            return 1.0F;
         } else {
            return switch (dir) {
               case DOWN -> 0.5F;
               case UP -> 1.0F;
               case NORTH, SOUTH -> 0.8F;
               case EAST, WEST -> 0.6F;
               default -> throw new IncompatibleClassChangeError();
            };
         }
      }
   }
}
