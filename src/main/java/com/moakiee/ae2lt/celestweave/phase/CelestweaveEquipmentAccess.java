package com.moakiee.ae2lt.celestweave.phase;

import com.moakiee.ae2lt.celestweave.BaseCelestweaveArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CelestweaveEquipmentAccess {
   private CelestweaveEquipmentAccess() {
   }

   public static ItemStack findArmor(Player player, EquipmentSlot slot) {
      if (slot.m_20743_() == Type.ARMOR && player instanceof ServerPlayer serverPlayer) {
         ItemStack privateArmor = PhaseLockService.getPrivateArmor(serverPlayer, slot);
         if (isArmor(privateArmor)) {
            return privateArmor;
         }
      }

      ItemStack equipped = player.m_6844_(slot);
      return isArmor(equipped) ? equipped : ItemStack.f_41583_;
   }

   private static boolean isArmor(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof BaseCelestweaveArmorItem;
   }
}
