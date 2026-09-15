package com.moakiee.ae2lt.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class NativeStackDropHelper {
   private NativeStackDropHelper() {
   }

   public static void popResource(Level level, BlockPos pos, ItemStack stack) {
      for (ItemStack nativeStack : splitForDrop(stack)) {
         Block.m_49840_(level, pos, nativeStack);
      }
   }

   public static void addDrops(List<ItemStack> drops, ItemStack stack) {
      drops.addAll(splitForDrop(stack));
   }

   static List<ItemStack> splitForDrop(ItemStack stack) {
      if (stack.m_41619_()) {
         return List.of();
      } else {
         ItemStack remaining = stack.m_41777_();
         int nativeLimit = Math.max(1, remaining.m_41741_());
         List<ItemStack> result = new ArrayList<>();

         while (!remaining.m_41619_()) {
            result.add(remaining.m_41620_(Math.min(nativeLimit, remaining.m_41613_())));
         }

         return result;
      }
   }
}
