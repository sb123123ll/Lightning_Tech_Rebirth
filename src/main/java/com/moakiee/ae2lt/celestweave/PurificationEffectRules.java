package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;

public final class PurificationEffectRules {
   public static final TagKey<MobEffect> MEKANISM_SPEED_UP_BLACKLIST = TagKey.m_203882_(
      Registries.f_256929_, new ResourceLocation("mekanism", "speed_up_blacklist")
   );

   private PurificationEffectRules() {
   }

   public static boolean canPurify(MobEffectInstance effect) {
      return effect != null && isConfiguredCategory(effect.m_19544_().m_19483_())
         ? effect.getCurativeItems().stream().anyMatch(stack -> stack.m_150930_(Items.f_42455_))
            && !BuiltInRegistries.f_256974_.m_263177_(effect.m_19544_()).m_203656_(MEKANISM_SPEED_UP_BLACKLIST)
         : false;
   }

   private static boolean isConfiguredCategory(MobEffectCategory category) {
      return switch (category) {
         case BENEFICIAL -> AE2LTCommonConfig.overloadArmorPurificationBeneficialEffects();
         case NEUTRAL -> AE2LTCommonConfig.overloadArmorPurificationNeutralEffects();
         case HARMFUL -> AE2LTCommonConfig.overloadArmorPurificationHarmfulEffects();
         default -> throw new IncompatibleClassChangeError();
      };
   }
}
