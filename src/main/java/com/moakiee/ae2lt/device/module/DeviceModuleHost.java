package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.device.DeviceKind;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

public interface DeviceModuleHost {
   DeviceKind deviceKind();

   List<ModuleSlotSpec> slotSpecs();

   ItemStack getModule(int var1);

   boolean trySetModule(int var1, ItemStack var2);

   Stream<ItemStack> installedModuleStacks();
}
