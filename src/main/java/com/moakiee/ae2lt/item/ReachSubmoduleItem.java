package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.ReachSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class ReachSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public ReachSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.CHEST,
         ReachSubmodule.INSTANCE,
         stack -> List.of(new DeviceCapability.InteractionRange(), new DeviceCapability.PassiveDrain(2500L))
      );
   }
}
