package com.moakiee.ae2lt.logic.tianshu.terminal;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.item.OverloadPatternItem;
import com.moakiee.ae2lt.logic.AdvancedAECompat;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopMemberPattern;
import com.moakiee.ae2lt.logic.tianshu.loop.ClosedLoopPatternPayload;
import com.moakiee.ae2lt.overload.runtime.model.EncodedOverloadPattern;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternPayload;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PatternEncodingDuplicateFilter {
   private static final Logger LOG = LoggerFactory.getLogger("ae2lt/TianshuDuplicate");

   public static boolean containsEquivalentPattern(InternalInventory inventory, ItemStack candidate, @Nullable Level level) {
      return checkEquivalentPattern(inventory, candidate, level).duplicate();
   }

   public static PatternEncodingDuplicateFilter.CheckResult checkEquivalentPattern(InternalInventory inventory, ItemStack candidate, @Nullable Level level) {
      if (inventory != null && candidate != null && !candidate.m_41619_()) {
         ClosedLoopPatternPayload closedLoopPayload = readClosedLoopPayload(candidate, level);
         boolean closedLoopCandidate = closedLoopPayload != null;
         OverloadPatternPayload overloadPayload = closedLoopCandidate ? null : readOverloadPayload(candidate);
         IPatternDetails candidateDetails = !closedLoopCandidate && overloadPayload == null ? decode(candidate, level) : null;
         int occupiedSlots = 0;
         int undecodableSlots = 0;

         for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            if (!stored.m_41619_()) {
               occupiedSlots++;
               if (closedLoopCandidate) {
                  if (sameClosedLoopPayload(readClosedLoopPayload(stored, level), closedLoopPayload)) {
                     return new PatternEncodingDuplicateFilter.CheckResult(
                        true, slot, PatternEncodingDuplicateFilter.MatchMethod.CLOSED_LOOP_PAYLOAD, occupiedSlots, undecodableSlots
                     );
                  }
               } else {
                  if (ItemStack.m_150942_(stored, candidate)) {
                     return new PatternEncodingDuplicateFilter.CheckResult(
                        true, slot, PatternEncodingDuplicateFilter.MatchMethod.EXACT_STACK, occupiedSlots, undecodableSlots
                     );
                  }

                  if (overloadPayload != null) {
                     if (sameOverloadPayload(readOverloadPayload(stored), overloadPayload, level)) {
                        return new PatternEncodingDuplicateFilter.CheckResult(
                           true, slot, PatternEncodingDuplicateFilter.MatchMethod.OVERLOAD_PAYLOAD, occupiedSlots, undecodableSlots
                        );
                     }
                  } else {
                     Boolean nativeComparison = compareNativePatternSemantics(stored, candidate, level);
                     if (nativeComparison != null) {
                        if (nativeComparison) {
                           return new PatternEncodingDuplicateFilter.CheckResult(
                              true, slot, PatternEncodingDuplicateFilter.MatchMethod.DECODED_DEFINITION, occupiedSlots, undecodableSlots
                           );
                        }
                     } else {
                        IPatternDetails storedDetails = decode(stored, level);
                        if (storedDetails == null) {
                           undecodableSlots++;
                        }

                        if (sameDetails(storedDetails, candidateDetails, stored, candidate)) {
                           return new PatternEncodingDuplicateFilter.CheckResult(
                              true, slot, PatternEncodingDuplicateFilter.MatchMethod.DECODED_DEFINITION, occupiedSlots, undecodableSlots
                           );
                        }
                     }
                  }
               }
            }
         }

         if (!closedLoopCandidate && overloadPayload == null && candidateDetails == null) {
            LOG.warn("Candidate could not be decoded during duplicate check: {}", describeStack(candidate));
         }

         return new PatternEncodingDuplicateFilter.CheckResult(false, -1, PatternEncodingDuplicateFilter.MatchMethod.NONE, occupiedSlots, undecodableSlots);
      } else {
         return new PatternEncodingDuplicateFilter.CheckResult(false, -1, PatternEncodingDuplicateFilter.MatchMethod.NONE, 0, 0);
      }
   }

   @Nullable
   private static ClosedLoopPatternPayload readClosedLoopPayload(ItemStack stack, @Nullable Level level) {
      return level != null && stack != null && stack.m_41720_() instanceof ClosedLoopPatternItem item ? item.readPayload(stack, level).orElse(null) : null;
   }

   static boolean sameClosedLoopPayload(@Nullable ClosedLoopPatternPayload stored, @Nullable ClosedLoopPatternPayload candidate) {
      return stored != null && candidate != null
         ? sameMembers(stored.memberPatterns(), candidate.memberPatterns())
            && sameStacks(stored.seeds(), candidate.seeds())
            && sameStacks(stored.externalInputs(), candidate.externalInputs())
            && sameStacks(stored.netOutputs(), candidate.netOutputs())
            && stored.executionSeedMultiplier() == candidate.executionSeedMultiplier()
            && stored.storedTaskMultiplier() == candidate.storedTaskMultiplier()
         : false;
   }

   private static boolean sameMembers(List<ClosedLoopMemberPattern> stored, List<ClosedLoopMemberPattern> candidate) {
      if (stored.size() != candidate.size()) {
         return false;
      } else {
         for (int i = 0; i < stored.size(); i++) {
            ClosedLoopMemberPattern left = stored.get(i);
            ClosedLoopMemberPattern right = candidate.get(i);
            if (left.copiesPerCycle() != right.copiesPerCycle() || !left.pattern().fingerprint().equals(right.pattern().fingerprint())) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean sameStacks(List<GenericStack> stored, List<GenericStack> candidate) {
      if (stored.size() != candidate.size()) {
         return false;
      } else {
         for (int i = 0; i < stored.size(); i++) {
            GenericStack left = stored.get(i);
            GenericStack right = candidate.get(i);
            if (left.amount() != right.amount() || !Objects.equals(left.what(), right.what())) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean sameDetails(@Nullable IPatternDetails stored, @Nullable IPatternDetails candidate, ItemStack storedStack, ItemStack candidateStack) {
      if (stored != null && candidate != null) {
         try {
            return AdvancedAECompat.samePatternSemantics(stored, candidate);
         } catch (RuntimeException var5) {
            LOG.warn(
               "Decoded pattern comparison failed (stored={}, candidate={}, storedType={}, candidateType={})",
               new Object[]{describeStack(storedStack), describeStack(candidateStack), stored.getClass().getName(), candidate.getClass().getName(), var5}
            );
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   private static Boolean compareNativePatternSemantics(ItemStack stored, ItemStack candidate, @Nullable Level level) {
      if (AEItems.CRAFTING_PATTERN.isSameAs(candidate)) {
         return !AEItems.CRAFTING_PATTERN.isSameAs(stored) ? false : sameNativePatternDefinition(stored, candidate, level);
      } else if (AEItems.PROCESSING_PATTERN.isSameAs(candidate)) {
         return !AEItems.PROCESSING_PATTERN.isSameAs(stored) ? false : sameNativePatternDefinition(stored, candidate, level);
      } else if (AEItems.SMITHING_TABLE_PATTERN.isSameAs(candidate)) {
         return !AEItems.SMITHING_TABLE_PATTERN.isSameAs(stored) ? false : sameNativePatternDefinition(stored, candidate, level);
      } else if (AEItems.STONECUTTING_PATTERN.isSameAs(candidate)) {
         return !AEItems.STONECUTTING_PATTERN.isSameAs(stored) ? false : sameNativePatternDefinition(stored, candidate, level);
      } else {
         return null;
      }
   }

   private static boolean sameNativePatternDefinition(ItemStack stored, ItemStack candidate, @Nullable Level level) {
      IPatternDetails storedDetails = decode(stored, level);
      IPatternDetails candidateDetails = decode(candidate, level);
      if (storedDetails != null && candidateDetails != null) {
         try {
            return Objects.equals(storedDetails.getDefinition(), candidateDetails.getDefinition());
         } catch (RuntimeException var6) {
            LOG.warn("Native pattern comparison failed (stored={}, candidate={})", new Object[]{describeStack(stored), describeStack(candidate), var6});
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   private static OverloadPatternPayload readOverloadPayload(ItemStack stack) {
      if (stack != null && stack.m_41720_() instanceof OverloadPatternItem item) {
         try {
            return item.readPayload(stack).orElse(null);
         } catch (RuntimeException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean sameOverloadPayload(@Nullable OverloadPatternPayload stored, @Nullable OverloadPatternPayload candidate, @Nullable Level level) {
      if (stored != null
         && candidate != null
         && level != null
         && stored.requiredHostKind() == candidate.requiredHostKind()
         && sameOverloadConfiguration(stored.encodedPattern(), candidate.encodedPattern())) {
         try {
            ItemStack storedSource = stored.sourcePattern().toItemStack();
            ItemStack candidateSource = candidate.sourcePattern().toItemStack();
            if (ItemStack.m_150942_(storedSource, candidateSource)) {
               return true;
            } else {
               Boolean nativeComparison = compareNativePatternSemantics(storedSource, candidateSource, level);
               return nativeComparison != null
                  ? nativeComparison
                  : AdvancedAECompat.samePatternSemantics(decode(storedSource, level), decode(candidateSource, level));
            }
         } catch (RuntimeException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean sameOverloadConfiguration(EncodedOverloadPattern stored, EncodedOverloadPattern candidate) {
      return List.copyOf(stored.inputSlots()).equals(List.copyOf(candidate.inputSlots()))
         && List.copyOf(stored.outputSlots()).equals(List.copyOf(candidate.outputSlots()));
   }

   @Nullable
   private static IPatternDetails decode(ItemStack stack, @Nullable Level level) {
      if (level == null) {
         return null;
      } else {
         try {
            return PatternDetailsHelper.decodePattern(stack, level);
         } catch (RuntimeException var3) {
            LOG.warn("Pattern decode failed during duplicate check: {}", describeStack(stack), var3);
            return null;
         }
      }
   }

   public static String describeStack(ItemStack stack) {
      if (stack == null) {
         return "null";
      } else if (stack.m_41619_()) {
         return "empty";
      } else {
         int fingerprint = Objects.hash(stack.m_41720_(), stack.m_41783_());
         return BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_()) + "#" + Integer.toUnsignedString(fingerprint, 16);
      }
   }

   public static String describeOccupiedStacks(InternalInventory inventory, int limit) {
      if (inventory == null) {
         return "null-inventory";
      } else {
         StringBuilder result = new StringBuilder();
         int described = 0;

         for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.m_41619_()) {
               if (described >= Math.max(1, limit)) {
                  result.append(", ...");
                  break;
               }

               if (described > 0) {
                  result.append(", ");
               }

               result.append(slot).append('=').append(describeStack(stack));
               described++;
            }
         }

         return described == 0 ? "empty" : result.toString();
      }
   }

   private PatternEncodingDuplicateFilter() {
   }

   public static record CheckResult(
      boolean duplicate, int matchedSlot, PatternEncodingDuplicateFilter.MatchMethod matchMethod, int occupiedSlots, int undecodableSlots
   ) {
   }

   public static enum MatchMethod {
      NONE,
      EXACT_STACK,
      DECODED_DEFINITION,
      OVERLOAD_PAYLOAD,
      CLOSED_LOOP_PAYLOAD;
   }
}
