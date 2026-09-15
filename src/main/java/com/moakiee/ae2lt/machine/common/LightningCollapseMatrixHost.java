package com.moakiee.ae2lt.machine.common;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import com.moakiee.ae2lt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public interface LightningCollapseMatrixHost {
   IItemHandlerModifiable getMatrixInventory();

   int getMatrixSlot();

   default int getInstalledMatrixCount() {
      ItemStack installed = this.getMatrixInventory().getStackInSlot(this.getMatrixSlot());
      return !installed.m_150930_((Item)ModItems.LIGHTNING_COLLAPSE_MATRIX.get()) ? 0 : Math.min(installed.m_41613_(), this.getMatrixSlotLimit());
   }

   default int getMatrixSlotLimit() {
      return this.getMatrixInventory().getSlotLimit(this.getMatrixSlot());
   }

   default boolean insertMatricesFromHand(Player player, InteractionHand hand) {
      ItemStack heldItem = player.m_21120_(hand);
      ItemStack remainder = this.getMatrixInventory().insertItem(this.getMatrixSlot(), heldItem.m_41777_(), false);
      int inserted = heldItem.m_41613_() - remainder.m_41613_();
      if (inserted <= 0) {
         return false;
      } else {
         if (!player.m_150110_().f_35937_) {
            heldItem.m_41774_(inserted);
         }

         return true;
      }
   }

   default int restoreMatricesFromMemoryCard(@Nullable Player player, int requestedCount) {
      if (player != null && !player.m_9236_().m_5776_()) {
         int desiredCount = LightningCollapseMatrixCounts.clampToSlotLimit(requestedCount, this.getMatrixSlotLimit());
         int installedCount = this.getInstalledMatrixCount();
         if (installedCount > desiredCount) {
            ItemStack removed = this.getMatrixInventory().extractItem(this.getMatrixSlot(), installedCount - desiredCount, false);
            if (!player.m_150110_().f_35937_) {
               returnToPlayer(player, removed);
            }

            installedCount -= removed.m_41613_();
         }

         int missingCount = desiredCount - installedCount;
         if (missingCount <= 0) {
            return 0;
         } else {
            ItemStack desiredStack = new ItemStack((ItemLike)ModItems.LIGHTNING_COLLAPSE_MATRIX.get(), missingCount);
            ItemStack simulatedRemainder = this.getMatrixInventory().insertItem(this.getMatrixSlot(), desiredStack, true);
            int insertableCount = missingCount - simulatedRemainder.m_41613_();
            if (insertableCount <= 0) {
               return missingCount;
            } else if (player.m_150110_().f_35937_) {
               ItemStack remainder = this.getMatrixInventory().insertItem(this.getMatrixSlot(), desiredStack.m_255036_(insertableCount), false);
               return missingCount - insertableCount + remainder.m_41613_();
            } else {
               InternalInventory source = new PlayerInternalInventory(player.m_150109_());
               ItemStack supplied = source.removeItems(insertableCount, desiredStack, null);
               if (supplied.m_41619_()) {
                  return missingCount;
               } else {
                  ItemStack remainder = this.getMatrixInventory().insertItem(this.getMatrixSlot(), supplied, false);
                  int inserted = supplied.m_41613_() - remainder.m_41613_();
                  if (!remainder.m_41619_()) {
                     ItemStack sourceRemainder = source.addItems(remainder);
                     returnToPlayer(player, sourceRemainder);
                  }

                  return missingCount - inserted;
               }
            }
         }
      } else {
         return 0;
      }
   }

   @Nullable
   static LightningCollapseMatrixHost find(Level level, BlockPos pos) {
      BlockEntity lowerPos = level.m_7702_(pos);
      if (lowerPos instanceof LightningCollapseMatrixHost) {
         return (LightningCollapseMatrixHost)lowerPos;
      } else {
         BlockState state = level.m_8055_(pos);
         if (state.m_61138_(BlockStateProperties.f_61401_) && state.m_61143_(BlockStateProperties.f_61401_) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPosx = pos.m_7495_();
            BlockState lowerState = level.m_8055_(lowerPosx);
            if (lowerState.m_60713_(state.m_60734_())
               && lowerState.m_61138_(BlockStateProperties.f_61401_)
               && lowerState.m_61143_(BlockStateProperties.f_61401_) == DoubleBlockHalf.LOWER) {
               return level.m_7702_(lowerPosx) instanceof LightningCollapseMatrixHost host ? host : null;
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private static void returnToPlayer(Player player, ItemStack stack) {
      if (!stack.m_41619_()) {
         InternalInventory destination = new PlayerInternalInventory(player.m_150109_());
         ItemStack remainder = destination.addItems(stack);
         if (!remainder.m_41619_()) {
            player.m_36176_(remainder, false);
         }
      }
   }
}
