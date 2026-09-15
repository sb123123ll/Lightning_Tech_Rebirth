package com.moakiee.ae2lt.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternDecoder;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternPayload;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternPayloadTagCodec;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public final class ClosedLoopPatternItem extends EncodedPatternItem {
   private static final String TAG_PAYLOAD = "ClosedLoopPattern";
   private static final String TAG_EXECUTION_MEMBER = "ExecutionMember";

   public ClosedLoopPatternItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public boolean hasPayload(ItemStack stack) {
      return ItemStackTagSupport.getTagCopy(stack).m_128425_("ClosedLoopPattern", 10);
   }

   public Optional<ClosedLoopPatternPayload> readPayload(ItemStack stack, Level level) {
      if (stack != null && level != null && stack.m_41720_() == this) {
         CompoundTag root = ItemStackTagSupport.getTagCopy(stack);
         if (!root.m_128425_("ClosedLoopPattern", 10)) {
            return Optional.empty();
         } else {
            try {
               return Optional.of(ClosedLoopPatternPayloadTagCodec.read(root.m_128469_("ClosedLoopPattern")));
            } catch (RuntimeException var5) {
               return Optional.empty();
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public int readExecutionMember(ItemStack stack) {
      if (stack != null && stack.m_41720_() == this) {
         CompoundTag root = ItemStackTagSupport.getTagCopy(stack);
         return root.m_128425_("ExecutionMember", 3) ? root.m_128451_("ExecutionMember") : -1;
      } else {
         return -1;
      }
   }

   public void writePayload(ItemStack stack, ClosedLoopPatternPayload payload, Provider registries) {
      if (stack != null && stack.m_41720_() == this) {
         ItemStackTagSupport.updateTag(stack, root -> root.m_128365_("ClosedLoopPattern", ClosedLoopPatternPayloadTagCodec.write(payload)));
      } else {
         throw new IllegalArgumentException("payload target must be a closed-loop pattern item");
      }
   }

   public ItemStack createStack(ClosedLoopPatternPayload payload, Provider registries) {
      ItemStack stack = new ItemStack(this);
      this.writePayload(stack, payload, registries);
      return stack;
   }

   public ItemStack createExecutionMemberStack(ClosedLoopPatternPayload payload, int memberIndex, Provider registries) {
      if (memberIndex >= 0 && memberIndex < payload.memberPatterns().size()) {
         ItemStack stack = this.createStack(payload, registries);
         ItemStackTagSupport.updateTag(stack, root -> root.m_128405_("ExecutionMember", memberIndex));
         return stack;
      } else {
         throw new IllegalArgumentException("closed-loop execution member index is out of bounds");
      }
   }

   public IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
      return stack.m_41720_() == this ? ClosedLoopPatternDecoder.INSTANCE.decodePattern(AEItemKey.of(stack), level) : null;
   }

   public IPatternDetails decode(AEItemKey what, Level level) {
      return what != null && what.getItem() == this ? ClosedLoopPatternDecoder.INSTANCE.decodePattern(what, level) : null;
   }
}
