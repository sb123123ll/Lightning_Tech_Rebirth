package com.moakiee.ae2lt.item;

import appeng.items.storage.BasicStorageCell;
import com.moakiee.ae2lt.me.key.LightningKeyType;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;

public final class LightningStorageComponentItem extends BasicStorageCell {
   private static final int BYTES_PER_TYPE = 8;
   private static final int TOTAL_TYPES = 2;
   private final int usableCapacity;
   private final int totalBytes;

   public LightningStorageComponentItem(ItemLike coreItem, int usableCapacity, double idleDrain) {
      super(new Properties().m_41487_(1), coreItem, (ItemLike)ModItems.LIGHTNING_ITEM_CELL_HOUSING.get(), idleDrain, 1, 8, 2, LightningKeyType.INSTANCE);
      this.usableCapacity = usableCapacity;
      if ((usableCapacity & 7) != 0) {
         throw new IllegalArgumentException("Lightning storage component capacity must be a multiple of 8: " + usableCapacity);
      } else {
         this.totalBytes = usableCapacity + 16;
      }
   }

   public int getBytes(ItemStack cellItem) {
      return this.totalBytes;
   }

   public int getUsableCapacity() {
      return this.usableCapacity;
   }
}
