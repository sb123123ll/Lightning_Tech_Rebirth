package com.moakiee.ae2lt.logic;

import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class FluidTankInteractionHelper {
   private FluidTankInteractionHelper() {
   }

   public static boolean insertFromCarried(Player player, FluidTank tank) {
      return interactSource(player, source -> tryEmptyOne(player, tank, source));
   }

   public static boolean extractToCarried(Player player, IFluidHandler tankHandler) {
      return interactSource(player, source -> tryFillOne(player, tankHandler, source));
   }

   public static void clear(FluidTank tank) {
      tank.setFluid(FluidStack.EMPTY);
   }

   private static boolean interactSource(Player player, Function<ItemStack, ItemStack> action) {
      ItemStack carried = player.f_36096_.m_142621_();
      if (!carried.m_41619_()) {
         ItemStack result = action.apply(carried.m_255036_(1));
         if (result == null) {
            return false;
         } else {
            consumeOneFromCarried(player);
            stashIntoCarriedOrInventory(player, result);
            return true;
         }
      } else {
         int selected = player.m_150109_().f_35977_;
         if (selected >= 0 && selected < player.m_150109_().f_35974_.size()) {
            ItemStack hotbar = (ItemStack)player.m_150109_().f_35974_.get(selected);
            if (hotbar.m_41619_()) {
               return false;
            } else {
               ItemStack result = action.apply(hotbar.m_255036_(1));
               if (result == null) {
                  return false;
               } else {
                  hotbar.m_41774_(1);
                  if (hotbar.m_41619_()) {
                     player.m_150109_().f_35974_.set(selected, ItemStack.f_41583_);
                  }

                  if (!result.m_41619_()) {
                     ItemStack slotStack = (ItemStack)player.m_150109_().f_35974_.get(selected);
                     if (slotStack.m_41619_()) {
                        player.m_150109_().f_35974_.set(selected, result);
                     } else if (ItemStack.m_150942_(slotStack, result) && slotStack.m_41613_() + result.m_41613_() <= slotStack.m_41741_()) {
                        slotStack.m_41769_(result.m_41613_());
                     } else if (!player.m_150109_().m_36054_(result)) {
                        player.m_36176_(result, false);
                     }
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      }
   }

   private static ItemStack tryEmptyOne(Player player, FluidTank tank, ItemStack singleUnit) {
      FluidActionResult result = FluidUtil.tryEmptyContainer(singleUnit, tank, Integer.MAX_VALUE, player, true);
      return result.isSuccess() ? result.getResult() : null;
   }

   private static ItemStack tryFillOne(Player player, IFluidHandler tankHandler, ItemStack singleUnit) {
      FluidActionResult result = FluidUtil.tryFillContainer(singleUnit, tankHandler, Integer.MAX_VALUE, player, true);
      return result.isSuccess() ? result.getResult() : null;
   }

   private static void consumeOneFromCarried(Player player) {
      ItemStack carried = player.f_36096_.m_142621_();
      carried.m_41774_(1);
      player.f_36096_.m_142503_(carried.m_41619_() ? ItemStack.f_41583_ : carried);
   }

   private static void stashIntoCarriedOrInventory(Player player, ItemStack result) {
      if (!result.m_41619_()) {
         ItemStack carried = player.f_36096_.m_142621_();
         if (carried.m_41619_()) {
            player.f_36096_.m_142503_(result);
         } else if (ItemStack.m_150942_(carried, result) && carried.m_41613_() + result.m_41613_() <= carried.m_41741_()) {
            carried.m_41769_(result.m_41613_());
            player.f_36096_.m_142503_(carried);
         } else {
            if (!player.m_150109_().m_36054_(result)) {
               player.m_36176_(result, false);
            }
         }
      }
   }
}
