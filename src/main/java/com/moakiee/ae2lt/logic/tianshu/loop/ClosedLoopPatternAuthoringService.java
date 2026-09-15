package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ClosedLoopPatternAuthoringService {
   private static final ResourceLocation CLOSED_LOOP_PATTERN_ITEM_ID = new ResourceLocation("ae2lt", "closed_loop_pattern");

   public static ClosedLoopPatternAuthoringService.Result create(
      List<ClosedLoopPatternAuthoringService.MarkedMember> markedMembers, AEKey mainOutput, int executionSeedMultiplier, int storedTaskMultiplier
   ) {
      if (markedMembers != null
         && !markedMembers.isEmpty()
         && markedMembers.size() <= 27
         && mainOutput != null
         && executionSeedMultiplier >= 1
         && storedTaskMultiplier >= 1) {
         ArrayList<IPatternDetails> details = new ArrayList<>(markedMembers.size());
         ArrayList<ClosedLoopPatternAnalyzer.Member> analyzed = new ArrayList<>(markedMembers.size());
         IdentityHashMap<ClosedLoopPatternAnalyzer.Member, ClosedLoopPatternAuthoringService.MarkedMember> byMember = new IdentityHashMap<>();

         for (ClosedLoopPatternAuthoringService.MarkedMember marked : markedMembers) {
            if (marked == null
               || marked.details() == null
               || marked.snapshot() == null
               || marked.details() instanceof TianshuClosedLoopPatternDetails
               || CLOSED_LOOP_PATTERN_ITEM_ID.equals(marked.snapshot().itemId())
               || marked.copiesPerCycle() < 1L) {
               return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_MARKING, null);
            }

            details.add(marked.details());
            ClosedLoopPatternAnalyzer.Member analyzerMember = new ClosedLoopPatternAnalyzer.Member(marked.details(), marked.copiesPerCycle());
            analyzed.add(analyzerMember);
            byMember.put(analyzerMember, marked);
         }

         if (!ClosedLoopPatternAnalyzer.isMinimalIntegerRatio(analyzed.stream().mapToLong(ClosedLoopPatternAnalyzer.Member::copies).toArray())) {
            return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.NON_MINIMAL_COPIES, null);
         } else {
            ClosedLoopPatternAnalyzer.StructureStatus structure = ClosedLoopPatternAnalyzer.validateStructure(details);
            if (structure != ClosedLoopPatternAnalyzer.StructureStatus.VALID) {
               return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_SEED_ROUTING, null);
            } else {
               ClosedLoopPatternAnalyzer.OrderedAnalysis ordered = ClosedLoopPatternAnalyzer.analyzeBestOrder(analyzed, mainOutput);
               if (ordered == null) {
                  ClosedLoopAnalysis writtenAnalysis = ClosedLoopPatternAnalyzer.analyze(analyzed, mainOutput);
                  return writtenAnalysis != null && !ClosedLoopPatternAnalyzer.hasInputSeedPerMember(analyzed, writtenAnalysis.seeds())
                     ? new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_SEED_ROUTING, null)
                     : new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.NOT_BALANCED, null);
               } else {
                  ArrayList<ClosedLoopMemberPattern> stored = new ArrayList<>(ordered.members().size());

                  for (ClosedLoopPatternAnalyzer.Member member : ordered.members()) {
                     ClosedLoopPatternAuthoringService.MarkedMember marked = byMember.get(member);
                     if (marked == null || marked.copiesPerCycle() != member.copies()) {
                        return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_MARKING, null);
                     }

                     stored.add(new ClosedLoopMemberPattern(marked.snapshot(), member.copies()));
                  }

                  ClosedLoopAnalysis analysis = ordered.analysis();
                  if (!ClosedLoopPatternAnalyzer.hasInputSeedPerMember(ordered.members(), analysis.seeds())) {
                     return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.NOT_BALANCED, null);
                  } else {
                     ArrayList<GenericStack> declaredOutputs = new ArrayList<>(Math.min(9, analysis.netOutputs().size()));

                     for (GenericStack output : analysis.netOutputs()) {
                        if (output.what().equals(mainOutput)) {
                           declaredOutputs.add(output);
                           break;
                        }
                     }

                     for (GenericStack outputx : analysis.netOutputs()) {
                        if (declaredOutputs.size() >= 9) {
                           break;
                        }

                        if (!outputx.what().equals(mainOutput)) {
                           declaredOutputs.add(outputx);
                        }
                     }

                     ClosedLoopPatternPayload payload = new ClosedLoopPatternPayload(
                        stored, analysis.seeds(), analysis.externalInputs(), declaredOutputs, executionSeedMultiplier, storedTaskMultiplier, true
                     );
                     return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.VALID, payload);
                  }
               }
            }
         }
      } else {
         return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_MARKING, null);
      }
   }

   @Deprecated
   public static ClosedLoopPatternAuthoringService.Result create(
      List<ClosedLoopPatternAuthoringService.MarkedMember> markedMembers, AEKey mainOutput, int seedMultiplier
   ) {
      return create(markedMembers, mainOutput, seedMultiplier, 1);
   }

   public static ClosedLoopPatternAuthoringService.Result createFromDraft(
      List<ClosedLoopMemberPattern> draftMembers, AEKey mainOutput, int executionSeedMultiplier, int storedTaskMultiplier, Level level
   ) {
      if (draftMembers != null && level != null) {
         ClosedLoopPatternFlattener.Result flattened = ClosedLoopPatternFlattener.flatten(draftMembers, level);
         if (!flattened.valid()) {
            return new ClosedLoopPatternAuthoringService.Result(switch (flattened.status()) {
               case MEMBER_UNDECODABLE -> ClosedLoopPatternAuthoringService.Status.MEMBER_UNDECODABLE;
               case TOO_MANY_MEMBERS -> ClosedLoopPatternAuthoringService.Status.TOO_MANY_MEMBERS;
               case NON_MINIMAL_COPIES -> ClosedLoopPatternAuthoringService.Status.NON_MINIMAL_COPIES;
               case INVALID_INPUT -> ClosedLoopPatternAuthoringService.Status.INVALID_MARKING;
               default -> ClosedLoopPatternAuthoringService.Status.MEMBER_UNDECODABLE;
            }, null);
         } else {
            ArrayList<ClosedLoopPatternAuthoringService.MarkedMember> marked = new ArrayList<>(flattened.members().size());

            for (ClosedLoopPatternFlattener.LeafMember stored : flattened.members()) {
               marked.add(new ClosedLoopPatternAuthoringService.MarkedMember(stored.details(), stored.snapshot(), stored.totalCopies()));
            }

            return create(marked, mainOutput, executionSeedMultiplier, storedTaskMultiplier);
         }
      } else {
         return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_MARKING, null);
      }
   }

   @Deprecated
   public static ClosedLoopPatternAuthoringService.Result createFromDraft(
      List<ClosedLoopMemberPattern> draftMembers, AEKey mainOutput, int seedMultiplier, Level level
   ) {
      return createFromDraft(draftMembers, mainOutput, seedMultiplier, 1, level);
   }

   public static ClosedLoopPatternAuthoringService.Result createFromDraft(
      List<ClosedLoopMemberPattern> draftMembers, List<AEKey> declaredOutputs, int executionSeedMultiplier, int storedTaskMultiplier, Level level
   ) {
      if (declaredOutputs != null
         && !declaredOutputs.isEmpty()
         && declaredOutputs.size() <= 9
         && new LinkedHashSet<>(declaredOutputs).size() == declaredOutputs.size()
         && !declaredOutputs.stream().anyMatch(Objects::isNull)) {
         ClosedLoopPatternAuthoringService.Result authored = createFromDraft(
            draftMembers, declaredOutputs.get(0), executionSeedMultiplier, storedTaskMultiplier, level
         );
         if (!authored.valid()) {
            return authored;
         } else {
            LinkedHashMap<AEKey, GenericStack> analyzedOutputs = new LinkedHashMap<>();

            for (GenericStack output : authored.payload().netOutputs()) {
               analyzedOutputs.put(output.what(), output);
            }

            ArrayList<GenericStack> selected = new ArrayList<>(declaredOutputs.size());

            for (AEKey key : declaredOutputs) {
               GenericStack output = analyzedOutputs.get(key);
               if (output == null) {
                  return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_MARKING, null);
               }

               selected.add(output);
            }

            ClosedLoopPatternPayload payload = authored.payload();
            return new ClosedLoopPatternAuthoringService.Result(
               ClosedLoopPatternAuthoringService.Status.VALID,
               new ClosedLoopPatternPayload(
                  payload.memberPatterns(),
                  payload.seeds(),
                  payload.externalInputs(),
                  selected,
                  payload.executionSeedMultiplier(),
                  payload.storedTaskMultiplier(),
                  payload.enabled()
               )
            );
         }
      } else {
         return new ClosedLoopPatternAuthoringService.Result(ClosedLoopPatternAuthoringService.Status.INVALID_MARKING, null);
      }
   }

   private ClosedLoopPatternAuthoringService() {
   }

   public static record MarkedMember(IPatternDetails details, SourcePatternSnapshot snapshot, long copiesPerCycle) {
      public MarkedMember(IPatternDetails details, SourcePatternSnapshot snapshot, long copiesPerCycle) {
         Objects.requireNonNull(details, "details");
         Objects.requireNonNull(snapshot, "snapshot");
         if (copiesPerCycle < 1L) {
            throw new IllegalArgumentException("copies must be positive");
         } else {
            this.details = details;
            this.snapshot = snapshot;
            this.copiesPerCycle = copiesPerCycle;
         }
      }
   }

   public static record Result(ClosedLoopPatternAuthoringService.Status status, ClosedLoopPatternPayload payload) {
      public boolean valid() {
         return this.status == ClosedLoopPatternAuthoringService.Status.VALID && this.payload != null;
      }
   }

   public static enum Status {
      VALID,
      MEMBER_UNDECODABLE,
      TOO_MANY_MEMBERS,
      INVALID_MARKING,
      NON_MINIMAL_COPIES,
      NOT_BALANCED,
      INVALID_SEED_ROUTING;
   }
}
