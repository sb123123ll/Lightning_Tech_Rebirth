package com.moakiee.ae2lt.me.cell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.moakiee.ae2lt.item.FixedInfiniteCellItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FixedInfiniteCellHandler implements ICellHandler {
   public static final FixedInfiniteCellHandler INSTANCE = new FixedInfiniteCellHandler();

   private FixedInfiniteCellHandler() {
   }

   public boolean isCell(ItemStack stack) {
      return stack.m_41720_() instanceof FixedInfiniteCellItem;
   }

   @Nullable
   public StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
      if (!(stack.m_41720_() instanceof FixedInfiniteCellItem)) {
         return null;
      } else {
         if (FixedInfiniteCellItem.isOuterCell(stack)) {
            FixedInfiniteCellItem.initializeOuterCell(stack);
         }

         return new FixedInfiniteCellInventory(stack, 32.0, host);
      }
   }
}
