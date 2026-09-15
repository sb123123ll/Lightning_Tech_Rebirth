package com.moakiee.ae2lt.logic.railgun;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorUndyingHandler;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.railgun.RailgunEnergyRules;
import com.moakiee.ae2lt.item.railgun.RailgunExecutionMode;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.registry.ModDamageTypes;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OverloadExecutionService {
   private static final Logger LOGGER = LoggerFactory.getLogger("ae2lt/OverloadExecution");
   private static final String TAG_TARGETS = "OverloadExecutionTargets";
   private static final String TAG_UUID = "uuid";
   private static final String TAG_RECORDED_HP = "recordedHp";
   private static final String TAG_LAST_HIT_TICK = "lastHitTick";
   private static final float OFF_OVERLOAD_DAMAGE = 600.0F;
   private static final float OFF_MULTIDIMENSIONAL_DAMAGE = Float.MAX_VALUE;

   private OverloadExecutionService() {
   }

   public static void onHit(ServerLevel level, ServerPlayer player, ItemStack stack, LivingEntity target, double damage) {
      if (AE2LTCommonConfig.overloadExecutionEnabled()) {
         RailgunSettings settings = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(stack, RailgunSettings.DEFAULT);
         boolean allowPlayerTargets = settings.allowsPlayerTargets(AE2LTCommonConfig.railgunDamagePlayers());
         if (RailgunTargetRules.canAffect(player, target, allowPlayerTargets)) {
            RailgunModuleEntries mods = ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY);
            RailgunExecutionMode executionMode = settings.executionMode();
            DamageSource damageSource = new DamageSource(ModDamageTypes.electromagneticHolder(level), player, player);
            if (mods.hasMultidimensionalExecution()) {
               if (!executionMode.entersExecutionFlow()) {
                  applyOrdinaryDamage(target, damageSource, Float.MAX_VALUE);
               } else {
                  execute(target, Math.max(damage, (double)target.m_21223_()), damageSource, executionMode.forcesRemoval());
               }
            } else if (mods.hasOverloadExecution()) {
               if (!executionMode.entersExecutionFlow()) {
                  applyOrdinaryDamage(target, damageSource, 600.0F);
               } else {
                  int maxTracked = AE2LTCommonConfig.overloadExecutionMaxTracked();
                  int decayWindow = AE2LTCommonConfig.overloadExecutionDecayWindowTicks();
                  double decayPower = AE2LTCommonConfig.overloadExecutionDecayPower();
                  UUID targetUuid = target.m_20148_();
                  long now = level.m_46467_();
                  double currentHp = (double)target.m_21223_();
                  double maxHp = (double)target.m_21233_();
                  CompoundTag root = ItemStackTagSupport.getTagCopy(stack);
                  ListTag targets = root.m_128437_("OverloadExecutionTargets", 10);
                  int existingIdx = indexOf(targets, targetUuid);
                  long feCost = RailgunEnergyRules.overloadExecutionCostFe();
                  RailgunEnergyBuffer.refillFromNetwork(stack, player, Math.max(0L, feCost - RailgunEnergyBuffer.read(stack)));
                  if (!RailgunEnergyBuffer.tryConsume(stack, player, feCost)) {
                     RailgunFireService.sendFail(player, "ae2lt.railgun.fail.no_fe");
                  } else {
                     double basis;
                     if (existingIdx < 0) {
                        basis = currentHp;
                     } else {
                        CompoundTag entry = targets.m_128728_(existingIdx);
                        double recorded = entry.m_128459_("recordedHp");
                        long lastHit = entry.m_128454_("lastHitTick");
                        long elapsed = now - lastHit;
                        if (elapsed >= (long)decayWindow) {
                           targets.remove(existingIdx);
                           basis = currentHp;
                        } else {
                           double x = Math.max(0.0, Math.min(1.0, (double)elapsed / (double)decayWindow));
                           double f = Math.pow(x, decayPower);
                           double recovery = Math.max(0.0, maxHp - recorded) * f;
                           basis = Math.min(recorded + recovery, currentHp);
                        }
                     }

                     double finalHp = basis - damage;
                     if (finalHp <= 0.0) {
                        int idx = indexOf(targets, targetUuid);
                        if (idx >= 0) {
                           targets.remove(idx);
                        }

                        saveTargets(stack, root, targets);
                        execute(target, Math.max(damage, currentHp), damageSource, executionMode.forcesRemoval());
                     } else {
                        if (currentHp > finalHp) {
                           target.f_19802_ = 0;
                           target.m_21153_((float)finalHp);
                           target.m_21231_()
                              .m_289194_(new DamageSource(ModDamageTypes.electromagneticHolder(level), player, player), (float)(currentHp - finalHp));
                           target.f_20916_ = 10;
                           target.f_20917_ = 10;
                           target.m_146850_(GameEvent.f_223706_);
                           if (!target.m_6084_()) {
                              int idx = indexOf(targets, targetUuid);
                              if (idx >= 0) {
                                 targets.remove(idx);
                              }

                              saveTargets(stack, root, targets);
                              return;
                           }
                        }

                        CompoundTag entryx = new CompoundTag();
                        entryx.m_128359_("uuid", targetUuid.toString());
                        entryx.m_128347_("recordedHp", finalHp);
                        entryx.m_128356_("lastHitTick", now);
                        int idx = indexOf(targets, targetUuid);
                        if (idx >= 0) {
                           targets.set(idx, entryx);
                        } else {
                           targets.add(entryx);

                           while (targets.size() > maxTracked) {
                              targets.remove(0);
                           }
                        }

                        saveTargets(stack, root, targets);
                     }
                  }
               }
            }
         }
      }
   }

   public static void onDirectNonLivingHit(ServerLevel level, ServerPlayer player, ItemStack stack, Entity target, boolean allowPlayerTargets) {
      if (AE2LTCommonConfig.overloadExecutionEnabled()) {
         if (!(target instanceof LivingEntity)) {
            if (RailgunTargetRules.canAffect(player, target, allowPlayerTargets)) {
               RailgunModuleEntries mods = ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY);
               boolean multidimensional = mods.hasMultidimensionalExecution();
               if (multidimensional || mods.hasOverloadExecution()) {
                  RailgunSettings settings = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(stack, RailgunSettings.DEFAULT);
                  RailgunExecutionMode executionMode = settings.executionMode();
                  if (!executionMode.entersExecutionFlow()) {
                     DamageSource source = new DamageSource(ModDamageTypes.electromagneticHolder(level), player, player);
                     target.m_6469_(source, multidimensional ? Float.MAX_VALUE : 600.0F);
                  } else if (executionMode.forcesRemoval()) {
                     if (!multidimensional) {
                        long feCost = RailgunEnergyRules.overloadExecutionCostFe();
                        RailgunEnergyBuffer.refillFromNetwork(stack, player, Math.max(0L, feCost - RailgunEnergyBuffer.read(stack)));
                        if (!RailgunEnergyBuffer.tryConsume(stack, player, feCost)) {
                           RailgunFireService.sendFail(player, "ae2lt.railgun.fail.no_fe");
                           return;
                        }
                     }

                     forceRemoveNonLiving(target);
                  }
               }
            }
         }
      }
   }

   private static void applyOrdinaryDamage(LivingEntity target, DamageSource source, float damage) {
      target.f_19802_ = 0;
      target.m_6469_(source, damage);
   }

   private static void execute(LivingEntity target, double damage, DamageSource source, boolean forceRemoval) {
      establishKillCredit(target, source);
      if (forceRemoval && !(target instanceof ServerPlayer)) {
         forceRemove(target, source, (float)damage);
      } else {
         completeNormalDeath(target, source, (float)damage);
      }
   }

   static void completeNormalDeath(LivingEntity target, DamageSource source, float damage) {
      int playerDeathsBefore = deathCount(target);
      prepareLethalState(target, source, damage);

      try (OverloadExecutionContext.Scope ignored = OverloadExecutionContext.enter(target)) {
         target.m_6667_(source);
      } catch (LinkageError | RuntimeException var10) {
         LOGGER.warn("Target death callback failed for {}; applying normal-settlement fallback", target.m_6095_(), var10);
      }

      if (!CelestweaveArmorUndyingHandler.wasProtectedThisTick(target)) {
         if (!normalDeathCompleted(target, playerDeathsBefore)) {
            if (target instanceof ServerPlayer player) {
               if (player.m_21224_()) {
                  player.m_21153_(1.0F);
               }
            } else {
               try {
                  forceDie(target, source);
               } catch (LinkageError | RuntimeException var7) {
                  LOGGER.warn("Normal-settlement fallback failed for {}", target.m_6095_(), var7);
               }
            }
         }
      }
   }

   private static void forceRemove(LivingEntity target, DamageSource source, float damage) {
      prepareLethalState(target, source, damage);

      try (OverloadExecutionContext.Scope ignored = OverloadExecutionContext.enter(target)) {
         target.m_6667_(source);
      } catch (LinkageError | RuntimeException var10) {
         LOGGER.warn("Target death callback failed for {}; continuing forced removal", target.m_6095_(), var10);
      }

      if (needsKillFallback(target.f_20890_, target.m_213877_())) {
         try {
            target.m_6074_();
         } catch (LinkageError | RuntimeException var7) {
            LOGGER.warn("Target kill callback failed for {}; continuing forced removal", target.m_6095_(), var7);
         }
      }

      if (!target.m_213877_()) {
         try {
            target.m_142687_(RemovalReason.KILLED);
         } catch (LinkageError | RuntimeException var6) {
            LOGGER.warn("Target remove callback failed for {}; applying final removal", target.m_6095_(), var6);
         }
      }

      if (!target.m_213877_()) {
         target.m_142467_(RemovalReason.KILLED);
      }
   }

   static boolean needsKillFallback(boolean deathCommitted, boolean removed) {
      return !deathCommitted && !removed;
   }

   private static void forceRemoveNonLiving(Entity target) {
      try {
         target.m_6074_();
      } catch (LinkageError | RuntimeException var4) {
         LOGGER.warn("Non-living target kill callback failed for {}; continuing forced removal", target.m_6095_(), var4);
      }

      try {
         target.m_146870_();
      } catch (LinkageError | RuntimeException var3) {
         LOGGER.warn("Non-living target discard callback failed for {}; continuing forced removal", target.m_6095_(), var3);
      }

      try {
         target.m_142687_(RemovalReason.KILLED);
      } catch (LinkageError | RuntimeException var2) {
         LOGGER.warn("Non-living target remove callback failed for {}; applying final removal", target.m_6095_(), var2);
      }

      if (!target.m_213877_()) {
         target.m_142467_(RemovalReason.KILLED);
      }
   }

   private static void establishKillCredit(LivingEntity target, DamageSource source) {
      if (source.m_7639_() instanceof LivingEntity attacker) {
         target.m_6703_(attacker);
      }

      if (source.m_7639_() instanceof Player player) {
         target.m_6598_(player);
      }
   }

   private static int deathCount(LivingEntity target) {
      return target instanceof ServerPlayer player ? player.m_8951_().m_13015_(Stats.f_12988_.m_12902_(Stats.f_12935_)) : -1;
   }

   private static boolean normalDeathCompleted(LivingEntity target, int playerDeathsBefore) {
      int playerDeathsAfter = target instanceof ServerPlayer player ? player.m_8951_().m_13015_(Stats.f_12988_.m_12902_(Stats.f_12935_)) : -1;
      return normalDeathCompleted(target.f_20890_, target.m_213877_(), target instanceof ServerPlayer, playerDeathsBefore, playerDeathsAfter);
   }

   static boolean normalDeathCompleted(boolean deathCommitted, boolean removed, boolean serverPlayer, int playerDeathsBefore, int playerDeathsAfter) {
      return deathCommitted || removed || serverPlayer && playerDeathsAfter > playerDeathsBefore;
   }

   private static void prepareLethalState(LivingEntity victim, DamageSource source, float amount) {
      if (!victim.m_9236_().f_46443_) {
         if (victim.m_5803_()) {
            victim.m_5796_();
         }

         victim.m_21310_(0);
         victim.f_267362_.m_267771_(1.5F);
         victim.f_20898_ = amount;
         victim.f_19802_ = 0;
         victim.m_21231_().m_289194_(source, amount);
         victim.m_21153_(0.0F);
         victim.m_146850_(GameEvent.f_223706_);
         victim.f_20917_ = 10;
         victim.f_20916_ = victim.f_20917_;
      }
   }

   private static void forceDie(LivingEntity victim, DamageSource source) {
      if (!victim.m_213877_() && !victim.f_20890_) {
         LivingEntity killer = source.m_7639_() instanceof LivingEntity attacker ? attacker : victim.m_21232_();
         if (victim.f_20897_ >= 0 && killer != null) {
            killer.m_5993_(victim, victim.f_20897_, source);
         }

         if (victim.m_5803_()) {
            victim.m_5796_();
         }

         victim.f_20890_ = true;
         victim.m_21231_().m_19296_();
         if (victim.m_9236_() instanceof ServerLevel serverLevel) {
            Entity sourceEntity = source.m_7639_();
            if (sourceEntity == null || sourceEntity.m_214076_(serverLevel, victim)) {
               victim.m_146850_(GameEvent.f_223707_);
               victim.m_6668_(source);
            }

            serverLevel.m_7605_(victim, (byte)3);
         }

         victim.m_20124_(Pose.DYING);
      }
   }

   private static int indexOf(ListTag targets, UUID uuid) {
      String uuidStr = uuid.toString();

      for (int i = 0; i < targets.size(); i++) {
         if (uuidStr.equals(targets.m_128728_(i).m_128461_("uuid"))) {
            return i;
         }
      }

      return -1;
   }

   private static void saveTargets(ItemStack stack, CompoundTag root, ListTag targets) {
      ItemStackTagSupport.updateTag(stack, tag -> {
         if (targets.isEmpty()) {
            tag.m_128473_("OverloadExecutionTargets");
         } else {
            tag.m_128365_("OverloadExecutionTargets", targets);
         }
      });
   }
}
