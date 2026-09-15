package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.device.DeviceKind;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

public interface DeviceModuleStorage {
   DeviceKind deviceKind();

   List<ItemStack> listEntries(ItemStack var1);

   int getCount(ItemStack var1, String var2);

   boolean canInstallOne(ItemStack var1, ItemStack var2);

   boolean installOne(ItemStack var1, ItemStack var2);

   ItemStack uninstallOne(ItemStack var1, String var2);

   ItemStack uninstallAll(ItemStack var1, String var2);

   boolean hasAnyInstalled(ItemStack var1);

   Stream<ItemStack> installedModuleStacks(ItemStack var1);
}
