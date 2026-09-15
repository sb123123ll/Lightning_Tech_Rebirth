package com.moakiee.ae2lt.blockentity.workbench;

import appeng.menu.SlotSemantic;
import com.moakiee.ae2lt.celestweave.ArmorEnergyModuleItem;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import com.moakiee.ae2lt.device.energy.NetworkBoundEnergyBuffer;
import com.moakiee.ae2lt.device.module.DeviceModuleStorage;
import com.moakiee.ae2lt.device.network.DeviceNetworkBinding;
import com.moakiee.ae2lt.device.network.RailgunNetworkBinding;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import com.moakiee.ae2lt.item.railgun.RailgunModuleItem;
import com.moakiee.ae2lt.item.railgun.RailgunModuleStorage;
import com.moakiee.ae2lt.item.railgun.RailgunStructuralCore;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RailgunWorkbenchAdapter implements DeviceWorkbenchAdapter {
   public static final RailgunWorkbenchAdapter INSTANCE = new RailgunWorkbenchAdapter();
   private static final List<StructuralSlotSpec> STRUCTURAL_SLOTS = List.of(slot(0, DeviceSlotType.CORE, Ae2ltSlotSemantics.OVERLOAD_DEVICE_WORKBENCH_CORE));

   private RailgunWorkbenchAdapter() {
   }

   @Override
   public DeviceKind deviceKind() {
      return DeviceKind.RAILGUN;
   }

   @Override
   public DeviceModuleStorage moduleStorage() {
      return RailgunModuleStorage.INSTANCE;
   }

   @Override
   public DeviceEnergyBuffer energyBuffer() {
      return NetworkBoundEnergyBuffer.INSTANCE;
   }

   @Override
   public DeviceNetworkBinding networkBinding() {
      return RailgunNetworkBinding.INSTANCE;
   }

   @Override
   public List<StructuralSlotSpec> structuralSlots() {
      return STRUCTURAL_SLOTS;
   }

   @Override
   public Predicate<ItemStack> moduleInputValidator(ItemStack device, Provider registries) {
      return stack -> (stack.m_41720_() instanceof RailgunModuleItem || stack.m_41720_() instanceof ArmorEnergyModuleItem)
            && RailgunModuleStorage.INSTANCE.canInstallOne(device, stack);
   }

   @Override
   public List<ItemStack> listModuleEntries(ItemStack device, Provider registries) {
      return RailgunModuleStorage.INSTANCE.listEntries(device);
   }

   @Override
   public boolean canInstallOne(ItemStack device, Provider registries, ItemStack candidate) {
      return RailgunModuleStorage.INSTANCE.canInstallOne(device, candidate);
   }

   @Override
   public boolean installOne(ItemStack device, Provider registries, ItemStack candidate) {
      return RailgunModuleStorage.INSTANCE.installOne(device, candidate);
   }

   @Override
   public ItemStack uninstallOne(ItemStack device, Provider registries, String typeId) {
      return RailgunModuleStorage.INSTANCE.uninstallOne(device, typeId);
   }

   @Override
   public ItemStack uninstallAll(ItemStack device, Provider registries, String typeId) {
      return RailgunModuleStorage.INSTANCE.uninstallAll(device, typeId);
   }

   @Override
   public String moduleTypeId(ItemStack stack) {
      return RailgunModuleEntries.typeId(stack);
   }

   @Override
   public int maxInstallAmount(ItemStack stack) {
      if (stack.m_41720_() instanceof ArmorEnergyModuleItem) {
         return 1;
      } else {
         return stack.m_41720_() instanceof RailgunModuleItem module ? module.getMaxInstallAmount() : 0;
      }
   }

   @Override
   public ItemStack getStructuralSlot(ItemStack device, Provider registries, StructuralSlotSpec spec) {
      return switch (spec.slotType()) {
         case CORE -> RailgunStructuralCore.getCore(device);
         default -> ItemStack.f_41583_;
      };
   }

   @Override
   public void setStructuralSlot(ItemStack device, Provider registries, StructuralSlotSpec spec, ItemStack stack) {
      if (stack.m_41619_() || this.canPlaceStructural(device, registries, spec, stack)) {
         switch (spec.slotType()) {
            case CORE:
               RailgunStructuralCore.setCore(device, stack);
         }
      }
   }

   @Override
   public ItemStack removeStructuralSlot(ItemStack device, Provider registries, StructuralSlotSpec spec, int amount) {
      return switch (spec.slotType()) {
         case CORE -> RailgunStructuralCore.removeCore(device, amount);
         default -> ItemStack.f_41583_;
      };
   }

   @Override
   public boolean canPlaceStructural(ItemStack device, Provider registries, StructuralSlotSpec spec, ItemStack stack) {
      if (!device.m_41619_() && !stack.m_41619_()) {
         return switch (spec.slotType()) {
            case CORE -> stack.m_150930_((Item)ModItems.ULTIMATE_OVERLOAD_CORE.get()) && RailgunStructuralCore.canInstallCore(device, stack);
            default -> false;
         };
      } else {
         return false;
      }
   }

   private static StructuralSlotSpec slot(int index, DeviceSlotType type, SlotSemantic semantic) {
      return new StructuralSlotSpec(index, type, semantic);
   }
}
