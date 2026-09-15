package com.moakiee.ae2lt.event;

import com.moakiee.ae2lt.lightning.LightningTransformService;
import com.moakiee.ae2lt.lightning.ProtectedItemEntityHelper;
import com.moakiee.ae2lt.logic.research.ResearchRitualService;
import com.moakiee.ae2lt.network.EasterEggPacket;
import com.moakiee.ae2lt.network.NetworkInit;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt"
)
public final class LightningItemTransformationHandler {
   private static final String TRANSFORMATION_CHECKED_TAG = "ae2lt.lightning_item_transform_checked";
   private static final ResourceLocation FUMO_BLOCK_ID = new ResourceLocation("appliedcreate", "whichball_skin_doll");
   private static final int EASTER_EGG_SEARCH_RADIUS = 3;

   private LightningItemTransformationHandler() {
   }

   public static void handleLightningTick(LightningBolt lightningBolt) {
      if (lightningBolt.m_9236_() instanceof ServerLevel serverLevel) {
         CompoundTag data = lightningBolt.getPersistentData();
         if (!data.m_128471_("ae2lt.lightning_item_transform_checked")) {
            data.m_128379_("ae2lt.lightning_item_transform_checked", true);
            ResearchRitualService.handleLightning(serverLevel, lightningBolt);
            LightningTransformService.handleLightning(serverLevel, lightningBolt);
            checkEasterEgg(serverLevel, lightningBolt);
         }
      }
   }

   @SubscribeEvent
   public static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
      if (event.getEntity() instanceof ItemEntity itemEntity
         && (ProtectedItemEntityHelper.isProtectedItem(itemEntity) || ProtectedItemEntityHelper.isFireproofItem(itemEntity))) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent event) {
      if (event.phase == Phase.END && event.level instanceof ServerLevel serverLevel) {
         for (Entity entity : serverLevel.m_8583_()) {
            if (entity instanceof ItemEntity itemEntity) {
               ProtectedItemEntityHelper.tick(itemEntity);
            }
         }
      }
   }

   private static void checkEasterEgg(ServerLevel level, LightningBolt lightningBolt) {
      Optional<Block> fumoOpt = BuiltInRegistries.f_256975_.m_6612_(FUMO_BLOCK_ID);
      if (!fumoOpt.isEmpty()) {
         Block fumoBlock = fumoOpt.get();
         BlockPos center = BlockPos.m_274446_(lightningBolt.m_20182_());

         for (BlockPos pos : BlockPos.m_121940_(center.m_7918_(-3, -1, -3), center.m_7918_(3, 2, 3))) {
            if (level.m_8055_(pos).m_60713_(fumoBlock)) {
               for (ServerPlayer player : level.m_6907_()) {
                  if (player.m_20275_((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_()) < 4096.0) {
                     NetworkInit.sendToPlayer(player, new EasterEggPacket());
                  }
               }

               return;
            }
         }
      }
   }
}
