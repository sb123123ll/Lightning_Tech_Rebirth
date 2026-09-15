package com.moakiee.ae2lt.blockentity.workbench;

import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.device.DeviceKind;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class DeviceWorkbenchAdapters {
   private static final Map<DeviceKind, DeviceWorkbenchAdapter> BY_KIND = new EnumMap<>(DeviceKind.class);

   private DeviceWorkbenchAdapters() {
   }

   public static void register(DeviceWorkbenchAdapter adapter) {
      BY_KIND.put(adapter.deviceKind(), adapter);
   }

   public static Optional<DeviceWorkbenchAdapter> get(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof DeviceItem device
         ? Optional.ofNullable(BY_KIND.get(device.deviceKind()))
         : Optional.empty();
   }

   static {
      register(ArmorWorkbenchAdapter.HELMET);
      register(ArmorWorkbenchAdapter.CHESTPLATE);
      register(ArmorWorkbenchAdapter.LEGGINGS);
      register(ArmorWorkbenchAdapter.BOOTS);
      register(RailgunWorkbenchAdapter.INSTANCE);
   }
}
