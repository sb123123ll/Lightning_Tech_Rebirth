package com.moakiee.ae2lt.logic.railgun;

import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.item.railgun.RailgunChargeTier;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.railgun.RailgunRecoilFxPacket;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RailgunRecoilService {
   public static final String RECOIL_GRACE_TAG = "ae2lt.railgun_recoil_grace";

   private RailgunRecoilService() {
   }

   public static void apply(ServerPlayer player, RailgunChargeTier tier) {
      if (tier != RailgunChargeTier.HV) {
         if (!wearingFullCelestweaveSet(player)) {
            double speed = switch (tier) {
               case EHV1 -> 0.6;
               case EHV2 -> 1.2;
               case EHV3 -> 2.0;
               default -> 0.0;
            };

            float pitchUp = switch (tier) {
               case EHV1 -> 6.0F;
               case EHV2 -> 12.0F;
               case EHV3 -> 20.0F;
               default -> 0.0F;
            };
            if (player.m_6047_()) {
               speed *= 0.5;
               pitchUp *= 0.5F;
            }

            if (!player.m_20096_()) {
               speed *= 1.5;
            }

            Vec3 backDir = player.m_20154_().m_82490_(-1.0).m_82541_();
            Level level = player.m_9236_();
            BlockPos bp = BlockPos.m_274446_(player.m_20182_().m_82549_(backDir));
            if (!level.m_8055_(bp).m_60795_() && !level.m_8055_(bp).m_60812_(level, bp).m_83281_()) {
               speed = 0.0;
            }

            Vec3 push = backDir.m_82490_(speed);
            PhaseFlightMovementGuard.runAsSelfMovement(player, () -> player.m_20256_(player.m_20184_().m_82549_(push)));
            player.f_19864_ = true;
            player.f_19789_ = 0.0F;
            player.getPersistentData().m_128356_("ae2lt.railgun_recoil_grace", level.m_46467_() + 60L);
            NetworkInit.sendToPlayer(player, new RailgunRecoilFxPacket(pitchUp, tier.ordinal()));
         }
      }
   }

   private static boolean wearingFullCelestweaveSet(ServerPlayer player) {
      return CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.HEAD).m_150930_((Item)ModItems.CELESTWEAVE_OCULUS.get())
         && CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.CHEST).m_150930_((Item)ModItems.CELESTWEAVE_CORE.get())
         && CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.LEGS).m_150930_((Item)ModItems.CELESTWEAVE_CONDUIT.get())
         && CelestweaveEquipmentAccess.findArmor(player, EquipmentSlot.FEET).m_150930_((Item)ModItems.CELESTWEAVE_STRIDE.get());
   }

   public static boolean inRecoilGrace(ServerPlayer player) {
      long t = player.getPersistentData().m_128454_("ae2lt.railgun_recoil_grace");
      return t == 0L ? false : player.m_9236_().m_46467_() <= t;
   }
}
