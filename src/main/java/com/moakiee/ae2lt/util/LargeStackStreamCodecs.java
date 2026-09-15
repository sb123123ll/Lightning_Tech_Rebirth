package com.moakiee.ae2lt.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class LargeStackStreamCodecs {
   private LargeStackStreamCodecs() {
   }

   public static void writeItemStack(FriendlyByteBuf buffer, ItemStack stack) {
      if (stack.m_41619_()) {
         buffer.writeItemStack(ItemStack.f_41583_, true);
         buffer.m_130130_(0);
      } else {
         buffer.writeItemStack(stack.m_255036_(1), true);
         writeStackCount(buffer, stack.m_41613_());
      }
   }

   public static ItemStack readItemStack(FriendlyByteBuf buffer) {
      ItemStack stack = buffer.m_130267_();
      int count = readStackCount(buffer);
      return !stack.m_41619_() && count > 0 ? stack.m_255036_(count) : ItemStack.f_41583_;
   }

   static void writeStackCount(FriendlyByteBuf buffer, int count) {
      buffer.m_130130_(Math.max(0, count));
   }

   static int readStackCount(FriendlyByteBuf buffer) {
      return buffer.m_130242_();
   }
}
