package com.moakiee.ae2lt.util;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemStackTagSupport {
   private ItemStackTagSupport() {
   }

   public static CompoundTag getTagCopy(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag == null ? new CompoundTag() : tag.m_6426_();
   }

   public static void updateTag(ItemStack stack, Consumer<CompoundTag> updater) {
      CompoundTag tag = stack.m_41784_();
      updater.accept(tag);
      if (tag.m_128456_()) {
         stack.m_41751_(null);
      }
   }

   public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
      if (tag != null && !tag.m_128456_()) {
         stack.m_41751_(tag.m_6426_());
      } else {
         stack.m_41751_(null);
      }
   }
}
