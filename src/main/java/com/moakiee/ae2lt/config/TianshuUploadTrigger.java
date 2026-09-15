package com.moakiee.ae2lt.config;

public enum TianshuUploadTrigger {
   NO_SHIFT,
   SHIFT,
   CTRL,
   ALT,
   MANUAL_ONLY;

   public TianshuUploadTrigger next() {
      TianshuUploadTrigger[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }
}
