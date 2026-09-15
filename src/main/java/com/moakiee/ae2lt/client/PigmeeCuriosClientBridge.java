package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.registry.ModFumos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

final class PigmeeCuriosClientBridge {
   private PigmeeCuriosClientBridge() {
   }

   static void registerRenderers() {
      CuriosRendererRegistry.register((Item)ModFumos.PIGMEE_FUMO_ITEM.get(), PigmeeCuriosClientBridge.PigmeeHeadRenderer::new);
      CuriosRendererRegistry.register((Item)ModFumos.HYPERDIMENSIONAL_PIGMEE_FUMO_ITEM.get(), PigmeeCuriosClientBridge.PigmeeHeadRenderer::new);
      CuriosRendererRegistry.register((Item)ModFumos.CREATIVE_PIGMEE_FUMO_ITEM.get(), PigmeeCuriosClientBridge.PigmeeHeadRenderer::new);
   }

   private static final class PigmeeHeadRenderer implements ICurioRenderer {
      public <T extends LivingEntity, M extends EntityModel<T>> void render(
         ItemStack stack,
         SlotContext slotContext,
         PoseStack poseStack,
         RenderLayerParent<T, M> renderLayerParent,
         MultiBufferSource bufferSource,
         int packedLight,
         float limbSwing,
         float limbSwingAmount,
         float partialTick,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         if (renderLayerParent.m_7200_() instanceof HeadedModel headedModel) {
            LivingEntity var16 = slotContext.entity();
            poseStack.m_85836_();
            if (var16.m_6162_() && !(var16 instanceof Villager)) {
               poseStack.m_252880_(0.0F, 0.03125F, 0.0F);
               poseStack.m_85841_(0.7F, 0.7F, 0.7F);
               poseStack.m_252880_(0.0F, 1.0F, 0.0F);
            }

            headedModel.m_5585_().m_104299_(poseStack);
            boolean villagerHead = var16 instanceof Villager || var16 instanceof ZombieVillager;
            CustomHeadLayer.m_174483_(poseStack, villagerHead);
            Minecraft.m_91087_().m_91290_().m_234586_().m_269530_(var16, stack, ItemDisplayContext.HEAD, false, poseStack, bufferSource, packedLight);
            poseStack.m_85849_();
         }
      }
   }
}
