package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.SaturationSubmodule;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class SaturationSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public SaturationSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.HEAD,
         SaturationSubmodule.INSTANCE,
         stack -> List.of(
               new DeviceCapability.FoodSustain(20, 20.0F, AE2LTCommonConfig.overloadArmorSaturationCheckIntervalTicks()),
               new DeviceCapability.PassiveDrain(1200L)
            )
      );
   }
}
