package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.FlightSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.capability.FlightKind;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class FlightSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public FlightSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.LEGS,
         FlightSubmodule.INSTANCE,
         stack -> List.of(new DeviceCapability.FlightMode(FlightKind.CREATIVE), new DeviceCapability.ElytraFlight(), new DeviceCapability.PassiveDrain(5000L))
      );
   }
}
