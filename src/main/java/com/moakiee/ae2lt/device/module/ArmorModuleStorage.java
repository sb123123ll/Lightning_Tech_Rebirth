package com.moakiee.ae2lt.device.module;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.device.DeviceKind;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

public final class ArmorModuleStorage implements DeviceModuleStorage {
   public static final ArmorModuleStorage HEAD = new ArmorModuleStorage(ArmorPart.HEAD);
   public static final ArmorModuleStorage CHEST = new ArmorModuleStorage(ArmorPart.CHEST);
   public static final ArmorModuleStorage LEGS = new ArmorModuleStorage(ArmorPart.LEGS);
   public static final ArmorModuleStorage FEET = new ArmorModuleStorage(ArmorPart.FEET);
   private final ArmorPart part;

   public ArmorModuleStorage(ArmorPart part) {
      this.part = part;
   }

   @Override
   public DeviceKind deviceKind() {
      return this.part.deviceKind();
   }

   @Override
   public List<ItemStack> listEntries(ItemStack device) {
      return List.of();
   }

   @Override
   public int getCount(ItemStack device, String typeId) {
      return 0;
   }

   @Override
   public boolean canInstallOne(ItemStack device, ItemStack candidate) {
      return false;
   }

   @Override
   public boolean installOne(ItemStack device, ItemStack candidate) {
      return false;
   }

   @Override
   public ItemStack uninstallOne(ItemStack device, String typeId) {
      return ItemStack.f_41583_;
   }

   @Override
   public ItemStack uninstallAll(ItemStack device, String typeId) {
      return ItemStack.f_41583_;
   }

   @Override
   public boolean hasAnyInstalled(ItemStack device) {
      return false;
   }

   @Override
   public Stream<ItemStack> installedModuleStacks(ItemStack device) {
      ArrayList<ItemStack> result = new ArrayList<>();

      for (ItemStack stack : this.listEntries(device)) {
         for (int i = 0; i < stack.m_41613_(); i++) {
            result.add(stack.m_255036_(1));
         }
      }

      return result.stream();
   }
}
