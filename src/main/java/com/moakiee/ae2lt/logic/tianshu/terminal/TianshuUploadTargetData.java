package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.implementations.blockentities.PatternContainerGroup;

public record TianshuUploadTargetData(PatternContainerGroup group, int providerCount, int availableSlots) {
   public TianshuUploadTargetData(PatternContainerGroup group, int providerCount, int availableSlots) {
      if (group == null) {
         throw new IllegalArgumentException("group");
      } else {
         providerCount = Math.max(0, providerCount);
         availableSlots = Math.max(0, availableSlots);
         this.group = group;
         this.providerCount = providerCount;
         this.availableSlots = availableSlots;
      }
   }
}
