package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.config.AE2LTConfigMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class ConfigMigrationNoticeHandler {
   private static final String NOTIFIED_TAG = "ae2lt.config_migrated_v2_notified";

   private ConfigMigrationNoticeHandler() {
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (AE2LTConfigMigration.migrationOccurred()) {
         if (!(event.getEntity() instanceof FakePlayer) && event.getEntity() instanceof ServerPlayer serverPlayer) {
            CompoundTag data = serverPlayer.getPersistentData();
            if (!data.m_128471_("ae2lt.config_migrated_v2_notified")) {
               serverPlayer.m_213846_(Component.m_237115_("message.ae2lt.config_migrated_v2"));
               serverPlayer.m_213846_(Component.m_237115_("message.ae2lt.config_migrated_v2.matrix"));
               data.m_128379_("ae2lt.config_migrated_v2_notified", true);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      if (event.getOriginal().getPersistentData().m_128471_("ae2lt.config_migrated_v2_notified")) {
         event.getEntity().getPersistentData().m_128379_("ae2lt.config_migrated_v2_notified", true);
      }
   }
}
