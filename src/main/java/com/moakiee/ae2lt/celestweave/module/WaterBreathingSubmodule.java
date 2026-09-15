package com.moakiee.ae2lt.celestweave.module;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class WaterBreathingSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final WaterBreathingSubmodule INSTANCE = new WaterBreathingSubmodule();
   private static final int EFFECT_DURATION_TICKS = 300;
   private static final int REFRESH_INTERVAL_TICKS = 60;

   private WaterBreathingSubmodule() {
   }

   @Override
   public String id() {
      return "water_breathing";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.water_breathing.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.water_breathing.desc";
   }

   @Override
   public boolean defaultEnabled() {
      return true;
   }

   @Override
   public int getMaxInstallAmount() {
      return 1;
   }

   @Override
   public void onActivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         applyEffect(player);
      }
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         player.m_21195_(MobEffects.f_19608_);
      }
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER && player.f_19797_ % 60 == 0) {
         applyEffect(player);
      }

      return 0;
   }

   private static void applyEffect(Player player) {
      player.m_7292_(new MobEffectInstance(MobEffects.f_19608_, 300, 0, false, false, true));
   }
}
