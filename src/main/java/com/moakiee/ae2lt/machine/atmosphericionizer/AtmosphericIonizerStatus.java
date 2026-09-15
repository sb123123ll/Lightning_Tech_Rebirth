package com.moakiee.ae2lt.machine.atmosphericionizer;

public enum AtmosphericIonizerStatus {
   IDLE("ae2lt.gui.atmospheric_ionizer.status.idle"),
   CHARGING("ae2lt.gui.atmospheric_ionizer.status.charging"),
   WAITING_AE("ae2lt.gui.atmospheric_ionizer.status.waiting_ae"),
   WAITING_INPUT("ae2lt.gui.atmospheric_ionizer.status.waiting_input"),
   INVALID_DIMENSION("ae2lt.gui.atmospheric_ionizer.status.invalid_dimension"),
   TARGET_ALREADY_ACTIVE("ae2lt.gui.atmospheric_ionizer.status.target_active"),
   READY("ae2lt.gui.atmospheric_ionizer.status.ready");

   private final String translationKey;

   private AtmosphericIonizerStatus(String translationKey) {
      this.translationKey = translationKey;
   }

   public String translationKey() {
      return this.translationKey;
   }

   public static AtmosphericIonizerStatus fromOrdinal(int ordinal) {
      AtmosphericIonizerStatus[] values = values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
   }
}
