package com.moakiee.ae2lt.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class RecipeContainerInput implements Container {
   public abstract int size();

   public abstract ItemStack m_8020_(int var1);

   public abstract boolean m_7983_();

   public final int m_6643_() {
      return this.size();
   }

   public ItemStack m_7407_(int slot, int amount) {
      return ItemStack.f_41583_;
   }

   public ItemStack m_8016_(int slot) {
      return ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
   }

   public void m_6596_() {
   }

   public boolean m_6542_(Player player) {
      return true;
   }

   public void m_6211_() {
   }
}
