package com.moakiee.ae2lt.overload.pattern;

import com.moakiee.ae2lt.item.OverloadPatternItem;
import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.pattern.EditableOverloadPatternState;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternEditState;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternPayload;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternDefinition;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternInput;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternOutput;
import com.moakiee.ae2lt.overload.runtime.pattern.PatternExecutionHostKind;
import com.moakiee.ae2lt.overload.runtime.pattern.PlainPatternResolver;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class PatternConversionService {
   public OverloadPatternEditState createDefaultEditState(ParsedPatternDefinition parsedPattern) {
      return OverloadPatternEditState.fromPattern(parsedPattern, this.createDefaultEncoding(parsedPattern), false);
   }

   public OverloadPatternEditState createEditState(
      ParsedPatternDefinition parsedPattern, EncodedOverloadPattern encodedPattern, boolean sourceWasOverloadPattern
   ) {
      return OverloadPatternEditState.fromPattern(parsedPattern, encodedPattern, sourceWasOverloadPattern);
   }

   public EncodedOverloadPattern createDefaultEncoding(ParsedPatternDefinition parsedPattern) {
      Objects.requireNonNull(parsedPattern, "parsedPattern");
      EncodedOverloadPattern.Builder builder = EncodedOverloadPattern.builder();

      for (ParsedPatternInput input : parsedPattern.inputs()) {
         builder.input(input.slotIndex(), MatchMode.STRICT);
      }

      for (ParsedPatternOutput output : parsedPattern.outputs()) {
         builder.output(output.slotIndex(), MatchMode.STRICT);
      }

      return builder.build();
   }

   public OverloadPatternPayload createPayload(ParsedPatternDefinition parsedPattern) {
      return this.createPayload(parsedPattern, this.createDefaultEncoding(parsedPattern));
   }

   public OverloadPatternPayload createPayload(ParsedPatternDefinition parsedPattern, EncodedOverloadPattern encodedPattern) {
      Objects.requireNonNull(parsedPattern, "parsedPattern");
      Objects.requireNonNull(encodedPattern, "encodedPattern");
      return new OverloadPatternPayload(PatternExecutionHostKind.OVERLOADED_PATTERN_PROVIDER, parsedPattern.sourcePattern(), encodedPattern);
   }

   public ItemStack createOverloadPatternStack(
      OverloadPatternItem overloadPatternItem, ParsedPatternDefinition parsedPattern, EncodedOverloadPattern encodedPattern
   ) {
      Objects.requireNonNull(overloadPatternItem, "overloadPatternItem");
      OverloadPatternPayload payload = this.createPayload(parsedPattern, encodedPattern);
      return overloadPatternItem.createStack(payload);
   }

   public ItemStack createOverloadPatternStack(
      OverloadPatternItem overloadPatternItem, ParsedPatternDefinition parsedPattern, OverloadPatternEditState editState
   ) {
      Objects.requireNonNull(editState, "editState");
      return this.createOverloadPatternStack(overloadPatternItem, parsedPattern, editState.toEncodedPattern());
   }

   public Optional<EditableOverloadPatternState> restoreEditableState(
      OverloadPatternItem overloadPatternItem, ItemStack overloadPatternStack, PlainPatternResolver plainPatternResolver
   ) {
      Objects.requireNonNull(overloadPatternItem, "overloadPatternItem");
      Objects.requireNonNull(overloadPatternStack, "overloadPatternStack");
      Objects.requireNonNull(plainPatternResolver, "plainPatternResolver");
      return overloadPatternItem.readPayload(overloadPatternStack).map(payload -> {
         ItemStack sourcePatternStack = payload.sourcePattern().toItemStack();
         ParsedPatternDefinition parsedPattern = plainPatternResolver.resolve(sourcePatternStack);
         return new EditableOverloadPatternState(parsedPattern, payload.encodedPattern());
      });
   }

   public Optional<EditableOverloadPatternState> resolveEditableSource(ItemStack sourcePatternStack, PlainPatternResolver plainPatternResolver) {
      Objects.requireNonNull(sourcePatternStack, "sourcePatternStack");
      Objects.requireNonNull(plainPatternResolver, "plainPatternResolver");
      if (sourcePatternStack.m_41619_()) {
         return Optional.empty();
      } else if (sourcePatternStack.m_41720_() instanceof OverloadPatternItem overloadPatternItem) {
         return this.restoreEditableState(overloadPatternItem, sourcePatternStack, plainPatternResolver);
      } else {
         ParsedPatternDefinition parsedPattern = plainPatternResolver.resolve(sourcePatternStack);
         return Optional.of(new EditableOverloadPatternState(parsedPattern, this.createDefaultEncoding(parsedPattern)));
      }
   }

   public OverloadPatternDetails createRuntimeDetails(ParsedPatternDefinition parsedPattern, EncodedOverloadPattern encodedPattern) {
      return new OverloadPatternDetails(parsedPattern, encodedPattern);
   }
}
