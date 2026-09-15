package com.moakiee.ae2lt.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public final class PigmeeCoreItem extends Item {
   public PigmeeCoreItem(Properties properties) {
      super(properties);
   }

   public boolean hasCraftingRemainingItem(ItemStack stack) {
      return true;
   }

   public ItemStack getCraftingRemainingItem(ItemStack stack) {
      ItemStack remainder = stack.m_41777_();
      remainder.m_41764_(1);
      return remainder;
   }
}
