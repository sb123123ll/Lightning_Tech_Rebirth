package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class ClosedLoopPatternPayloadTagCodec {
   private static final String TAG_MEMBERS = "Members";
   private static final String TAG_MEMBER_PATTERN = "Pattern";
   private static final String TAG_MEMBER_COPIES = "Copies";
   private static final String TAG_MEMBER_SEED_WAVE_COPIES = "SeedWaveCopies";
   private static final String TAG_SEEDS = "Seeds";
   private static final String TAG_INPUTS = "Inputs";
   private static final String TAG_OUTPUTS = "Outputs";
   private static final String TAG_SEED_MULTIPLIER = "SeedMultiplier";
   private static final String TAG_EXECUTION_SEED_MULTIPLIER = "ExecutionSeedMultiplier";
   private static final String TAG_STORED_TASK_MULTIPLIER = "StoredTaskMultiplier";
   private static final String TAG_ENABLED = "Enabled";

   public static CompoundTag write(ClosedLoopPatternPayload payload) {
      CompoundTag tag = new CompoundTag();
      ListTag members = new ListTag();

      for (ClosedLoopMemberPattern member : payload.memberPatterns()) {
         members.add(writeMember(member));
      }

      tag.m_128365_("Members", members);
      tag.m_128365_("Seeds", writeStacks(payload.seeds()));
      tag.m_128365_("Inputs", writeStacks(payload.externalInputs()));
      tag.m_128365_("Outputs", writeStacks(payload.netOutputs()));
      tag.m_128405_("ExecutionSeedMultiplier", payload.executionSeedMultiplier());
      tag.m_128405_("StoredTaskMultiplier", payload.storedTaskMultiplier());
      tag.m_128405_("SeedMultiplier", payload.executionSeedMultiplier());
      tag.m_128379_("Enabled", payload.enabled());
      return tag;
   }

   static CompoundTag writeMember(ClosedLoopMemberPattern member) {
      CompoundTag memberTag = new CompoundTag();
      memberTag.m_128365_("Pattern", member.pattern().toTag());
      memberTag.m_128356_("Copies", member.copiesPerCycle());
      return memberTag;
   }

   public static ClosedLoopPatternPayload read(CompoundTag tag) {
      ArrayList<ClosedLoopMemberPattern> members = new ArrayList<>();
      ListTag memberTags = tag.m_128437_("Members", 10);
      if (memberTags.size() > 27) {
         throw new IllegalArgumentException("closed-loop payload has too many members");
      } else {
         for (int i = 0; i < memberTags.size(); i++) {
            members.add(readMember(memberTags.m_128728_(i)));
         }

         ClosedLoopPatternPayloadTagCodec.SeedMultipliers seedMultipliers = readSeedMultipliers(tag);
         return new ClosedLoopPatternPayload(
            members,
            readStacks(tag.m_128437_("Seeds", 10)),
            readStacks(tag.m_128437_("Inputs", 10)),
            readStacks(tag.m_128437_("Outputs", 10)),
            seedMultipliers.executionSeedMultiplier(),
            seedMultipliers.storedTaskMultiplier(),
            !tag.m_128425_("Enabled", 1) || tag.m_128471_("Enabled")
         );
      }
   }

   static ClosedLoopMemberPattern readMember(CompoundTag memberTag) {
      CompoundTag patternTag = memberTag.m_128425_("Pattern", 10) ? memberTag.m_128469_("Pattern") : memberTag;
      long copies = Math.max(1L, memberTag.m_128454_("Copies"));
      if (memberTag.m_128425_("SeedWaveCopies", 99) && memberTag.m_128454_("SeedWaveCopies") != copies) {
         throw new IllegalArgumentException("legacy seed-wave copies cannot encode execution repetition");
      } else {
         return new ClosedLoopMemberPattern(SourcePatternSnapshot.fromTag(patternTag), copies);
      }
   }

   static ClosedLoopPatternPayloadTagCodec.SeedMultipliers readSeedMultipliers(CompoundTag tag) {
      int legacySeedMultiplier = positiveIntOrOne(tag, "SeedMultiplier");
      int executionSeedMultiplier = tag.m_128425_("ExecutionSeedMultiplier", 99) ? positiveIntOrOne(tag, "ExecutionSeedMultiplier") : legacySeedMultiplier;
      return new ClosedLoopPatternPayloadTagCodec.SeedMultipliers(executionSeedMultiplier, positiveIntOrOne(tag, "StoredTaskMultiplier"));
   }

   private static int positiveIntOrOne(CompoundTag tag, String key) {
      return tag.m_128425_(key, 99) ? Math.max(1, tag.m_128451_(key)) : 1;
   }

   private static ListTag writeStacks(Iterable<GenericStack> stacks) {
      ListTag list = new ListTag();

      for (GenericStack stack : stacks) {
         list.add(GenericStack.writeTag(stack));
      }

      return list;
   }

   private static List<GenericStack> readStacks(ListTag tags) {
      ArrayList<GenericStack> stacks = new ArrayList<>(tags.size());

      for (int i = 0; i < tags.size(); i++) {
         GenericStack stack = GenericStack.readTag(tags.m_128728_(i));
         if (stack == null) {
            throw new IllegalArgumentException("invalid generic stack in closed-loop payload");
         }

         stacks.add(stack);
      }

      return stacks;
   }

   private ClosedLoopPatternPayloadTagCodec() {
   }

   static record SeedMultipliers(int executionSeedMultiplier, int storedTaskMultiplier) {
   }
}
