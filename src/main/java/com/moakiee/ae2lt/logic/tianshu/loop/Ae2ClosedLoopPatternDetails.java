package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.moakiee.ae2lt.crafting.runtime.ExecuteLoopPattern;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.logic.tianshu.TianshuCraftingCpuHost;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import com.moakiee.thunderbolt.api.crafting.cpu.ExtendedCraftingCpuClusterHost;
import com.moakiee.thunderbolt.core.crafting.loop.CraftingCpuRestrictedPattern;
import com.moakiee.thunderbolt.core.crafting.loop.PatternFiringExpander;
import com.moakiee.thunderbolt.core.crafting.loop.ReusableSeedPattern;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class Ae2ClosedLoopPatternDetails
   implements TianshuClosedLoopPatternDetails,
   PatternFiringExpander,
   CraftingCpuRestrictedPattern,
   ReusableSeedPattern {
   private final AEItemKey definition;
   private final ClosedLoopPatternPayload payload;
   private final IInput[] inputs;
   private final List<Ae2ClosedLoopPatternDetails.ExpandedMember> members;
   private final UUID owningTianshuId;
   private final UUID seedGroupId;
   private final Map<AEKey, Long> availableSeedSnapshot;
   private final Set<AEKey> cycleKeys;
   private final Map<AEKey, Set<AEKey>> acceptedSeedVariants;
   private final Set<AEKey> universallyFuzzySeedKeys;
   private final Set<AEKey> exactOnlyHostSeedKeys;
   private final boolean singleSeedInputPerMember;
   private final ClosedLoopConsumerRouting.RoutingPlan consumerRouting;

   public Ae2ClosedLoopPatternDetails(AEItemKey definition, ClosedLoopPatternPayload payload, Level level) {
      this(definition, payload, level, null, Map.of());
   }

   public Ae2ClosedLoopPatternDetails(AEItemKey definition, ClosedLoopPatternPayload payload, Level level, UUID owningTianshuId) {
      this(definition, payload, level, owningTianshuId, Map.of());
   }

   public Ae2ClosedLoopPatternDetails(
      AEItemKey definition, ClosedLoopPatternPayload payload, Level level, UUID owningTianshuId, Map<AEKey, Long> availableSeedSnapshot
   ) {
      this(definition, payload, level, owningTianshuId, ignored -> Map.copyOf(availableSeedSnapshot));
   }

   public Ae2ClosedLoopPatternDetails(
      AEItemKey definition,
      ClosedLoopPatternPayload payload,
      Level level,
      UUID owningTianshuId,
      Function<ReusableSeedPattern, Map<AEKey, Long>> availableSeedSnapshotFactory
   ) {
      this(definition, payload, level, owningTianshuId, availableSeedSnapshotFactory, ClosedLoopPatternDecoder.decodePayload(payload, level).members());
   }

   Ae2ClosedLoopPatternDetails(
      AEItemKey definition,
      ClosedLoopPatternPayload payload,
      Level level,
      UUID owningTianshuId,
      Function<ReusableSeedPattern, Map<AEKey, Long>> availableSeedSnapshotFactory,
      List<IPatternDetails> decodedMemberDetails
   ) {
      this.definition = Objects.requireNonNull(definition, "definition");
      this.payload = Objects.requireNonNull(payload, "payload");
      this.owningTianshuId = owningTianshuId;
      this.seedGroupId = ClosedLoopPatternIdentity.runtimeGroupId(definition, level.m_9598_());
      Objects.requireNonNull(availableSeedSnapshotFactory, "availableSeedSnapshotFactory");
      ArrayList<GenericStack> allInputs = new ArrayList<>(payload.seeds().size() + payload.externalInputs().size());

      for (GenericStack seed : payload.seeds()) {
         allInputs.add(new GenericStack(seed.what(), Sat.mul(seed.amount(), (long)payload.executionSeedMultiplier())));
      }

      for (GenericStack input : payload.externalInputs()) {
         allInputs.add(input);
      }

      this.inputs = new IInput[allInputs.size()];
      int slot = 0;

      for (GenericStack seed : payload.seeds()) {
         this.inputs[slot++] = new Ae2ClosedLoopPatternDetails.ExactInput(
            new GenericStack(seed.what(), Sat.mul(seed.amount(), (long)payload.executionSeedMultiplier())), true
         );
      }

      for (GenericStack input : payload.externalInputs()) {
         this.inputs[slot++] = new Ae2ClosedLoopPatternDetails.ExactInput(input, false);
      }

      List<IPatternDetails> rawMembers = List.copyOf(decodedMemberDetails);
      if (rawMembers.size() != payload.memberPatterns().size()) {
         throw new IllegalArgumentException("decoded closed-loop member count does not match payload");
      } else {
         for (IPatternDetails details : rawMembers) {
            if (details instanceof TianshuClosedLoopPatternDetails) {
               throw new IllegalArgumentException("closed-loop member pattern is no longer decodable");
            }
         }

         LinkedHashMap<AEKey, Long> seedAmounts = new LinkedHashMap<>();

         for (GenericStack seed : payload.seeds()) {
            seedAmounts.merge(seed.what(), Long.valueOf(seed.amount()), Sat::add);
         }

         this.cycleKeys = ClosedLoopCycleKeys.analyze(rawMembers, seedAmounts.keySet());
         ArrayList<ClosedLoopPatternAnalyzer.Member> analyzedMembers = new ArrayList<>(payload.memberPatterns().size());

         for (int i = 0; i < payload.memberPatterns().size(); i++) {
            analyzedMembers.add(new ClosedLoopPatternAnalyzer.Member(rawMembers.get(i), payload.memberPatterns().get(i).copiesPerCycle()));
         }

         List<ClosedLoopPatternAnalyzer.MemberFlow> memberFlows = ClosedLoopPatternAnalyzer.deriveMemberFlows(analyzedMembers, payload.seeds());
         if (memberFlows.size() != rawMembers.size()) {
            throw new IllegalArgumentException("closed-loop member seed transitions are unavailable");
         } else {
            validateFuzzyOutputSeedConsumers(analyzedMembers, memberFlows);
            LinkedHashMap<AEKey, Set<AEKey>> acceptedVariants = new LinkedHashMap<>();
            LinkedHashSet<AEKey> anyFuzzySeeds = new LinkedHashSet<>();
            LinkedHashSet<AEKey> universallyFuzzySeeds = new LinkedHashSet<>();
            collectAcceptedSeedVariants(rawMembers, memberFlows, seedAmounts.keySet(), acceptedVariants, anyFuzzySeeds, universallyFuzzySeeds);
            this.acceptedSeedVariants = Map.copyOf(acceptedVariants);
            this.universallyFuzzySeedKeys = Set.copyOf(universallyFuzzySeeds);
            this.singleSeedInputPerMember = isSharedSeedPoolSafe(
               ClosedLoopPatternAnalyzer.hasSingleSeedInputPerMember(memberFlows), seedAmounts.keySet(), acceptedVariants, anyFuzzySeeds
            );
            this.consumerRouting = ClosedLoopConsumerRouting.compile(this.seedGroupId, memberFlows);
            if (!this.consumerRouting.bootstrapSeed().equals(Map.copyOf(seedAmounts))) {
               throw new IllegalArgumentException("closed-loop consumer bootstrap does not match the encoded seed state");
            } else {
               ArrayList<Ae2ClosedLoopPatternDetails.ExpandedMember> decodedMembers = new ArrayList<>(payload.memberPatterns().size());
               int memberIndex = 0;

               for (ClosedLoopMemberPattern member : payload.memberPatterns()) {
                  IPatternDetails detailsx = rawMembers.get(memberIndex);
                  ClosedLoopPatternAnalyzer.MemberFlow memberFlow = memberFlows.get(memberIndex);
                  ClosedLoopPatternItem item = (ClosedLoopPatternItem)definition.getItem();
                  AEItemKey persistenceDefinition = AEItemKey.of(item.createExecutionMemberStack(payload, memberIndex, level.m_9598_()));
                  if (persistenceDefinition == null) {
                     throw new IllegalArgumentException("closed-loop member persistence key is unavailable");
                  }

                  decodedMembers.add(
                     new Ae2ClosedLoopPatternDetails.ExpandedMember(
                        ClosedLoopExpandedPatternDetails.wrap(
                           detailsx,
                           memberSeedAmounts(seedAmounts, memberFlow.inputSeed().keySet()),
                           this.cycleKeys,
                           this.seedGroupId,
                           this.singleSeedInputPerMember,
                           memberFlow.inputSeedBySlot(),
                           payload.memberPatterns().size() == 1,
                           persistenceDefinition,
                           memberIndex
                        ),
                        member.copiesPerCycle(),
                        memberFlow
                     )
                  );
                  memberIndex++;
               }

               this.members = List.copyOf(decodedMembers);
               this.exactOnlyHostSeedKeys = exactOnlyHostSeedKeys(
                  memberFlows, payload.memberPatterns().stream().map(ClosedLoopMemberPattern::copiesPerCycle).toList(), this.consumerRouting
               );
               this.availableSeedSnapshot = Map.copyOf(availableSeedSnapshotFactory.apply(this));
            }
         }
      }
   }

   public AEItemKey getDefinition() {
      return this.definition;
   }

   public IInput[] getInputs() {
      return (IInput[])this.inputs.clone();
   }

   public GenericStack[] getOutputs() {
      return this.payload.netOutputs().toArray(new GenericStack[0]);
   }

   @Override
   public ClosedLoopPatternPayload closedLoopPayload() {
      return this.payload;
   }

   public Map<IPatternDetails, Long> expandPatternFirings(long macroFirings) {
      if (macroFirings <= 0L) {
         return Map.of();
      } else {
         LinkedHashMap<IPatternDetails, Long> result = new LinkedHashMap<>();

         for (int memberIndex = 0; memberIndex < this.members.size(); memberIndex++) {
            Ae2ClosedLoopPatternDetails.ExpandedMember member = this.members.get(memberIndex);
            ClosedLoopConsumerRouting.ConsumerAccount consumer = this.consumerRouting.consumers().get(memberIndex);
            ClosedLoopConsumerRouting.ProducerRouting producer = this.consumerRouting.producers().get(memberIndex);

            for (Ae2ClosedLoopPatternDetails.MemberFlowSlice slice : splitMemberFlow(member.flow(), member.copiesPerCycle(), producer.targets())) {
               long count = expandedSliceCount(macroFirings, slice.copiesPerCycle());
               Map<UUID, Map<AEKey, Long>> sharedCredits = this.singleSeedInputPerMember
                  ? selectCredits(slice.outputSeedCredits(), producer.wrappedTargets(), true)
                  : Map.of();
               Map<UUID, Map<AEKey, Long>> consumerCredits = this.singleSeedInputPerMember
                  ? selectCredits(slice.outputSeedCredits(), producer.wrappedTargets(), false)
                  : slice.outputSeedCredits();
               result.put(
                  new ExecuteLoopPattern(
                     member.details(),
                     consumer.consumerId(),
                     scaledCounter(consumer.bootstrapSeed(), (long)this.payload.executionSeedMultiplier()),
                     counter(slice.inputSeed()),
                     counters(consumerCredits),
                     counters(sharedCredits)
                  ),
                  Long.valueOf(count)
               );
            }
         }

         return Collections.unmodifiableMap(result);
      }
   }

   public boolean acceptsCraftingCpu(ExtendedCraftingCpuClusterHost host) {
      if (this.owningTianshuId != null && host instanceof TianshuCraftingCpuHost tianshu && this.owningTianshuId.equals(tianshu.getTianshuId())) {
         return true;
      }

      return false;
   }

   public Map<AEKey, Long> totalReusableSeedRequirements() {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();

      for (GenericStack seed : this.payload.seeds()) {
         result.merge(seed.what(), Long.valueOf(Sat.mul(seed.amount(), (long)this.payload.executionSeedMultiplier())), Sat::add);
      }

      return Map.copyOf(result);
   }

   public Object reusableSeedStorageScope() {
      return this.owningTianshuId != null ? this.owningTianshuId : this.seedGroupId;
   }

   public boolean acceptsReusableSeedVariant(AEKey planned, AEKey actual) {
      if (planned == null || actual == null) {
         return false;
      } else if (planned.equals(actual)) {
         return true;
      } else if (this.exactOnlyHostSeedKeys.contains(planned)) {
         return false;
      } else {
         return this.universallyFuzzySeedKeys.contains(planned) && planned.dropSecondary().equals(actual.dropSecondary())
            ? true
            : this.acceptedSeedVariants.getOrDefault(planned, Set.of()).contains(actual);
      }
   }

   static Set<AEKey> exactOnlyHostSeedKeys(
      List<ClosedLoopPatternAnalyzer.MemberFlow> memberFlows, List<Long> copiesPerCycle, ClosedLoopConsumerRouting.RoutingPlan routing
   ) {
      if (memberFlows != null
         && copiesPerCycle != null
         && routing != null
         && memberFlows.size() == copiesPerCycle.size()
         && memberFlows.size() == routing.consumers().size()
         && memberFlows.size() == routing.producers().size()) {
         LinkedHashMap<UUID, Map<AEKey, Long>> bundleUnitsByConsumer = new LinkedHashMap<>();

         for (int memberIndex = 0; memberIndex < memberFlows.size(); memberIndex++) {
            long copies = copiesPerCycle.get(memberIndex);
            if (copies <= 0L) {
               throw new IllegalArgumentException("closed-loop member copies must be positive");
            }

            ClosedLoopConsumerRouting.ConsumerAccount consumer = routing.consumers().get(memberIndex);
            ClosedLoopConsumerRouting.ProducerRouting producer = routing.producers().get(memberIndex);
            Map<AEKey, Long> units = bundleUnitsByConsumer.computeIfAbsent(consumer.consumerId(), ignored -> new LinkedHashMap<>());

            for (Ae2ClosedLoopPatternDetails.MemberFlowSlice slice : splitMemberFlow(memberFlows.get(memberIndex), copies, producer.targets())) {
               for (Entry<AEKey, Long> input : slice.inputSeed().entrySet()) {
                  if (input.getValue() > 0L) {
                     Long previous = units.putIfAbsent(input.getKey(), input.getValue());
                     if (previous != null && previous != input.getValue()) {
                        units.put(input.getKey(), 0L);
                     }
                  }
               }
            }
         }

         LinkedHashSet<AEKey> result = new LinkedHashSet<>();

         for (ClosedLoopConsumerRouting.ConsumerAccount consumer : routing.consumers()) {
            Map<AEKey, Long> units = bundleUnitsByConsumer.getOrDefault(consumer.consumerId(), Map.of());

            for (Entry<AEKey, Long> bootstrap : consumer.bootstrapSeed().entrySet()) {
               if (bootstrap.getValue() > 0L && units.getOrDefault(bootstrap.getKey(), 0L) != 1L) {
                  result.add(bootstrap.getKey());
               }
            }
         }

         return Set.copyOf(result);
      } else {
         throw new IllegalArgumentException("closed-loop host bundle metadata is inconsistent");
      }
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

   public Map<AEKey, Long> availableReusableSeedSnapshot() {
      return this.availableSeedSnapshot;
   }

   static void collectAcceptedSeedVariants(
      List<IPatternDetails> members,
      List<ClosedLoopPatternAnalyzer.MemberFlow> memberFlows,
      Set<AEKey> seeds,
      Map<AEKey, Set<AEKey>> accepted,
      Set<AEKey> anyFuzzySeeds
   ) {
      collectAcceptedSeedVariants(members, memberFlows, seeds, accepted, anyFuzzySeeds, new LinkedHashSet<>());
   }

   static void collectAcceptedSeedVariants(
      List<IPatternDetails> members,
      List<ClosedLoopPatternAnalyzer.MemberFlow> memberFlows,
      Set<AEKey> seeds,
      Map<AEKey, Set<AEKey>> accepted,
      Set<AEKey> anyFuzzySeeds,
      Set<AEKey> universallyFuzzySeeds
   ) {
      LinkedHashMap<AEKey, ExecuteLoopPattern.SeedVariantRule> rules = new LinkedHashMap<>();

      for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
         IPatternDetails details = members.get(memberIndex);
         OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails candidate
            ? candidate
            : null;
         IInput[] inputs = ClosedLoopExpandedPatternDetails.pinReusableSeedInputs(details, memberFlows.get(memberIndex).inputSeedBySlot());

         for (Entry<Integer, AEKey> mapped : memberFlows.get(memberIndex).inputSeedBySlot().entrySet()) {
            int slot = mapped.getKey();
            AEKey seed = mapped.getValue();
            if (seeds.contains(seed) && slot >= 0 && slot < inputs.length) {
               boolean fuzzy = overload != null && overload.isFuzzyInput(slot);
               GenericStack[] possible = inputs[slot].getPossibleInputs();
               LinkedHashSet<AEKey> exact = new LinkedHashSet<>();
               LinkedHashSet<AEKey> fuzzyIdentities = new LinkedHashSet<>();
               exact.add(seed);
               boolean matchesSlot = false;

               for (GenericStack option : possible) {
                  if (option.what() != null) {
                     boolean matches = seed.equals(option.what()) || fuzzy && seed.dropSecondary().equals(option.what().dropSecondary());
                     if (matches) {
                        matchesSlot = true;
                        exact.add(option.what());
                        if (fuzzy) {
                           fuzzyIdentities.add(option.what().dropSecondary());
                        }
                     }
                  }
               }

               if (matchesSlot) {
                  if (fuzzy) {
                     anyFuzzySeeds.add(seed);
                  }

                  ExecuteLoopPattern.SeedVariantRule slotRule = new ExecuteLoopPattern.SeedVariantRule(exact, fuzzyIdentities);
                  rules.merge(seed, slotRule, ExecuteLoopPattern.SeedVariantRule::intersect);
               }
            }
         }
      }

      for (AEKey seed : seeds) {
         ExecuteLoopPattern.SeedVariantRule rule = rules.getOrDefault(seed, new ExecuteLoopPattern.SeedVariantRule(Set.of(seed), Set.of()));
         accepted.put(seed, rule.exactVariants());
         if (rule.fuzzyIdentities().contains(seed.dropSecondary())) {
            universallyFuzzySeeds.add(seed);
         }
      }
   }

   static void validateFuzzyOutputSeedConsumers(List<ClosedLoopPatternAnalyzer.Member> members, List<ClosedLoopPatternAnalyzer.MemberFlow> memberFlows) {
      if (!ClosedLoopPatternAnalyzer.hasSafeDynamicSeedRouting(members, memberFlows)) {
         throw new IllegalArgumentException("dynamic closed-loop seed output cannot reach a compatible P2 bundle");
      }
   }

   static boolean isSharedSeedPoolSafe(boolean structurallySingleSeed, Set<AEKey> seeds, Map<AEKey, Set<AEKey>> acceptedVariants, Set<AEKey> fuzzySeeds) {
      if (!structurallySingleSeed) {
         return false;
      } else {
         for (AEKey seed : seeds) {
            if (fuzzySeeds.contains(seed)) {
               return false;
            }

            for (AEKey variant : acceptedVariants.getOrDefault(seed, Set.of())) {
               if (!seed.equals(variant)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Ae2ClosedLoopPatternDetails other && this.definition.equals(other.definition)) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.definition.hashCode();
   }

   static List<Ae2ClosedLoopPatternDetails.MemberFlowSlice> splitMemberFlow(ClosedLoopPatternAnalyzer.MemberFlow flow, long copiesPerCycle) {
      UUID self = UUID.nameUUIDFromBytes("ae2lt:test-flow-slice".getBytes(StandardCharsets.UTF_8));
      return splitMemberFlow(flow, copiesPerCycle, Map.of(self, flow.outputSeed()));
   }

   static List<Ae2ClosedLoopPatternDetails.MemberFlowSlice> splitMemberFlow(
      ClosedLoopPatternAnalyzer.MemberFlow flow, long copiesPerCycle, Map<UUID, Map<AEKey, Long>> outputSeedCredits
   ) {
      if (flow != null && copiesPerCycle > 0L) {
         TreeSet<Long> boundaries = new TreeSet<>();
         boundaries.add(0L);
         boundaries.add(copiesPerCycle);
         addRemainderBoundaries(boundaries, flow.inputSeed(), copiesPerCycle);
         addRemainderBoundaries(boundaries, flow.outputSeed(), copiesPerCycle);
         addCreditBoundaries(boundaries, flow.outputSeed(), outputSeedCredits, copiesPerCycle);
         ArrayList<Long> points = new ArrayList<>(boundaries);
         ArrayList<Ae2ClosedLoopPatternDetails.MemberFlowSlice> slices = new ArrayList<>(Math.max(1, points.size() - 1));

         for (int i = 0; i + 1 < points.size(); i++) {
            long start = points.get(i);
            long end = points.get(i + 1);
            if (end > start) {
               slices.add(
                  new Ae2ClosedLoopPatternDetails.MemberFlowSlice(
                     perCopyAt(flow.inputSeed(), copiesPerCycle, start),
                     perCopyAt(flow.outputSeed(), copiesPerCycle, start),
                     perCopyCreditsAt(flow.outputSeed(), outputSeedCredits, copiesPerCycle, start),
                     end - start
                  )
               );
            }
         }

         return List.copyOf(slices);
      } else {
         return List.of();
      }
   }

   static long expandedSliceCount(long macroFirings, long sliceCopies) {
      return macroFirings > 0L && sliceCopies > 0L ? Sat.mul(macroFirings, sliceCopies) : 0L;
   }

   private static void addRemainderBoundaries(Set<Long> boundaries, Map<AEKey, Long> values, long copiesPerCycle) {
      for (Long amount : values.values()) {
         if (amount != null && amount > 0L) {
            long remainder = amount % copiesPerCycle;
            if (remainder > 0L) {
               boundaries.add(remainder);
            }
         }
      }
   }

   private static Map<AEKey, Long> perCopyAt(Map<AEKey, Long> values, long copiesPerCycle, long copyIndex) {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();

      for (Entry<AEKey, Long> entry : values.entrySet()) {
         long total = entry.getValue();
         if (total > 0L) {
            long amount = total / copiesPerCycle;
            if (copyIndex < total % copiesPerCycle) {
               amount = Sat.add(amount, 1L);
            }

            if (amount > 0L) {
               result.put(entry.getKey(), Long.valueOf(amount));
            }
         }
      }

      return Collections.unmodifiableMap(result);
   }

   private static KeyCounter counter(Map<AEKey, Long> values) {
      KeyCounter result = new KeyCounter();

      for (Entry<AEKey, Long> entry : values.entrySet()) {
         if (entry.getValue() > 0L) {
            result.add(entry.getKey(), entry.getValue());
         }
      }

      return result;
   }

   static Map<AEKey, Long> memberSeedAmounts(Map<AEKey, Long> bootstrap, Set<AEKey> memberInputs) {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();

      for (AEKey key : memberInputs) {
         result.put(key, Long.valueOf(Math.max(1L, bootstrap.getOrDefault(key, 1L))));
      }

      return Collections.unmodifiableMap(result);
   }

   private static KeyCounter scaledCounter(Map<AEKey, Long> values, long scale) {
      KeyCounter result = new KeyCounter();

      for (Entry<AEKey, Long> entry : values.entrySet()) {
         long amount = Sat.mul(entry.getValue(), scale);
         if (amount > 0L) {
            result.add(entry.getKey(), amount);
         }
      }

      return result;
   }

   private static Map<UUID, KeyCounter> counters(Map<UUID, Map<AEKey, Long>> values) {
      LinkedHashMap<UUID, KeyCounter> result = new LinkedHashMap<>();

      for (Entry<UUID, Map<AEKey, Long>> entry : values.entrySet()) {
         KeyCounter counter = counter(entry.getValue());
         if (!counter.isEmpty()) {
            result.put(entry.getKey(), counter);
         }
      }

      return Collections.unmodifiableMap(result);
   }

   private static Map<UUID, Map<AEKey, Long>> perCopyCreditsAt(
      Map<AEKey, Long> outputSeed, Map<UUID, Map<AEKey, Long>> values, long copiesPerCycle, long copyIndex
   ) {
      LinkedHashMap<UUID, Map<AEKey, Long>> result = new LinkedHashMap<>();

      for (Entry<AEKey, Long> output : outputSeed.entrySet()) {
         long total = Math.max(0L, output.getValue());
         if (total > 0L) {
            long copyStart = creditPrefix(total, copiesPerCycle, copyIndex);
            long copyEnd = creditPrefix(total, copiesPerCycle, copyIndex + 1L);
            long targetStart = 0L;

            for (Entry<UUID, Map<AEKey, Long>> target : values.entrySet()) {
               long targetAmount = Math.max(0L, target.getValue().getOrDefault(output.getKey(), 0L));
               long targetEnd = Sat.add(targetStart, targetAmount);
               long overlapStart = Math.max(copyStart, targetStart);
               long overlapEnd = Math.min(copyEnd, targetEnd);
               if (overlapEnd > overlapStart) {
                  LinkedHashMap<AEKey, Long> targetCredits = new LinkedHashMap<>(result.getOrDefault(target.getKey(), Map.of()));
                  targetCredits.put(output.getKey(), Long.valueOf(overlapEnd - overlapStart));
                  result.put(target.getKey(), Collections.unmodifiableMap(targetCredits));
               }

               targetStart = targetEnd;
            }

            if (targetStart != total) {
               throw new IllegalArgumentException("consumer credits do not match member seed output for " + output.getKey());
            }
         }
      }

      return Collections.unmodifiableMap(result);
   }

   private static Map<UUID, Map<AEKey, Long>> selectCredits(
      Map<UUID, Map<AEKey, Long>> credits, Map<UUID, Map<AEKey, Long>> wrappedCredits, boolean selectWrapped
   ) {
      LinkedHashMap<UUID, Map<AEKey, Long>> result = new LinkedHashMap<>();

      for (Entry<UUID, Map<AEKey, Long>> target : credits.entrySet()) {
         LinkedHashMap<AEKey, Long> selected = new LinkedHashMap<>();
         Map<AEKey, Long> wrappedForTarget = wrappedCredits.getOrDefault(target.getKey(), Map.of());

         for (Entry<AEKey, Long> entry : target.getValue().entrySet()) {
            boolean wrapped = wrappedForTarget.getOrDefault(entry.getKey(), 0L) > 0L;
            if (wrapped == selectWrapped) {
               selected.put(entry.getKey(), entry.getValue());
            }
         }

         if (!selected.isEmpty()) {
            result.put(target.getKey(), Collections.unmodifiableMap(selected));
         }
      }

      return Collections.unmodifiableMap(result);
   }

   private static void addCreditBoundaries(Set<Long> boundaries, Map<AEKey, Long> outputSeed, Map<UUID, Map<AEKey, Long>> credits, long copiesPerCycle) {
      LinkedHashSet<AEKey> creditedKeys = new LinkedHashSet<>();

      for (Map<AEKey, Long> target : credits.values()) {
         for (Entry<AEKey, Long> entry : target.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0L) {
               creditedKeys.add(entry.getKey());
            }
         }
      }

      for (AEKey key : creditedKeys) {
         if (outputSeed.getOrDefault(key, 0L) <= 0L) {
            throw new IllegalArgumentException("consumer credit has no matching member seed output for " + key);
         }
      }

      for (Entry<AEKey, Long> output : outputSeed.entrySet()) {
         long total = Math.max(0L, output.getValue());
         if (total > 0L) {
            long cursor = 0L;

            for (Map<AEKey, Long> target : credits.values()) {
               long amount = Math.max(0L, target.getOrDefault(output.getKey(), 0L));
               cursor = Sat.add(cursor, amount);
               addCreditBoundary(boundaries, total, copiesPerCycle, cursor);
            }

            if (cursor != total) {
               throw new IllegalArgumentException("consumer credits do not match member seed output for " + output.getKey());
            }
         }
      }
   }

   private static void addCreditBoundary(Set<Long> boundaries, long total, long copiesPerCycle, long flattenedOffset) {
      if (flattenedOffset <= 0L) {
         boundaries.add(0L);
      } else if (flattenedOffset >= total) {
         boundaries.add(copiesPerCycle);
      } else {
         long low = 0L;
         long high = copiesPerCycle;

         while (low < high) {
            long mid = low + (high - low >>> 1);
            if (creditPrefix(total, copiesPerCycle, mid) < flattenedOffset) {
               low = mid + 1L;
            } else {
               high = mid;
            }
         }

         if (creditPrefix(total, copiesPerCycle, low) != flattenedOffset && low > 0L) {
            boundaries.add(low - 1L);
         }

         boundaries.add(low);
      }
   }

   private static long creditPrefix(long total, long copiesPerCycle, long copyIndex) {
      if (total <= 0L || copiesPerCycle <= 0L || copyIndex <= 0L) {
         return 0L;
      } else if (copyIndex >= copiesPerCycle) {
         return total;
      } else {
         long quotient = total / copiesPerCycle;
         long remainder = total % copiesPerCycle;
         return quotient * copyIndex + Math.min(copyIndex, remainder);
      }
   }

   private static final class ExactInput implements IInput {
      private final GenericStack[] possibleInputs;
      private final boolean returned;

      private ExactInput(GenericStack input, boolean returned) {
         this.possibleInputs = new GenericStack[]{input};
         this.returned = returned;
      }

      public GenericStack[] getPossibleInputs() {
         return (GenericStack[])this.possibleInputs.clone();
      }

      public long getMultiplier() {
         return 1L;
      }

      public boolean isValid(AEKey input, Level level) {
         return this.possibleInputs[0].what().equals(input);
      }

      @Nullable
      public AEKey getRemainingKey(AEKey template) {
         return this.returned && this.possibleInputs[0].what().equals(template) ? template : null;
      }
   }

   private static record ExpandedMember(IPatternDetails details, long copiesPerCycle, ClosedLoopPatternAnalyzer.MemberFlow flow) {
   }

   static record MemberFlowSlice(Map<AEKey, Long> inputSeed, Map<AEKey, Long> outputSeed, Map<UUID, Map<AEKey, Long>> outputSeedCredits, long copiesPerCycle) {
      MemberFlowSlice(Map<AEKey, Long> inputSeed, Map<AEKey, Long> outputSeed, Map<UUID, Map<AEKey, Long>> outputSeedCredits, long copiesPerCycle) {
         inputSeed = Collections.unmodifiableMap(new LinkedHashMap<>(inputSeed));
         outputSeed = Collections.unmodifiableMap(new LinkedHashMap<>(outputSeed));
         LinkedHashMap<UUID, Map<AEKey, Long>> copiedCredits = new LinkedHashMap<>();

         for (Entry<UUID, Map<AEKey, Long>> entry : outputSeedCredits.entrySet()) {
            copiedCredits.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
         }

         outputSeedCredits = Collections.unmodifiableMap(copiedCredits);
         this.inputSeed = inputSeed;
         this.outputSeed = outputSeed;
         this.outputSeedCredits = outputSeedCredits;
         this.copiesPerCycle = copiesPerCycle;
      }
   }
}
