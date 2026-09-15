package com.moakiee.ae2lt.me.cell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.moakiee.ae2lt.item.VoidStorageCellItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class VoidCellHandler implements ICellHandler {
   public static final VoidCellHandler INSTANCE = new VoidCellHandler();

   private VoidCellHandler() {
   }

   public boolean isCell(ItemStack stack) {
      return stack.m_41720_() instanceof VoidStorageCellItem;
   }

   @Nullable
   public StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
      return this.isCell(stack) ? new VoidCellInventory(stack, host) : null;
   }
}
