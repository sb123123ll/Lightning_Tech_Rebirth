package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.PurificationSubmodule;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class PurificationSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public PurificationSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.CHEST,
         PurificationSubmodule.INSTANCE,
         stack -> List.of(
               new DeviceCapability.PurificationTuning(AE2LTCommonConfig.overloadArmorPurificationPeriodTicks(), Integer.MAX_VALUE),
               new DeviceCapability.PassiveDrain(6000L)
            )
      );
   }
}
