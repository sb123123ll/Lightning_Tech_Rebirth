package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.machine.common.LightningCollapseMatrixHost;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class LightningCollapseMatrixItem extends Item {
   public LightningCollapseMatrixItem(Properties properties) {
      super(properties);
   }

   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return this.insertIntoMachine(context);
   }

   public InteractionResult m_6225_(UseOnContext context) {
      return this.insertIntoMachine(context);
   }

   private InteractionResult insertIntoMachine(UseOnContext context) {
      Player player = context.m_43723_();
      if (player != null && player.m_36341_()) {
         Level level = context.m_43725_();
         LightningCollapseMatrixHost host = LightningCollapseMatrixHost.find(level, context.m_8083_());
         if (host == null) {
            return InteractionResult.PASS;
         } else {
            if (!level.m_5776_()) {
               host.insertMatricesFromHand(player, context.m_43724_());
            }

            return InteractionResult.m_19078_(level.m_5776_());
         }
      } else {
         return InteractionResult.PASS;
      }
   }
}
