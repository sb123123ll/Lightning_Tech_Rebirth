package com.moakiee.ae2lt.item.railgun;

import com.moakiee.ae2lt.registry.ModDataComponents;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RailgunStructuralCore {
   private RailgunStructuralCore() {
   }

   public static ItemStack getCore(ItemStack railgun) {
      return railgun != null && !railgun.m_41619_()
         ? ModDataComponents.RAILGUN_STRUCTURAL_CORE.getOrDefault(railgun, ItemStack.f_41583_).m_255036_(1)
         : ItemStack.f_41583_;
   }

   public static void setCore(ItemStack railgun, ItemStack core) {
      if (railgun != null && !railgun.m_41619_()) {
         if (core == null || core.m_41619_()) {
            ModDataComponents.RAILGUN_STRUCTURAL_CORE.remove(railgun);
         } else if (isValidCore(core)) {
            ModDataComponents.RAILGUN_STRUCTURAL_CORE.set(railgun, core.m_255036_(1));
         }
      }
   }

   public static ItemStack removeCore(ItemStack railgun, int amount) {
      if (amount <= 0) {
         return ItemStack.f_41583_;
      } else {
         ItemStack existing = getCore(railgun);
         if (existing.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            setCore(railgun, ItemStack.f_41583_);
            return existing;
         }
      }
   }

   public static boolean hasCore(ItemStack railgun) {
      return isValidCore(getCore(railgun));
   }

   public static boolean canInstallCore(ItemStack railgun, ItemStack candidateCore) {
      return candidateCore != null && !candidateCore.m_41619_() ? isValidCore(candidateCore) : true;
   }

   private static boolean isValidCore(ItemStack core) {
      return core != null && !core.m_41619_() && core.m_150930_((Item)ModItems.ULTIMATE_OVERLOAD_CORE.get());
   }
}
