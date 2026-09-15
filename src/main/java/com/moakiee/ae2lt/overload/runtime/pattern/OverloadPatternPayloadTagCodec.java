package com.moakiee.ae2lt.overload.runtime.pattern;

import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.model.OverloadPatternSlot;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class OverloadPatternPayloadTagCodec {
   private static final String TAG_HOST_KIND = "HostKind";
   private static final String TAG_SOURCE_PATTERN = "SourcePattern";
   private static final String TAG_RULES = "Rules";
   private static final String TAG_INPUTS = "Inputs";
   private static final String TAG_OUTPUTS = "Outputs";
   private static final String TAG_SLOT = "Slot";
   private static final String TAG_MODE = "Mode";

   private OverloadPatternPayloadTagCodec() {
   }

   public static CompoundTag writePayload(OverloadPatternPayload payload) {
      Objects.requireNonNull(payload, "payload");
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("HostKind", payload.requiredHostKind().name());
      tag.m_128365_("SourcePattern", payload.sourcePattern().toTag());
      tag.m_128365_("Rules", writeEncodedPattern(payload.encodedPattern()));
      return tag;
   }

   public static OverloadPatternPayload readPayload(CompoundTag tag) {
      Objects.requireNonNull(tag, "tag");
      PatternExecutionHostKind hostKind = PatternExecutionHostKind.valueOf(tag.m_128461_("HostKind"));
      SourcePatternSnapshot sourcePattern = SourcePatternSnapshot.fromTag(tag.m_128469_("SourcePattern"));
      EncodedOverloadPattern encodedPattern = readEncodedPattern(tag.m_128469_("Rules"));
      return new OverloadPatternPayload(hostKind, sourcePattern, encodedPattern);
   }

   public static CompoundTag writeEncodedPattern(EncodedOverloadPattern encodedPattern) {
      Objects.requireNonNull(encodedPattern, "encodedPattern");
      CompoundTag tag = new CompoundTag();
      tag.m_128365_("Inputs", writeSlots(encodedPattern.inputSlots()));
      tag.m_128365_("Outputs", writeSlots(encodedPattern.outputSlots()));
      return tag;
   }

   public static EncodedOverloadPattern readEncodedPattern(CompoundTag tag) {
      Objects.requireNonNull(tag, "tag");
      EncodedOverloadPattern.Builder builder = EncodedOverloadPattern.builder();
      if (tag.m_128425_("Inputs", 9)) {
         ListTag inputs = tag.m_128437_("Inputs", 10);

         for (int i = 0; i < inputs.size(); i++) {
            CompoundTag slotTag = inputs.m_128728_(i);
            builder.input(slotTag.m_128451_("Slot"), MatchMode.valueOf(slotTag.m_128461_("Mode")));
         }
      }

      if (tag.m_128425_("Outputs", 9)) {
         ListTag outputs = tag.m_128437_("Outputs", 10);

         for (int i = 0; i < outputs.size(); i++) {
            CompoundTag slotTag = outputs.m_128728_(i);
            builder.output(slotTag.m_128451_("Slot"), MatchMode.valueOf(slotTag.m_128461_("Mode")));
         }
      }

      return builder.build();
   }

   private static ListTag writeSlots(Iterable<OverloadPatternSlot> slots) {
      ListTag list = new ListTag();

      for (OverloadPatternSlot slot : slots) {
         CompoundTag slotTag = new CompoundTag();
         slotTag.m_128405_("Slot", slot.slotIndex());
         slotTag.m_128359_("Mode", slot.matchMode().name());
         list.add(slotTag);
      }

      return list;
   }
}
