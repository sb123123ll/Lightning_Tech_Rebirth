package com.moakiee.ae2lt.registry;

import com.moakiee.ae2lt.effect.ElectromagneticParalysisEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMobEffects {
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.f_256929_, "ae2lt");
   public static final RegistryObject<ElectromagneticParalysisEffect> ELECTROMAGNETIC_PARALYSIS = EFFECTS.register("electromagnetic_paralysis", () -> {
      ElectromagneticParalysisEffect effect = new ElectromagneticParalysisEffect();
      effect.m_19472_(Attributes.f_22279_, "2c6f9d8a-4e5b-4a3f-b7d1-9e8c2a5f0b64", -0.75, Operation.MULTIPLY_TOTAL);
      return effect;
   });

   private ModMobEffects() {
   }
}
