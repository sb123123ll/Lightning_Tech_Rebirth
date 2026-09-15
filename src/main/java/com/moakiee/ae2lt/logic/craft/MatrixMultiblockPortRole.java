package com.moakiee.ae2lt.logic.craft;

public enum MatrixMultiblockPortRole {
   CONTROLLER(false, true),
   INTERFACE(true, true),
   STRUCTURE(false, false);

   private final boolean exposesAeNetwork;
   private final boolean requiredForFormation;

   private MatrixMultiblockPortRole(boolean exposesAeNetwork, boolean requiredForFormation) {
      this.exposesAeNetwork = exposesAeNetwork;
      this.requiredForFormation = requiredForFormation;
   }

   public boolean exposesAeNetwork() {
      return this.exposesAeNetwork;
   }

   public boolean requiredForFormation() {
      return this.requiredForFormation;
   }
}
