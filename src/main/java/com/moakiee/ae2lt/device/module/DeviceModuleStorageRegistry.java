package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.device.DeviceItem;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.item.railgun.RailgunModuleStorage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class DeviceModuleStorageRegistry {
   private static final Map<DeviceKind, DeviceModuleStorage> BY_KIND = new EnumMap<>(DeviceKind.class);

   private DeviceModuleStorageRegistry() {
   }

   public static void register(DeviceModuleStorage storage) {
      BY_KIND.put(storage.deviceKind(), storage);
   }

   public static Optional<DeviceModuleStorage> get(DeviceKind kind) {
      return Optional.ofNullable(BY_KIND.get(kind));
   }

   public static Optional<DeviceModuleStorage> get(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof DeviceItem device ? get(device.deviceKind()) : Optional.empty();
   }

   static {
      register(RailgunModuleStorage.INSTANCE);
      register(ArmorModuleStorage.HEAD);
      register(ArmorModuleStorage.CHEST);
      register(ArmorModuleStorage.LEGS);
      register(ArmorModuleStorage.FEET);
   }
}
