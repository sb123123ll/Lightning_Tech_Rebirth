package com.moakiee.ae2lt.logic.railgun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class RailgunChainResolver {
   private RailgunChainResolver() {
   }

   public static List<RailgunChainResolver.Hit> resolveChain(ServerLevel level, ServerPlayer source, LivingEntity start, DamageContext ctx) {
      return resolveChainFrom(level, source, start, ctx, Set.of(), null);
   }

   public static List<RailgunChainResolver.Hit> resolveChainForkedFrom(
      ServerLevel level, ServerPlayer source, LivingEntity start, DamageContext ctx, Set<Integer> initiallyVisited, @Nullable Vec3 firstSegmentVisualStart
   ) {
      int forks = Math.max(1, ctx.chainForkCount());
      if (forks <= 1) {
         return resolveChainFrom(level, source, start, ctx, initiallyVisited, firstSegmentVisualStart);
      } else {
         List<RailgunChainResolver.Hit> out = new ArrayList<>();
         if (ctx.chainSegments() <= 0) {
            return out;
         } else {
            Set<Integer> visited = new HashSet<>(initiallyVisited);
            visited.add(start.m_19879_());
            List<LivingEntity> seeds = findNearestK(level, source, start, ctx.chainRadius(), visited, ctx.pvp(), forks);
            if (seeds.isEmpty()) {
               return out;
            } else {
               Vec3 branchVisualStart = firstSegmentVisualStart != null
                  ? firstSegmentVisualStart
                  : start.m_20182_().m_82520_(0.0, (double)start.m_20206_() / 2.0, 0.0);

               for (LivingEntity seed : seeds) {
                  visited.add(seed.m_19879_());
                  out.addAll(walkBranch(level, source, seed, ctx, visited, branchVisualStart));
               }

               return out;
            }
         }
      }
   }

   public static List<RailgunChainResolver.Hit> resolveChainFromPoint(ServerLevel level, ServerPlayer source, Vec3 start, DamageContext ctx) {
      List<RailgunChainResolver.Hit> out = new ArrayList<>();
      if (ctx.chainSegments() <= 0) {
         return out;
      } else {
         Set<Integer> visited = new HashSet<>();
         List<LivingEntity> seeds = findNearestK(level, source, start, ctx.chainRadius(), visited, ctx.pvp(), 1);
         if (seeds.isEmpty()) {
            return out;
         } else {
            LivingEntity seed = seeds.get(0);
            visited.add(seed.m_19879_());
            out.addAll(walkBranch(level, source, seed, ctx, visited, start));
            return out;
         }
      }
   }

   private static List<RailgunChainResolver.Hit> walkBranch(
      ServerLevel level, ServerPlayer source, LivingEntity seed, DamageContext ctx, Set<Integer> visited, Vec3 branchVisualStart
   ) {
      List<RailgunChainResolver.Hit> out = new ArrayList<>();
      double damage = ctx.firstDamage();
      if (!ctx.isMaxCharged()) {
         damage *= ctx.chainDecay();
      }

      out.add(new RailgunChainResolver.Hit(seed, damage, false, false, branchVisualStart));
      LivingEntity prev = seed;

      for (int i = 1; i < ctx.chainSegments(); i++) {
         damage *= ctx.chainDecay();
         LivingEntity next = findNearest(level, source, prev, ctx.chainRadius(), visited, ctx.pvp());
         if (next == null) {
            break;
         }

         visited.add(next.m_19879_());
         out.add(new RailgunChainResolver.Hit(next, damage, false, false, null));
         prev = next;
      }

      return out;
   }

   public static List<RailgunChainResolver.Hit> resolveChainFrom(
      ServerLevel level, ServerPlayer source, LivingEntity start, DamageContext ctx, Set<Integer> initiallyVisited, @Nullable Vec3 firstSegmentVisualStart
   ) {
      List<RailgunChainResolver.Hit> out = new ArrayList<>();
      if (ctx.chainSegments() <= 0) {
         return out;
      } else {
         Set<Integer> visited = new HashSet<>(initiallyVisited);
         visited.add(start.m_19879_());
         LivingEntity prev = start;
         double damage = ctx.firstDamage();
         boolean firstEmitted = false;

         for (int i = 0; i < ctx.chainSegments(); i++) {
            if (!ctx.isMaxCharged() || i > 0) {
               damage *= ctx.chainDecay();
            }

            LivingEntity next = findNearest(level, source, prev, ctx.chainRadius(), visited, ctx.pvp());
            if (next == null) {
               break;
            }

            visited.add(next.m_19879_());
            Vec3 visualStart = !firstEmitted ? firstSegmentVisualStart : null;
            out.add(new RailgunChainResolver.Hit(next, damage, false, false, visualStart));
            firstEmitted = true;
            prev = next;
         }

         return out;
      }
   }

   public static List<RailgunChainResolver.Hit> resolvePenetration(
      ServerLevel level, ServerPlayer source, LivingEntity firstHit, DamageContext ctx, int maxTargets
   ) {
      List<RailgunChainResolver.Hit> out = new ArrayList<>();
      if (ctx.isMaxCharged() && maxTargets > 0) {
         Vec3 from = source.m_146892_();
         Vec3 dir = firstHit.m_20182_().m_82546_(from).m_82541_();
         double maxLen = 32.0;
         Vec3 to = from.m_82549_(dir.m_82490_(maxLen));
         AABB box = new AABB(from, to).m_82400_(1.5);
         List<LivingEntity> candidates = level.m_6443_(LivingEntity.class, box, e -> e != source && e != firstHit && shouldTarget(e, ctx.pvp()));
         candidates.sort((a, b) -> {
            double da = a.m_20182_().m_82546_(from).m_82526_(dir);
            double db = b.m_20182_().m_82546_(from).m_82526_(dir);
            return Double.compare(da, db);
         });
         int taken = 0;

         for (LivingEntity ent : candidates) {
            Vec3 v = ent.m_20182_().m_82546_(from);
            double along = v.m_82526_(dir);
            if (!(along < 0.0) && !(along > maxLen)) {
               double perp = v.m_82546_(dir.m_82490_(along)).m_82553_();
               if (!(perp > 1.5)) {
                  out.add(new RailgunChainResolver.Hit(ent, ctx.firstDamage(), true, false));
                  if (++taken >= maxTargets) {
                     break;
                  }
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static List<RailgunChainResolver.Hit> resolvePulse(
      ServerLevel level, ServerPlayer source, Vec3 center, double radius, double damageRatio, DamageContext ctx, int excludeId
   ) {
      List<RailgunChainResolver.Hit> out = new ArrayList<>();
      if (ctx.isMaxCharged() && !(radius <= 0.0)) {
         AABB box = new AABB(center, center).m_82400_(radius);
         double r2 = radius * radius;

         for (LivingEntity ent : level.m_6443_(LivingEntity.class, box, e -> e != source && e.m_19879_() != excludeId && shouldTarget(e, ctx.pvp()))) {
            if (!(ent.m_20182_().m_82557_(center) > r2)) {
               double dmg = ctx.firstDamage() * damageRatio;
               if (ent instanceof Player) {
                  dmg *= 0.5;
               }

               out.add(new RailgunChainResolver.Hit(ent, dmg, false, true));
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static List<RailgunChainResolver.Hit> resolveImpactSplash(
      ServerLevel level, ServerPlayer source, Vec3 center, double radius, double damageRatio, int primaryId, DamageContext ctx
   ) {
      List<RailgunChainResolver.Hit> out = new ArrayList<>();
      if (!(radius <= 0.0) && !(damageRatio <= 0.0)) {
         AABB box = new AABB(center, center).m_82400_(radius);
         double r2 = radius * radius;

         for (LivingEntity ent : level.m_6443_(LivingEntity.class, box, e -> e != source && shouldTarget(e, ctx.pvp()))) {
            if (ent.m_19879_() != primaryId) {
               double d2 = ent.m_20182_().m_82557_(center);
               if (!(d2 > r2)) {
                  double d = Math.sqrt(d2);
                  double falloff = 1.0 - d / radius * 0.6;
                  double dmg = ctx.firstDamage() * damageRatio * falloff;
                  if (ent instanceof Player) {
                     dmg *= 0.5;
                  }

                  out.add(new RailgunChainResolver.Hit(ent, dmg, false, true));
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static LivingEntity findNearest(ServerLevel level, ServerPlayer source, Entity from, double radius, Set<Integer> visited, boolean pvp) {
      Vec3 c = from.m_20182_();
      AABB box = new AABB(c, c).m_82400_(radius);
      LivingEntity best = null;
      double r2 = radius * radius;
      double bestDistSqr = Double.MAX_VALUE;

      for (LivingEntity ent : level.m_6443_(LivingEntity.class, box, e -> shouldTarget(e, pvp))) {
         if (ent != source && !visited.contains(ent.m_19879_())) {
            double d2 = ent.m_20182_().m_82557_(c);
            if (!(d2 > r2) && d2 < bestDistSqr) {
               best = ent;
               bestDistSqr = d2;
            }
         }
      }

      return best;
   }

   private static List<LivingEntity> findNearestK(ServerLevel level, ServerPlayer source, Entity from, double radius, Set<Integer> visited, boolean pvp, int k) {
      return findNearestK(level, source, from.m_20182_(), radius, visited, pvp, k);
   }

   private static List<LivingEntity> findNearestK(ServerLevel level, ServerPlayer source, Vec3 center, double radius, Set<Integer> visited, boolean pvp, int k) {
      if (k <= 0) {
         return List.of();
      } else {
         AABB box = new AABB(center, center).m_82400_(radius);
         double r2 = radius * radius;
         List<LivingEntity> all = new ArrayList<>();

         for (LivingEntity ent : level.m_6443_(LivingEntity.class, box, e -> shouldTarget(e, pvp))) {
            if (ent != source && !visited.contains(ent.m_19879_()) && !(ent.m_20182_().m_82557_(center) > r2)) {
               all.add(ent);
            }
         }

         all.sort((a, b) -> Double.compare(a.m_20182_().m_82557_(center), b.m_20182_().m_82557_(center)));
         return (List<LivingEntity>)(all.size() > k ? new ArrayList<>(all.subList(0, k)) : all);
      }
   }

   private static boolean shouldTarget(LivingEntity entity, boolean pvp) {
      return RailgunTargetRules.canAffect(null, entity, pvp);
   }

   public static record Hit(LivingEntity target, double damage, boolean penetration, boolean pulse, @Nullable Vec3 chainStartAt, boolean chainPropagation) {
      public Hit(LivingEntity target, double damage, boolean penetration, boolean pulse) {
         this(target, damage, penetration, pulse, null, false);
      }

      public Hit(LivingEntity target, double damage, boolean penetration, boolean pulse, @Nullable Vec3 chainStartAt) {
         this(target, damage, penetration, pulse, chainStartAt, true);
      }
   }
}
