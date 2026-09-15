package com.moakiee.ae2lt.logic.railgun;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

public final class RailgunTargetRules {
   private RailgunTargetRules() {
   }

   public static boolean canAffect(@Nullable Entity shooter, @Nullable Entity target, boolean allowPlayerTargets) {
      if (target == null || target == shooter) {
         return false;
      } else if (target instanceof Player targetPlayer) {
         return allowPlayerTargets
            && !(targetPlayer instanceof FakePlayer)
            && !targetPlayer.m_5833_()
            && !targetPlayer.m_7500_()
            && targetPlayer.m_6084_()
            && !targetPlayer.m_21224_();
      } else {
         return !(target instanceof LivingEntity living) ? target.m_271807_() : living.m_6084_() && !living.m_21224_();
      }
   }
}
