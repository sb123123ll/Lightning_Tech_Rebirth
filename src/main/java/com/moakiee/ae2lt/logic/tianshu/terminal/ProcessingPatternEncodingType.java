package com.moakiee.ae2lt.logic.tianshu.terminal;

public enum ProcessingPatternEncodingType {
   NORMAL(false, false),
   ADVANCED(true, false),
   OVERLOAD(false, true),
   ADVANCED_OVERLOAD(true, true);

   private final boolean advanced;
   private final boolean overload;

   private ProcessingPatternEncodingType(boolean advanced, boolean overload) {
      this.advanced = advanced;
      this.overload = overload;
   }

   public boolean hasAdvanced() {
      return this.advanced;
   }

   public boolean hasOverload() {
      return this.overload;
   }

   public boolean includes(ProcessingPatternEncodingType capability) {
      return (!capability.advanced || this.advanced) && (!capability.overload || this.overload);
   }

   public static ProcessingPatternEncodingType fromConfigs(
      ProcessingPatternEncodingType.AdvancedConfig advancedConfig, ProcessingPatternEncodingType.OverloadConfig overloadConfig
   ) {
      if (advancedConfig != null) {
         return overloadConfig != null ? ADVANCED_OVERLOAD : ADVANCED;
      } else {
         return overloadConfig != null ? OVERLOAD : NORMAL;
      }
   }

   public static record AdvancedConfig(int[] directions) {
      public AdvancedConfig(int[] directions) {
         directions = directions == null ? new int[0] : (int[])directions.clone();
         this.directions = directions;
      }

      public int[] directions() {
         return (int[])this.directions.clone();
      }

      public int direction(int slot) {
         return this.directions != null && slot >= 0 && slot < this.directions.length ? Math.max(0, Math.min(6, this.directions[slot])) : 0;
      }
   }

   public static record OverloadConfig(int[] inputIdOnly, int[] outputIdOnly) {
      public OverloadConfig(int[] inputIdOnly, int[] outputIdOnly) {
         inputIdOnly = inputIdOnly == null ? new int[0] : (int[])inputIdOnly.clone();
         outputIdOnly = outputIdOnly == null ? new int[0] : (int[])outputIdOnly.clone();
         this.inputIdOnly = inputIdOnly;
         this.outputIdOnly = outputIdOnly;
      }

      public int[] inputIdOnly() {
         return (int[])this.inputIdOnly.clone();
      }

      public int[] outputIdOnly() {
         return (int[])this.outputIdOnly.clone();
      }

      public boolean isInputIdOnly(int slot) {
         return contains(this.inputIdOnly, slot);
      }

      public boolean isOutputIdOnly(int slot) {
         return contains(this.outputIdOnly, slot);
      }

      private static boolean contains(int[] slots, int slot) {
         if (slots == null) {
            return false;
         } else {
            for (int candidate : slots) {
               if (candidate == slot) {
                  return true;
               }
            }

            return false;
         }
      }
   }
}
