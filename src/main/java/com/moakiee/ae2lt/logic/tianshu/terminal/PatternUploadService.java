package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PatternUploadService {
   public static List<PatternUploadTarget> listTargets(IGrid grid) {
      if (grid == null) {
         return List.of();
      } else {
         ArrayList<PatternUploadTarget> result = new ArrayList<>();

         for (PatternProviderLogicHost host : grid.getActiveMachines(PatternProviderLogicHost.class)) {
            BlockEntity blockEntity = host.getBlockEntity();
            if (blockEntity != null && !blockEntity.m_58901_()) {
               Level inventory = blockEntity.m_58904_();
               if (inventory instanceof ServerLevel) {
                  ServerLevel level = (ServerLevel)inventory;
                  InternalInventory inventoryx = host.getTerminalPatternInventory();
                  if (inventoryx != null && inventoryx.size() > 0) {
                     int free = countFreeSlots(inventoryx);
                     result.add(new PatternUploadTarget(level.m_46472_(), blockEntity.m_58899_(), host.getTerminalGroup(), free, inventoryx.size()));
                  }
               }
            }
         }

         result.sort(
            Comparator.<PatternUploadTarget, String>comparing(target -> target.dimension().m_135782_().toString())
               .thenComparingLong(target -> target.pos().m_121878_())
         );
         return List.copyOf(result);
      }
   }

   public static PatternUploadService.UploadResult uploadFromSlot(IGrid grid, PatternUploadTarget selected, InternalInventory source, int sourceSlot) {
      if (grid != null && selected != null && source != null && sourceSlot >= 0 && sourceSlot < source.size()) {
         ItemStack sourceStack = source.getStackInSlot(sourceSlot);
         if (!isOrdinaryUploadablePattern(sourceStack)) {
            return PatternUploadService.UploadResult.INVALID_PATTERN;
         } else {
            PatternProviderLogicHost host = findHost(grid, selected);
            if (host == null) {
               return PatternUploadService.UploadResult.TARGET_OFFLINE;
            } else {
               InternalInventory target = host.getTerminalPatternInventory();
               int targetSlot = firstFreeValidSlot(target, sourceStack);
               if (targetSlot < 0) {
                  return PatternUploadService.UploadResult.NO_FREE_SLOT;
               } else {
                  ItemStack removed = source.extractItem(sourceSlot, 1, false);
                  if (!removed.m_41619_() && ItemStack.m_150942_(sourceStack, removed)) {
                     try {
                        target.setItemDirect(targetSlot, removed);
                        return new PatternUploadService.UploadResult(PatternUploadService.Status.SUCCESS, targetSlot);
                     } catch (RuntimeException var10) {
                        source.addItems(removed);
                        return PatternUploadService.UploadResult.TARGET_OFFLINE;
                     }
                  } else {
                     if (!removed.m_41619_()) {
                        source.addItems(removed);
                     }

                     return PatternUploadService.UploadResult.INVALID_SOURCE;
                  }
               }
            }
         }
      } else {
         return PatternUploadService.UploadResult.INVALID_SOURCE;
      }
   }

   private static PatternProviderLogicHost findHost(IGrid grid, PatternUploadTarget selected) {
      for (PatternProviderLogicHost host : grid.getActiveMachines(PatternProviderLogicHost.class)) {
         BlockEntity blockEntity = host.getBlockEntity();
         if (blockEntity != null
            && !blockEntity.m_58901_()
            && blockEntity.m_58904_() instanceof ServerLevel level
            && selected.dimension().equals(level.m_46472_())
            && selected.pos().equals(blockEntity.m_58899_())) {
            return host;
         }
      }

      return null;
   }

   private static boolean isOrdinaryUploadablePattern(ItemStack stack) {
      return stack != null && !stack.m_41619_() && !(stack.m_41720_() instanceof ClosedLoopPatternItem) && PatternDetailsHelper.isEncodedPattern(stack);
   }

   private static int countFreeSlots(InternalInventory inventory) {
      int result = 0;

      for (int slot = 0; slot < inventory.size(); slot++) {
         if (inventory.getStackInSlot(slot).m_41619_()) {
            result++;
         }
      }

      return result;
   }

   private static int firstFreeValidSlot(InternalInventory inventory, ItemStack stack) {
      if (inventory == null) {
         return -1;
      } else {
         for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).m_41619_() && inventory.isItemValid(slot, stack)) {
               return slot;
            }
         }

         return -1;
      }
   }

   private PatternUploadService() {
   }

   public static enum Status {
      SUCCESS,
      INVALID_SOURCE,
      INVALID_PATTERN,
      TARGET_OFFLINE,
      NO_FREE_SLOT;
   }

   public static record UploadResult(PatternUploadService.Status status, int targetSlot) {
      private static final PatternUploadService.UploadResult INVALID_SOURCE = new PatternUploadService.UploadResult(
         PatternUploadService.Status.INVALID_SOURCE, -1
      );
      private static final PatternUploadService.UploadResult INVALID_PATTERN = new PatternUploadService.UploadResult(
         PatternUploadService.Status.INVALID_PATTERN, -1
      );
      private static final PatternUploadService.UploadResult TARGET_OFFLINE = new PatternUploadService.UploadResult(
         PatternUploadService.Status.TARGET_OFFLINE, -1
      );
      private static final PatternUploadService.UploadResult NO_FREE_SLOT = new PatternUploadService.UploadResult(PatternUploadService.Status.NO_FREE_SLOT, -1);

      public boolean successful() {
         return this.status == PatternUploadService.Status.SUCCESS;
      }
   }
}
