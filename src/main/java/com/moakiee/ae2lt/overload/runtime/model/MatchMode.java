package com.moakiee.ae2lt.overload.runtime.model;

public enum MatchMode {
   STRICT,
   ID_ONLY;

   public boolean ignoresComponents() {
      return this == ID_ONLY;
   }
}
