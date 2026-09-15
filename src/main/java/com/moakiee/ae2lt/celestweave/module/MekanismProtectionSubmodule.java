package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.integration.mekanism.MekanismArmorIntegration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class MekanismProtectionSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final MekanismProtectionSubmodule RADIATION = new MekanismProtectionSubmodule(
      "radiation_protection", "radiation", "ae2lt.celestweave.feature.radiation_protection.name", "ae2lt.celestweave.feature.radiation_protection.desc"
   );
   public static final MekanismProtectionSubmodule LASER = new MekanismProtectionSubmodule(
      "laser_protection", "laser", "ae2lt.celestweave.feature.laser_protection.name", "ae2lt.celestweave.feature.laser_protection.desc"
   );
   private final String id;
   private final ResourceKey<DamageType> damageType;
   private final String nameKey;
   private final String descriptionKey;

   private MekanismProtectionSubmodule(String id, String mekanismDamageType, String nameKey, String descriptionKey) {
      this.id = id;
      this.damageType = ResourceKey.m_135785_(Registries.f_268580_, new ResourceLocation("mekanism", mekanismDamageType));
      this.nameKey = nameKey;
      this.descriptionKey = descriptionKey;
   }

   @Override
   public String id() {
      return this.id;
   }

   public ResourceKey<DamageType> damageType() {
      return this.damageType;
   }

   @Override
   public String nameKey() {
      return this.nameKey;
   }

   @Override
   public String descriptionKey() {
      return this.descriptionKey;
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
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      if (this == RADIATION && dist == Dist.DEDICATED_SERVER && player instanceof ServerPlayer serverPlayer && ModList.get().isLoaded("mekanism")) {
         MekanismArmorIntegration.tickRadiationRegeneration(serverPlayer);
      }

      return 0;
   }
}
