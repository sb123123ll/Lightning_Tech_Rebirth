package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.block.LightningSimulationChamberBlock;
import com.moakiee.ae2lt.blockentity.LightningSimulationChamberBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LightningSimulationChamberRenderer implements BlockEntityRenderer<LightningSimulationChamberBlockEntity> {
   private static final float ITEM_SCALE = 0.35F;
   private static final float ITEM_BASE_HEIGHT = 0.128125F;
   private static final float ITEM_LAYER_OFFSET = 0.01F;
   private static final float ITEM_DEPTH = 0.5F;
   private static final float[] INPUT_X_POSITIONS = new float[]{0.34F, 0.5F, 0.66F};

   public LightningSimulationChamberRenderer(Context context) {
   }

   public void render(
      LightningSimulationChamberBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay
   ) {
      ItemRenderer itemRenderer = Minecraft.m_91087_().m_91291_();
      Direction facing = blockEntity.m_58900_().m_61138_(LightningSimulationChamberBlock.FACING)
         ? (Direction)blockEntity.m_58900_().m_61143_(LightningSimulationChamberBlock.FACING)
         : Direction.NORTH;
      poseStack.m_85836_();
      poseStack.m_85837_(0.5, 0.0, 0.5);
      poseStack.m_252781_(Axis.f_252436_.m_252977_(facing.m_122435_()));
      poseStack.m_85837_(-0.5, 0.0, -0.5);

      for (int i = 0; i < INPUT_X_POSITIONS.length; i++) {
         ItemStack stack = blockEntity.getInventory().getStackInSlot(0 + i);
         if (!stack.m_41619_()) {
            poseStack.m_85836_();
            poseStack.m_252880_(INPUT_X_POSITIONS[i], 0.128125F + 0.01F * (float)i, 0.5F);
            poseStack.m_252781_(Axis.f_252529_.m_252977_(90.0F));
            poseStack.m_85841_(0.35F, 0.35F, 0.35F);
            itemRenderer.m_269128_(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, blockEntity.m_58904_(), 0);
            poseStack.m_85849_();
         }
      }

      poseStack.m_85849_();
   }
}
