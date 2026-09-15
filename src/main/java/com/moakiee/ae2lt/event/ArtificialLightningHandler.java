package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import com.moakiee.ae2lt.item.OverloadCrystalItem;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class ArtificialLightningHandler {
   private static final String HELD_TICKS_TAG = "ae2lt.overload_held_ticks";
   private static final int SUMMON_DELAY_TICKS = 200;
   private static final int HELD_TICK_INTERVAL = 4;

   private ArtificialLightningHandler() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.START) {
         Player player = event.player;
         if (!(player.m_9236_() instanceof ServerLevel serverLevel) || player.m_5833_()) {
            return;
         }

         if (player.f_19797_ % 4 == 0) {
            boolean carryingOverloadCrystal = isCarryingConfiguredCrystal(player);
            if (!carryingOverloadCrystal) {
               player.getPersistentData().m_128473_("ae2lt.overload_held_ticks");
            } else {
               int heldTicks = player.getPersistentData().m_128451_("ae2lt.overload_held_ticks") + 4;
               if (heldTicks < 200) {
                  player.getPersistentData().m_128405_("ae2lt.overload_held_ticks", heldTicks);
               } else {
                  player.getPersistentData().m_128405_("ae2lt.overload_held_ticks", 0);
                  spawnArtificialLightning(serverLevel, player.m_20182_(), player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
               }
            }
         }
      }
   }

   public static void spawnArtificialLightning(ServerLevel level, Vec3 position, @Nullable ServerPlayer cause) {
      LightningBolt lightningBolt = (LightningBolt)EntityType.f_20465_.m_20615_(level);
      if (lightningBolt != null) {
         lightningBolt.m_20219_(position);
         lightningBolt.m_20874_(false);
         if (cause != null) {
            lightningBolt.m_20879_(cause);
         }

         level.m_7967_(lightningBolt);
      }
   }

   private static boolean isCarryingConfiguredCrystal(Player player) {
      if (AE2LTCommonConfig.artificialLightningTriggerFromHotbar()) {
         if (player.m_21206_().m_41720_() instanceof OverloadCrystalItem) {
            return true;
         }

         for (int slot = 0; slot < 9; slot++) {
            if (((ItemStack)player.m_150109_().f_35974_.get(slot)).m_41720_() instanceof OverloadCrystalItem) {
               return true;
            }
         }
      }

      if (AE2LTCommonConfig.artificialLightningTriggerFromBackpack()) {
         for (int slotx = 9; slotx < player.m_150109_().f_35974_.size(); slotx++) {
            if (((ItemStack)player.m_150109_().f_35974_.get(slotx)).m_41720_() instanceof OverloadCrystalItem) {
               return true;
            }
         }
      }

      return false;
   }
}
