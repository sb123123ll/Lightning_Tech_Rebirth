package com.moakiee.ae2lt.item;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TerminalFrequencyCardFinder {
   private TerminalFrequencyCardFinder() {
   }

   public static List<ItemStack> findFrequencyCards(Player player) {
      List<ItemStack> result = new ArrayList<>();
      Inventory inventory = player.m_150109_();

      for (ItemStack stack : inventory.f_35974_) {
         collectFromTerminal(stack, result);
      }

      for (ItemStack stack : inventory.f_35976_) {
         collectFromTerminal(stack, result);
      }

      for (ItemStack stack : CuriosFrequencyCardFinder.findAllEquippedStacks(player)) {
         collectFromTerminal(stack, result);
      }

      return result;
   }

   private static void collectFromTerminal(ItemStack terminalStack, List<ItemStack> out) {
      if (!terminalStack.m_41619_() && terminalStack.m_41720_() instanceof IUpgradeableItem upgradeable) {
         IUpgradeInventory var6 = upgradeable.getUpgrades(terminalStack);

         for (int slot = 0; slot < var6.size(); slot++) {
            ItemStack card = var6.getStackInSlot(slot);
            if (card.m_41720_() instanceof OverloadedFrequencyCardItem) {
               out.add(card);
            }
         }
      }
   }
}
