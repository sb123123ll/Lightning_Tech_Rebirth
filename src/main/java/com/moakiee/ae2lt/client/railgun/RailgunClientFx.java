package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.network.railgun.RailgunFirePacket;
import com.moakiee.ae2lt.registry.ModSounds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class RailgunClientFx {
   private RailgunClientFx() {
   }

   public static void playCharged(RailgunFirePacket p) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91073_ != null) {
         Vec3 hit = p.firstHit();
         if (RailgunArcRenderer.isRenderableSegment(p.from(), hit)) {
            boolean isMax = p.isMax();
            int tier = Math.max(1, p.tier());
            float partialTick = RailgunVisuals.currentPartialTick();
            Player shooter = mc.f_91073_.m_46003_(p.shooterId());
            Vec3 plasmaOrigin = shooter == null ? p.from() : RailgunVisuals.computeBarrelOrigin(shooter, partialTick);
            Vec3 plasmaEnd = shooter == null ? hit : RailgunVisuals.computeBarrelEndpoint(shooter, plasmaOrigin, p.from(), hit, partialTick);
            float radius = p.impactRadius();
            if (radius > 0.0F) {
               int waveLife = isMax ? 80 : 56 + tier * 6;
               RailgunShockwaveRenderer.spawn(hit, radius, waveLife);
               RailgunShockwaveRenderer.spawn(hit, radius * 0.5F, Math.max(35, waveLife - 15));
            }

            List<Vec3> path = p.chainPath();

            for (int i = 0; i + 1 < path.size(); i += 2) {
               Vec3 a = path.get(i);
               Vec3 b = path.get(i + 1);
               int chainLife = isMax ? 40 : 32;
               RailgunArcRenderer.spawnChain(a, b, chainLife);

               for (int s = 0; s < 4; s++) {
                  mc.f_91073_
                     .m_7106_(
                        ParticleTypes.f_175830_,
                        b.f_82479_,
                        b.f_82480_,
                        b.f_82481_,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * 0.4,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * 0.4,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * 0.4
                     );
               }
            }

            RailgunArcRenderer.spawnPlasma(
               plasmaOrigin, plasmaEnd, Math.max(10, (int)Math.round(plasmaOrigin.m_82554_(plasmaEnd) * 1.2)), isMax ? 0.3F : 0.18F, isMax ? 36 : 30
            );
            if (radius > 0.0F) {
               int bolts = isMax ? 18 : 8 + tier * 2;
               RandomSource rng = mc.f_91073_.f_46441_;

               for (int i = 0; i < bolts; i++) {
                  double yaw = rng.m_188500_() * Math.PI * 2.0;
                  double pitch = (rng.m_188500_() - 0.3) * Math.PI;
                  double r = (double)(radius * (0.6F + rng.m_188501_() * 0.6F));
                  double dx = Math.cos(yaw) * Math.cos(pitch) * r;
                  double dy = Math.sin(pitch) * r;
                  double dz = Math.sin(yaw) * Math.cos(pitch) * r;
                  Vec3 end = hit.m_82520_(dx, dy, dz);
                  RailgunArcRenderer.spawnImpactSpark(hit, end, 22 + rng.m_188503_(10));
               }
            }

            float radiusScale = Math.max(1.0F, radius / 7.0F);
            mc.f_91073_.m_7106_(ParticleTypes.f_123747_, hit.f_82479_, hit.f_82480_, hit.f_82481_, 0.0, 0.0, 0.0);
            if (isMax) {
               int flashCount = (int)(4.0F * radiusScale);
               float flashSpread = 0.7F * radiusScale;

               for (int i = 0; i < flashCount; i++) {
                  double ox = (mc.f_91073_.f_46441_.m_188500_() - 0.5) * (double)flashSpread;
                  double oy = (mc.f_91073_.f_46441_.m_188500_() - 0.5) * (double)flashSpread;
                  double oz = (mc.f_91073_.f_46441_.m_188500_() - 0.5) * (double)flashSpread;
                  mc.f_91073_.m_7106_(ParticleTypes.f_123747_, hit.f_82479_ + ox, hit.f_82480_ + oy, hit.f_82481_ + oz, 0.0, 0.0, 0.0);
               }

               int smokeCount = (int)(48.0F * radiusScale);
               double smokeMotion = 0.3 * (double)radiusScale;
               double smokeUp = 0.25 * (double)radiusScale;

               for (int i = 0; i < smokeCount; i++) {
                  mc.f_91073_
                     .m_7106_(
                        ParticleTypes.f_123755_,
                        hit.f_82479_,
                        hit.f_82480_,
                        hit.f_82481_,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * smokeMotion,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.2) * smokeUp,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * smokeMotion
                     );
               }
            }

            int sparkCount = (int)((float)(14 + tier * 8) * radiusScale);
            double sparkVel = (0.4 + (double)tier * 0.18) * (double)radiusScale;

            for (int i = 0; i < sparkCount; i++) {
               mc.f_91073_
                  .m_7106_(
                     ParticleTypes.f_175830_,
                     hit.f_82479_,
                     hit.f_82480_,
                     hit.f_82481_,
                     (mc.f_91073_.f_46441_.m_188500_() - 0.5) * sparkVel,
                     (mc.f_91073_.f_46441_.m_188500_() - 0.5) * sparkVel,
                     (mc.f_91073_.f_46441_.m_188500_() - 0.5) * sparkVel
                  );
            }

            if (p.soundEnabled()) {
               SoundEvent sound = isMax ? (SoundEvent)ModSounds.RAILGUN_FIRE_MAX.get() : (SoundEvent)ModSounds.RAILGUN_FIRE_CHARGED.get();
               mc.f_91073_
                  .m_7785_(
                     plasmaOrigin.f_82479_,
                     plasmaOrigin.f_82480_,
                     plasmaOrigin.f_82481_,
                     sound,
                     SoundSource.PLAYERS,
                     isMax ? 1.7F : 0.9F + 0.15F * (float)tier,
                     1.0F,
                     false
                  );
               if (isMax) {
                  mc.f_91073_
                     .m_7785_(hit.f_82479_, hit.f_82480_, hit.f_82481_, (SoundEvent)ModSounds.RAILGUN_FIRE_IMPACT.get(), SoundSource.PLAYERS, 1.4F, 0.7F, false);
               }
            }
         }
      }
   }
}
