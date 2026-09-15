package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.item.CelestweaveCoreItem;
import com.moakiee.ae2lt.item.PhaseLockProjectionItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class PhaseWingLayer extends ElytraLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation WING_TEXTURE = new ResourceLocation("textures/entity/elytra.png");

   public PhaseWingLayer(PlayerRenderer renderer, EntityModelSet models) {
      super(renderer, models);
   }

   public boolean shouldRender(ItemStack stack, AbstractClientPlayer player) {
      return player.m_21255_() && isCelestweaveChest(stack);
   }

   public ResourceLocation getElytraTexture(ItemStack stack, AbstractClientPlayer player) {
      return WING_TEXTURE;
   }

   private static boolean isCelestweaveChest(ItemStack stack) {
      if (stack.m_41720_() instanceof CelestweaveCoreItem) {
         return true;
      } else {
         if (stack.m_41720_() instanceof PhaseLockProjectionItem projection && projection.equipmentSlot() == EquipmentSlot.CHEST) {
            return true;
         }

         return false;
      }
   }
}
