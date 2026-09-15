package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmodule;
import com.moakiee.ae2lt.celestweave.module.CelestweaveArmorSubmoduleItem;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.module.ModuleTooltip;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSingleArmorSubmoduleItem extends Item implements CelestweaveArmorSubmoduleItem {
   private final ArmorPart armorPart;
   private final CelestweaveArmorSubmodule submodule;
   private final Function<ItemStack, List<DeviceCapability>> capabilityFactory;

   protected AbstractSingleArmorSubmoduleItem(
      Properties properties, ArmorPart armorPart, CelestweaveArmorSubmodule submodule, Function<ItemStack, List<DeviceCapability>> capabilityFactory
   ) {
      super(properties.m_41487_(16));
      this.armorPart = armorPart;
      this.submodule = submodule;
      this.capabilityFactory = capabilityFactory;
   }

   @Override
   public ArmorPart armorPart() {
      return this.armorPart;
   }

   @Override
   public void collectSubmodules(ItemStack stack, Consumer<CelestweaveArmorSubmodule> output) {
      output.accept(this.submodule);
   }

   @Override
   public List<DeviceCapability> capabilities(ItemStack stack) {
      return this.capabilityFactory.apply(stack.m_255036_(1));
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      ModuleTooltip.appendInstallInfo(this, tooltip);
   }
}
