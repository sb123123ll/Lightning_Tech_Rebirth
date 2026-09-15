package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.MovementAssistSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class MovementAssistSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public MovementAssistSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.FEET,
         MovementAssistSubmodule.INSTANCE,
         stack -> List.of(new DeviceCapability.MovementAssist(), new DeviceCapability.PassiveDrain(2000L))
      );
   }
}
