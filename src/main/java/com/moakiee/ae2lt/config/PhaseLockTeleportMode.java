package com.moakiee.ae2lt.config;

public enum PhaseLockTeleportMode {
   IGNORE_ALL("ignore-all"),
   IGNORE_COMMAND("ignore-command"),
   IGNORE_NONE("ignore-none");

   private final String configValue;

   private PhaseLockTeleportMode(String configValue) {
      this.configValue = configValue;
   }

   public String configValue() {
      return this.configValue;
   }

   public boolean disablesProtection() {
      return this == IGNORE_ALL;
   }

   public boolean ignoresPrivilegedCommands() {
      return this == IGNORE_COMMAND;
   }

   public static PhaseLockTeleportMode fromConfigValue(String value) {
      for (PhaseLockTeleportMode mode : values()) {
         if (mode.configValue.equals(value)) {
            return mode;
         }
      }

      return IGNORE_COMMAND;
   }

   public static boolean isValidConfigValue(Object value) {
      if (value instanceof String text) {
         for (PhaseLockTeleportMode mode : values()) {
            if (mode.configValue.equals(text)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
