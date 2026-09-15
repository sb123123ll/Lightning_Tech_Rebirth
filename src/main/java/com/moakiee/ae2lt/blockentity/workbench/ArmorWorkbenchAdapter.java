package com.moakiee.ae2lt.blockentity.workbench;

import appeng.menu.SlotSemantic;
import com.moakiee.ae2lt.celestweave.ArmorDeviceEnergyBuffer;
import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.device.energy.DeviceEnergyBuffer;
import com.moakiee.ae2lt.device.module.ArmorModuleStorage;
import com.moakiee.ae2lt.device.module.DeviceModuleStorage;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import com.moakiee.ae2lt.device.network.ArmorNetworkBinding;
import com.moakiee.ae2lt.device.network.DeviceNetworkBinding;
import com.moakiee.ae2lt.menu.Ae2ltSlotSemantics;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;

public final class ArmorWorkbenchAdapter implements DeviceWorkbenchAdapter {
   public static final ArmorWorkbenchAdapter HELMET = new ArmorWorkbenchAdapter(ArmorPart.HEAD);
   public static final ArmorWorkbenchAdapter CHESTPLATE = new ArmorWorkbenchAdapter(ArmorPart.CHEST);
   public static final ArmorWorkbenchAdapter LEGGINGS = new ArmorWorkbenchAdapter(ArmorPart.LEGS);
   public static final ArmorWorkbenchAdapter BOOTS = new ArmorWorkbenchAdapter(ArmorPart.FEET);
   private static final List<StructuralSlotSpec> STRUCTURAL_SLOTS = List.of(slot(0, DeviceSlotType.CORE, Ae2ltSlotSemantics.OVERLOAD_DEVICE_WORKBENCH_CORE));
   private final ArmorPart part;
   private final ArmorModuleStorage moduleStorage;

   private ArmorWorkbenchAdapter(ArmorPart part) {
      this.part = part;
      this.moduleStorage = new ArmorModuleStorage(part);
   }

   @Override
   public DeviceKind deviceKind() {
      return this.part.deviceKind();
   }

   @Override
   public DeviceModuleStorage moduleStorage() {
      return this.moduleStorage;
   }

   @Override
   public DeviceEnergyBuffer energyBuffer() {
      return ArmorDeviceEnergyBuffer.INSTANCE;
   }

   @Override
   public DeviceNetworkBinding networkBinding() {
      return ArmorNetworkBinding.INSTANCE;
   }

   @Override
   public List<StructuralSlotSpec> structuralSlots() {
      return STRUCTURAL_SLOTS;
   }

   @Override
   public Predicate<ItemStack> moduleInputValidator(ItemStack device, Provider registries) {
      return stack -> stack.m_41720_() instanceof OverloadDeviceModuleItem && CelestweaveArmorState.canInstallModule(device, registries, stack);
   }

   @Override
   public List<ItemStack> listModuleEntries(ItemStack device, Provider registries) {
      return CelestweaveArmorState.loadModuleStacks(device, registries);
   }

   @Override
   public boolean canInstallOne(ItemStack device, Provider registries, ItemStack candidate) {
      return CelestweaveArmorState.canInstallModule(device, registries, candidate);
   }

   @Override
   public boolean installOne(ItemStack device, Provider registries, ItemStack candidate) {
      return CelestweaveArmorState.installOneModule(device, registries, candidate);
   }

   @Override
   public ItemStack uninstallOne(ItemStack device, Provider registries, String typeId) {
      return CelestweaveArmorState.uninstallOneModule(device, registries, typeId);
   }

   @Override
   public ItemStack uninstallAll(ItemStack device, Provider registries, String typeId) {
      return CelestweaveArmorState.uninstallAllOfType(device, registries, typeId);
   }

   @Override
   public String moduleTypeId(ItemStack stack) {
      return CelestweaveArmorState.moduleTypeId(stack);
   }

   @Override
   public int maxInstallAmount(ItemStack stack) {
      return CelestweaveArmorState.getSubmoduleMaxInstallAmountForStack(stack);
   }

   @Override
   public ItemStack getStructuralSlot(ItemStack device, Provider registries, StructuralSlotSpec spec) {
      return CelestweaveArmorState.getSlot(device, registries, toArmorSlot(spec));
   }

   @Override
   public void setStructuralSlot(ItemStack device, Provider registries, StructuralSlotSpec spec, ItemStack stack) {
      if (stack == null || stack.m_41619_() || spec.slotType() != DeviceSlotType.CORE || CelestweaveArmorState.canInstallCore(device, registries, stack)) {
         CelestweaveArmorState.ensureArmorId(device);
         CelestweaveArmorState.setSlot(device, registries, toArmorSlot(spec), stack.m_41777_());
      }
   }

   @Override
   public ItemStack removeStructuralSlot(ItemStack device, Provider registries, StructuralSlotSpec spec, int amount) {
      if (amount <= 0) {
         return ItemStack.f_41583_;
      } else {
         ItemStack existing = this.getStructuralSlot(device, registries, spec);
         if (existing.m_41619_()) {
            return ItemStack.f_41583_;
         } else if (amount >= existing.m_41613_()) {
            this.setStructuralSlot(device, registries, spec, ItemStack.f_41583_);
            return existing;
         } else {
            ItemStack remaining = existing.m_41777_();
            ItemStack removed = remaining.m_41620_(amount);
            this.setStructuralSlot(device, registries, spec, remaining);
            return removed;
         }
      }
   }

   @Override
   public boolean canPlaceStructural(ItemStack device, Provider registries, StructuralSlotSpec spec, ItemStack stack) {
      if (!device.m_41619_() && !stack.m_41619_()) {
         return switch (spec.slotType()) {
            case CORE -> stack.m_150930_((Item)ModItems.ULTIMATE_OVERLOAD_CORE.get()) && CelestweaveArmorState.canInstallCore(device, registries, stack);
            default -> false;
         };
      } else {
         return false;
      }
   }

   @Override
   public boolean mayPickupStructural(ItemStack device, Provider registries, StructuralSlotSpec spec, Player player, ItemStack carried) {
      return !device.m_41619_();
   }

   @Override
   public void onDeviceInserted(ItemStack device) {
      CelestweaveArmorState.ensureArmorId(device);
   }

   @Override
   public void onModulesChanged(ItemStack device, Provider registries, Dist dist) {
      CelestweaveArmorState.ensureArmorId(device);
      CelestweaveArmorState.reconcileInstalledSubmodules(null, device, registries, dist);
   }

   private static StructuralSlotSpec slot(int index, DeviceSlotType type, SlotSemantic semantic) {
      return new StructuralSlotSpec(index, type, semantic);
   }

   private static int toArmorSlot(StructuralSlotSpec spec) {
      return switch (spec.slotType()) {
         case CORE -> 0;
         default -> -1;
      };
   }
}
