package com.moakiee.ae2lt.client;

import appeng.client.render.overlay.OverlayRenderType;
import com.moakiee.ae2lt.blockentity.OverloadedInterfaceBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import com.moakiee.ae2lt.blockentity.OverloadedPowerSupplyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

public class WirelessConnectorHostRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   private static final int COLOR_HOST = -2147450625;
   private static final int COLOR_HOST_SELECTED = -2130706688;

   public WirelessConnectorHostRenderer(Context context) {
   }

   public void m_6922_(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
      Level level = blockEntity.m_58904_();
      if (level != null) {
         String hostType = getRenderableHostType(blockEntity);
         if (hostType != null) {
            ItemStack stack = WirelessConnectorRenderer.getHeldConnectorStack();
            if (!stack.m_41619_()) {
               boolean selected = WirelessConnectorRenderer.isSelectedHost(stack, level, blockEntity.m_58899_(), hostType);
               renderInnerCube(poseStack, buffer, selected ? -2130706688 : -2147450625);
            }
         }
      }
   }

   public boolean m_5932_(T blockEntity) {
      return true;
   }

   private static String getRenderableHostType(BlockEntity blockEntity) {
      if (blockEntity instanceof OverloadedPatternProviderBlockEntity provider
         && provider.getProviderMode() != OverloadedPatternProviderBlockEntity.ProviderMode.NORMAL) {
         return "provider";
      }

      if (blockEntity instanceof OverloadedInterfaceBlockEntity iface && iface.getInterfaceMode() == OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
         return "interface";
      }

      return blockEntity instanceof OverloadedPowerSupplyBlockEntity ? "power_supply" : null;
   }

   private static void renderInnerCube(PoseStack poseStack, MultiBufferSource buffer, int color) {
      VertexConsumer vc = buffer.m_6299_(Ae2ltRenderTypes.getFaceSeeThrough());
      int[] c = OverlayRenderType.decomposeColor(color);
      poseStack.m_85836_();
      Matrix4f mat = poseStack.m_85850_().m_252922_();
      float lo = 0.25F;
      float hi = 0.75F;
      quad(vc, mat, c, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi, 0.0F, -1.0F, 0.0F);
      quad(vc, mat, c, lo, hi, hi, hi, hi, hi, hi, hi, lo, lo, hi, lo, 0.0F, 1.0F, 0.0F);
      quad(vc, mat, c, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo, lo, 0.0F, 0.0F, -1.0F);
      quad(vc, mat, c, hi, lo, hi, hi, hi, hi, lo, hi, hi, lo, lo, hi, 0.0F, 0.0F, 1.0F);
      quad(vc, mat, c, lo, lo, hi, lo, hi, hi, lo, hi, lo, lo, lo, lo, -1.0F, 0.0F, 0.0F);
      quad(vc, mat, c, hi, lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, 1.0F, 0.0F, 0.0F);
      poseStack.m_85849_();
   }

   private static void quad(
      VertexConsumer vc,
      Matrix4f mat,
      int[] c,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float nx,
      float ny,
      float nz
   ) {
      vc.m_252986_(mat, x1, y1, z1).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      vc.m_252986_(mat, x2, y2, z2).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      vc.m_252986_(mat, x3, y3, z3).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
      vc.m_252986_(mat, x4, y4, z4).m_6122_(c[1], c[2], c[3], c[0]).m_5601_(nx, ny, nz).m_5752_();
   }
}
