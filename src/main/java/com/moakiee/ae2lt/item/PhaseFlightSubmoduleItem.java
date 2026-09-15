package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.capability.FlightKind;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class PhaseFlightSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public PhaseFlightSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.LEGS,
         PhaseFlightSubmodule.INSTANCE,
         stack -> List.of(
               new DeviceCapability.FlightMode(FlightKind.CREATIVE),
               new DeviceCapability.ElytraFlight(),
               new DeviceCapability.PassiveDrain(5000L),
               new DeviceCapability.PhaseTraversal(400000L)
            )
      );
   }
}
