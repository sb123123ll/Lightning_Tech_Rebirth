package com.moakiee.ae2lt.me;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import org.jetbrains.annotations.Nullable;

public final class GridNodeAccess {
   private GridNodeAccess() {
   }

   @Nullable
   public static IGrid getGridIfPresent(@Nullable IGridNode node) {
      if (node == null) {
         return null;
      } else {
         try {
            return node.getGrid();
         } catch (IllegalStateException var2) {
            return null;
         }
      }
   }

   @Nullable
   public static IGrid getActiveGrid(@Nullable IGridNode node) {
      return node != null && node.isActive() ? getGridIfPresent(node) : null;
   }
}
