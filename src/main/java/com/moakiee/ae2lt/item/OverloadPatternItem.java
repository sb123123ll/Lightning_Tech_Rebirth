package com.moakiee.ae2lt.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.moakiee.ae2lt.api.patternprovider.EncodedPatternPayloadValidator;
import com.moakiee.ae2lt.overload.pattern.OverloadPatternDecoder;
import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternPayload;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternPayloadTagCodec;
import com.moakiee.ae2lt.overload.runtime.pattern.PatternExecutionHostKind;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import com.moakiee.ae2lt.util.ItemStackTagSupport;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class OverloadPatternItem extends EncodedPatternItem implements EncodedPatternPayloadValidator {
   private static final String TAG_OVERLOAD_PATTERN = "OverloadPattern";

   public OverloadPatternItem(Properties properties) {
      super(properties.m_41487_(1));
   }

   public boolean hasPayload(ItemStack stack) {
      return readRootTag(stack).m_128425_("OverloadPattern", 10);
   }

   @Override
   public boolean hasEncodedPatternPayload(ItemStack stack) {
      return this.hasPayload(stack);
   }

   public Optional<OverloadPatternPayload> readPayload(ItemStack stack) {
      Objects.requireNonNull(stack, "stack");
      CompoundTag rootTag = readRootTag(stack);
      if (!rootTag.m_128425_("OverloadPattern", 10)) {
         return Optional.empty();
      } else {
         CompoundTag payloadTag = rootTag.m_128469_("OverloadPattern");
         return Optional.of(OverloadPatternPayloadTagCodec.readPayload(payloadTag));
      }
   }

   public Optional<EncodedOverloadPattern> readEncodedPattern(ItemStack stack) {
      return this.readPayload(stack).map(OverloadPatternPayload::encodedPattern);
   }

   public Optional<SourcePatternSnapshot> readSourcePattern(ItemStack stack) {
      return this.readPayload(stack).map(OverloadPatternPayload::sourcePattern);
   }

   public PatternExecutionHostKind requiredHostKind(ItemStack stack) {
      return this.readPayload(stack).map(OverloadPatternPayload::requiredHostKind).orElse(PatternExecutionHostKind.OVERLOADED_PATTERN_PROVIDER);
   }

   public void writePayload(ItemStack stack, OverloadPatternPayload payload) {
      Objects.requireNonNull(stack, "stack");
      Objects.requireNonNull(payload, "payload");
      ItemStackTagSupport.updateTag(stack, rootTag -> rootTag.m_128365_("OverloadPattern", OverloadPatternPayloadTagCodec.writePayload(payload)));
   }

   public void writeEncodedPattern(ItemStack stack, EncodedOverloadPattern encodedPattern) {
      Objects.requireNonNull(encodedPattern, "encodedPattern");
      OverloadPatternPayload payload = this.readPayload(stack)
         .orElseThrow(() -> new IllegalStateException("cannot update encoded overload pattern without existing payload"));
      this.writePayload(stack, new OverloadPatternPayload(payload.requiredHostKind(), payload.sourcePattern(), encodedPattern));
   }

   public ItemStack createStack(OverloadPatternPayload payload) {
      ItemStack stack = new ItemStack(this);
      this.writePayload(stack, payload);
      return stack;
   }

   public IPatternDetails decode(ItemStack stack, Level level) {
      return stack.m_41720_() == this ? OverloadPatternDecoder.INSTANCE.decodePattern(AEItemKey.of(stack), level) : null;
   }

   public IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
      return this.decode(stack, level);
   }

   public IPatternDetails decode(AEItemKey what, Level level) {
      return what != null && what.getItem() == this ? this.decode(what.toStack(), level) : null;
   }

   public void m_7373_(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
      super.m_7373_(stack, level, lines, advancedTooltips);
      if (this.hasPayload(stack) && this.requiredHostKind(stack) == PatternExecutionHostKind.OVERLOADED_PATTERN_PROVIDER) {
         lines.add(Component.m_237115_("tooltip.ae2lt.overload_pattern.provider_only"));
      }
   }

   private static CompoundTag readRootTag(ItemStack stack) {
      return ItemStackTagSupport.getTagCopy(stack);
   }
}
