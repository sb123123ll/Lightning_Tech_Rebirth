package com.moakiee.ae2lt.item;

import com.moakiee.ae2lt.celestweave.ArmorPart;
import com.moakiee.ae2lt.celestweave.module.NightVisionSubmodule;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item.Properties;

public final class NightVisionSubmoduleItem extends AbstractSingleArmorSubmoduleItem {
   public NightVisionSubmoduleItem(Properties properties) {
      super(
         properties,
         ArmorPart.HEAD,
         NightVisionSubmodule.INSTANCE,
         stack -> List.of(
               new DeviceCapability.StatusEffectGrant(BuiltInRegistries.f_256974_.m_263177_(MobEffects.f_19611_), 0), new DeviceCapability.PassiveDrain(2000L)
            )
      );
   }
}
