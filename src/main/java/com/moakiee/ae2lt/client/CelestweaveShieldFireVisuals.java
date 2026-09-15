package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.celestweave.service.ArmorCapabilityCollector;
import com.moakiee.ae2lt.device.capability.DeviceCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class CelestweaveShieldFireVisuals {
   private CelestweaveShieldFireVisuals() {
   }

   @SubscribeEvent
   public static void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
      if (event.getOverlayType() == OverlayType.FIRE && shouldHideFire(event.getPlayer())) {
         event.setCanceled(true);
      }
   }

   public static boolean shouldHideFire(Entity entity) {
      return entity instanceof Player player
         ? ArmorCapabilityCollector.collectPerInstalledStack(player)
            .stream()
            .anyMatch(active -> active.capability() instanceof DeviceCapability.StagedMitigation)
         : false;
   }
}
