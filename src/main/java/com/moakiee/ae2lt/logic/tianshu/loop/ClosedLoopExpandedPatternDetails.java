package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.crafting.IPatternDetails.PatternInputSink;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import com.moakiee.thunderbolt.core.crafting.loop.CraftingTaskPersistenceDefinition;
import com.moakiee.thunderbolt.core.crafting.loop.IPlannedSeedSlotPattern;
import com.moakiee.thunderbolt.core.crafting.loop.IPrioritizedCraftingTask;
import com.moakiee.thunderbolt.core.crafting.loop.ISeedPreservingCraftingTask;
import com.moakiee.thunderbolt.core.crafting.pattern.IProviderLookupPattern;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ClosedLoopExpandedPatternDetails
   implements IPatternDetails,
   CraftingTaskPersistenceDefinition,
   IPrioritizedCraftingTask,
   IProviderLookupPattern,
   ISeedPreservingCraftingTask,
   IPlannedSeedSlotPattern {
   public static final int CLOSED_LOOP_DISPATCH_PRIORITY = 1000;
   protected final IPatternDetails delegate;
   protected final Set<AEKey> seedKeys;
   protected final Set<AEKey> cycleKeys;
   protected final Map<AEKey, Long> sharedOutputAmounts;
   private final AEItemKey persistenceDefinition;
   private final int dispatchOrder;
   private final IInput[] executionInputs;
   private final UUID seedGroupId;
   private final boolean singleSeedInputPerMember;
   private final Map<Integer, AEKey> plannedSeedInputSlots;

   public ClosedLoopExpandedPatternDetails(IPatternDetails delegate, Map<AEKey, Long> seedAmounts, AEItemKey persistenceDefinition) {
      this(delegate, seedAmounts, persistenceDefinition, 0);
   }

   public ClosedLoopExpandedPatternDetails(IPatternDetails delegate, Map<AEKey, Long> seedAmounts, AEItemKey persistenceDefinition, int dispatchOrder) {
      this(delegate, seedAmounts, seedAmounts.keySet(), fallbackGroupId(persistenceDefinition), persistenceDefinition, dispatchOrder);
   }

   public ClosedLoopExpandedPatternDetails(
      IPatternDetails delegate, Map<AEKey, Long> seedAmounts, Set<AEKey> cycleKeys, UUID seedGroupId, AEItemKey persistenceDefinition, int dispatchOrder
   ) {
      this(delegate, seedAmounts, cycleKeys, seedGroupId, seedAmounts.size() == 1, persistenceDefinition, dispatchOrder);
   }

   public ClosedLoopExpandedPatternDetails(
      IPatternDetails delegate,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      this(delegate, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, Map.of(), persistenceDefinition, dispatchOrder);
   }

   public ClosedLoopExpandedPatternDetails(
      IPatternDetails delegate,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      Map<Integer, AEKey> plannedSeedInputSlots,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.seedKeys = Set.copyOf(seedAmounts.keySet());
      this.cycleKeys = Set.copyOf(cycleKeys);
      this.seedGroupId = Objects.requireNonNull(seedGroupId, "seedGroupId");
      this.singleSeedInputPerMember = singleSeedInputPerMember;
      this.plannedSeedInputSlots = Map.copyOf(plannedSeedInputSlots);
      this.executionInputs = this.plannedSeedInputSlots.isEmpty()
         ? pinReusableSeedInputs(delegate, this.seedKeys)
         : pinReusableSeedInputs(delegate, this.plannedSeedInputSlots);
      this.sharedOutputAmounts = sharedOutputAmounts(delegate, seedAmounts);
      this.persistenceDefinition = Objects.requireNonNull(persistenceDefinition, "persistenceDefinition");
      this.dispatchOrder = Math.max(0, dispatchOrder);
   }

   public static ClosedLoopExpandedPatternDetails wrap(IPatternDetails delegate, Set<AEKey> seedKeys, boolean singleMemberLoop, AEItemKey persistenceDefinition) {
      LinkedHashMap<AEKey, Long> seedAmounts = new LinkedHashMap<>();

      for (AEKey seedKey : seedKeys) {
         seedAmounts.put(seedKey, Long.valueOf(1L));
      }

      return wrap(delegate, seedAmounts, singleMemberLoop, persistenceDefinition, 0);
   }

   public static ClosedLoopExpandedPatternDetails wrap(
      IPatternDetails delegate, Map<AEKey, Long> seedAmounts, boolean singleMemberLoop, AEItemKey persistenceDefinition
   ) {
      return wrap(delegate, seedAmounts, singleMemberLoop, persistenceDefinition, 0);
   }

   public static ClosedLoopExpandedPatternDetails wrap(
      IPatternDetails delegate, Map<AEKey, Long> seedAmounts, boolean singleMemberLoop, AEItemKey persistenceDefinition, int dispatchOrder
   ) {
      return wrap(delegate, seedAmounts, seedAmounts.keySet(), fallbackGroupId(persistenceDefinition), singleMemberLoop, persistenceDefinition, dispatchOrder);
   }

   public static ClosedLoopExpandedPatternDetails wrap(
      IPatternDetails delegate,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleMemberLoop,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      return wrap(delegate, seedAmounts, cycleKeys, seedGroupId, seedAmounts.size() == 1, singleMemberLoop, persistenceDefinition, dispatchOrder);
   }

   public static ClosedLoopExpandedPatternDetails wrap(
      IPatternDetails delegate,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      boolean singleMemberLoop,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      return wrap(delegate, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, Map.of(), singleMemberLoop, persistenceDefinition, dispatchOrder);
   }

   public static ClosedLoopExpandedPatternDetails wrap(
      IPatternDetails delegate,
      Map<AEKey, Long> seedAmounts,
      Set<AEKey> cycleKeys,
      UUID seedGroupId,
      boolean singleSeedInputPerMember,
      Map<Integer, AEKey> plannedSeedInputSlots,
      boolean singleMemberLoop,
      AEItemKey persistenceDefinition,
      int dispatchOrder
   ) {
      boolean reusableSeed = singleMemberLoop && seedAmounts.size() == 1 && hasSeedInput(delegate, seedAmounts.keySet());
      if (delegate instanceof IMolecularAssemblerSupportedPattern molecular) {
         return (ClosedLoopExpandedPatternDetails)(reusableSeed
            ? new SingleSeedClosedLoopMolecularPatternDetails(
               molecular, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder
            )
            : new ClosedLoopMolecularPatternDetails(
               molecular, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder
            ));
      } else {
         return (ClosedLoopExpandedPatternDetails)(reusableSeed
            ? new SingleSeedClosedLoopPatternDetails(
               delegate, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder
            )
            : new ClosedLoopExpandedPatternDetails(
               delegate, seedAmounts, cycleKeys, seedGroupId, singleSeedInputPerMember, plannedSeedInputSlots, persistenceDefinition, dispatchOrder
            ));
      }
   }

   public IPatternDetails delegate() {
      return this.delegate;
   }

   public IPatternDetails providerLookupPattern() {
      return this.delegate;
   }

   public AEItemKey craftingTaskPersistenceDefinition() {
      return this.persistenceDefinition;
   }

   public int dispatchPriority() {
      return 1000;
   }

   public int dispatchOrder() {
      return this.dispatchOrder;
   }

   public UUID reusableSeedGroupId() {
      return this.seedGroupId;
   }

   public Set<AEKey> reusableSeedCycleKeys() {
      return this.cycleKeys;
   }

   public boolean hasSingleSeedInputPerMember() {
      return this.singleSeedInputPerMember;
   }

   public Map<Integer, AEKey> plannedSeedInputSlots() {
      return this.plannedSeedInputSlots;
   }

   public AEItemKey getDefinition() {
      return this.delegate.getDefinition();
   }

   public IInput[] getInputs() {
      return (IInput[])this.executionInputs.clone();
   }

   public GenericStack[] getOutputs() {
      return this.delegate.getOutputs();
   }

   public boolean supportsPushInputsToExternalInventory() {
      return this.delegate.supportsPushInputsToExternalInventory();
   }

   public void pushInputsToExternalInventory(KeyCounter[] inputHolder, PatternInputSink inputSink) {
      this.delegate.pushInputsToExternalInventory(inputHolder, inputSink);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof ClosedLoopExpandedPatternDetails other && this.persistenceDefinition.equals(other.persistenceDefinition)) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.persistenceDefinition.hashCode();
   }

   protected boolean isReusableSeedInput(int slot, AEKey concreteKey) {
      if (concreteKey != null && this.seedKeys.contains(concreteKey)) {
         IInput[] inputs = this.getInputs();
         return slot >= 0 && slot < inputs.length;
      } else {
         return false;
      }
   }

   protected long sharedBatchOutputAmount(AEKey outputKey) {
      return this.sharedOutputAmounts.getOrDefault(outputKey, 0L);
   }

   private static boolean hasSeedInput(IPatternDetails details, Set<AEKey> seedKeys) {
      IInput[] inputs = details.getInputs();

      for (int slot = 0; slot < inputs.length; slot++) {
         for (GenericStack possible : inputs[slot].getPossibleInputs()) {
            if (possible.what() != null && seedKeys.contains(possible.what())) {
               return true;
            }
         }
      }

      return false;
   }

   public static IInput[] pinReusableSeedInputs(IPatternDetails details, Set<AEKey> seedKeys) {
      IInput[] source = details.getInputs();
      IInput[] result = Arrays.copyOf(source, source.length, IInput[].class);

      for (int slot = 0; slot < source.length; slot++) {
         IInput input;
         GenericStack[] possible;
         boolean var10000;
         label68: {
            input = source[slot];
            possible = input.getPossibleInputs();
            if (CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails overload && overload.isFuzzyInput(slot)) {
               var10000 = true;
               break label68;
            }

            var10000 = false;
         }

         boolean ignoreSecondary = var10000;
         AEKey selected = null;
         long selectedAmount = 0L;

         for (AEKey seedKey : seedKeys) {
            GenericStack firstMatch = null;
            GenericStack exactMatch = null;
            Long physicalAmount = null;

            for (GenericStack candidate : possible) {
               if (candidate.what() != null && sameSeedIdentity(candidate.what(), seedKey, ignoreSecondary)) {
                  if (physicalAmount != null && physicalAmount != candidate.amount()) {
                     throw new IllegalArgumentException("fuzzy closed-loop seed candidates use different physical amounts");
                  }

                  physicalAmount = candidate.amount();
                  if (firstMatch == null) {
                     firstMatch = candidate;
                  }

                  if (candidate.what().equals(seedKey)) {
                     exactMatch = candidate;
                  }
               }
            }

            GenericStack chosen = exactMatch != null ? exactMatch : firstMatch;
            if (chosen != null) {
               if (selected != null && !selected.equals(seedKey)) {
                  throw new IllegalArgumentException("fuzzy closed-loop seed slot matches multiple concrete seed variants");
               }

               selected = seedKey;
               selectedAmount = chosen.amount();
            }
         }

         if (selected != null) {
            AEKey selectedRemainder = input.getRemainingKey(selected);
            ArrayList<GenericStack> executionCandidates = new ArrayList<>();
            if (ignoreSecondary) {
               executionCandidates.add(new GenericStack(selected, selectedAmount));
            } else {
               for (GenericStack candidatex : possible) {
                  if (candidatex.what() != null && transitionEquivalent(input, selected, selectedAmount, selectedRemainder, candidatex)) {
                     executionCandidates.add(candidatex);
                  }
               }
            }

            result[slot] = new ClosedLoopExpandedPatternDetails.PinnedSeedInput(
               input, executionCandidates.toArray(GenericStack[]::new), selected, input.getMultiplier(), ignoreSecondary
            );
         }
      }

      return result;
   }

   public static IInput[] pinReusableSeedInputs(IPatternDetails details, Map<Integer, AEKey> plannedSeedInputSlots) {
      IInput[] source = details.getInputs();
      IInput[] result = Arrays.copyOf(source, source.length, IInput[].class);

      for (Entry<Integer, AEKey> entry : plannedSeedInputSlots.entrySet()) {
         int slot = entry.getKey();
         if (slot < 0 || slot >= source.length || entry.getValue() == null) {
            throw new IllegalArgumentException("closed-loop seed slot mapping is invalid");
         }

         result[slot] = pinReusableSeedInput(details, source[slot], slot, entry.getValue());
      }

      return result;
   }

   private static IInput pinReusableSeedInput(IPatternDetails details, IInput input, int slot, AEKey selected) {
      boolean var10000;
      label67: {
         if (CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails overload && overload.isFuzzyInput(slot)) {
            var10000 = true;
            break label67;
         }

         var10000 = false;
      }

      boolean ignoreSecondary = var10000;
      GenericStack[] possible = input.getPossibleInputs();
      GenericStack firstMatch = null;
      GenericStack exactMatch = null;
      Long physicalAmount = null;

      for (GenericStack candidate : possible) {
         if (candidate.what() != null && sameSeedIdentity(candidate.what(), selected, ignoreSecondary)) {
            if (physicalAmount != null && physicalAmount != candidate.amount()) {
               throw new IllegalArgumentException("fuzzy closed-loop seed candidates use different physical amounts");
            }

            physicalAmount = candidate.amount();
            if (firstMatch == null) {
               firstMatch = candidate;
            }

            if (candidate.what().equals(selected)) {
               exactMatch = candidate;
            }
         }
      }

      GenericStack chosen = exactMatch != null ? exactMatch : firstMatch;
      if (chosen == null) {
         throw new IllegalArgumentException("planned closed-loop seed is not valid for its slot");
      } else {
         AEKey selectedRemainder = input.getRemainingKey(selected);
         ArrayList<GenericStack> executionCandidates = new ArrayList<>();
         if (ignoreSecondary) {
            executionCandidates.add(new GenericStack(selected, chosen.amount()));
         } else {
            for (GenericStack candidatex : possible) {
               if (candidatex.what() != null && transitionEquivalent(input, selected, chosen.amount(), selectedRemainder, candidatex)) {
                  executionCandidates.add(candidatex);
               }
            }
         }

         return new ClosedLoopExpandedPatternDetails.PinnedSeedInput(
            input, executionCandidates.toArray(GenericStack[]::new), selected, input.getMultiplier(), ignoreSecondary
         );
      }
   }

   private static boolean transitionEquivalent(IInput input, AEKey selected, long selectedAmount, @Nullable AEKey selectedRemainder, GenericStack candidate) {
      if (candidate.amount() != selectedAmount) {
         return false;
      } else {
         AEKey candidateRemainder = input.getRemainingKey(candidate.what());
         if (selectedRemainder == null) {
            return candidateRemainder == null;
         } else if (candidateRemainder == null) {
            return false;
         } else {
            return selectedRemainder.equals(selected) ? candidateRemainder.equals(candidate.what()) : selectedRemainder.equals(candidateRemainder);
         }
      }
   }

   private static boolean sameSeedIdentity(AEKey candidate, AEKey seed, boolean ignoreSecondary) {
      return candidate.equals(seed) || ignoreSecondary && candidate.dropSecondary().equals(seed.dropSecondary());
   }

   private static Map<AEKey, Long> sharedOutputAmounts(IPatternDetails details, Map<AEKey, Long> seedAmounts) {
      LinkedHashMap<AEKey, Long> returnedAsRemainder = new LinkedHashMap<>();

      for (IInput input : details.getInputs()) {
         GenericStack[] possible = input.getPossibleInputs();
         if (possible.length == 1 && possible[0].what() != null) {
            AEKey key = possible[0].what();
            if (seedAmounts.containsKey(key) && key.equals(input.getRemainingKey(key))) {
               long amount = input.getMultiplier();
               returnedAsRemainder.merge(key, Long.valueOf(amount), Sat::add);
            }
         }
      }

      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();

      for (Entry<AEKey, Long> entry : seedAmounts.entrySet()) {
         long fromOutputs = Math.max(0L, entry.getValue() - returnedAsRemainder.getOrDefault(entry.getKey(), 0L));
         if (fromOutputs > 0L) {
            result.put(entry.getKey(), Long.valueOf(fromOutputs));
         }
      }

      return Map.copyOf(result);
   }

   private static UUID fallbackGroupId(AEItemKey persistenceDefinition) {
      return UUID.nameUUIDFromBytes(Objects.requireNonNull(persistenceDefinition, "persistenceDefinition").toString().getBytes(StandardCharsets.UTF_8));
   }

   private static final class PinnedSeedInput implements IInput {
      private final IInput source;
      private final GenericStack[] possible;
      private final AEKey selected;
      private final long multiplier;
      private final boolean ignoreSecondary;

      private PinnedSeedInput(IInput source, GenericStack[] possible, AEKey selected, long multiplier, boolean ignoreSecondary) {
         this.source = source;
         this.possible = (GenericStack[])possible.clone();
         this.selected = selected;
         this.multiplier = multiplier;
         this.ignoreSecondary = ignoreSecondary;
      }

      public GenericStack[] getPossibleInputs() {
         return (GenericStack[])this.possible.clone();
      }

      public long getMultiplier() {
         return this.multiplier;
      }

      public boolean isValid(AEKey input, Level level) {
         boolean allowed = this.ignoreSecondary
            ? ClosedLoopExpandedPatternDetails.sameSeedIdentity(this.selected, input, true)
            : Arrays.stream(this.possible).anyMatch(candidate -> candidate.what().equals(input));
         return allowed && (this.ignoreSecondary || this.source.isValid(input, level));
      }

      @Nullable
      public AEKey getRemainingKey(AEKey template) {
         boolean allowed = this.ignoreSecondary
            ? ClosedLoopExpandedPatternDetails.sameSeedIdentity(this.selected, template, true)
            : Arrays.stream(this.possible).anyMatch(candidate -> candidate.what().equals(template));
         return !allowed ? null : this.source.getRemainingKey(template);
      }
   }
}
