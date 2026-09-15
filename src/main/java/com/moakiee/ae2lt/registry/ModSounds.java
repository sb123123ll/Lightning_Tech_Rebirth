package com.moakiee.ae2lt.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.f_256840_, "ae2lt");
   public static final RegistryObject<SoundEvent> RAILGUN_FIRE_CHARGED = register("railgun.fire.charged");
   public static final RegistryObject<SoundEvent> RAILGUN_FIRE_MAX = register("railgun.fire.max");
   public static final RegistryObject<SoundEvent> RAILGUN_FIRE_IMPACT = register("railgun.fire.impact");
   public static final RegistryObject<SoundEvent> RAILGUN_BEAM_CHAIN = register("railgun.beam.chain");
   public static final RegistryObject<SoundEvent> RAILGUN_BEAM_LOOP = register("railgun.beam.loop");
   public static final RegistryObject<SoundEvent> RAILGUN_CHARGE_RAMP = register("railgun.charge.ramp");
   public static final RegistryObject<SoundEvent> RAILGUN_CHARGE_SUSTAIN = register("railgun.charge.loop");

   private static RegistryObject<SoundEvent> register(String name) {
      ResourceLocation id = new ResourceLocation("ae2lt", name);
      return SOUND_EVENTS.register(name, () -> SoundEvent.m_262824_(id));
   }

   private ModSounds() {
   }
}
