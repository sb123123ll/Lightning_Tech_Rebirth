package com.moakiee.ae2lt.client.railgun;

import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.railgun.RailgunBeamTogglePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "ae2lt",
   value = {Dist.CLIENT}
)
public final class RailgunBeamInput {
   private static boolean firing = false;

   private RailgunBeamInput() {
   }

   public static void reset() {
      firing = false;
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent e) {
      if (e.phase == Phase.START) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ != null && e.player == mc.f_91074_) {
            boolean holdingGun = mc.f_91074_.m_21205_().m_41720_() instanceof ElectromagneticRailgunItem;
            boolean attackPressed = mc.f_91066_.f_92096_.m_90857_() && mc.f_91080_ == null && holdingGun;
            if (attackPressed != firing) {
               firing = attackPressed;
               RailgunBeamRenderClient.setLocalRequestedFiring(firing);
               NetworkInit.sendToServer(new RailgunBeamTogglePacket(firing, InteractionHand.MAIN_HAND));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onAttackKeyPressed(InteractionKeyMappingTriggered e) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc.f_91074_ != null) {
         if (e.isAttack()) {
            ItemStack main = mc.f_91074_.m_21205_();
            if (main.m_41720_() instanceof ElectromagneticRailgunItem) {
               e.setCanceled(true);
               e.setSwingHand(false);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent e) {
      if (e.getEntity().m_21205_().m_41720_() instanceof ElectromagneticRailgunItem) {
         e.setCanceled(true);
      }
   }
}
