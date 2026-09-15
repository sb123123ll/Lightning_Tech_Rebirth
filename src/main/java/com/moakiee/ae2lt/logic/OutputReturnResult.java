package com.moakiee.ae2lt.logic;

public enum OutputReturnResult {
   EXTRACTED(true),
   BLOCKED(true),
   EMPTY(false),
   UNAVAILABLE(false);

   private final boolean active;

   private OutputReturnResult(boolean active) {
      this.active = active;
   }

   boolean keepsSweepActive() {
      return this.active;
   }
}
