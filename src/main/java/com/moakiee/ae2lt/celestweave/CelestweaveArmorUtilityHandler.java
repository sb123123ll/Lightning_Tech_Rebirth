package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.PhaseFlightSubmodule;
import com.moakiee.ae2lt.celestweave.module.SaturationSubmodule;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.celestweave.phase.PhaseLockService;
import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.celestweave.service.ArmorInteractionRangeService;
import com.moakiee.ae2lt.celestweave.service.ArmorMovementAssistService;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Applicable;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class CelestweaveArmorUtilityHandler {
   private CelestweaveArmorUtilityHandler() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         if (event.player instanceof ServerPlayer player) {
            PhaseLockService.tick(player);
            List var5 = ArmorCapabilityCollector.collectPerInstalledStack(player);
            extinguishShieldedPlayer(player, var5);
            ArmorInteractionRangeService.tick(player, var5);
            ArmorMovementAssistService.tick(player, var5);
            tickPurification(player, var5);
            tickFoodSustain(player, var5);
            ArmorCapabilityCollector.ActiveCapability phaseTraversal = findActivePhaseTraversal(var5);
            if (phaseTraversal != null && PhaseFlightSubmodule.shouldUsePhaseTraversal(player, phaseTraversal.armor())) {
               PhaseFlightSubmodule.applyTransientPhaseState(player);
            } else if (phaseTraversal == null && PhaseFlightSubmodule.hasTransientPhaseState(player)) {
               ItemStack escapeArmor = findPhaseFlightArmor(player);
               if (!PhaseFlightSubmodule.tickEscapePhase(player, escapeArmor)) {
                  PhaseFlightSubmodule.clearTransientPhaseState(player);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ItemStack removedArmor = event.getFrom();
         if (!removedArmor.m_41619_() && removedArmor.m_41720_() instanceof BaseCelestweaveArmorItem) {
            if (!PhaseLockService.keepsLogicallyEquipped(player, event.getSlot(), removedArmor)) {
               if (PhaseFlightArmorRemovalRules.shouldDeactivateRemovedArmor(CelestweaveArmorState.getArmorId(removedArmor), celestweaveArmorId(event.getTo()))
                  )
                {
                  deactivateRemovedArmor(player, removedArmor);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      clearPlayerRuntime(event.getEntity());
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      clearPlayerRuntime(event.getOriginal());
      clearPlayerRuntime(event.getEntity());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         forceResyncEquippedArmor(player);
      }
   }

   @SubscribeEvent
   public static void onBreakSpeed(BreakSpeed event) {
      Player player = event.getEntity();
      List<ArmorCapabilityCollector.ActiveCapability> capabilities = ArmorCapabilityCollector.collectPerInstalledStack(player);
      double underwaterMultiplier = 1.0;
      double airborneMultiplier = 1.0;
      ArmorCapabilityCollector.ActiveCapability pulseSource = null;

      for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
         DeviceCapability var11 = active.capability();
         if (var11 instanceof DeviceCapability.DigAffinity) {
            DeviceCapability.DigAffinity dig = (DeviceCapability.DigAffinity)var11;
            if ("underwater".equals(dig.env())) {
               underwaterMultiplier = Math.max(underwaterMultiplier, dig.speedMul());
            } else if ("airborne".equals(dig.env())) {
               airborneMultiplier = Math.max(airborneMultiplier, dig.speedMul());
            }

            pulseSource = active;
         }
      }

      if (pulseSource != null) {
         boolean underwater = player.m_204029_(FluidTags.f_13131_) || player.m_5842_();
         boolean airborne = !player.m_20096_();
         double multiplier = digSpeedMultiplier(underwater, airborne, underwaterMultiplier, airborneMultiplier);
         if (!(multiplier <= 1.0)) {
            event.setNewSpeed((float)((double)event.getNewSpeed() * multiplier));
         }
      }
   }

   @SubscribeEvent
   public static void onMobEffectApplicable(Applicable event) {
      if (event.getEntity() instanceof ServerPlayer player && PurificationEffectRules.canPurify(event.getEffectInstance())) {
         for (ArmorCapabilityCollector.ActiveCapability active : ArmorCapabilityCollector.collectPerInstalledStack(player)) {
            if (active.capability() instanceof DeviceCapability.PurificationTuning) {
               event.setResult(Result.DENY);
               return;
            }
         }

         return;
      }
   }

   private static void tickPurification(ServerPlayer player, List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
         DeviceCapability period = active.capability();
         if (period instanceof DeviceCapability.PurificationTuning) {
            DeviceCapability.PurificationTuning purification = (DeviceCapability.PurificationTuning)period;
            int periodx = Math.max(1, purification.periodTicks());
            if (player.f_19797_ % periodx == 0) {
               int limit = Math.max(1, purification.strength());
               purifyEffects(player, limit);
            }
         }
      }
   }

   private static int purifyEffects(ServerPlayer player, int maxEffects) {
      int removed = 0;

      for (MobEffectInstance effect : List.copyOf(player.m_21220_())) {
         if (removed >= maxEffects) {
            break;
         }

         if (PurificationEffectRules.canPurify(effect) && player.m_21195_(effect.m_19544_())) {
            removed++;
         }
      }

      return removed;
   }

   private static void tickFoodSustain(ServerPlayer player, List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      if (!player.m_7500_() && !player.m_5833_()) {
         for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
            if (active.capability() instanceof DeviceCapability.FoodSustain foodSustain && SaturationSubmodule.getCooldown(active.armor(), player) <= 0) {
               int intervalTicks = Math.max(1, foodSustain.checkIntervalTicks());
               int targetFood = Math.max(0, Math.min(20, foodSustain.targetFood()));
               float targetSaturation = Math.max(0.0F, Math.min(20.0F, foodSustain.targetSaturation()));
               FoodData foodData = player.m_36324_();
               int currentFood = foodData.m_38702_();
               float currentSaturation = foodData.m_38722_();
               if (currentFood >= targetFood && currentSaturation >= targetSaturation) {
                  SaturationSubmodule.setCooldown(active.armor(), player, intervalTicks);
                  return;
               }

               if (currentFood < targetFood) {
                  foodData.m_38705_(Math.max(currentFood, targetFood));
               }

               if (currentSaturation < targetSaturation) {
                  foodData.m_38717_(Math.max(currentSaturation, targetSaturation));
               }

               SaturationSubmodule.setCooldown(active.armor(), player, intervalTicks);
               return;
            }
         }
      }
   }

   private static void extinguishShieldedPlayer(ServerPlayer player, List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      if (player.m_6060_()) {
         for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
            if (active.capability() instanceof DeviceCapability.StagedMitigation) {
               player.m_20095_();
               player.m_7311_(0);
               return;
            }
         }
      }
   }

   @Nullable
   private static ArmorCapabilityCollector.ActiveCapability findActivePhaseTraversal(List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
         if (active.capability() instanceof DeviceCapability.PhaseTraversal) {
            return active;
         }
      }

      return null;
   }

   private static ItemStack findPhaseFlightArmor(Player player) {
      for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
         ItemStack armor = CelestweaveEquipmentAccess.findArmor(player, slot);
         if (!armor.m_41619_() && CelestweaveArmorState.isSubmoduleInstalled(armor, player.m_9236_().m_9598_(), PhaseFlightSubmodule.INSTANCE.id())) {
            return armor;
         }
      }

      return ItemStack.f_41583_;
   }

   @Nullable
   private static UUID celestweaveArmorId(ItemStack armor) {
      return !armor.m_41619_() && armor.m_41720_() instanceof BaseCelestweaveArmorItem ? CelestweaveArmorState.getArmorId(armor) : null;
   }

   private static void deactivateRemovedArmor(ServerPlayer player, ItemStack armor) {
      ArmorCapabilityCollector.clearCache(player);
      CelestweaveArmorState.syncSubmoduleActiveState(player, armor, player.m_9236_().m_9598_(), false, Dist.DEDICATED_SERVER);
   }

   private static void forceResyncEquippedArmor(ServerPlayer player) {
      ArmorCapabilityCollector.clearCache(player);

      for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
         ItemStack armor = CelestweaveEquipmentAccess.findArmor(player, slot);
         if (!armor.m_41619_()) {
            CelestweaveArmorState.syncSubmoduleActiveState(player, armor, player.m_9236_().m_9598_(), true, Dist.DEDICATED_SERVER, true);
         }
      }
   }

   private static void clearPlayerRuntime(Player player) {
      ArmorCapabilityCollector.clearCache(player);
      PhaseFlightMovementGuard.clear(player);
      PhaseFlightSubmodule.clearTransientPhaseState(player);

      for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
         ItemStack armor = CelestweaveEquipmentAccess.findArmor(player, slot);
         if (!armor.m_41619_()) {
            CelestweaveArmorState.clearTransientRuntimeAndCaches(armor);
         }
      }
   }

   private static double digSpeedMultiplier(boolean underwater, boolean airborne, double underwaterMultiplier, double airborneMultiplier) {
      double multiplier = 1.0;
      if (underwater) {
         multiplier *= Math.max(1.0, underwaterMultiplier);
      }

      if (airborne) {
         multiplier *= Math.max(1.0, airborneMultiplier);
      }

      return multiplier;
   }
}
