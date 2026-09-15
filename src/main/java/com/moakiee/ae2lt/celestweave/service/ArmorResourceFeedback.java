package com.moakiee.ae2lt.celestweave.service;

import com.moakiee.ae2lt.me.key.LightningKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ArmorResourceFeedback {
   private static final int COOLDOWN_TICKS = 40;
   private static final String TAG_PREFIX = "ae2lt.armor_resource_feedback.";

   private ArmorResourceFeedback() {
   }

   public static void noFe(ServerPlayer player) {
      notify(player, "ae2lt.celestweave.fail.no_fe", "fe");
   }

   public static void noHighVoltage(ServerPlayer player) {
      notify(player, "ae2lt.celestweave.fail.no_hv", "hv");
   }

   public static void noExtremeHighVoltage(ServerPlayer player) {
      notify(player, "ae2lt.celestweave.fail.no_ehv", "ehv");
   }

   public static void noLightning(ServerPlayer player, ItemStack armor, ArmorLightningService.LightningCost cost) {
      if (cost != null && !cost.isEmpty()) {
         if (cost.highVoltage() > 0L && !ArmorLightningService.hasCost(player, armor, LightningKey.HIGH_VOLTAGE, cost.highVoltage())) {
            noHighVoltage(player);
         } else if (cost.extremeHighVoltage() > 0L
            && !ArmorLightningService.hasCost(player, armor, LightningKey.EXTREME_HIGH_VOLTAGE, cost.extremeHighVoltage())) {
            noExtremeHighVoltage(player);
         } else {
            if (cost.highVoltage() > 0L) {
               noHighVoltage(player);
            } else if (cost.extremeHighVoltage() > 0L) {
               noExtremeHighVoltage(player);
            }
         }
      }
   }

   private static void notify(ServerPlayer player, String key, String resource) {
      if (player != null) {
         long now = player.m_9236_().m_46467_();
         String tag = "ae2lt.armor_resource_feedback." + resource;
         if (player.getPersistentData().m_128454_(tag) <= now) {
            player.getPersistentData().m_128356_(tag, saturatingAdd(now, 40L));
            player.m_5661_(Component.m_237115_(key), true);
         }
      }
   }

   private static long saturatingAdd(long left, long right) {
      if (right <= 0L) {
         return left;
      } else {
         return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
      }
   }
}
