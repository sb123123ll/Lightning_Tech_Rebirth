package com.moakiee.ae2lt.client;

import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import com.moakiee.ae2lt.item.PhaseLockProjectionItem;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import com.moakiee.ae2lt.network.DashPacket;
import com.moakiee.ae2lt.network.NetworkInit;
import com.moakiee.ae2lt.network.hub.OpenDeviceHubPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "ae2lt",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class DeviceHubKeyMappings {
   private static final String CATEGORY = "key.categories.ae2lt";
   public static final KeyMapping DASH = new KeyMapping("key.ae2lt.dash", 86, "key.categories.ae2lt");
   public static final KeyMapping OPEN_CONFIG = new KeyMapping("key.ae2lt.open_config", 71, "key.categories.ae2lt");

   private DeviceHubKeyMappings() {
   }

   @SubscribeEvent
   public static void register(RegisterKeyMappingsEvent event) {
      event.register(DASH);
      event.register(OPEN_CONFIG);
   }

   @EventBusSubscriber(
      modid = "ae2lt",
      bus = Bus.FORGE,
      value = {Dist.CLIENT}
   )
   public static final class DeviceHubRuntimeHandler {
      private DeviceHubRuntimeHandler() {
      }

      @SubscribeEvent
      public static void onClientTick(ClientTickEvent event) {
         if (event.phase == Phase.END) {
            Minecraft minecraft = Minecraft.m_91087_();
            if (minecraft.f_91074_ != null && minecraft.f_91080_ == null) {
               while (DeviceHubKeyMappings.DASH.m_90859_()) {
                  NetworkInit.sendToServer(new DashPacket());
               }

               while (DeviceHubKeyMappings.OPEN_CONFIG.m_90859_()) {
                  int defaultTab = -1;
                  if (minecraft.f_91074_.m_21205_().m_41720_() instanceof ElectromagneticRailgunItem
                     || minecraft.f_91074_.m_21206_().m_41720_() instanceof ElectromagneticRailgunItem) {
                     defaultTab = 4;
                  }

                  if (defaultTab < 0) {
                     for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        if (minecraft.f_91074_.m_6844_(slot).m_41720_() instanceof PhaseLockProjectionItem projection && projection.equipmentSlot() == slot) {
                           defaultTab = tabFor(slot);
                           break;
                        }
                     }
                  }

                  if (defaultTab < 0) {
                     for (EquipmentSlot slotx : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        ItemStack armor = minecraft.f_91074_.m_6844_(slotx);
                        if (armor.m_41720_() instanceof BaseCelestweaveArmorItem) {
                           defaultTab = tabFor(slotx);
                           break;
                        }
                     }
                  }

                  if (defaultTab >= 0) {
                     NetworkInit.sendToServer(new OpenDeviceHubPacket(defaultTab));
                  }
               }
            }
         }
      }

      private static int tabFor(EquipmentSlot slot) {
         return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
         };
      }
   }
}
