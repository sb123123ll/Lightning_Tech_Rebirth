package com.moakiee.ae2lt.logic.railgun;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.item.railgun.RailgunChargeTier;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import com.moakiee.ae2lt.item.railgun.RailgunSettings;
import com.moakiee.ae2lt.item.railgun.RailgunStructuralCore;
import com.moakiee.ae2lt.me.key.LightningKey;
import com.moakiee.ae2lt.network.NetworkHandler;
import com.moakiee.ae2lt.network.railgun.RailgunBeamChainFxPacket;
import com.moakiee.ae2lt.network.railgun.RailgunBeamUpdatePacket;
import com.moakiee.ae2lt.registry.ModDamageTypes;
import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.registry.ModMobEffects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class RailgunBeamService {
   private static final Map<UUID, RailgunBeamService.BeamState> ACTIVE = new ConcurrentHashMap<>();

   private RailgunBeamService() {
   }

   public static void setFiring(ServerPlayer player, InteractionHand hand, boolean firing) {
      if (firing) {
         int max = 16;
         if (ACTIVE.size() >= max && !ACTIVE.containsKey(player.m_20148_())) {
            broadcastStop(player);
            return;
         }

         ACTIVE.put(player.m_20148_(), new RailgunBeamService.BeamState(hand, player.f_19797_));
      } else {
         RailgunBeamService.BeamState removed = ACTIVE.remove(player.m_20148_());
         if (removed != null) {
            broadcastStop(player);
         }
      }
   }

   public static boolean isFiring(ServerPlayer player) {
      return ACTIVE.containsKey(player.m_20148_());
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent e) {
      if (e.phase == Phase.END) {
         if (!ACTIVE.isEmpty()) {
            MinecraftServer server = e.getServer();
            Iterator<Entry<UUID, RailgunBeamService.BeamState>> it = ACTIVE.entrySet().iterator();
            int settleInterval = Math.max(1, 2);
            int chainThrottle = Math.max(1, 5);

            while (it.hasNext()) {
               Entry<UUID, RailgunBeamService.BeamState> entry = it.next();
               ServerPlayer player = server.m_6846_().m_11259_(entry.getKey());
               RailgunBeamService.BeamState s = entry.getValue();
               if (player != null && !player.m_21224_() && !player.m_213877_()) {
                  ItemStack stack = player.m_21120_(s.hand);
                  if (!(stack.m_41720_() instanceof ElectromagneticRailgunItem)) {
                     it.remove();
                     broadcastStop(player);
                  } else if (player.m_6117_() && player.m_21211_() == stack) {
                     it.remove();
                     broadcastStop(player);
                  } else if (player.f_19797_ - s.lastSettleTick >= settleInterval) {
                     s.lastSettleTick = player.f_19797_;
                     s.settleCount++;
                     boolean cont = settle((ServerLevel)player.m_9236_(), player, stack, s, chainThrottle);
                     if (!cont) {
                        it.remove();
                        broadcastStop(player);
                     }
                  }
               } else {
                  it.remove();
               }
            }
         }
      }
   }

   private static boolean settle(ServerLevel level, ServerPlayer player, ItemStack stack, RailgunBeamService.BeamState s, int chainThrottle) {
      if (!RailgunStructuralCore.hasCore(stack)) {
         RailgunFireService.sendFail(player, "ae2lt.railgun.structural_core_required");
         return false;
      } else {
         RailgunModuleEntries mods = ModDataComponents.RAILGUN_MODULE_ENTRIES.getOrDefault(stack, RailgunModuleEntries.EMPTY);
         RailgunBinding.Result bound = RailgunBinding.resolve(stack, player);
         if (!bound.success()) {
            RailgunFireService.sendFail(player, RailgunBinding.failKey(bound.failure()));
            return false;
         } else {
            IGrid grid = bound.grid();
            RailgunSettings settings = ModDataComponents.RAILGUN_SETTINGS.getOrDefault(stack, RailgunSettings.DEFAULT);
            boolean allowPlayerTargets = settings.allowsPlayerTargets(AE2LTCommonConfig.railgunDamagePlayers());
            long feCost = AmmoCost.beamFeCost(mods);
            IActionSource src = IActionSource.ofPlayer(player);
            LightningKey primaryKey = LightningKey.HIGH_VOLTAGE;
            int interval = AmmoCost.beamHvCostInterval(mods);
            long primaryNeeded = s.settleCount % interval == 0 ? 1L : 0L;
            String failKey = "ae2lt.railgun.fail.no_hv";
            long primaryAvail = 0L;
            if (primaryNeeded > 0L) {
               primaryAvail = grid.getStorageService().getInventory().extract(primaryKey, primaryNeeded, Actionable.SIMULATE, src);
               if (primaryAvail < primaryNeeded) {
                  RailgunFireService.sendFail(player, failKey);
                  return false;
               }
            }

            RailgunEnergyBuffer.refillFromNetwork(stack, player, Math.max(0L, feCost - RailgunEnergyBuffer.read(stack)));
            if (!RailgunEnergyBuffer.tryConsume(stack, player, feCost)) {
               RailgunFireService.sendFail(player, "ae2lt.railgun.fail.no_fe");
               return false;
            } else {
               if (primaryNeeded > 0L) {
                  long takePrimary = Math.min(primaryAvail, primaryNeeded);
                  long gotPrimary = takePrimary > 0L ? grid.getStorageService().getInventory().extract(primaryKey, takePrimary, Actionable.MODULATE, src) : 0L;
                  if (gotPrimary < takePrimary) {
                     RailgunEnergyBuffer.refund(stack, feCost);
                     RailgunFireService.sendFail(player, failKey);
                     return false;
                  }
               }

               RailgunBeamService.BeamTrace trace = traceBeam(level, player, allowPlayerTargets, mods);
               EntityHitResult ehr = trace.entityHit();
               DamageContext ctx = DamageContext.buildBeam(player, mods, level, allowPlayerTargets);
               LivingEntity chainAnchor = null;
               if (ehr != null) {
                  Entity target = ehr.m_82443_();
                  DamageSource ds = beamDamageSource(level, player);
                  if (target instanceof LivingEntity primary) {
                     chainAnchor = primary;
                     double armorReduction = DamageContext.effectiveArmorReduction(primary);
                     double finalDamage = DamageContext.finalDamage(ctx.firstDamage(), ctx.bypassRatio(), armorReduction);
                     primary.m_6469_(ds, (float)finalDamage);
                     if (primary.m_6084_()) {
                        primary.m_147207_(new MobEffectInstance((MobEffect)ModMobEffects.ELECTROMAGNETIC_PARALYSIS.get(), 40, 0, false, true, true), player);
                     }
                  } else {
                     target.m_6469_(ds, (float)ctx.firstDamage());
                  }
               }

               if (settings.chainDamage() && player.f_19797_ - s.lastChainTick >= chainThrottle) {
                  s.lastChainTick = player.f_19797_;
                  Vec3 chainOrigin;
                  List<RailgunChainResolver.Hit> chain;
                  if (chainAnchor != null) {
                     chainOrigin = lockedTargetPoint(chainAnchor);
                     chain = RailgunChainResolver.resolveChain(level, player, chainAnchor, ctx);
                  } else {
                     chainOrigin = trace.endPoint();
                     chain = RailgunChainResolver.resolveChainFromPoint(level, player, chainOrigin, ctx);
                  }

                  RailgunFireService.applyAll(level, player, chain, ctx, stack, RailgunChargeTier.HV);
                  if (!chain.isEmpty()) {
                     broadcastBeamChainFx(level, player, chainOrigin, chain, settings.soundEnabled());
                  }
               }

               broadcastTrace(level, player, trace);
               return true;
            }
         }
      }
   }

   private static RailgunBeamService.BeamTrace traceBeam(ServerLevel level, ServerPlayer player, boolean pvp, RailgunModuleEntries mods) {
      Vec3 from = player.m_146892_();
      double range = RailgunRangePolicy.effectiveRange(64.0, mods.capabilities());
      Vec3 dir = player.m_20154_();
      Vec3 to = from.m_82549_(dir.m_82490_(range));
      RailgunRaycastService.Result raycast = RailgunRaycastService.traceFirst(
         level, player, from, to, 0.5, 0.0F, e -> RailgunTargetRules.canAffect(player, e, pvp)
      );
      EntityHitResult ehr = raycast.entityHit();
      Vec3 endPoint = ehr != null ? lockedTargetPoint(ehr.m_82443_()) : raycast.blockEnd();
      return new RailgunBeamService.BeamTrace(from, endPoint, ehr);
   }

   private static Vec3 lockedTargetPoint(Entity target) {
      return target.m_20191_().m_82399_();
   }

   private static void broadcastTrace(ServerLevel level, ServerPlayer player, RailgunBeamService.BeamTrace trace) {
      RailgunBeamUpdatePacket pkt = new RailgunBeamUpdatePacket(player.m_20148_(), trace.from(), trace.endPoint(), true);
      NetworkHandler.sendToTrackingChunk(level, player.m_146902_(), pkt);
   }

   private static void broadcastBeamChainFx(ServerLevel level, ServerPlayer player, Vec3 origin, List<RailgunChainResolver.Hit> chain, boolean soundEnabled) {
      List<Vec3> path = new ArrayList<>(chain.size() * 2);
      Vec3 prev = origin;

      for (RailgunChainResolver.Hit h : chain) {
         if (h.target() != null) {
            Vec3 cur = h.target().m_20182_().m_82520_(0.0, (double)h.target().m_20206_() / 2.0, 0.0);
            path.add(prev);
            path.add(cur);
            prev = cur;
         }
      }

      if (!path.isEmpty()) {
         RailgunBeamChainFxPacket pkt = new RailgunBeamChainFxPacket(player.m_20148_(), origin, path, soundEnabled);
         NetworkHandler.sendToTrackingChunk(level, player.m_146902_(), pkt);
      }
   }

   private static DamageSource beamDamageSource(ServerLevel level, ServerPlayer player) {
      Holder<DamageType> h = ModDamageTypes.electromagneticHolder(level);
      return new DamageSource(h, player, player);
   }

   private static void broadcastStop(ServerPlayer player) {
      if (player.m_9236_() instanceof ServerLevel sl) {
         RailgunBeamUpdatePacket var3 = new RailgunBeamUpdatePacket(player.m_20148_(), Vec3.f_82478_, Vec3.f_82478_, false);
         NetworkHandler.sendToTrackingChunk(sl, player.m_146902_(), var3);
      }
   }

   public static final class BeamState {
      private final InteractionHand hand;
      private int lastSettleTick;
      private int lastChainTick;
      private int settleCount;

      BeamState(InteractionHand hand, int now) {
         this.hand = hand;
         this.lastSettleTick = now;
         this.lastChainTick = now;
         this.settleCount = 0;
      }

      public InteractionHand hand() {
         return this.hand;
      }
   }

   private static record BeamTrace(Vec3 from, Vec3 endPoint, EntityHitResult entityHit) {
   }
}
