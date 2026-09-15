package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.MultidimensionalProtectionSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class MultidimensionalProtectionSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public MultidimensionalProtectionSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.CHEST,
         MultidimensionalProtectionSubmodule.INSTANCE,
         stack -> List.of(new DeviceCapability.StagedMitigation("multidimensional_protection"), new DeviceCapability.LastStandTuning(0L, 0))
      );
   }
}
