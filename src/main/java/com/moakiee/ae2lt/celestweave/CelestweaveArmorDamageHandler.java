package com.moakiee.ae2lt.celestweave;

import com.moakiee.ae2lt.celestweave.module.MultidimensionalProtectionSubmodule;
import com.moakiee.ae2lt.celestweave.module.ResistanceSubmodule;
import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.celestweave.service.ArmorEnergyService;
import com.moakiee.ae2lt.celestweave.service.ArmorLightningService;
import com.moakiee.ae2lt.celestweave.service.ArmorModuleLightningPolicy;
import com.moakiee.ae2lt.celestweave.service.ArmorResourceFeedback;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.ShieldHitFeedbackSuppressionPacket;
import com.moakiee.ae2lt.registry.ModDamageTypes;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class CelestweaveArmorDamageHandler {
   private static final ThreadLocal<Boolean> REFLECTING_DAMAGE = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static final ThreadLocal<Set<Integer>> SUPPRESSING_SHIELD_HIT_FEEDBACK = ThreadLocal.withInitial(HashSet::new);
   private static final ThreadLocal<IdentityHashMap<LivingEntity, ArrayDeque<Float>>> INCOMING_DAMAGE_STACKS = ThreadLocal.withInitial(IdentityHashMap::new);

   private CelestweaveArmorDamageHandler() {
   }

   public static CelestweaveArmorDamageHandler.IncomingDamageResult onIncomingDamage(LivingEntity entity, DamageSource source, float incoming) {
      if (entity instanceof Player player && !player.m_9236_().m_5776_()) {
         for (ArmorCapabilityCollector.ActiveCapability active : ArmorCapabilityCollector.collectPerInstalledStack(player)) {
            if (active.capability() instanceof DeviceCapability.DamageTypeImmunity immunity && source.m_276093_(immunity.damageType())) {
               return CelestweaveArmorDamageHandler.IncomingDamageResult.cancel();
            }
         }

         if (incoming <= 0.0F) {
            return CelestweaveArmorDamageHandler.IncomingDamageResult.pass(incoming);
         }

         List<ArmorCapabilityCollector.ActiveCapability> capabilities = ArmorCapabilityCollector.collectPerInstalledUnit(player);
         ArmorCapabilityCollector.ActiveCapability mitigation = collectMitigation(capabilities);
         if (mitigation != null && mitigation.capability() instanceof DeviceCapability.StagedMitigation staged) {
            float afterMitigation = ArmorMitigationRules.apply(staged.stage(), classifyDamage(source), incoming);
            if (payMitigationLightning(player, mitigation, staged, incoming - afterMitigation)) {
               if (afterMitigation <= 0.0F) {
                  if (!isReflectingDamage()) {
                     reflectIncomingDamage(player, source, incoming);
                  }

                  return CelestweaveArmorDamageHandler.IncomingDamageResult.cancel();
               }

               if (!isHitFeedbackEnabled(mitigation.armor(), staged.stage())) {
                  markSuppressShieldHitFeedback(player);
               }

               return CelestweaveArmorDamageHandler.IncomingDamageResult.pass(afterMitigation);
            }
         }

         return CelestweaveArmorDamageHandler.IncomingDamageResult.pass(incoming);
      }

      return CelestweaveArmorDamageHandler.IncomingDamageResult.pass(incoming);
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onPre(LivingDamageEvent event) {
      if (event.getEntity() instanceof Player player && !player.m_9236_().m_5776_()) {
         if (!isReflectingDamage()) {
            reflectIncomingDamage(player, event.getSource(), currentIncomingDamage(player, event.getAmount()));
         }

         return;
      }
   }

   public static int beginOriginalDamage(LivingEntity entity, float originalDamage) {
      if (entity == null) {
         return 0;
      } else {
         ArrayDeque<Float> stack = INCOMING_DAMAGE_STACKS.get().get(entity);
         int initialDepth = stack == null ? 0 : stack.size();
         INCOMING_DAMAGE_STACKS.get().computeIfAbsent(entity, ignored -> new ArrayDeque<>()).push(originalDamage);
         return initialDepth;
      }
   }

   public static void finishOriginalDamage(LivingEntity entity, int targetDepth) {
      if (entity != null) {
         IdentityHashMap<LivingEntity, ArrayDeque<Float>> stacks = INCOMING_DAMAGE_STACKS.get();
         ArrayDeque<Float> stack = stacks.get(entity);
         if (stack != null && !stack.isEmpty()) {
            int safeTargetDepth = Math.max(0, targetDepth);

            while (stack.size() > safeTargetDepth) {
               stack.pop();
            }

            if (stack.isEmpty()) {
               stacks.remove(entity);
            }

            if (stacks.isEmpty()) {
               INCOMING_DAMAGE_STACKS.remove();
            }
         } else {
            if (stacks.isEmpty()) {
               INCOMING_DAMAGE_STACKS.remove();
            }
         }
      }
   }

   private static float currentIncomingDamage(LivingEntity entity, float fallback) {
      ArrayDeque<Float> stack = INCOMING_DAMAGE_STACKS.get().get(entity);
      return stack != null && !stack.isEmpty() ? stack.peek() : fallback;
   }

   private static ArmorCapabilityCollector.ActiveCapability collectMitigation(List<ArmorCapabilityCollector.ActiveCapability> capabilities) {
      for (ArmorCapabilityCollector.ActiveCapability active : capabilities) {
         if (active.capability() instanceof DeviceCapability.StagedMitigation) {
            return active;
         }
      }

      return null;
   }

   private static ArmorMitigationRules.DamageClass classifyDamage(DamageSource source) {
      return ArmorMitigationRules.classify(isEnvironmentDamage(source), isHardDamage(source));
   }

   private static boolean isHardDamage(DamageSource source) {
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

   private static boolean isEnvironmentDamage(DamageSource source) {
      return source.m_269533_(DamageTypeTags.f_268745_)
         || source.m_269533_(DamageTypeTags.f_268549_)
         || source.m_269533_(DamageTypeTags.f_268581_)
         || source.m_276093_(DamageTypes.f_268631_)
         || source.m_276093_(DamageTypes.f_268468_)
         || source.m_276093_(DamageTypes.f_268546_)
         || source.m_276093_(DamageTypes.f_268434_)
         || source.m_276093_(DamageTypes.f_268612_)
         || source.m_276093_(DamageTypes.f_268613_)
         || source.m_276093_(DamageTypes.f_268585_)
         || source.m_276093_(DamageTypes.f_268752_)
         || source.m_276093_(DamageTypes.f_268469_)
         || source.m_276093_(DamageTypes.f_268444_)
         || source.m_276093_(DamageTypes.f_268669_);
   }

   private static boolean payMitigationLightning(
      Player player, ArmorCapabilityCollector.ActiveCapability mitigation, DeviceCapability.StagedMitigation staged, float preventedDamage
   ) {
      if ("multidimensional_protection".equals(staged.stage())) {
         return true;
      } else {
         if (!(player instanceof ServerPlayer serverPlayer) || preventedDamage <= 0.0F) {
            return true;
         }

         if ("phase_shield".equals(staged.stage())) {
            return payPhaseShield(serverPlayer, mitigation, preventedDamage);
         } else {
            long amount = (long)Math.ceil(
               (double)(preventedDamage * (float)ArmorModuleLightningPolicy.triggeredCost(ArmorModuleLightningPolicy.Trigger.MATRIX_SHIELD).highVoltage())
            );
            long feCost = (long)Math.ceil((double)(preventedDamage * 5000.0F));
            if (amount <= 0L && feCost <= 0L) {
               return true;
            } else {
               int comboIndex = ArmorOverloadCombo.nextComboIndex(mitigation.armor(), ResistanceSubmodule.T1, serverPlayer.m_9236_().m_46467_());
               long finalAmount = ArmorOverloadCombo.scaledCost(amount, comboIndex);
               ArmorEnergyService.EnergyPayment payment = ArmorEnergyService.consumeActiveCostPayment(serverPlayer, mitigation.armor(), feCost);
               if (!payment.paid()) {
                  ArmorResourceFeedback.noFe(serverPlayer);
                  return false;
               } else {
                  ArmorLightningService.LightningCost lightningCost = ArmorModuleLightningPolicy.triggeredCost(ArmorModuleLightningPolicy.Trigger.MATRIX_SHIELD)
                     .times(finalAmount);
                  if (!ArmorLightningService.consume(serverPlayer, mitigation.armor(), lightningCost)) {
                     payment.refund();
                     if (lightningCost.extremeHighVoltage() > 0L) {
                        ArmorResourceFeedback.noExtremeHighVoltage(serverPlayer);
                     } else {
                        ArmorResourceFeedback.noHighVoltage(serverPlayer);
                     }

                     return false;
                  } else {
                     ArmorOverloadCombo.recordTrigger(
                        mitigation.armor(),
                        ResistanceSubmodule.T1,
                        serverPlayer.m_9236_().m_46467_(),
                        AE2LTCommonConfig.overloadArmorShieldComboWindowTicks(),
                        comboIndex
                     );
                     return true;
                  }
               }
            }
         }
      }
   }

   private static boolean isHitFeedbackEnabled(ItemStack armor, String stage) {
      return "multidimensional_protection".equals(stage)
         ? MultidimensionalProtectionSubmodule.isHitFeedbackEnabled(armor)
         : ResistanceSubmodule.isHitFeedbackEnabled(armor, stage);
   }

   private static boolean payPhaseShield(ServerPlayer player, ArmorCapabilityCollector.ActiveCapability mitigation, float preventedDamage) {
      PhaseShieldChargeWindow.Quote quote = PhaseShieldChargeWindow.quote(mitigation.armor(), player.m_9236_().m_46467_(), preventedDamage);
      ArmorLightningService.LightningCost lightningCost = ArmorLightningService.LightningCost.ehv(quote.ehvCost());
      if (!ArmorLightningService.hasCost(player, mitigation.armor(), lightningCost)) {
         ArmorResourceFeedback.noExtremeHighVoltage(player);
         return false;
      } else {
         ArmorEnergyService.EnergyPayment payment = ArmorEnergyService.consumeActiveCostPayment(player, mitigation.armor(), quote.feCost());
         if (!payment.paid()) {
            ArmorResourceFeedback.noFe(player);
            return false;
         } else if (!ArmorLightningService.consume(player, mitigation.armor(), lightningCost)) {
            payment.refund();
            ArmorResourceFeedback.noExtremeHighVoltage(player);
            return false;
         } else {
            PhaseShieldChargeWindow.record(mitigation.armor(), quote);
            return true;
         }
      }
   }

   public static boolean shouldSuppressShieldHitFeedback(LivingEntity entity) {
      return entity != null && SUPPRESSING_SHIELD_HIT_FEEDBACK.get().contains(entity.m_19879_());
   }

   public static void suppressShieldHitFeedback(LivingEntity entity) {
      if (entity != null) {
         entity.f_20916_ = 0;
         entity.f_20917_ = 0;
      }
   }

   public static void clearSuppressShieldHitFeedback(LivingEntity entity) {
      if (entity != null) {
         Set<Integer> suppressing = SUPPRESSING_SHIELD_HIT_FEEDBACK.get();
         suppressing.remove(entity.m_19879_());
         if (suppressing.isEmpty()) {
            SUPPRESSING_SHIELD_HIT_FEEDBACK.remove();
         }
      }
   }

   private static void markSuppressShieldHitFeedback(LivingEntity entity) {
      SUPPRESSING_SHIELD_HIT_FEEDBACK.get().add(entity.m_19879_());
      if (entity instanceof ServerPlayer serverPlayer) {
         NetworkInit.sendToPlayer(serverPlayer, new ShieldHitFeedbackSuppressionPacket(serverPlayer.m_19879_()));
      }
   }

   private static void reflectIncomingDamage(Player player, DamageSource source, float incomingDamage) {
      if (source.m_7639_() instanceof LivingEntity attacker) {
         float reflected = collectReflectedDamage(player, incomingDamage);
         if (reflected > 0.0F) {
            hurtWithReflectGuard(attacker, source, reflected);
         }
      }
   }

   private static boolean isReflectingDamage() {
      return Boolean.TRUE.equals(REFLECTING_DAMAGE.get());
   }

   private static void hurtWithReflectGuard(LivingEntity target, DamageSource source, float damage) {
      boolean wasReflecting = isReflectingDamage();
      REFLECTING_DAMAGE.set(Boolean.TRUE);

      try {
         target.m_6469_(source, damage);
      } finally {
         REFLECTING_DAMAGE.set(wasReflecting);
      }
   }

   private static float collectReflectedDamage(Player player, float damage) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return 0.0F;
      } else if (damage <= 0.0F) {
         return 0.0F;
      } else {
         float reflected = 0.0F;

         for (ArmorCapabilityCollector.ActiveCapability active : ArmorCapabilityCollector.collectPerInstalledUnit(player)) {
            DeviceCapability remaining = active.capability();
            if (remaining instanceof DeviceCapability.ReflectTuning) {
               DeviceCapability.ReflectTuning reflect = (DeviceCapability.ReflectTuning)remaining;
               if (!(reflect.reflectPct() <= 0.0)) {
                  float remainingx = Math.max(0.0F, damage - reflected);
                  float amount = Math.min(remainingx, damage * (float)reflect.reflectPct());
                  if (!(amount <= 0.0F)) {
                     long cost = (long)Math.ceil((double)(amount * (float)Math.max(0L, reflect.fePerDamage())));
                     ArmorEnergyService.EnergyPayment payment = ArmorEnergyService.consumeActiveCostPayment(serverPlayer, active.armor(), cost);
                     if (!payment.paid()) {
                        ArmorResourceFeedback.noFe(serverPlayer);
                     } else {
                        reflected += amount;
                        if (reflected >= damage) {
                           break;
                        }
                     }
                  }
               }
            }
         }

         return reflected;
      }
   }

   public static record IncomingDamageResult(float amount, boolean canceled) {
      private static CelestweaveArmorDamageHandler.IncomingDamageResult pass(float amount) {
         return new CelestweaveArmorDamageHandler.IncomingDamageResult(amount, false);
      }

      private static CelestweaveArmorDamageHandler.IncomingDamageResult cancel() {
         return new CelestweaveArmorDamageHandler.IncomingDamageResult(0.0F, true);
      }
   }
}
