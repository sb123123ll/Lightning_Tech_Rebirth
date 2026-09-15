package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.network.railgun.RailgunBeamChainFxPacket;
import com.moakiee.ae2lt.registry.ModSounds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class RailgunBeamChainFx {
   private RailgunBeamChainFx() {
   }

   public static void play(RailgunBeamChainFxPacket p) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91073_ != null) {
         List<Vec3> path = p.chainPath();
         if (!path.isEmpty()) {
            for (int i = 0; i + 1 < path.size(); i += 2) {
               Vec3 a = path.get(i);
               Vec3 b = path.get(i + 1);
               RailgunArcRenderer.spawnHighVoltageChain(a, b, 14);

               for (int s = 0; s < 3; s++) {
                  mc.f_91073_
                     .m_7106_(
                        ParticleTypes.f_175830_,
                        b.f_82479_,
                        b.f_82480_,
                        b.f_82481_,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * 0.35,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * 0.35,
                        (mc.f_91073_.f_46441_.m_188500_() - 0.5) * 0.35
                     );
               }
            }

            Vec3 first = p.firstHit();
            mc.f_91073_.m_7106_(ParticleTypes.f_123747_, first.f_82479_, first.f_82480_, first.f_82481_, 0.0, 0.0, 0.0);
            if (p.soundEnabled()) {
               mc.f_91073_
                  .m_7785_(
                     first.f_82479_, first.f_82480_, first.f_82481_, (SoundEvent)ModSounds.RAILGUN_BEAM_CHAIN.get(), SoundSource.PLAYERS, 0.35F, 1.6F, false
                  );
            }
         }
      }
   }
}
