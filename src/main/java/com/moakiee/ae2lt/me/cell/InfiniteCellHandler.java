package com.moakiee.ae2lt.me.cell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import com.moakiee.ae2lt.item.InfiniteStorageCellItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class InfiniteCellHandler implements ICellHandler {
   public static final InfiniteCellHandler INSTANCE = new InfiniteCellHandler();

   private InfiniteCellHandler() {
   }

   public boolean isCell(ItemStack is) {
      Item item = is.m_41720_();
      return item instanceof InfiniteStorageCellItem || item instanceof FixedInfiniteCellItem;
   }

   @Nullable
   public StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host) {
      if (is.m_41720_() instanceof InfiniteStorageCellItem cell) {
         return InfiniteCellInventory.create(
            is, host, cell.getBytesPerType(), cell.getMaxTypes(), cell.getCapacityLo(), cell.getCapacityHi(), cell.getIdleDrain()
         );
      } else if (is.m_41720_() instanceof FixedInfiniteCellItem) {
         if (FixedInfiniteCellItem.isOuterCell(is)) {
            FixedInfiniteCellItem.initializeOuterCell(is);
         }

         return new FixedInfiniteCellInventory(is, 32.0, host);
      } else {
         return null;
      }
   }
}
