package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.ResistanceSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.world.item.Item.Properties;

public final class ResistanceSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public ResistanceSubmoduleItem(Properties properties, ResistanceSubmodule submodule) {
      super(
         properties,
         ArmorPart.CHEST,
         submodule,
         stack -> List.of(new DeviceCapability.StagedMitigation(submodule.id()), new DeviceCapability.PassiveDrain(1000L))
      );
   }
}
