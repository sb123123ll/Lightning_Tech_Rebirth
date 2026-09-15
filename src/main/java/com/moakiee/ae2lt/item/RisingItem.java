package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.entity.FloatingMatterEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class RisingItem extends Item {
   public RisingItem(Properties properties) {
      super(properties);
   }

   public boolean hasCustomEntity(ItemStack stack) {
      return true;
   }

   public Entity createEntity(Level level, Entity location, ItemStack stack) {
      FloatingMatterEntity matter = new FloatingMatterEntity(level, location.m_20185_(), location.m_20186_(), location.m_20189_(), stack);
      matter.m_20256_(location.m_20184_());
      matter.m_32010_(40);
      return matter;
   }
}
