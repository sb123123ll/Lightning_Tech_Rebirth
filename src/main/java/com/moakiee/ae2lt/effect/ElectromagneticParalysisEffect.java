package com.moakiee.ae2lt.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ElectromagneticParalysisEffect extends MobEffect {
   public ElectromagneticParalysisEffect() {
      super(MobEffectCategory.HARMFUL, 8961023);
   }

   public boolean m_6584_(int duration, int amplifier) {
      return duration % 4 == 0;
   }

   public void m_6742_(LivingEntity entity, int amplifier) {
      if (entity.m_9236_() instanceof ServerLevel level) {
         double w = (double)entity.m_20205_() * 0.4;
         double h = (double)entity.m_20206_() * 0.4;
         level.m_8767_(ParticleTypes.f_175830_, entity.m_20185_(), entity.m_20186_() + (double)entity.m_20206_() / 2.0, entity.m_20189_(), 4, w, h, w, 0.07);
      }
   }
}
