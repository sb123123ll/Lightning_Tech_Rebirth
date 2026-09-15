package com.moakiee.ae2lt.logic.tianshu;

public enum TianshuMultiblockComponent {
   AIR,
   CASING,
   COOLING,
   GLASS,
   CONTROLLER,
   PORT,
   MAIN_BASELINE,
   MAIN_QUANTUM,
   MAIN_OVERLOAD,
   MAIN_MULTIDIMENSIONAL,
   BLANK_UNIT,
   STORAGE_UNIT,
   PARALLEL_UNIT,
   AMPLIFIER_UNIT,
   CLOSED_LOOP_PATTERN_STORAGE,
   CLOSED_LOOP_SEED_STORAGE,
   OTHER;

   public boolean fillsCoolingPosition() {
      return this == COOLING || this.isClosedLoopStorage();
   }

   public boolean isClosedLoopStorage() {
      return this == CLOSED_LOOP_PATTERN_STORAGE || this == CLOSED_LOOP_SEED_STORAGE;
   }
}
