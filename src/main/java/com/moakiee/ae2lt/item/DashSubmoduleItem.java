package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.DashSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class DashSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public DashSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.FEET,
         DashSubmodule.INSTANCE,
         stack -> List.of(new DeviceCapability.DashEffect(1.8, 40), new DeviceCapability.PassiveDrain(0L), new DeviceCapability.ActiveCost("dash", 50000L))
      );
   }
}
