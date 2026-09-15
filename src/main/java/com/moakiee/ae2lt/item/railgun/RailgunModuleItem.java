package com.moakiee.ae2lt.item.railgun;

import com.moakiee.ae2lt.device.DeviceKind;
import com.moakiee.ae2lt.device.DeviceSlotType;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.ModuleTooltip;
import com.moakiee.ae2lt.device.module.OverloadDeviceModuleItem;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class RailgunModuleItem extends Item implements OverloadDeviceModuleItem {
   private final RailgunModuleType type;

   public RailgunModuleItem(Properties properties, RailgunModuleType type) {
      super(properties);
      this.type = type;
   }

   public RailgunModuleType moduleType() {
      return this.type;
   }

   @Override
   public int getMaxInstallAmount() {
      return maxInstallAmount(this.type);
   }

   static int maxInstallAmount(RailgunModuleType type) {
      return RailgunModuleRules.maxInstallAmount(type);
   }

   @Override
   public Set<DeviceKind> acceptableDevices() {
      return RailgunModuleRules.acceptableDevices(this.type);
   }

   @Override
   public DeviceSlotType acceptableSlot() {
      return RailgunModuleRules.acceptableSlot(this.type);
   }

   @Override
   public boolean accepts(DeviceKind deviceKind, DeviceSlotType slotType) {
      return accepts(this.type, deviceKind, slotType);
   }

   static boolean accepts(RailgunModuleType type, DeviceKind deviceKind, DeviceSlotType slotType) {
      return RailgunModuleRules.accepts(type, deviceKind, slotType);
   }

   @Override
   public String moduleTypeId(ItemStack stack) {
      return this.type.m_7912_();
   }

   @Override
   public List<DeviceCapability> capabilities(ItemStack stack) {
      return capabilitiesFor(this.type);
   }

   static List<DeviceCapability> capabilitiesFor(RailgunModuleType type) {
      return RailgunModuleRules.capabilitiesFor(type);
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      super.m_7373_(stack, level, tooltip, flag);
      ModuleTooltip.appendInstallInfo(this, tooltip);
   }
}
