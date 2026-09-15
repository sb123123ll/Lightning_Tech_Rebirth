package com.moakiee.ae2lt.logic.persistence;

import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ControllerMachineIdentity {
   private static final String TAG_MACHINE_ID = "ae2lt:controller_machine_id";

   private ControllerMachineIdentity() {
   }

   public static UUID read(ItemStack stack) {
      if (stack != null && !stack.m_41619_()) {
         CompoundTag tag = ItemStackTagSupport.getTagCopy(stack);
         return read(tag);
      } else {
         return null;
      }
   }

   public static void write(ItemStack stack, UUID id) {
      if (stack != null && !stack.m_41619_() && id != null) {
         ItemStackTagSupport.updateTag(stack, tag -> write(tag, id));
      }
   }

   static UUID read(CompoundTag tag) {
      return tag != null && tag.m_128403_("ae2lt:controller_machine_id") ? tag.m_128342_("ae2lt:controller_machine_id") : null;
   }

   static void write(CompoundTag tag, UUID id) {
      if (tag != null && id != null) {
         tag.m_128362_("ae2lt:controller_machine_id", id);
      }
   }
}
