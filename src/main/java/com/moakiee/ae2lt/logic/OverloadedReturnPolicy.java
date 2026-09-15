package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class OverloadedReturnPolicy {
   private static final String TAG_UNLOCK_MATCH_MODE = "Ae2ltUnlockMatchMode";
   private static final String TAG_UNLOCK_TEMPLATE = "Ae2ltUnlockTemplate";
   @Nullable
   private AllowedOutputFilter outputFilter;
   private boolean outputFilterDirty = true;
   @Nullable
   private MatchMode unlockMatchMode;
   @Nullable
   private ItemStack unlockTemplate;

   AllowedOutputFilter outputFilter(List<IPatternDetails> patterns) {
      if (!this.outputFilterDirty && this.outputFilter != null) {
         return this.outputFilter;
      } else {
         this.outputFilter = collectOutputFilter(patterns);
         this.outputFilterDirty = false;
         return this.outputFilter;
      }
   }

   void patternsChanged() {
      this.outputFilter = null;
      this.outputFilterDirty = true;
   }

   void synchronizeUnlockRule(IPatternDetails pattern, boolean lockUntilResult) {
      this.clearUnlockRule();
      if (lockUntilResult && pattern instanceof OverloadedProviderOnlyPatternDetails overload) {
         OverloadPatternDetails details = overload.overloadPatternDetailsView();
         int outputIndex = resolveUnlockOutputIndex(pattern, details);
         if (outputIndex >= 0 && outputIndex < details.outputs().size()) {
            OverloadPatternDetails.OutputSlot output = details.outputs().get(outputIndex);
            this.unlockMatchMode = output.matchMode();
            this.unlockTemplate = output.template();
         }
      }
   }

   boolean matchesUnlock(GenericStack unlockStack, GenericStack returnedStack) {
      if (this.unlockMatchMode != MatchMode.ID_ONLY) {
         return unlockStack.what().equals(returnedStack.what());
      } else {
         Item expectedItem = null;
         if (this.unlockTemplate != null && !this.unlockTemplate.m_41619_()) {
            expectedItem = this.unlockTemplate.m_41720_();
         } else if (unlockStack.what() instanceof AEItemKey unlockItemKey) {
            expectedItem = unlockItemKey.getItem();
         }

         return expectedItem != null && returnedStack.what() instanceof AEItemKey returnedItemKey && returnedItemKey.getItem() == expectedItem;
      }
   }

   void clearUnlockRule() {
      this.unlockMatchMode = null;
      this.unlockTemplate = null;
   }

   void writeToNBT(CompoundTag tag) {
      if (this.unlockMatchMode != null) {
         tag.m_128359_("Ae2ltUnlockMatchMode", this.unlockMatchMode.name());
      }

      if (this.unlockTemplate != null && !this.unlockTemplate.m_41619_()) {
         tag.m_128365_("Ae2ltUnlockTemplate", this.unlockTemplate.m_41739_(new CompoundTag()));
      }
   }

   void readFromNBT(CompoundTag tag) {
      this.clearUnlockRule();
      if (tag.m_128425_("Ae2ltUnlockMatchMode", 8)) {
         try {
            this.unlockMatchMode = MatchMode.valueOf(tag.m_128461_("Ae2ltUnlockMatchMode"));
         } catch (IllegalArgumentException var3) {
            this.unlockMatchMode = null;
         }
      }

      if (tag.m_128425_("Ae2ltUnlockTemplate", 10)) {
         this.unlockTemplate = ItemStack.m_41712_(tag.m_128469_("Ae2ltUnlockTemplate"));
         if (this.unlockTemplate.m_41619_()) {
            this.unlockTemplate = null;
         }
      }

      this.patternsChanged();
   }

   private static AllowedOutputFilter collectOutputFilter(List<IPatternDetails> patterns) {
      AllowedOutputFilter filter = new AllowedOutputFilter();

      for (IPatternDetails pattern : patterns) {
         if (pattern instanceof OverloadedProviderOnlyPatternDetails) {
            OverloadedProviderOnlyPatternDetails overload = (OverloadedProviderOnlyPatternDetails)pattern;
            GenericStack[] ae2Outputs = pattern.getOutputs();
            List<OverloadPatternDetails.OutputSlot> overloadOutputs = overload.overloadPatternDetailsView().outputs();
            int count = Math.min(ae2Outputs.length, overloadOutputs.size());

            for (int i = 0; i < count; i++) {
               AEKey key = ae2Outputs[i].what();
               if (overloadOutputs.get(i).matchMode() == MatchMode.ID_ONLY) {
                  filter.allowIdOnly(key);
               } else {
                  filter.allowStrict(key);
               }
            }
         } else {
            for (GenericStack output : pattern.getOutputs()) {
               filter.allowStrict(output.what());
            }
         }
      }

      return filter;
   }

   private static int resolveUnlockOutputIndex(IPatternDetails pattern, OverloadPatternDetails overloadDetails) {
      GenericStack[] actualOutputs = pattern.getOutputs();
      List<OverloadPatternDetails.OutputSlot> overloadOutputs = overloadDetails.outputs();
      int count = Math.min(actualOutputs.length, overloadOutputs.size());
      if (count <= 0) {
         return -1;
      } else {
         GenericStack primaryOutput = pattern.getPrimaryOutput();

         for (int i = 0; i < count; i++) {
            GenericStack candidate = actualOutputs[i];
            if (candidate.what().equals(primaryOutput.what()) && candidate.amount() == primaryOutput.amount()) {
               return i;
            }
         }

         for (int ix = 0; ix < count; ix++) {
            if (overloadOutputs.get(ix).primaryOutput()) {
               return ix;
            }
         }

         return 0;
      }
   }
}
