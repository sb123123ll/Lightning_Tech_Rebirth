package com.moakiee.ae2lt.celestweave.phase;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;

public final class PhaseLockProjectionRules {
   private PhaseLockProjectionRules() {
   }

   public static int expectedInventorySlot(EquipmentSlot slot) {
      return slot != null && slot.m_20743_() == Type.ARMOR ? 36 + slot.m_20749_() : -1;
   }

   public static boolean isExpectedSlot(EquipmentSlot slot, int slotId) {
      return slotId == expectedInventorySlot(slot);
   }
}
