package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.UndyingSubmodule;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class UndyingSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public UndyingSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.CHEST,
         UndyingSubmodule.INSTANCE,
         stack -> List.of(
               new DeviceCapability.LastStandTuning(2000000000L, AE2LTCommonConfig.overloadArmorUndyingComboWindowTicks()),
               new DeviceCapability.PassiveDrain(4000L)
            )
      );
   }
}
