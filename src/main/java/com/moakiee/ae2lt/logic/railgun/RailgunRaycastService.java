package com.moakiee.ae2lt.logic.railgun;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class RailgunRaycastService {
   private static final double BLOCK_HIT_RAY_TOLERANCE_SQR = 1.0;

   private RailgunRaycastService() {
   }

   public static RailgunRaycastService.Result traceFirst(
      ServerLevel level, Entity shooter, Vec3 from, Vec3 to, double queryPadding, float targetInflation, Predicate<Entity> filter
   ) {
      BlockHitResult blockHit = level.m_45547_(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, shooter));
      Vec3 rawBlockEnd = blockHit.m_6662_() == Type.MISS ? to : blockHit.m_82450_();
      Vec3 blockEnd = sanitizeBlockEnd(from, to, rawBlockEnd);
      EntityHitResult entityHit = traceEntities(level, shooter, from, blockEnd, queryPadding, targetInflation, filter);
      Vec3 terrainEnd = isFinite(rawBlockEnd) ? rawBlockEnd : blockEnd;
      return new RailgunRaycastService.Result(blockHit, blockEnd, terrainEnd, entityHit);
   }

   static Vec3 sanitizeBlockEnd(Vec3 from, Vec3 to, Vec3 candidate) {
      if (isFinite(from) && isFinite(to) && isFinite(candidate)) {
         Vec3 ray = to.m_82546_(from);
         double rayLengthSqr = ray.m_82556_();
         if (Double.isFinite(rayLengthSqr) && !(rayLengthSqr <= 1.0E-12)) {
            Vec3 offset = candidate.m_82546_(from);
            double progress = offset.m_82526_(ray) / rayLengthSqr;
            if (Double.isFinite(progress) && !(progress < 0.0) && !(progress > 1.0)) {
               Vec3 nearest = from.m_82549_(ray.m_82490_(progress));
               double offRayDistanceSqr = candidate.m_82557_(nearest);
               return Double.isFinite(offRayDistanceSqr) && !(offRayDistanceSqr > 1.0) ? candidate : to;
            } else {
               return to;
            }
         } else {
            return from;
         }
      } else {
         return isFinite(to) ? to : Vec3.f_82478_;
      }
   }

   private static boolean isFinite(Vec3 value) {
      return value != null && Double.isFinite(value.f_82479_) && Double.isFinite(value.f_82480_) && Double.isFinite(value.f_82481_);
   }

   @Nullable
   private static EntityHitResult traceEntities(
      ServerLevel level, Entity shooter, Vec3 from, Vec3 to, double queryPadding, float targetInflation, Predicate<Entity> filter
   ) {
      Vec3 ray = to.m_82546_(from);
      double distance = ray.m_82553_();
      int segments = segmentCount(distance);
      if (segments == 0) {
         return null;
      } else {
         Vec3 step = ray.m_82490_(1.0 / (double)segments);
         Vec3 segmentStart = from;
         double padding = Math.max(0.0, queryPadding);
         float inflation = Math.max(0.0F, targetInflation);

         for (int segment = 1; segment <= segments; segment++) {
            Vec3 segmentEnd = segment == segments ? to : from.m_82549_(step.m_82490_((double)segment));
            AABB candidates = new AABB(segmentStart, segmentEnd).m_82400_(padding);
            EntityHitResult hit = ProjectileUtil.m_150175_(level, shooter, segmentStart, segmentEnd, candidates, filter, inflation);
            if (hit != null) {
               return hit;
            }

            segmentStart = segmentEnd;
         }

         return null;
      }
   }

   static int segmentCount(double distance) {
      return Double.isFinite(distance) && !(distance <= 0.0) ? Math.max(1, (int)Math.ceil(distance / 16.0)) : 0;
   }

   public static record Result(BlockHitResult blockHit, Vec3 blockEnd, Vec3 terrainEnd, @Nullable EntityHitResult entityHit) {
   }
}
