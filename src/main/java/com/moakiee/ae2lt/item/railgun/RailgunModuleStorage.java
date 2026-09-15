package com.moakiee.ae2lt.item.railgun;

import com.moakiee.ae2lt.celestweave.ArmorEnergyModuleItem;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.DeviceModuleStorage;
import com.moakiee.ae2lt.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

public final class RailgunModuleStorage implements DeviceModuleStorage {
   public static final RailgunModuleStorage INSTANCE = new RailgunModuleStorage();

   private RailgunModuleStorage() {
   }

   @Override
   public DeviceKind deviceKind() {
      return DeviceKind.RAILGUN;
   }

   @Override
   public List<ItemStack> listEntries(ItemStack device) {
      return entryData(device).entries().stream().<ItemStack>map(ItemStack::m_41777_).toList();
   }

   @Override
   public int getCount(ItemStack device, String typeId) {
      return entryData(device).getCount(typeId);
   }

   @Override
   public boolean canInstallOne(ItemStack device, ItemStack candidate) {
      if (candidate != null && !candidate.m_41619_()) {
         if (candidate.m_41720_() instanceof ArmorEnergyModuleItem energyModule) {
            RailgunModuleEntries entries = entryData(device);
            return energyModule.acceptableDevices().contains(DeviceKind.RAILGUN) && entries.getCount("energy") < 1;
         } else if (candidate.m_41720_() instanceof RailgunModuleItem module) {
            RailgunModuleEntries var7 = entryData(device);
            if (!RailgunStructuralCore.hasCore(device)) {
               return false;
            } else {
               return (module.moduleType() == RailgunModuleType.OVERLOAD_EXECUTION || module.moduleType() == RailgunModuleType.MULTIDIMENSIONAL_EXECUTION)
                     && var7.hasAnyExecution()
                  ? false
                  : var7.getCount(module.moduleType()) < module.getMaxInstallAmount();
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean installOne(ItemStack device, ItemStack candidate) {
      if (!this.canInstallOne(device, candidate)) {
         return false;
      } else {
         ArrayList<ItemStack> stacks = new ArrayList<>(this.listEntries(device));
         String typeId = RailgunModuleEntries.typeId(candidate);

         for (ItemStack stack : stacks) {
            if (typeId.equals(RailgunModuleEntries.typeId(stack))) {
               stack.m_41769_(1);
               setEntries(device, new RailgunModuleEntries(stacks));
               return true;
            }
         }

         stacks.add(candidate.m_255036_(1));
         setEntries(device, new RailgunModuleEntries(stacks));
         return true;
      }
   }

   @Override
   public ItemStack uninstallOne(ItemStack device, String typeId) {
      if (typeId != null && !typeId.isBlank()) {
         ArrayList<ItemStack> stacks = new ArrayList<>(this.listEntries(device));

         for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (typeId.equals(RailgunModuleEntries.typeId(stack))) {
               ItemStack detached = stack.m_255036_(1);
               if (stack.m_41613_() <= 1) {
                  stacks.remove(index);
               } else {
                  stack.m_41774_(1);
               }

               setEntries(device, new RailgunModuleEntries(stacks));
               return detached;
            }
         }

         return ItemStack.f_41583_;
      } else {
         return ItemStack.f_41583_;
      }
   }

   @Override
   public ItemStack uninstallAll(ItemStack device, String typeId) {
      if (typeId != null && !typeId.isBlank()) {
         ArrayList<ItemStack> stacks = new ArrayList<>(this.listEntries(device));

         for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (typeId.equals(RailgunModuleEntries.typeId(stack))) {
               ItemStack detached = stack.m_41777_();
               stacks.remove(index);
               setEntries(device, new RailgunModuleEntries(stacks));
               return detached;
            }
         }

         return ItemStack.f_41583_;
      } else {
         return ItemStack.f_41583_;
      }
   }

   @Override
   public boolean hasAnyInstalled(ItemStack device) {
      return !entryData(device).entries().isEmpty();
   }

   @Override
   public Stream<ItemStack> installedModuleStacks(ItemStack device) {
      return entryData(device).installedModuleStacks();
   }

   public List<DeviceCapability> capabilities(ItemStack device) {
      return entryData(device).capabilities();
   }

   public static RailgunModuleEntries entryData(ItemStack device) {
      return ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(device, RailgunModuleEntries.EMPTY);
   }

   public static void setEntries(ItemStack device, RailgunModuleEntries entries) {
      if (entries != null && !entries.entries().isEmpty()) {
         ModDataComponents.RAILGUN_MODULE_ENTRIES.set(device, entries);
      } else {
         ModDataComponents.RAILGUN_MODULE_ENTRIES.remove(device);
      }
   }
}
