package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.stacks.GenericStack;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record ClosedLoopPatternPayload(
   List<ClosedLoopMemberPattern> memberPatterns,
   List<GenericStack> seeds,
   List<GenericStack> externalInputs,
   List<GenericStack> netOutputs,
   int executionSeedMultiplier,
   int storedTaskMultiplier,
   boolean enabled
) {
   public static final int MAX_NET_OUTPUTS = 9;

   public ClosedLoopPatternPayload(
      List<ClosedLoopMemberPattern> memberPatterns,
      List<GenericStack> seeds,
      List<GenericStack> externalInputs,
      List<GenericStack> netOutputs,
      int executionSeedMultiplier,
      int storedTaskMultiplier,
      boolean enabled
   ) {
      memberPatterns = copyMembers(memberPatterns);
      seeds = copyStacks(seeds, "seeds");
      externalInputs = copyStacks(externalInputs, "externalInputs");
      netOutputs = copyStacks(netOutputs, "netOutputs");
      if (memberPatterns.isEmpty()) {
         throw new IllegalArgumentException("closed-loop pattern requires members");
      } else if (memberPatterns.size() > 27) {
         throw new IllegalArgumentException("closed-loop pattern has too many members");
      } else if (!ClosedLoopPatternAnalyzer.isMinimalIntegerRatio(memberPatterns.stream().mapToLong(ClosedLoopMemberPattern::copiesPerCycle).toArray())) {
         throw new IllegalArgumentException("closed-loop member copies must use the minimal integer ratio");
      } else if (seeds.isEmpty()) {
         throw new IllegalArgumentException("closed-loop pattern requires at least one seed");
      } else if (netOutputs.isEmpty()) {
         throw new IllegalArgumentException("closed-loop pattern requires a net output");
      } else if (netOutputs.size() > 9) {
         throw new IllegalArgumentException("closed-loop pattern has too many declared outputs");
      } else if (executionSeedMultiplier < 1) {
         throw new IllegalArgumentException("execution seed multiplier must be positive");
      } else if (storedTaskMultiplier < 1) {
         throw new IllegalArgumentException("stored task multiplier must be positive");
      } else {
         HashSet<Object> outputKeys = new HashSet<>();

         for (GenericStack output : netOutputs) {
            if (!outputKeys.add(output.what())) {
               throw new IllegalArgumentException("duplicate net output key: " + output.what());
            }
         }

         this.memberPatterns = memberPatterns;
         this.seeds = seeds;
         this.externalInputs = externalInputs;
         this.netOutputs = netOutputs;
         this.executionSeedMultiplier = executionSeedMultiplier;
         this.storedTaskMultiplier = storedTaskMultiplier;
         this.enabled = enabled;
      }
   }

   public ClosedLoopPatternPayload withExecutionSeedMultiplier(int newExecutionSeedMultiplier) {
      return this.withSeedMultipliers(newExecutionSeedMultiplier, this.storedTaskMultiplier);
   }

   @Deprecated
   public int seedMultiplier() {
      return this.executionSeedMultiplier;
   }

   @Deprecated
   public ClosedLoopPatternPayload withSeedMultiplier(int newSeedMultiplier) {
      return this.withExecutionSeedMultiplier(newSeedMultiplier);
   }

   public ClosedLoopPatternPayload withStoredTaskMultiplier(int newStoredTaskMultiplier) {
      return this.withSeedMultipliers(this.executionSeedMultiplier, newStoredTaskMultiplier);
   }

   public ClosedLoopPatternPayload withSeedMultipliers(int newExecutionSeedMultiplier, int newStoredTaskMultiplier) {
      return this.executionSeedMultiplier == newExecutionSeedMultiplier && this.storedTaskMultiplier == newStoredTaskMultiplier
         ? this
         : new ClosedLoopPatternPayload(
            this.memberPatterns, this.seeds, this.externalInputs, this.netOutputs, newExecutionSeedMultiplier, newStoredTaskMultiplier, this.enabled
         );
   }

   public ClosedLoopPatternPayload withEnabled(boolean newEnabled) {
      return this.enabled == newEnabled
         ? this
         : new ClosedLoopPatternPayload(
            this.memberPatterns, this.seeds, this.externalInputs, this.netOutputs, this.executionSeedMultiplier, this.storedTaskMultiplier, newEnabled
         );
   }

   private static List<ClosedLoopMemberPattern> copyMembers(List<ClosedLoopMemberPattern> members) {
      Objects.requireNonNull(members, "memberPatterns");

      for (ClosedLoopMemberPattern member : members) {
         Objects.requireNonNull(member, "memberPattern");
      }

      return List.copyOf(members);
   }

   private static List<GenericStack> copyStacks(List<GenericStack> stacks, String name) {
      Objects.requireNonNull(stacks, name);

      for (GenericStack stack : stacks) {
         Objects.requireNonNull(stack, name + " entry");
         Objects.requireNonNull(stack.what(), name + " key");
         if (stack.amount() <= 0L) {
            throw new IllegalArgumentException(name + " amounts must be positive");
         }
      }

      return List.copyOf(stacks);
   }
}
