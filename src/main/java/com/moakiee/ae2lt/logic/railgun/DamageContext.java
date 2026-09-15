package com.moakiee.ae2lt.logic.railgun;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import com.moakiee.ae2lt.item.railgun.RailgunChargeTier;
import com.moakiee.ae2lt.item.railgun.RailgunModuleEntries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record DamageContext(
   double firstDamage,
   double bypassRatio,
   double chainDecay,
   int chainSegments,
   int chainForkCount,
   double chainRadius,
   boolean isMaxCharged,
   boolean isBeam,
   boolean pvp
) {
   public static double effectiveArmorReduction(LivingEntity target) {
      return Math.min(0.8, (double)target.m_21230_() / 25.0);
   }

   public static double finalDamage(double base, double bypass, double armorReduction) {
      double effective = armorReduction * (1.0 - bypass);
      return base * (1.0 - effective);
   }

   public static DamageContext buildBeam(Player player, RailgunModuleEntries mods, Level level, boolean pvp) {
      boolean storm = isStorming(level, player);
      int compute = countChainTuning(mods);
      double base = (double)AE2LTCommonConfig.railgunBeamDamagePerSettle();
      double bypass = AE2LTCommonConfig.railgunBeamBypass();
      if (storm) {
         base *= 1.25;
      }

      int chains = compute > 0 ? 3 + (compute - 1) * 2 : 0;
      if (storm && chains > 0) {
         chains += 2;
      }

      chains = Math.min(chains, 50);
      double radius = 16.0;
      if (storm) {
         radius += 4.0;
      }

      return new DamageContext(base, bypass, 0.92, chains, compute > 0 ? 1 : 0, radius, false, true, pvp);
   }

   public static DamageContext buildCharged(Player player, RailgunChargeTier tier, RailgunModuleEntries mods, Level level, boolean pvp) {
      boolean storm = isStorming(level, player);
      int compute = countChainTuning(mods);

      double base = switch (tier) {
         case EHV1 -> (double)AE2LTCommonConfig.railgunBaseDamageEhv1();
         case EHV2 -> (double)AE2LTCommonConfig.railgunBaseDamageEhv2();
         case EHV3 -> (double)AE2LTCommonConfig.railgunBaseDamageEhv3();
         default -> 0.0;
      };
      if (storm) {
         base *= 1.25;
      }

      double bypass = tier == RailgunChargeTier.HV ? 0.0 : AE2LTCommonConfig.railgunChargedBypass();
      int var19;
      if (compute > 0) {
         var19 = switch (tier) {
            case EHV1 -> 8;
            case EHV2 -> 14;
            case EHV3 -> 24;
            default -> 0;
         } + Math.max(0, compute - 1) * 4;
      } else {
         var19 = 0;
      }

      int chains = var19;
      if (storm) {
         chains += 2;
      }

      chains = Math.min(chains, 50);

      int forkBase = switch (tier) {
         case EHV1 -> 2;
         case EHV2 -> 3;
         case EHV3 -> 4;
         default -> 1;
      };
      int forks = compute > 0 ? forkBase + Math.max(0, compute - 1) * 1 : 0;
      if (storm) {
         forks++;
      }

      forks = Math.min(forks, 8);

      double radius = switch (tier) {
         case EHV1 -> 20.0;
         case EHV2 -> 26.0;
         case EHV3 -> 32.0;
         default -> 16.0;
      };
      if (storm) {
         radius += 4.0;
      }

      return new DamageContext(base, bypass, 0.92, chains, forks, radius, tier.isMax(), false, pvp);
   }

   private static boolean isStorming(Level level, Player player) {
      return level.m_46470_() && level.m_45527_(player.m_20183_());
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
}
