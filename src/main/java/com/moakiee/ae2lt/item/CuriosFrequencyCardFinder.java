package com.moakiee.ae2lt.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

final class CuriosFrequencyCardFinder {
   private CuriosFrequencyCardFinder() {
   }

   static List<ItemStack> findFrequencyCards(Player player) {
      return !ModList.get().isLoaded("curios") ? List.of() : CuriosFrequencyCardFinder.Bridge.findFrequencyCards(player);
   }

   static List<ItemStack> findAllEquippedStacks(Player player) {
      return !ModList.get().isLoaded("curios") ? List.of() : CuriosFrequencyCardFinder.Bridge.findAllEquippedStacks(player);
   }

   private static final class Bridge {
      static List<ItemStack> findFrequencyCards(Player player) {
         return CuriosApi.getCuriosInventory(player)
            .map(handler -> handler.findCurios(stack -> stack.m_41720_() instanceof OverloadedFrequencyCardItem).stream().map(SlotResult::stack).toList())
            .orElse(List.of());
      }

      static List<ItemStack> findAllEquippedStacks(Player player) {
         return CuriosApi.getCuriosInventory(player).map(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            List<ItemStack> stacks = new ArrayList<>(equipped.getSlots());

            for (int slot = 0; slot < equipped.getSlots(); slot++) {
               stacks.add(equipped.getStackInSlot(slot));
            }

            return stacks;
         }).orElse(List.of());
      }
   }
}
