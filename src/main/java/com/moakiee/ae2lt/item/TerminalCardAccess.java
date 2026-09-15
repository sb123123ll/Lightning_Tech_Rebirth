package com.moakiee.ae2lt.item;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableItem;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.ItemStack;

public final class TerminalCardAccess {
   private TerminalCardAccess() {
   }

   public static OverloadedFrequencyCardData readCardData(ItemStack terminalStack) {
      ItemStack card = findCard(terminalStack);
      return card.m_41619_() ? OverloadedFrequencyCardData.empty() : OverloadedFrequencyCardItem.getData(card);
   }

   public static OverloadedFrequencyCardData readCardData(IUpgradeInventory upgrades) {
      ItemStack card = findCard(upgrades);
      return card.m_41619_() ? OverloadedFrequencyCardData.empty() : OverloadedFrequencyCardItem.getData(card);
   }

   public static boolean hasCard(ItemStack terminalStack) {
      return !findCard(terminalStack).m_41619_();
   }

   public static boolean hasCard(IUpgradeInventory upgrades) {
      return !findCard(upgrades).m_41619_();
   }

   public static ItemStack findCard(ItemStack terminalStack) {
      return !terminalStack.m_41619_() && terminalStack.m_41720_() instanceof IUpgradeableItem upgradeable
         ? findCard(upgradeable.getUpgrades(terminalStack))
         : ItemStack.f_41583_;
   }

   public static ItemStack findCard(IUpgradeInventory upgrades) {
      for (int slot = 0; slot < upgrades.size(); slot++) {
         ItemStack card = upgrades.getStackInSlot(slot);
         if (card.m_41720_() instanceof OverloadedFrequencyCardItem) {
            return card;
         }
      }

      return ItemStack.f_41583_;
   }

   public static boolean updateCard(ItemStack terminalStack, UnaryOperator<OverloadedFrequencyCardData> mutation) {
      return !terminalStack.m_41619_() && terminalStack.m_41720_() instanceof IUpgradeableItem upgradeable
         ? updateCard(upgradeable.getUpgrades(terminalStack), mutation)
         : false;
   }

   public static boolean updateCard(IUpgradeInventory upgrades, UnaryOperator<OverloadedFrequencyCardData> mutation) {
      for (int slot = 0; slot < upgrades.size(); slot++) {
         ItemStack card = upgrades.getStackInSlot(slot);
         if (card.m_41720_() instanceof OverloadedFrequencyCardItem) {
            ItemStack updated = card.m_41777_();
            OverloadedFrequencyCardItem.setData(updated, mutation.apply(OverloadedFrequencyCardItem.getData(updated)));
            upgrades.setItemDirect(slot, updated);
            return true;
         }
      }

      return false;
   }
}
