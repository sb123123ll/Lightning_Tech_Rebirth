package com.moakiee.ae2lt.device.energy;

import appeng.api.stacks.AEKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

public interface DeviceEnergyBuffer {
   long stored(ItemStack var1);

   long capacity(ItemStack var1);

   boolean tryConsume(ItemStack var1, ServerPlayer var2, long var3);

   void refill(ItemStack var1, ServerPlayer var2);

   default int receiveFe(ItemStack stack, int amount, boolean simulate) {
      return 0;
   }

   default IEnergyStorage asEnergyStorage(ItemStack stack) {
      return null;
   }

   default boolean tryConsumeKey(ItemStack stack, ServerPlayer player, AEKey key, long amount) {
      return false;
   }
}
