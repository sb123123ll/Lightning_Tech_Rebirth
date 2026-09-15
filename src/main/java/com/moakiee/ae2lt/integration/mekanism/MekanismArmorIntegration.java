package com.moakiee.ae2lt.integration.mekanism;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.MekanismProtectionRules;
import com.moakiee.ae2lt.celestweave.module.MekanismProtectionSubmodule;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.celestweave.phase.PhaseLockProjectionLink;
import com.moakiee.ae2lt.celestweave.service.ArmorEnergyService;
import com.moakiee.ae2lt.celestweave.service.CelestweaveAdvancementService;
import com.moakiee.ae2lt.celestweave.state.ArmorRuntimeRegistry;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.registry.ModItems;
import java.util.Optional;
import mekanism.api.lasers.ILaserDissipation;
import mekanism.api.math.FloatingLong;
import mekanism.api.radiation.IRadiationManager;
import mekanism.api.radiation.capability.IRadiationShielding;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.lib.radiation.RadiationManager.RadiationScale;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class MekanismArmorIntegration {
   private static final FloatingLong MAX_SIGNED_LONG = FloatingLong.createConst(Long.MAX_VALUE);
   private static final IRadiationShielding FULL_RADIATION_SHIELDING = () -> 1.0;
   private static final MekanismArmorIntegration.CelestweaveLaserDissipation FULL_LASER_DISSIPATION = new MekanismArmorIntegration.CelestweaveLaserDissipation();

   private MekanismArmorIntegration() {
   }

   public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
      final ItemStack stack = (ItemStack)event.getObject();
      if (stack.m_150930_((Item)ModItems.CELESTWEAVE_CORE.get()) || stack.m_150930_((Item)ModItems.PHASE_LOCK_PROJECTION.get())) {
         event.addCapability(
            new ResourceLocation("ae2lt", "mekanism_armor_protection"),
            new ICapabilityProvider() {
               public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                  if (cap == Capabilities.RADIATION_SHIELDING && MekanismArmorIntegration.isProtectionActive(stack, MekanismProtectionSubmodule.RADIATION.id())
                     )
                   {
                     return LazyOptional.of(() -> MekanismArmorIntegration.FULL_RADIATION_SHIELDING).cast();
                  } else {
                     return cap == Capabilities.LASER_DISSIPATION && MekanismArmorIntegration.isProtectionActive(stack, MekanismProtectionSubmodule.LASER.id())
                        ? LazyOptional.of(() -> MekanismArmorIntegration.FULL_LASER_DISSIPATION).cast()
                        : LazyOptional.empty();
                  }
               }
            }
         );
      }
   }

   static boolean isProtectionActive(ItemStack stack, String submoduleId) {
      PhaseLockProjectionLink projectionLink = ModDataComponents.PHASE_LOCK_PROJECTION_LINK.get(stack);
      return projectionLink != null
         ? ArmorRuntimeRegistry.isSubmoduleRuntimeActive(projectionLink.armorId(), submoduleId)
         : CelestweaveArmorState.isSubmoduleRuntimeActive(stack, submoduleId);
   }

   public static void tickRadiationRegeneration(ServerPlayer player) {
      IRadiationManager radiationManager = IRadiationManager.INSTANCE;
      double radiationLevel = radiationManager.getRadiationLevel(player);
      if (MekanismProtectionRules.shouldRegenerate(player.m_9236_().m_46467_(), radiationLevel, 1.0E-5, player.m_21223_(), player.m_21233_())) {
         float healthBefore = player.m_21223_();
         player.m_5634_(MekanismProtectionRules.radiationHealing(RadiationScale.getScaledDoseSeverity(radiationLevel)));
         if (player.m_21223_() > healthBefore) {
            CelestweaveAdvancementService.awardRadiationAssimilation(player);
         }
      }
   }

   public static void absorbLaserEnergy(LivingEntity target, FloatingLong absorbedEnergy) {
      if (target instanceof ServerPlayer player) {
         ItemStack chest = CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.CHEST);
         if (!chest.m_41619_() && CelestweaveArmorState.isSubmoduleRuntimeActive(chest, MekanismProtectionSubmodule.LASER.id())) {
            Optional<ILaserDissipation> laserDissipation = chest.getCapability(Capabilities.LASER_DISSIPATION).resolve();
            if (!laserDissipation.isEmpty() && laserDissipation.get() == FULL_LASER_DISSIPATION) {
               if (absorbedEnergy != null && !absorbedEnergy.isZero()) {
                  FloatingLong convertedEnergy = EnergyUnit.FORGE_ENERGY.convertFrom(absorbedEnergy);
                  long forgeEnergy = convertedEnergy.greaterThan(MAX_SIGNED_LONG) ? Long.MAX_VALUE : Math.max(0L, convertedEnergy.longValue());
                  ArmorEnergyService.receiveExternalEnergy(player, chest, forgeEnergy);
               }
            }
         }
      }
   }

   public static final class CelestweaveLaserDissipation implements ILaserDissipation {
      private CelestweaveLaserDissipation() {
      }

      public double getDissipationPercent() {
         return 1.0;
      }

      public double getRefractionPercent() {
         return 0.0;
      }
   }
}
