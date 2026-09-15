package com.moakiee.ae2lt.me.cell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.moakiee.ae2lt.item.BulkLightningStorageCellItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class BulkLightningCellHandler implements ICellHandler {
   public static final BulkLightningCellHandler INSTANCE = new BulkLightningCellHandler();

   private BulkLightningCellHandler() {
   }

   public boolean isCell(ItemStack stack) {
      return stack.m_41720_() instanceof BulkLightningStorageCellItem;
   }

   @Nullable
   public StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
      return this.isCell(stack) ? new BulkLightningCellInventory(stack, host) : null;
   }
}
