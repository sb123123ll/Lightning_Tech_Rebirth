package com.moakiee.ae2lt.overload.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import com.moakiee.ae2lt.item.OverloadPatternItem;
import com.moakiee.ae2lt.overload.runtime.pattern.Ae2OverloadPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternPayload;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternSupport;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.ParsedPatternDefinition;
import com.moakiee.ae2lt.overload.runtime.pattern.PatternExecutionHostKind;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class OverloadPatternDecoder implements IPatternDetailsDecoder {
   public static final OverloadPatternDecoder INSTANCE = new OverloadPatternDecoder();

   private OverloadPatternDecoder() {
   }

   public boolean isEncodedPattern(ItemStack stack) {
      if (stack.m_41720_() instanceof OverloadPatternItem overloadPatternItem && overloadPatternItem.hasPayload(stack)) {
         return true;
      }

      return false;
   }

   @Nullable
   public IPatternDetails decodePattern(ItemStack stack, Level level, boolean tryRecovery) {
      return !stack.m_41619_() && stack.m_41720_() instanceof OverloadPatternItem ? this.decodePattern(AEItemKey.of(stack), level) : null;
   }

   @Nullable
   public IPatternDetails decodePattern(AEItemKey what, Level level) {
      if (what != null && what.getItem() instanceof OverloadPatternItem overloadPatternItem) {
         OverloadPatternPayload payload = overloadPatternItem.readPayload(what.toStack()).orElse(null);
         if (payload != null && payload.requiredHostKind() == PatternExecutionHostKind.OVERLOADED_PATTERN_PROVIDER) {
            ItemStack sourceStack = payload.sourcePattern().toItemStack();
            IPatternDetails sourceDetails = PatternDetailsHelper.decodePattern(sourceStack, level);
            if (sourceDetails != null && !(sourceDetails instanceof OverloadedProviderOnlyPatternDetails)) {
               ParsedPatternDefinition parsed = OverloadPatternSupport.toParsedDefinition(sourceStack, sourceDetails);
               OverloadPatternDetails overloadDetails = new OverloadPatternDetails(parsed, payload.encodedPattern());
               return new Ae2OverloadPatternDetails(what, overloadDetails, sourceDetails);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
