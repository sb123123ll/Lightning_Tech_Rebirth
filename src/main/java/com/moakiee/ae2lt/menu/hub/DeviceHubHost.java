package com.moakiee.ae2lt.menu.hub;

import com.moakiee.ae2lt.celestweave.phase.CelestweaveEquipmentAccess;
import com.moakiee.ae2lt.item.railgun.ElectromagneticRailgunItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

public class DeviceHubHost implements MenuProvider {
   private final int defaultTab;

   public DeviceHubHost(int defaultTab) {
      this.defaultTab = defaultTab;
   }

   public Component m_5446_() {
      return Component.m_237115_("gui.ae2lt.device_hub");
   }

   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new DeviceHubMenu(containerId, playerInventory, this.defaultTab);
   }

   public static void open(ServerPlayer player, int defaultTab) {
      int resolvedTab = resolveDefaultTab(player, defaultTab);
      if (resolvedTab >= 0) {
         NetworkHooks.openScreen(player, new DeviceHubHost(resolvedTab), buf -> buf.m_130130_(resolvedTab));
      }
   }

   public static int tabForArmorSlot(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> 0;
         case CHEST -> 1;
         case LEGS -> 2;
         case FEET -> 3;
         default -> 1;
      };
   }

   private static int resolveDefaultTab(Player player, int defaultTab) {
      if (hasDeviceForTab(player, defaultTab)) {
         return defaultTab;
      } else {
         for (int tab = 0; tab < 5; tab++) {
            if (hasDeviceForTab(player, tab)) {
               return tab;
            }
         }

         return -1;
      }
   }

   private static boolean hasDeviceForTab(Player player, int tab) {
      return switch (tab) {
         case 0 -> hasArmor(player, EquipmentSlot.HEAD);
         case 1 -> hasArmor(player, EquipmentSlot.CHEST);
         case 2 -> hasArmor(player, EquipmentSlot.LEGS);
         case 3 -> hasArmor(player, EquipmentSlot.FEET);
         case 4 -> hasRailgun(player);
         default -> false;
      };
   }

   private static boolean hasArmor(Player player, EquipmentSlot slot) {
      return !CelestweaveEquipmentAccess.findArmor(player, slot).m_41619_();
   }

   private static boolean hasRailgun(Player player) {
      return player.m_21205_().m_41720_() instanceof ElectromagneticRailgunItem || player.m_21206_().m_41720_() instanceof ElectromagneticRailgunItem;
   }
}
