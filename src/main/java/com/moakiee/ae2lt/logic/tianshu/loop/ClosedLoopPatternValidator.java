package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.Level;

public final class ClosedLoopPatternValidator {
   public static ClosedLoopValidationResult validate(ClosedLoopPatternPayload payload, Level level) {
      return ClosedLoopPatternDecoder.decodePayload(payload, level).validation();
   }

   static ClosedLoopValidationResult validateDecoded(ClosedLoopPatternPayload payload, List<IPatternDetails> decodedMembers) {
      if (payload != null && decodedMembers != null && decodedMembers.size() == payload.memberPatterns().size()) {
         ArrayList<ClosedLoopPatternAnalyzer.Member> members = new ArrayList<>(payload.memberPatterns().size());

         for (int i = 0; i < decodedMembers.size(); i++) {
            IPatternDetails details = decodedMembers.get(i);
            if (details == null) {
               return invalid(ClosedLoopValidationResult.Status.MEMBER_UNDECODABLE);
            }

            if (details instanceof TianshuClosedLoopPatternDetails) {
               return invalid(ClosedLoopValidationResult.Status.MEMBER_IS_CLOSED_LOOP);
            }

            members.add(new ClosedLoopPatternAnalyzer.Member(details, payload.memberPatterns().get(i).copiesPerCycle()));
         }

         if (!ClosedLoopPatternAnalyzer.isPrimitiveCycle(members)) {
            return invalid(ClosedLoopValidationResult.Status.STRUCTURE_CHECK_FAILED);
         } else {
            ClosedLoopPatternAnalyzer.StructureStatus structure = ClosedLoopPatternAnalyzer.validateStructure(
               members.stream().map(ClosedLoopPatternAnalyzer.Member::details).toList()
            );
            if (structure != ClosedLoopPatternAnalyzer.StructureStatus.VALID) {
               return invalid(ClosedLoopValidationResult.Status.STRUCTURE_CHECK_FAILED);
            } else {
               GenericStack primary = payload.netOutputs().get(0);
               ClosedLoopAnalysis analysis = ClosedLoopPatternAnalyzer.analyze(members, primary.what());
               if (analysis == null) {
                  return invalid(ClosedLoopValidationResult.Status.NO_VALID_NET_OUTPUT);
               } else if (!ClosedLoopPatternAnalyzer.hasInputSeedPerMember(members, analysis.seeds())) {
                  return invalid(ClosedLoopValidationResult.Status.MEMBER_WITHOUT_INPUT_SEED);
               } else if (sameStacks(payload.seeds(), analysis.seeds()) && sameStacks(payload.externalInputs(), analysis.externalInputs())) {
                  Map<AEKey, Long> analyzedOutputs = amounts(analysis.netOutputs());

                  for (GenericStack declared : payload.netOutputs()) {
                     if (!declared.what().equals(primary.what()) && !analyzedOutputs.containsKey(declared.what())) {
                        return new ClosedLoopValidationResult(ClosedLoopValidationResult.Status.DECLARATION_MISMATCH, analysis);
                     }

                     if (analyzedOutputs.getOrDefault(declared.what(), 0L) != declared.amount()) {
                        return new ClosedLoopValidationResult(ClosedLoopValidationResult.Status.DECLARATION_MISMATCH, analysis);
                     }
                  }

                  return new ClosedLoopValidationResult(ClosedLoopValidationResult.Status.VALID, analysis);
               } else {
                  return new ClosedLoopValidationResult(ClosedLoopValidationResult.Status.DECLARATION_MISMATCH, analysis);
               }
            }
         }
      } else {
         return invalid(ClosedLoopValidationResult.Status.MEMBER_UNDECODABLE);
      }
   }

   private static boolean sameStacks(List<GenericStack> left, List<GenericStack> right) {
      return amounts(left).equals(amounts(right));
   }

   private static Map<AEKey, Long> amounts(List<GenericStack> stacks) {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();

      for (GenericStack stack : stacks) {
         result.merge(stack.what(), Long.valueOf(stack.amount()), Sat::add);
      }

      return Map.copyOf(result);
   }

   static ClosedLoopValidationResult invalid(ClosedLoopValidationResult.Status status) {
      return new ClosedLoopValidationResult(status, null);
   }

   private ClosedLoopPatternValidator() {
   }
}
