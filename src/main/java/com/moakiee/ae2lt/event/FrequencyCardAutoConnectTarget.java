package com.moakiee.ae2lt.event;

record FrequencyCardAutoConnectTarget(FrequencyCardAutoConnectTarget.GridPos pos, String sideName) {
   static FrequencyCardAutoConnectTarget fromPartPlacement(
      FrequencyCardAutoConnectTarget.GridPos clickedPos, String clickedSideName, FrequencyCardAutoConnectTarget.GridPos placementPos, String placementSideName
   ) {
      return new FrequencyCardAutoConnectTarget(placementPos, placementSideName);
   }

   static String placedPartStorageSideName(String placementSideName, boolean cablePart) {
      return cablePart ? "" : placementSideName;
   }

   static record GridPos(int x, int y, int z) {
   }
}
