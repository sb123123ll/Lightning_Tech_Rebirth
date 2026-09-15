package com.moakiee.ae2lt.item;

import appeng.api.stacks.AEKeyType;
import appeng.items.storage.BasicStorageCell;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;

public final class PigmeeStorageCellItem extends BasicStorageCell {
   public static final int TOTAL_BYTES = 256;
   public static final int BYTES_PER_TYPE = 8;
   public static final int TOTAL_TYPES = 16;
   public static final double IDLE_DRAIN = 0.5;

   public PigmeeStorageCellItem(Properties properties) {
      super(
         properties.m_41487_(1),
         (ItemLike)ModItems.PIGMEE_STORAGE_COMPONENT.get(),
         (ItemLike)ModItems.PIGMEE_ITEM_CELL_HOUSING.get(),
         0.5,
         1,
         8,
         16,
         AEKeyType.items()
      );
   }

   public int getBytes(ItemStack cellItem) {
      return 256;
   }
}
