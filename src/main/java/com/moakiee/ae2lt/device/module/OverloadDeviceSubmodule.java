package com.moakiee.ae2lt.device.module;

import net.minecraft.world.item.ItemStack;

public interface OverloadDeviceSubmodule {
   String id();

   default boolean isInstalled(ItemStack device) {
      return true;
   }

   default boolean isActive(ItemStack device) {
      return false;
   }
}
