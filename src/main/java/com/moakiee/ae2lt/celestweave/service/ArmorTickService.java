package com.moakiee.ae2lt.celestweave.service;

import com.moakiee.ae2lt.celestweave.CelestweaveArmorState;
import java.util.List;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;

public final class ArmorTickService {
   private ArmorTickService() {
   }

   public static void tickEquipped(Player player, ItemStack armor, boolean equipped, Provider registries, Dist dist) {
      CelestweaveArmorState.ensureArmorId(armor);
      List<CelestweaveArmorState.InstalledSubmodule> installedSubmodules = CelestweaveArmorState.collectInstalledSubmoduleEntries(armor, registries);
      if (!equipped) {
         CelestweaveArmorState.syncSubmoduleActiveState(player, armor, installedSubmodules, false, dist);
      } else {
         if (player instanceof ServerPlayer serverPlayer) {
            ArmorEnergyService.refillFromBoundNetworkIfLow(serverPlayer, armor, registries);
            if (!ArmorEnergyService.consumePassiveDrain(serverPlayer, armor, installedSubmodules, registries)) {
               CelestweaveArmorState.setModulesPowered(armor, false);
               CelestweaveArmorState.syncSubmoduleActiveState(player, armor, installedSubmodules, false, dist);
               CelestweaveArmorState.tickEquipped(player, armor, installedSubmodules, registries);
               return;
            }

            CelestweaveArmorState.setModulesPowered(armor, true);
         }

         CelestweaveArmorState.syncSubmoduleActiveState(player, armor, installedSubmodules, true, dist);
         CelestweaveArmorState.tickActiveSubmodules(player, armor, installedSubmodules, dist);
         CelestweaveArmorState.tickEquipped(player, armor, installedSubmodules, registries);
      }
   }
}
