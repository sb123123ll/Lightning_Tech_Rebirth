package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.ReflectSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class ReflectSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public ReflectSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.CHEST,
         ReflectSubmodule.INSTANCE,
         stack -> List.of(new DeviceCapability.ReflectTuning(0.3, 5000L), new DeviceCapability.PassiveDrain(0L))
      );
   }
}
