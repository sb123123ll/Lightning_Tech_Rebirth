package com.moakiee.ae2lt.logic.tianshu.terminal;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ClosedLoopTerminalDraft(
   ItemStack source,
   List<ItemStack> members,
   List<Long> memberCopies,
   List<ItemStack> outputs,
   List<Integer> outputRoles,
   int executionSeedMultiplier,
   int storedTaskMultiplier,
   boolean representsEncodedPattern
) {
   private static final String TAG_SOURCE = "Source";
   private static final String TAG_MEMBERS = "Members";
   private static final String TAG_MEMBER_COPIES = "MemberCopies";
   private static final String TAG_OUTPUTS = "Outputs";
   private static final String TAG_OUTPUT_ROLES = "OutputRoles";
   private static final String TAG_EXECUTION_MULTIPLIER = "ExecutionSeedMultiplier";
   private static final String TAG_STORED_MULTIPLIER = "StoredTaskMultiplier";
   private static final String TAG_REPRESENTS_ENCODED = "RepresentsEncodedPattern";
   private static final String TAG_SLOT = "Slot";

   public ClosedLoopTerminalDraft(
      ItemStack source,
      List<ItemStack> members,
      List<Long> memberCopies,
      List<ItemStack> outputs,
      List<Integer> outputRoles,
      int executionSeedMultiplier,
      int storedTaskMultiplier,
      boolean representsEncodedPattern
   ) {
      source = source == null ? ItemStack.f_41583_ : source.m_41777_();
      members = copyStacks(members, 27, "members");
      outputs = copyStacks(outputs, 9, "outputs");
      memberCopies = List.copyOf(memberCopies);
      outputRoles = List.copyOf(outputRoles);
      if (memberCopies.size() != 27) {
         throw new IllegalArgumentException("invalid closed-loop member-copy count");
      } else if (outputRoles.size() != 9) {
         throw new IllegalArgumentException("invalid closed-loop output-role count");
      } else {
         for (long copies : memberCopies) {
            if (copies < 0L || copies == Long.MAX_VALUE) {
               throw new IllegalArgumentException("invalid closed-loop member copies");
            }
         }

         for (int role : outputRoles) {
            if (role < 0 || role > 2) {
               throw new IllegalArgumentException("invalid closed-loop output role");
            }
         }

         if (executionSeedMultiplier >= 1 && storedTaskMultiplier >= 1) {
            this.source = source;
            this.members = members;
            this.memberCopies = memberCopies;
            this.outputs = outputs;
            this.outputRoles = outputRoles;
            this.executionSeedMultiplier = executionSeedMultiplier;
            this.storedTaskMultiplier = storedTaskMultiplier;
            this.representsEncodedPattern = representsEncodedPattern;
         } else {
            throw new IllegalArgumentException("invalid closed-loop multiplier");
         }
      }
   }

   public CompoundTag write() {
      CompoundTag tag = new CompoundTag();
      tag.m_128365_("Source", this.source.m_41739_(new CompoundTag()));
      tag.m_128365_("Members", writeStacks(this.members));
      tag.m_128388_("MemberCopies", this.memberCopies.stream().mapToLong(Long::longValue).toArray());
      tag.m_128365_("Outputs", writeStacks(this.outputs));
      tag.m_128385_("OutputRoles", this.outputRoles.stream().mapToInt(Integer::intValue).toArray());
      tag.m_128405_("ExecutionSeedMultiplier", this.executionSeedMultiplier);
      tag.m_128405_("StoredTaskMultiplier", this.storedTaskMultiplier);
      tag.m_128379_("RepresentsEncodedPattern", this.representsEncodedPattern);
      return tag;
   }

   @Nullable
   public static ClosedLoopTerminalDraft read(CompoundTag tag) {
      try {
         ItemStack source = ItemStack.m_41712_(tag.m_128469_("Source"));
         long[] copiesArray = tag.m_128467_("MemberCopies");
         int[] rolesArray = tag.m_128465_("OutputRoles");
         if (copiesArray.length == 27 && rolesArray.length == 9) {
            ArrayList<Long> copies = new ArrayList<>(copiesArray.length);

            for (long value : copiesArray) {
               copies.add(value);
            }

            ArrayList<Integer> roles = new ArrayList<>(rolesArray.length);

            for (int value : rolesArray) {
               roles.add(value);
            }

            return new ClosedLoopTerminalDraft(
               source,
               readStacks(tag.m_128437_("Members", 10), 27),
               copies,
               readStacks(tag.m_128437_("Outputs", 10), 9),
               roles,
               Math.max(1, tag.m_128451_("ExecutionSeedMultiplier")),
               Math.max(1, tag.m_128451_("StoredTaskMultiplier")),
               tag.m_128471_("RepresentsEncodedPattern")
            );
         } else {
            return null;
         }
      } catch (RuntimeException var10) {
         return null;
      }
   }

   public static boolean sameState(@Nullable ClosedLoopTerminalDraft left, @Nullable ClosedLoopTerminalDraft right) {
      if (left == right) {
         return true;
      } else {
         return left != null
               && right != null
               && ItemStack.m_41728_(left.source, right.source)
               && sameStacks(left.members, right.members)
               && sameStacks(left.outputs, right.outputs)
            ? left.memberCopies.equals(right.memberCopies)
               && left.outputRoles.equals(right.outputRoles)
               && left.executionSeedMultiplier == right.executionSeedMultiplier
               && left.storedTaskMultiplier == right.storedTaskMultiplier
               && left.representsEncodedPattern == right.representsEncodedPattern
            : false;
      }
   }

   private static List<ItemStack> copyStacks(List<ItemStack> stacks, int expectedSize, String name) {
      if (stacks != null && stacks.size() == expectedSize) {
         ArrayList<ItemStack> result = new ArrayList<>(expectedSize);

         for (ItemStack stack : stacks) {
            result.add(stack == null ? ItemStack.f_41583_ : stack.m_41777_());
         }

         return List.copyOf(result);
      } else {
         throw new IllegalArgumentException("invalid closed-loop " + name + " count");
      }
   }

   private static ListTag writeStacks(List<ItemStack> stacks) {
      ListTag result = new ListTag();

      for (int slot = 0; slot < stacks.size(); slot++) {
         ItemStack stack = stacks.get(slot);
         if (!stack.m_41619_()) {
            CompoundTag entry = new CompoundTag();
            entry.m_128405_("Slot", slot);
            result.add(stack.m_41739_(entry));
         }
      }

      return result;
   }

   private static List<ItemStack> readStacks(ListTag entries, int size) {
      ArrayList<ItemStack> result = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         result.add(ItemStack.f_41583_);
      }

      for (int i = 0; i < entries.size(); i++) {
         CompoundTag entry = entries.m_128728_(i);
         int slot = entry.m_128451_("Slot");
         if (slot >= 0 && slot < size) {
            result.set(slot, ItemStack.m_41712_(entry));
         }
      }

      return List.copyOf(result);
   }

   private static boolean sameStacks(List<ItemStack> left, List<ItemStack> right) {
      if (left.size() != right.size()) {
         return false;
      } else {
         for (int i = 0; i < left.size(); i++) {
            if (!ItemStack.m_41728_(left.get(i), right.get(i))) {
               return false;
            }
         }

         return true;
      }
   }
}
