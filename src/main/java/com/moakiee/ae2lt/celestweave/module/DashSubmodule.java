package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import com.moakiee.ae2lt.celestweave.PhaseFlightMovementGuard;
import com.moakiee.ae2lt.celestweave.service.ArmorEnergyService;
import com.moakiee.ae2lt.celestweave.service.ArmorResourceFeedback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class DashSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final DashSubmodule INSTANCE = new DashSubmodule();
   private static final double IMPULSE = 1.8;
   private static final int COOLDOWN_TICKS = 40;
   private static final String TAG_READY_AT_TICK = "DashReadyAtTick";

   private DashSubmodule() {
   }

   @Override
   public String id() {
      return "dash";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.dash.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.dash.desc";
   }

   @Override
   public boolean defaultEnabled() {
      return true;
   }

   @Override
   public int getMaxInstallAmount() {
      return 1;
   }

   @Override
   public void onDeactivated(@Nullable Player player, Dist dist, ItemStack armor) {
      if (player != null && dist == Dist.DEDICATED_SERVER) {
         setCooldown(armor, player, 0);
      }
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      return 0;
   }

   public static void applyDash(ServerPlayer player, ItemStack armor) {
      DashSubmodule sub = INSTANCE;
      if (sub.isActive(armor)) {
         if (getCooldown(armor, player) > 0) {
            player.m_5661_(Component.m_237115_("ae2lt.celestweave.feature.dash.cooldown"), true);
         } else {
            long feCost = 50000L;
            ArmorEnergyService.EnergyPayment payment = ArmorEnergyService.consumeActiveCostPayment(player, armor, feCost);
            if (!payment.paid()) {
               ArmorResourceFeedback.noFe(player);
            } else {
               Vec3 look = player.m_20154_();
               PhaseFlightMovementGuard.runAsSelfMovement(
                  player,
                  () -> player.m_20334_(
                        player.m_20184_().f_82479_ + look.f_82479_ * 1.8,
                        Math.max(player.m_20184_().f_82480_, 0.0) + 0.3,
                        player.m_20184_().f_82481_ + look.f_82481_ * 1.8
                     )
               );
               player.f_19864_ = true;
               player.m_183634_();
               setCooldown(armor, player, 40);
            }
         }
      }
   }

   @Deprecated
   public static int getCooldown(ItemStack armor) {
      return 0;
   }

   public static int getCooldown(ItemStack armor, @Nullable Player player) {
      if (player == null) {
         return 0;
      } else {
         CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
         if (!data.m_128425_("DashReadyAtTick", 4)) {
            return 0;
         } else {
            long remaining = data.m_128454_("DashReadyAtTick") - player.m_9236_().m_46467_();
            return (int)Math.min(2147483647L, Math.max(0L, remaining));
         }
      }
   }

   private static void setCooldown(ItemStack armor, @Nullable Player player, int ticks) {
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
      if (ticks > 0 && player != null) {
         data.m_128356_("DashReadyAtTick", player.m_9236_().m_46467_() + (long)ticks);
      } else {
         data.m_128473_("DashReadyAtTick");
      }

      CelestweaveArmorState.setSubmoduleData(armor, INSTANCE, data);
   }
}
