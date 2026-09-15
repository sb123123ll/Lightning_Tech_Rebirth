package com.moakiee.ae2lt.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class OverloadedFilterComponentItem extends AE2LTItem implements ICellWorkbenchItem {
   private static final int CONFIG_SLOTS = 63;
   private static final int UPGRADE_SLOTS = 2;
   private static final String FUZZY_MODE_TAG = "FuzzyMode";

   public OverloadedFilterComponentItem(Properties properties) {
      super(properties);
   }

   public boolean isEditable(ItemStack stack) {
      return true;
   }

   public ConfigInventory getConfigInventory(ItemStack stack) {
      return CellConfig.create(key -> true, stack, 63);
   }

   public FuzzyMode getFuzzyMode(ItemStack stack) {
      String fuzzyMode = stack.m_41784_().m_128461_("FuzzyMode");

      try {
         return FuzzyMode.valueOf(fuzzyMode);
      } catch (IllegalArgumentException var4) {
         return FuzzyMode.IGNORE_ALL;
      }
   }

   public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
      stack.m_41784_().m_128359_("FuzzyMode", mode.name());
   }

   public IUpgradeInventory getUpgrades(ItemStack stack) {
      return UpgradeInventories.forItem(stack, 2);
   }
}
