package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.DigAffinitySubmodule;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class DigAffinitySubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public DigAffinitySubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.FEET,
         DigAffinitySubmodule.INSTANCE,
         stack -> List.of(
               new DeviceCapability.DigAffinity("underwater", AE2LTCommonConfig.overloadArmorUnderwaterDigMultiplier()),
               new DeviceCapability.DigAffinity("airborne", AE2LTCommonConfig.overloadArmorAirborneDigMultiplier()),
               new DeviceCapability.PassiveDrain(1800L)
            )
      );
   }
}
