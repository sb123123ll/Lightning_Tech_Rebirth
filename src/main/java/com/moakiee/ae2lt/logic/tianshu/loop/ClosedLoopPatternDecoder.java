package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.thunderbolt.core.crafting.loop.ReusableSeedPattern;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ClosedLoopPatternDecoder implements IPatternDetailsDecoder {
   public static final ClosedLoopPatternDecoder INSTANCE = new ClosedLoopPatternDecoder();

   private ClosedLoopPatternDecoder() {
   }

   public boolean isEncodedPattern(ItemStack stack) {
      if (stack.m_41720_() instanceof ClosedLoopPatternItem item && item.hasPayload(stack)) {
         return true;
      }

      return false;
   }

   @Nullable
   public IPatternDetails decodePattern(AEItemKey what, Level level) {
      if (what != null && what.getItem() instanceof ClosedLoopPatternItem item) {
         ItemStack var20 = what.toStack();
         ClosedLoopPatternPayload payload = item.readPayload(var20, level).orElse(null);
         if (payload == null) {
            return null;
         } else {
            try {
               int executionMember = item.readExecutionMember(var20);
               if (executionMember < 0) {
                  if (!payload.enabled()) {
                     return null;
                  } else {
                     ClosedLoopPatternDecoder.DecodedPayload decoded = decodePayload(payload, level);
                     return decoded.valid() ? decoded.createDetails(what, level, null, ignoredx -> Map.of()) : null;
                  }
               } else if (executionMember >= payload.memberPatterns().size()) {
                  return null;
               } else {
                  ClosedLoopPatternDecoder.MemberDecoding memberDecoding = decodeMembers(payload, level);
                  if (!memberDecoding.valid()) {
                     return null;
                  } else {
                     List<IPatternDetails> decodedMembers = memberDecoding.members();
                     IPatternDetails delegate = decodedMembers.get(executionMember);
                     LinkedHashMap<AEKey, Long> seedAmounts = new LinkedHashMap<>();

                     for (GenericStack seed : payload.seeds()) {
                        seedAmounts.merge(seed.what(), Long.valueOf(seed.amount()), Sat::add);
                     }

                     Set<AEKey> cycleKeys = ClosedLoopCycleKeys.analyze(decodedMembers, seedAmounts.keySet());
                     ArrayList<ClosedLoopPatternAnalyzer.Member> analyzedMembers = new ArrayList<>(decodedMembers.size());

                     for (int i = 0; i < decodedMembers.size(); i++) {
                        analyzedMembers.add(new ClosedLoopPatternAnalyzer.Member(decodedMembers.get(i), payload.memberPatterns().get(i).copiesPerCycle()));
                     }

                     List<ClosedLoopPatternAnalyzer.MemberFlow> memberFlows = ClosedLoopPatternAnalyzer.deriveMemberFlows(analyzedMembers, payload.seeds());
                     if (memberFlows.size() != decodedMembers.size()) {
                        return null;
                     } else {
                        Ae2ClosedLoopPatternDetails.validateFuzzyOutputSeedConsumers(analyzedMembers, memberFlows);
                        LinkedHashMap<AEKey, Set<AEKey>> acceptedVariants = new LinkedHashMap<>();
                        LinkedHashSet<AEKey> fuzzySeeds = new LinkedHashSet<>();
                        Ae2ClosedLoopPatternDetails.collectAcceptedSeedVariants(decodedMembers, memberFlows, seedAmounts.keySet(), acceptedVariants, fuzzySeeds);
                        AEItemKey rootDefinition = AEItemKey.of(item.createStack(payload, level.m_9598_()));
                        if (rootDefinition == null) {
                           return null;
                        } else {
                           boolean singleSeedInputPerMember = Ae2ClosedLoopPatternDetails.isSharedSeedPoolSafe(
                              ClosedLoopPatternAnalyzer.hasSingleSeedInputPerMember(memberFlows), seedAmounts.keySet(), acceptedVariants, fuzzySeeds
                           );
                           ClosedLoopPatternAnalyzer.MemberFlow memberFlow = memberFlows.get(executionMember);
                           return ClosedLoopExpandedPatternDetails.wrap(
                              delegate,
                              Ae2ClosedLoopPatternDetails.memberSeedAmounts(seedAmounts, memberFlow.inputSeed().keySet()),
                              cycleKeys,
                              ClosedLoopPatternIdentity.runtimeGroupId(rootDefinition, level.m_9598_()),
                              singleSeedInputPerMember,
                              memberFlow.inputSeedBySlot(),
                              payload.memberPatterns().size() == 1,
                              what,
                              executionMember
                           );
                        }
                     }
                  }
               }
            } catch (RuntimeException var19) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   @Nullable
   public IPatternDetails decodePattern(ItemStack stack, Level level, boolean trySort) {
      if (stack != null && !stack.m_41619_()) {
         AEItemKey key = AEItemKey.of(stack);
         return key != null ? this.decodePattern(key, level) : null;
      } else {
         return null;
      }
   }

   public static ClosedLoopPatternDecoder.DecodedPayload decodePayload(ClosedLoopPatternPayload payload, Level level) {
      if (payload != null && level != null) {
         ClosedLoopPatternDecoder.MemberDecoding memberDecoding = decodeMembers(payload, level);
         return !memberDecoding.valid()
            ? ClosedLoopPatternDecoder.DecodedPayload.invalid(memberDecoding.failure())
            : new ClosedLoopPatternDecoder.DecodedPayload(
               payload, ClosedLoopPatternValidator.validateDecoded(payload, memberDecoding.members()), memberDecoding.members()
            );
      } else {
         return ClosedLoopPatternDecoder.DecodedPayload.invalid(ClosedLoopValidationResult.Status.MEMBER_UNDECODABLE);
      }
   }

   private static ClosedLoopPatternDecoder.MemberDecoding decodeMembers(ClosedLoopPatternPayload payload, Level level) {
      ArrayList<IPatternDetails> decodedMembers = new ArrayList<>(payload.memberPatterns().size());

      for (ClosedLoopMemberPattern stored : payload.memberPatterns()) {
         ItemStack memberStack;
         try {
            memberStack = stored.pattern().toItemStack();
         } catch (RuntimeException var9) {
            return ClosedLoopPatternDecoder.MemberDecoding.invalid(ClosedLoopValidationResult.Status.MEMBER_UNDECODABLE);
         }

         if (memberStack.m_41720_() instanceof ClosedLoopPatternItem) {
            return ClosedLoopPatternDecoder.MemberDecoding.invalid(ClosedLoopValidationResult.Status.MEMBER_IS_CLOSED_LOOP);
         }

         IPatternDetails decoded;
         try {
            decoded = PatternDetailsHelper.decodePattern(memberStack, level);
         } catch (RuntimeException var8) {
            return ClosedLoopPatternDecoder.MemberDecoding.invalid(ClosedLoopValidationResult.Status.MEMBER_UNDECODABLE);
         }

         if (decoded == null) {
            return ClosedLoopPatternDecoder.MemberDecoding.invalid(ClosedLoopValidationResult.Status.MEMBER_UNDECODABLE);
         }

         if (decoded instanceof TianshuClosedLoopPatternDetails) {
            return ClosedLoopPatternDecoder.MemberDecoding.invalid(ClosedLoopValidationResult.Status.MEMBER_IS_CLOSED_LOOP);
         }

         decodedMembers.add(decoded);
      }

      return new ClosedLoopPatternDecoder.MemberDecoding(List.copyOf(decodedMembers), null);
   }

   public static record DecodedPayload(@Nullable ClosedLoopPatternPayload payload, ClosedLoopValidationResult validation, List<IPatternDetails> members) {
      public DecodedPayload(@Nullable ClosedLoopPatternPayload payload, ClosedLoopValidationResult validation, List<IPatternDetails> members) {
         validation = Objects.requireNonNull(validation, "validation");
         members = List.copyOf(members);
         this.payload = payload;
         this.validation = validation;
         this.members = members;
      }

      public boolean valid() {
         return this.validation.valid();
      }

      public Ae2ClosedLoopPatternDetails createDetails(
         AEItemKey definition, Level level, UUID owningTianshuId, Function<ReusableSeedPattern, Map<AEKey, Long>> availableSeedSnapshotFactory
      ) {
         if (this.valid() && this.payload != null) {
            return new Ae2ClosedLoopPatternDetails(definition, this.payload, level, owningTianshuId, availableSeedSnapshotFactory, this.members);
         } else {
            throw new IllegalStateException("cannot create details from an invalid closed-loop payload");
         }
      }

      private static ClosedLoopPatternDecoder.DecodedPayload invalid(ClosedLoopValidationResult.Status status) {
         return new ClosedLoopPatternDecoder.DecodedPayload(null, ClosedLoopPatternValidator.invalid(status), List.of());
      }
   }

   private static record MemberDecoding(List<IPatternDetails> members, @Nullable ClosedLoopValidationResult.Status failure) {
      private boolean valid() {
         return this.failure == null;
      }

      private static ClosedLoopPatternDecoder.MemberDecoding invalid(ClosedLoopValidationResult.Status status) {
         return new ClosedLoopPatternDecoder.MemberDecoding(List.of(), status);
      }
   }
}
