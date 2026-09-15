package com.moakiee.ae2lt.overload.runtime.pattern;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

public record ParsedPatternOutput(int slotIndex, ItemStack stack, boolean primaryOutput) {
   public ParsedPatternOutput(int slotIndex, ItemStack stack, boolean primaryOutput) {
      if (slotIndex < 0) {
         throw new IllegalArgumentException("slotIndex must be >= 0");
      } else {
         Objects.requireNonNull(stack, "stack");
         if (stack.m_41619_()) {
            throw new IllegalArgumentException("output stack must not be empty");
         } else {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.primaryOutput = primaryOutput;
         }
      }
   }

   public ItemStack stack() {
      return this.stack.m_41777_();
   }

   public int amountPerCraft() {
      return this.stack.m_41613_();
   }
}
