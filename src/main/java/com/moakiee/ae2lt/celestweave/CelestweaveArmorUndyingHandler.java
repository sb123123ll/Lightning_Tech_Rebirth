package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.UndyingSubmodule;
import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.celestweave.service.ArmorEnergyService;
import com.moakiee.ae2lt.celestweave.service.ArmorLightningService;
import com.moakiee.ae2lt.celestweave.service.ArmorModuleLightningPolicy;
import com.moakiee.ae2lt.celestweave.service.ArmorResourceFeedback;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.registry.ModDamageTypes;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class CelestweaveArmorUndyingHandler {
   private static final String TAG_PROTECTED_TICK = "ae2lt.undying_protected_tick";
   private static final String TAG_PROTECTED_UNTIL = "ae2lt.undying_protected_until";
   private static final int PROTECTION_WINDOW_TICKS = 10;

   private CelestweaveArmorUndyingHandler() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onIncomingFatalDamage(LivingHurtEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !player.m_9236_().m_5776_()) {
         float damage = event.getAmount();
         if (!(damage <= 0.0F) && isIncomingUndyingCandidate(event.getSource()) && !(damage < player.m_21223_() + player.m_6103_())) {
            long now = player.m_9236_().m_46467_();
            if (tryProtectWithinWindow(player, now)) {
               event.setAmount(0.0F);
               event.setCanceled(true);
            } else if (tryTrigger(player, now)) {
               event.setAmount(0.0F);
               event.setCanceled(true);
            }

            return;
         }

         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onFatalDamage(LivingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !player.m_9236_().m_5776_()) {
         float damage = event.getAmount();
         if (!(damage <= 0.0F) && !(damage < player.m_21223_() + player.m_6103_())) {
            long now = player.m_9236_().m_46467_();
            if (tryProtectWithinWindow(player, now)) {
               event.setAmount(0.0F);
            } else if (tryTrigger(player, now)) {
               event.setAmount(0.0F);
            }

            return;
         }

         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !player.m_9236_().m_5776_()) {
         if (tryProtectForcedDeath(player)) {
            event.setCanceled(true);
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      tryProtectDeadOrDying(event.player);
   }

   private static void tryProtectDeadOrDying(Player player) {
      if (player instanceof ServerPlayer serverPlayer && !serverPlayer.m_9236_().m_5776_()) {
         if (serverPlayer.m_21224_() || serverPlayer.m_21223_() <= 0.0F) {
            tryProtectForcedDeath(serverPlayer);
         }

         return;
      }
   }

   public static boolean tryProtectForcedDeath(ServerPlayer player) {
      if (player == null || player.m_9236_().m_5776_()) {
         return false;
      } else if (wasProtectedThisTick(player)) {
         restoreSurvivalState(player);
         return true;
      } else {
         long now = player.m_9236_().m_46467_();
         return tryProtectWithinWindow(player, now) ? true : tryTrigger(player, now);
      }
   }

   public static boolean wasProtectedThisTick(LivingEntity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      } else {
         CompoundTag data = player.getPersistentData();
         return data.m_128441_("ae2lt.undying_protected_tick") && data.m_128454_("ae2lt.undying_protected_tick") == player.m_9236_().m_46467_();
      }
   }

   public static boolean protectBeforeDeathSideEffect(ServerPlayer player) {
      if (player == null || player.m_9236_().m_5776_()) {
         return false;
      } else if (wasProtectedThisTick(player)) {
         restoreSurvivalState(player);
         return true;
      } else {
         return !player.m_21224_() && player.m_21223_() > 0.0F ? false : tryProtectForcedDeath(player);
      }
   }

   private static boolean tryProtectWithinWindow(ServerPlayer player, long now) {
      if (hasActiveLastStand(player) && hasActiveProtectionWindow(player, now)) {
         recordProtectedTick(player, now);
         restoreSurvivalState(player);
         return true;
      } else {
         return false;
      }
   }

   private static boolean tryTrigger(ServerPlayer player, long now) {
      for (CelestweaveArmorUndyingHandler.ActiveLastStand active : collectActiveLastStand(player)) {
         if ("multidimensional_protection".equals(active.submoduleId())) {
            recordProtectionWindow(player, now);
            restoreSurvivalState(player);
            return true;
         }

         int comboIndex = capComboIndexForWindow(
            ArmorOverloadCombo.nextComboIndex(active.armor(), UndyingSubmodule.INSTANCE, now), active.tuning().comboWindowTicks()
         );
         long cost = ArmorOverloadCombo.scaledCost(active.tuning().feCost(), comboIndex);
         ArmorLightningService.LightningCost lightningCost = ArmorModuleLightningPolicy.triggeredCost(ArmorModuleLightningPolicy.Trigger.UNDYING)
            .times((long)comboIndex);
         if (!ArmorLightningService.hasCost(player, active.armor(), lightningCost)) {
            ArmorResourceFeedback.noExtremeHighVoltage(player);
         } else {
            ArmorEnergyService.EnergyPayment payment = ArmorEnergyService.consumeActiveCostPayment(player, active.armor(), cost);
            if (!payment.paid()) {
               ArmorResourceFeedback.noFe(player);
            } else {
               if (ArmorLightningService.consume(player, active.armor(), lightningCost)) {
                  ArmorOverloadCombo.recordTrigger(active.armor(), UndyingSubmodule.INSTANCE, now, Math.max(1, active.tuning().comboWindowTicks()), comboIndex);
                  recordProtectionWindow(player, now);
                  restoreSurvivalState(player);
                  return true;
               }

               payment.refund();
               ArmorResourceFeedback.noExtremeHighVoltage(player);
            }
         }
      }

      return false;
   }

   private static boolean hasActiveProtectionWindow(ServerPlayer player, long now) {
      long protectedUntil = player.getPersistentData().m_128454_("ae2lt.undying_protected_until");
      return protectedUntil > now;
   }

   private static boolean hasActiveLastStand(ServerPlayer player) {
      return !collectActiveLastStand(player).isEmpty();
   }

   private static boolean isIncomingUndyingCandidate(DamageSource source) {
      return source.m_269533_(DamageTypeTags.f_268490_)
         || source.m_269533_(DamageTypeTags.f_268738_)
         || source.m_269533_(DamageTypeTags.f_268437_)
         || source.m_269533_(DamageTypeTags.f_268630_)
         || source.m_269533_(DamageTypeTags.f_268413_)
         || source.m_276093_(DamageTypes.f_268724_)
         || source.m_276093_(DamageTypes.f_286979_)
         || source.m_276093_(DamageTypes.f_268441_)
         || source.m_276093_(DamageTypes.f_268515_)
         || source.m_276093_(DamageTypes.f_268530_)
         || source.m_276093_(DamageTypes.f_268493_)
         || source.m_276093_(DamageTypes.f_268641_)
         || source.m_276093_(ModDamageTypes.ELECTROMAGNETIC);
   }

   private static void recordProtectedTick(ServerPlayer player, long now) {
      player.getPersistentData().m_128356_("ae2lt.undying_protected_tick", now);
   }

   private static void recordProtectionWindow(ServerPlayer player, long now) {
      recordProtectedTick(player, now);
      player.getPersistentData().m_128356_("ae2lt.undying_protected_until", saturatingAdd(now, 10L));
   }

   private static long saturatingAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }

   static int capComboIndexForWindow(int comboIndex, int comboWindowTicks) {
      int safeWindow = Math.max(1, comboWindowTicks);
      int maximum = Math.max(1, (safeWindow + 10 - 1) / 10);
      return Math.min(Math.max(1, comboIndex), maximum);
   }

   private static void restoreSurvivalState(ServerPlayer player) {
      player.m_20095_();
      player.m_7311_(0);
      player.m_183634_();
      float targetHealth = Math.max(1.0F, player.m_21233_());
      if (player.m_21223_() < targetHealth) {
         player.m_21153_(targetHealth);
      }

      player.f_19802_ = Math.max(player.f_19802_, 20);
      player.f_20916_ = 0;
      player.f_20917_ = 0;
   }

   private static List<CelestweaveArmorUndyingHandler.ActiveLastStand> collectActiveLastStand(ServerPlayer player) {
      return ArmorCapabilityCollector.collectPerInstalledStack(player)
         .stream()
         .flatMap(
            active -> active.capability() instanceof DeviceCapability.LastStandTuning tuning
                  ? Stream.of(new CelestweaveArmorUndyingHandler.ActiveLastStand(active.armor(), active.submoduleId(), tuning))
                  : Stream.empty()
         )
         .toList();
   }

   private static record ActiveLastStand(ItemStack armor, String submoduleId, DeviceCapability.LastStandTuning tuning) {
   }
}
