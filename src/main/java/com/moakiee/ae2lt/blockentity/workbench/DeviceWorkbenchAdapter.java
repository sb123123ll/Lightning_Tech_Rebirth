package com.moakiee.ae2lt.blockentity.workbench;

import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import com.moakiee.ae2lt.device.module.DeviceModuleStorage;
import com.moakiee.ae2lt.device.network.DeviceNetworkBinding;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;

public interface DeviceWorkbenchAdapter {
   DeviceKind deviceKind();

   DeviceModuleStorage moduleStorage();

   DeviceEnergyBuffer energyBuffer();

   DeviceNetworkBinding networkBinding();

   List<StructuralSlotSpec> structuralSlots();

   Predicate<ItemStack> moduleInputValidator(ItemStack var1, Provider var2);

   List<ItemStack> listModuleEntries(ItemStack var1, Provider var2);

   boolean canInstallOne(ItemStack var1, Provider var2, ItemStack var3);

   boolean installOne(ItemStack var1, Provider var2, ItemStack var3);

   ItemStack uninstallOne(ItemStack var1, Provider var2, String var3);

   ItemStack uninstallAll(ItemStack var1, Provider var2, String var3);

   String moduleTypeId(ItemStack var1);

   int maxInstallAmount(ItemStack var1);

   ItemStack getStructuralSlot(ItemStack var1, Provider var2, StructuralSlotSpec var3);

   void setStructuralSlot(ItemStack var1, Provider var2, StructuralSlotSpec var3, ItemStack var4);

   ItemStack removeStructuralSlot(ItemStack var1, Provider var2, StructuralSlotSpec var3, int var4);

   boolean canPlaceStructural(ItemStack var1, Provider var2, StructuralSlotSpec var3, ItemStack var4);

   default boolean mayPickupStructural(ItemStack device, Provider registries, StructuralSlotSpec spec, Player player, ItemStack carried) {
      return !device.m_41619_();
   }

   default void onDeviceInserted(ItemStack device) {
   }

   default void onModulesChanged(ItemStack device, Provider registries, Dist dist) {
   }
}
