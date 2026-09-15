package com.moakiee.ae2lt.celestweave.module;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

public final class SaturationSubmodule extends AbstractCelestweaveArmorSubmodule {
   public static final SaturationSubmodule INSTANCE = new SaturationSubmodule();
   private static final String TAG_READY_AT_TICK = "SaturationCheckReadyAtTick";

   private SaturationSubmodule() {
   }

   @Override
   public String id() {
      return "saturation";
   }

   @Override
   public String nameKey() {
      return "ae2lt.celestweave.feature.saturation.name";
   }

   @Override
   public String descriptionKey() {
      return "ae2lt.celestweave.feature.saturation.desc";
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
      if (dist == Dist.DEDICATED_SERVER) {
         setCooldown(armor, player, 0);
      }
   }

   @Override
   public int tickActive(@Nullable Player player, Dist dist, ItemStack armor) {
      return 0;
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
         if (!data.m_128425_("SaturationCheckReadyAtTick", 4)) {
            return 0;
         } else {
            long remaining = data.m_128454_("SaturationCheckReadyAtTick") - player.m_9236_().m_46467_();
            return (int)Math.min(2147483647L, Math.max(0L, remaining));
         }
      }
   }

   public static void setCooldown(ItemStack armor, @Nullable Player player, int ticks) {
      CompoundTag data = CelestweaveArmorState.getSubmoduleData(armor, INSTANCE);
      if (ticks > 0 && player != null) {
         data.m_128356_("SaturationCheckReadyAtTick", player.m_9236_().m_46467_() + (long)ticks);
      } else {
         data.m_128473_("SaturationCheckReadyAtTick");
      }

      CelestweaveArmorState.setSubmoduleData(armor, INSTANCE, data);
   }
}
