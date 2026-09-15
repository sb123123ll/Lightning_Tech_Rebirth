package com.moakiee.ae2lt.device.network;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface DeviceNetworkBinding {
   @Nullable
   GlobalPos getBoundPos(ItemStack var1);

   void bind(ItemStack var1, GlobalPos var2);

   void unbind(ItemStack var1);

   BindingResolveResult resolve(ItemStack var1, ServerPlayer var2);
}
