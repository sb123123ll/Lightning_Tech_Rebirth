package com.moakiee.ae2lt.logic.railgun;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.device.energy.LightningCompensationPolicy;
import com.moakiee.ae2lt.item.railgun.RailgunChargeTier;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.item.railgun.RailgunStructuralCore;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.network.NetworkHandler;
import com.moakiee.ae2lt.network.railgun.RailgunFirePacket;
import com.moakiee.ae2lt.registry.ModDamageTypes;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.registry.ModMobEffects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class RailgunFireService {
   private RailgunFireService() {
   }

   public static RailgunChargeTier tierForCharge(long ticks, RailgunModuleEntries mods) {
      return RailgunChargeTier.fromTicks(ticks, 10, 24, 40);
   }

   public static int countAccelerationModules(RailgunModuleEntries mods) {
      return countAccelerationFactor(mods);
   }

   public static void fireCharged(ServerLevel level, ServerPlayer player, ItemStack stack, RailgunChargeTier tier) {
      fireCharged(level, player, stack, tier, -1L);
   }

   public static void fireCharged(ServerLevel level, ServerPlayer player, ItemStack stack, RailgunChargeTier tier, long chargedTicks) {
      if (tier != RailgunChargeTier.HV) {
         if (!RailgunStructuralCore.hasCore(stack)) {
            sendFail(player, "ae2lt.railgun.structural_core_required");
         } else {
            RailgunModuleEntries mods = ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY);
            if (!mods.hasCore()) {
               sendFail(player, "ae2lt.railgun.core_required");
            } else {
               RailgunBinding.Result bound = RailgunBinding.resolve(stack, player);
               if (!bound.success()) {
                  sendFail(player, RailgunBinding.failKey(bound.failure()));
               } else {
                  IGrid grid = bound.grid();
                  RailgunSettings settings = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(stack, RailgunSettings.DEFAULT);
                  boolean allowPlayerTargets = settings.allowsPlayerTargets(AE2LTCommonConfig.railgunDamagePlayers());
                  AmmoCost cost = AmmoCost.forCharged(tier, mods);
                  IActionSource src = IActionSource.ofPlayer(player);
                  MEStorage inv = grid.getStorageService().getInventory();
                  RailgunEnergyBuffer.refillFromNetwork(stack, player, Math.max(0L, cost.feEnergy() - RailgunEnergyBuffer.read(stack)));
                  if (!RailgunEnergyBuffer.tryConsume(stack, player, cost.feEnergy())) {
                     sendFail(player, "ae2lt.railgun.fail.no_fe");
                  } else {
                     long ehvNeeded = cost.ehv();
                     long ehvGot = inv.extract(LightningKey.EXTREME_HIGH_VOLTAGE, ehvNeeded, Actionable.MODULATE, src);
                     long ehvShort = ehvNeeded - ehvGot;
                     if (ehvShort > 0L) {
                        int compensationRatio = LightningCompensationPolicy.bestRatio(mods.capabilities());
                        if (compensationRatio <= 0) {
                           inv.insert(LightningKey.EXTREME_HIGH_VOLTAGE, ehvGot, Actionable.MODULATE, src);
                           RailgunEnergyBuffer.refund(stack, cost.feEnergy());
                           sendFail(player, "ae2lt.railgun.fail.no_ehv");
                           return;
                        }

                        long needHv = LightningCompensationPolicy.highVoltageRequired(ehvShort, compensationRatio);
                        long gotHv = inv.extract(LightningKey.HIGH_VOLTAGE, needHv, Actionable.MODULATE, src);
                        if (gotHv < needHv) {
                           inv.insert(LightningKey.EXTREME_HIGH_VOLTAGE, ehvGot, Actionable.MODULATE, src);
                           inv.insert(LightningKey.HIGH_VOLTAGE, gotHv, Actionable.MODULATE, src);
                           RailgunEnergyBuffer.refund(stack, cost.feEnergy());
                           sendFail(player, "ae2lt.railgun.fail.no_compensation_hv");
                           return;
                        }
                     }

                     Vec3 from = player.m_146892_();
                     double range = RailgunRangePolicy.effectiveRange(64.0, mods.capabilities());
                     Vec3 dir = player.m_20154_();
                     Vec3 to = from.m_82549_(dir.m_82490_(range));
                     RailgunRaycastService.Result raycast = RailgunRaycastService.traceFirst(
                        level, player, from, to, 1.0, 0.3F, e -> RailgunTargetRules.canAffect(player, e, allowPlayerTargets)
                     );
                     Vec3 endBlock = raycast.blockEnd();
                     EntityHitResult ehr = raycast.entityHit();
                     DamageContext ctx = DamageContext.buildCharged(player, tier, mods, level, allowPlayerTargets);
                     Vec3 firstHitPos = ehr != null ? ehr.m_82450_() : endBlock;
                     Entity directTarget = ehr != null ? ehr.m_82443_() : null;
                     List<RailgunChainResolver.Hit> hits = new ArrayList<>();
                     int primaryId = -1;
                     int chainTuningCount = countChainTuning(mods);
                     double effectivePulseRadius = tier.isMax() ? 10.0 + 1.5 * (double)chainTuningCount : 0.0;

                     double effectivePulseRatio = switch (chainTuningCount) {
                        case 0 -> 0.6;
                        case 1 -> 0.85;
                        default -> 1.0;
                     };
                     if (directTarget instanceof LivingEntity primary) {
                        primaryId = primary.m_19879_();
                        hits.add(new RailgunChainResolver.Hit(primary, ctx.firstDamage(), false, false));
                        if (settings.chainDamage()) {
                           hits.addAll(RailgunChainResolver.resolveChainForkedFrom(level, player, primary, ctx, Set.of(), null));
                        }

                        if (tier.isMax()) {
                           hits.addAll(RailgunChainResolver.resolvePenetration(level, player, primary, ctx, 5));
                           hits.addAll(
                              RailgunChainResolver.resolvePulse(level, player, primary.m_20182_(), effectivePulseRadius, effectivePulseRatio, ctx, primaryId)
                           );
                        }
                     }

                     double var54;
                     if (settings.chargedSplash()) {
                        switch (tier) {
                           case EHV1:
                              var54 = 5.5;
                              break;
                           case EHV2:
                              var54 = 8.0;
                              break;
                           case EHV3:
                              var54 = 12.0;
                              break;
                           default:
                              var54 = 0.0;
                        }
                     } else {
                        var54 = 0.0;
                     }

                     double impactRadius = var54;

                     double impactRatio = switch (tier) {
                        case EHV1 -> 0.45;
                        case EHV2 -> 0.55;
                        case EHV3 -> 0.65;
                        default -> 0.0;
                     };
                     if (impactRadius > 0.0 && impactRatio > 0.0) {
                        hits.addAll(RailgunChainResolver.resolveImpactSplash(level, player, firstHitPos, impactRadius, impactRatio, primaryId, ctx));
                     }

                     LivingEntity splashAnchor = findClosestSplashAnchor(hits, firstHitPos, primaryId);
                     if (settings.chainDamage() && splashAnchor != null) {
                        Set<Integer> alreadyHit = new HashSet<>();

                        for (RailgunChainResolver.Hit h : hits) {
                           if (h.target() != null) {
                              alreadyHit.add(h.target().m_19879_());
                           }
                        }

                        hits.addAll(RailgunChainResolver.resolveChainForkedFrom(level, player, splashAnchor, ctx, alreadyHit, firstHitPos));
                     }

                     if (!hits.isEmpty()) {
                        applyAll(level, player, hits, ctx, stack, tier, new RailgunFireService.ExecutionScope(firstHitPos, impactRadius * 0.5, primaryId));
                     }

                     if (directTarget != null && !(directTarget instanceof LivingEntity)) {
                        applyDirectNonLivingHit(level, player, directTarget, ctx.firstDamage(), stack, tier, allowPlayerTargets);
                     }

                     if (AE2LTCommonConfig.railgunTerrainDestructionEnabled() && settings.terrainDestruction()) {
                        Vec3 terrainCenter = ehr != null ? firstHitPos : raycast.terrainEnd();
                        RailgunTerrainService.queueDestroy(level, terrainCenter, tier, player, stack);
                        if (tier.isMax()) {
                           List<Vec3> tunnel = new ArrayList<>();

                           for (RailgunChainResolver.Hit hx : hits) {
                              if (hx.penetration()) {
                                 tunnel.add(hx.target().m_20182_());
                              }
                           }

                           if (!tunnel.isEmpty()) {
                              RailgunTerrainService.queueDestroyAlongPath(level, tunnel, player, stack);
                           }
                        }
                     }

                     RailgunRecoilService.apply(player, tier);
                     broadcastFire(level, player, from, firstHitPos, tier, hits, effectivePulseRadius, impactRadius, settings.soundEnabled());
                  }
               }
            }
         }
      }
   }

   public static void applyAll(ServerLevel level, ServerPlayer player, List<RailgunChainResolver.Hit> hits, DamageContext ctx) {
      applyAll(level, player, hits, ctx, ItemStack.f_41583_, RailgunChargeTier.HV);
   }

   public static void applyAll(
      ServerLevel level, ServerPlayer player, List<RailgunChainResolver.Hit> hits, DamageContext ctx, ItemStack railgunStack, RailgunChargeTier tier
   ) {
      applyAll(level, player, hits, ctx, railgunStack, tier, RailgunFireService.ExecutionScope.NONE);
   }

   private static void applyAll(
      ServerLevel level,
      ServerPlayer player,
      List<RailgunChainResolver.Hit> hits,
      DamageContext ctx,
      ItemStack railgunStack,
      RailgunChargeTier tier,
      RailgunFireService.ExecutionScope executionScope
   ) {
      if (!hits.isEmpty()) {
         Holder<DamageType> damageHolder = ModDamageTypes.electromagneticHolder(level);
         DamageSource ds = new DamageSource(damageHolder, player, player);
         boolean damagePlayers = AE2LTCommonConfig.railgunDamagePlayers();
         boolean allowPlayerTargets = damagePlayers && ctx.pvp();
         boolean paralyzePlayers = AE2LTCommonConfig.railgunParalysisOnPlayers();
         int paralysisDur = 40;
         boolean overloadEligible = tier == RailgunChargeTier.EHV3 && !railgunStack.m_41619_();

         for (RailgunChainResolver.Hit hit : hits) {
            LivingEntity target = hit.target();
            if (RailgunTargetRules.canAffect(player, target, allowPlayerTargets)) {
               double armorReduction = DamageContext.effectiveArmorReduction(target);
               double finalDamage = DamageContext.finalDamage(hit.damage(), ctx.bypassRatio(), armorReduction);
               target.f_19802_ = 0;
               target.m_6469_(ds, (float)finalDamage);
               if (paralysisDur > 0 && (!(target instanceof Player) || paralyzePlayers)) {
                  target.m_147207_(new MobEffectInstance((MobEffect)ModMobEffects.ELECTROMAGNETIC_PARALYSIS.get(), paralysisDur, 0, false, true, true), player);
               }

               if (overloadEligible && !hit.chainPropagation() && executionScope.includes(target)) {
                  OverloadExecutionService.onHit(level, player, railgunStack, target, finalDamage);
               }
            }
         }
      }
   }

   private static void applyDirectNonLivingHit(
      ServerLevel level, ServerPlayer player, Entity target, double damage, ItemStack railgunStack, RailgunChargeTier tier, boolean allowPlayerTargets
   ) {
      if (RailgunTargetRules.canAffect(player, target, allowPlayerTargets)) {
         DamageSource source = new DamageSource(ModDamageTypes.electromagneticHolder(level), player, player);
         target.m_6469_(source, (float)damage);
         if (tier == RailgunChargeTier.EHV3 && !railgunStack.m_41619_() && !target.m_213877_()) {
            OverloadExecutionService.onDirectNonLivingHit(level, player, railgunStack, target, allowPlayerTargets);
         }
      }
   }

   private static void broadcastFire(
      ServerLevel level,
      ServerPlayer player,
      Vec3 from,
      Vec3 firstHit,
      RailgunChargeTier tier,
      List<RailgunChainResolver.Hit> hits,
      double effectivePulseRadius,
      double impactRadius,
      boolean soundEnabled
   ) {
      List<Vec3> chainPath = new ArrayList<>();
      Vec3 prev = firstHit;

      for (RailgunChainResolver.Hit h : hits) {
         if (!h.penetration() && !h.pulse()) {
            if (h.chainStartAt() != null) {
               prev = h.chainStartAt();
            }

            Vec3 cur = h.target().m_20182_().m_82520_(0.0, (double)h.target().m_20206_() / 2.0, 0.0);
            chainPath.add(prev);
            chainPath.add(cur);
            prev = cur;
         }
      }

      double impactR = impactRadius;
      if (tier.isMax()) {
         impactR = Math.max(impactRadius, effectivePulseRadius);
      }

      RailgunFirePacket pkt = new RailgunFirePacket(player.m_20148_(), from, firstHit, chainPath, tier.ordinal(), tier.isMax(), soundEnabled, (float)impactR);
      NetworkHandler.sendToTrackingChunk(level, player.m_146902_(), pkt);
   }

   @Nullable
   private static LivingEntity findClosestSplashAnchor(List<RailgunChainResolver.Hit> hits, Vec3 impactCenter, int primaryId) {
      LivingEntity best = null;
      double bestSq = Double.MAX_VALUE;

      for (RailgunChainResolver.Hit h : hits) {
         if (h.pulse()) {
            LivingEntity t = h.target();
            if (t != null && t.m_6084_() && t.m_19879_() != primaryId) {
               double dSq = t.m_20182_().m_82557_(impactCenter);
               if (dSq < bestSq) {
                  bestSq = dSq;
                  best = t;
               }
            }
         }
      }

      return best;
   }

   public static void sendFail(@Nullable ServerPlayer player, String key) {
      if (player != null) {
         player.m_5661_(Component.m_237115_(key), true);
      }
   }

   private static int countAccelerationFactor(RailgunModuleEntries mods) {
      int n = 0;

      for (DeviceCapability cap : mods.capabilities()) {
         if (cap instanceof DeviceCapability.AccelerationFactor) {
            n++;
         }
      }

      return n;
   }

   private static int countChainTuning(RailgunModuleEntries mods) {
      int n = 0;

      for (DeviceCapability cap : mods.capabilities()) {
         if (cap instanceof DeviceCapability.ChainTuning) {
            n++;
         }
      }

      return n;
   }

   private static record ExecutionScope(Vec3 center, double radius, int directTargetId) {
      private static final RailgunFireService.ExecutionScope NONE = new RailgunFireService.ExecutionScope(Vec3.f_82478_, 0.0, -1);

      private boolean includes(LivingEntity target) {
         return target.m_19879_() == this.directTargetId ? true : this.radius > 0.0 && target.m_20182_().m_82557_(this.center) <= this.radius * this.radius;
      }
   }
}
