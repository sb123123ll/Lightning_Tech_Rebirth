package com.moakiee.ae2lt.logic.craft;

public enum MatrixMultiblockRole {
   EMPTY('.'),
   CASING('S'),
   CONSTRAINT_FRAME('P'),
   GLASS('G'),
   CONTROLLER('C'),
   PORT_CANDIDATE('I'),
   PATTERN_BAY('T'),
   CRAFTING_BAY('H');

   private final char templateKey;

   private MatrixMultiblockRole(char templateKey) {
      this.templateKey = templateKey;
   }

   public char templateKey() {
      return this.templateKey;
   }

   public static MatrixMultiblockRole fromTemplateKey(char key) {
      for (MatrixMultiblockRole role : values()) {
         if (role.templateKey == key) {
            return role;
         }
      }

      throw new IllegalArgumentException("Unknown matrix multiblock template key: " + key);
   }
}
