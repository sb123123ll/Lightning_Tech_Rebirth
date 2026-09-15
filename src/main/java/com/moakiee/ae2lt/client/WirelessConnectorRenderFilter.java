package com.moakiee.ae2lt.client;

final class WirelessConnectorRenderFilter {
   private WirelessConnectorRenderFilter() {
   }

   static boolean shouldRenderHost(
      boolean hasSelection, boolean selectionInCurrentDimension, long selectedPos, String selectedHostType, String hostType, long hostPos
   ) {
      return !hasSelection ? true : selectionInCurrentDimension && hostPos == selectedPos && hostType.equals(selectedHostType);
   }
}
