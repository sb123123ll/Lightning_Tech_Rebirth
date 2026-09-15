package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.MekanismProtectionSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class MekanismProtectionSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public MekanismProtectionSubmoduleItem(Properties properties, MekanismProtectionSubmodule submodule) {
      super(properties, ArmorPart.CHEST, submodule, stack -> List.of(new DeviceCapability.DamageTypeImmunity(submodule.damageType())));
   }
}
