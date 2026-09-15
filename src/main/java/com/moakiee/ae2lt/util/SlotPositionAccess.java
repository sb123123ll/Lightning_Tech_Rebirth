package com.moakiee.ae2lt.util;

import net.minecraft.world.inventory.Slot;

public final class SlotPositionAccess {
   private SlotPositionAccess() {
   }

   public static void set(Slot slot, int x, int y) {
      slot.f_40220_ = x;
      slot.f_40221_ = y;
   }
}
