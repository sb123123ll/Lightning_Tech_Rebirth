package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.item.ClosedLoopPatternItem;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.SourcePatternSnapshot;
import com.moakiee.thunderbolt.core.crafting.planner.PositiveIntegerLinearSolver.Result;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ClosedLoopDiscoveryService {
   private static final int MAX_DEPTH = 8;
   private static final int MAX_PATHS_PER_INPUT = 32;
   private static final int MAX_MEMBER_COMBINATIONS = 128;
   private static final int MAX_COMPOSITE_EXPANSIONS = 128;

   public static List<ClosedLoopDiscoveryCandidate> discover(ICraftingService crafting, Level level, AEKey requestedOutput) {
      return discoverDetailed(crafting, level, requestedOutput).candidates();
   }

   public static ClosedLoopDiscoveryService.DiscoveryResult discoverDetailed(ICraftingService crafting, Level level, AEKey requestedOutput) {
      if (crafting != null && level != null && requestedOutput != null) {
         ArrayList<ClosedLoopDiscoveryCandidate> result = new ArrayList<>();
         HashSet<String> signatures = new HashSet<>();
         boolean rejectedUndecodablePattern = false;

         for (ClosedLoopDiscoveryService.ResolvedCandidate resolved : resolveCandidates(
            crafting::getCraftingFor,
            template -> crafting.getCraftables(candidate -> candidate != null && template.dropSecondary().equals(candidate.dropSecondary())),
            requestedOutput
         )) {
            ArrayList<ClosedLoopMemberPattern> storedMembers = new ArrayList<>(resolved.members().size());
            boolean valid = true;

            for (int i = 0; i < resolved.members().size(); i++) {
               AEItemKey definition = resolved.members().get(i).getDefinition();
               if (definition == null) {
                  valid = false;
                  break;
               }

               ItemStack definitionStack = definition.toStack();
               if (definitionStack.m_41720_() instanceof ClosedLoopPatternItem item && item.readExecutionMember(definitionStack) >= 0) {
                  valid = false;
                  break;
               }

               IPatternDetails decoded = PatternDetailsHelper.decodePattern(definition, level);
               if (decoded == null) {
                  rejectedUndecodablePattern = true;
                  valid = false;
                  break;
               }

               storedMembers.add(new ClosedLoopMemberPattern(SourcePatternSnapshot.fromItemStack(definition.toStack()), resolved.coefficients()[i]));
            }

            if (valid && storedMembers.size() <= 27) {
               String signature = storedMembers.stream()
                  .map(member -> member.pattern().toTag() + "x" + member.copiesPerCycle())
                  .sorted()
                  .reduce("", (a, b) -> a + "|" + b);
               if (signatures.add(signature)) {
                  ClosedLoopAnalysis analysis = resolved.analysis();
                  ArrayList<GenericStack> declaredOutputs = new ArrayList<>(Math.min(9, analysis.netOutputs().size()));

                  for (GenericStack output : analysis.netOutputs()) {
                     if (output.what().equals(requestedOutput)) {
                        declaredOutputs.add(output);
                        break;
                     }
                  }

                  for (GenericStack outputx : analysis.netOutputs()) {
                     if (declaredOutputs.size() >= 9) {
                        break;
                     }

                     if (!outputx.what().equals(requestedOutput)) {
                        declaredOutputs.add(outputx);
                     }
                  }

                  result.add(
                     new ClosedLoopDiscoveryCandidate(
                        new ClosedLoopPatternPayload(storedMembers, analysis.seeds(), analysis.externalInputs(), declaredOutputs, 1, 1, true)
                     )
                  );
               }
            }
         }

         return new ClosedLoopDiscoveryService.DiscoveryResult(result, rejectedUndecodablePattern);
      } else {
         return new ClosedLoopDiscoveryService.DiscoveryResult(List.of(), false);
      }
   }

   static List<ClosedLoopDiscoveryService.ResolvedCandidate> resolveCandidates(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor, AEKey requestedOutput
   ) {
      return resolveCandidates(patternsFor, ignored -> List.of(), requestedOutput);
   }

   static List<ClosedLoopDiscoveryService.ResolvedCandidate> resolveCandidates(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor, Function<AEKey, ? extends Iterable<AEKey>> fuzzyCraftablesFor, AEKey requestedOutput
   ) {
      if (patternsFor != null && fuzzyCraftablesFor != null && requestedOutput != null) {
         HashMap<AEKey, List<IPatternDetails>> patternCache = new HashMap<>();
         Function<AEKey, Iterable<IPatternDetails>> cachedPatternsFor = key -> patternCache.computeIfAbsent(
               key, ignored -> copyPatterns((Iterable<IPatternDetails>)patternsFor.apply(key))
            );
         HashMap<AEKey, List<AEKey>> fuzzyCache = new HashMap<>();
         Function<AEKey, Iterable<AEKey>> cachedFuzzyCraftablesFor = template -> fuzzyCache.computeIfAbsent(
               template.dropSecondary(), ignored -> copyKeys((Iterable<AEKey>)fuzzyCraftablesFor.apply(template))
            );
         return resolveCandidates(cachedPatternsFor, cachedFuzzyCraftablesFor, requestedOutput, new HashSet<>(), 0);
      } else {
         return List.of();
      }
   }

   private static List<ClosedLoopDiscoveryService.ResolvedCandidate> resolveCandidates(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor,
      Function<AEKey, ? extends Iterable<AEKey>> fuzzyCraftablesFor,
      AEKey requestedOutput,
      Set<AEKey> resolvingOutputs,
      int depth
   ) {
      if (depth < 8 && resolvingOutputs.add(requestedOutput)) {
         List result;
         try {
            List<ClosedLoopDiscoveryService.ResolvedCandidate> direct = resolveDirectCandidates(patternsFor, fuzzyCraftablesFor, requestedOutput);
            if (!direct.isEmpty()) {
               ArrayList<ClosedLoopDiscoveryService.ResolvedCandidate> resultx = new ArrayList<>(direct);
               ArrayList<ClosedLoopDiscoveryService.ResolvedCandidate> queue = new ArrayList<>(direct);
               HashSet<Set<IPatternDetails>> signatures = new HashSet<>();

               for (ClosedLoopDiscoveryService.ResolvedCandidate candidate : direct) {
                  signatures.add(Set.copyOf(candidate.members()));
               }

               HashMap<AEKey, List<ClosedLoopDiscoveryService.ResolvedCandidate>> nestedByOutput = new HashMap<>();
               int queueIndex = 0;
               int expansions = 0;

               while (queueIndex < queue.size() && expansions < 128) {
                  ClosedLoopDiscoveryService.ResolvedCandidate base = queue.get(queueIndex++);
                  if (base.members().size() < 27) {
                     LinkedHashSet<AEKey> externalKeys = new LinkedHashSet<>();

                     for (GenericStack external : base.analysis().externalInputs()) {
                        if (external != null && external.what() != null && external.amount() > 0L) {
                           externalKeys.add(external.what());
                        }
                     }

                     for (AEKey externalKey : externalKeys) {
                        if (!resolvingOutputs.contains(externalKey)) {
                           List<ClosedLoopDiscoveryService.ResolvedCandidate> nested = nestedByOutput.get(externalKey);
                           if (nested == null) {
                              nested = resolveCandidates(patternsFor, fuzzyCraftablesFor, externalKey, resolvingOutputs, depth + 1);
                              nestedByOutput.put(externalKey, nested);
                           }

                           for (ClosedLoopDiscoveryService.ResolvedCandidate supplier : nested) {
                              ClosedLoopDiscoveryService.ResolvedCandidate combined = combineCandidates(base, supplier, requestedOutput);
                              if (combined != null) {
                                 Set<IPatternDetails> signature = Set.copyOf(combined.members());
                                 if (signatures.add(signature)) {
                                    resultx.add(combined);
                                    queue.add(combined);
                                    if (++expansions >= 128) {
                                       break;
                                    }
                                 }
                              }
                           }

                           if (expansions >= 128) {
                              break;
                           }
                        }
                     }
                  }
               }

               return List.copyOf(resultx);
            }

            result = List.of();
         } finally {
            resolvingOutputs.remove(requestedOutput);
         }

         return result;
      } else {
         return List.of();
      }
   }

   private static List<ClosedLoopDiscoveryService.ResolvedCandidate> resolveDirectCandidates(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor, Function<AEKey, ? extends Iterable<AEKey>> fuzzyCraftablesFor, AEKey requestedOutput
   ) {
      ArrayList<ClosedLoopDiscoveryService.ResolvedCandidate> result = new ArrayList<>();
      HashSet<Set<IPatternDetails>> memberSetSignatures = new HashSet<>();

      for (IPatternDetails root : safePatterns(patternsFor, requestedOutput)) {
         List<ClosedLoopDiscoveryService.InputRequirement> rootInputs = inputRequirements(root);
         List<ClosedLoopDiscoveryService.PatternOutput> rootAnchors = exactOutputs(root, requestedOutput);
         if (rootInputs != null && !rootAnchors.isEmpty()) {
            List<ClosedLoopDiscoveryService.PatternOutput> rootOutputs = outputs(root);
            ArrayList<List<ClosedLoopDiscoveryService.PathOption>> optionsByInput = new ArrayList<>(rootInputs.size());

            for (ClosedLoopDiscoveryService.InputRequirement input : rootInputs) {
               ArrayList<ClosedLoopDiscoveryService.PathOption> options = new ArrayList<>();
               options.add(new ClosedLoopDiscoveryService.PathOption(List.of(), acceptsAny(input, rootOutputs)));
               LinkedHashSet<ClosedLoopDiscoveryService.PatternOutput> cycleAnchors = new LinkedHashSet<>(rootOutputs);

               for (AEKey key : input.keys()) {
                  cycleAnchors.add(new ClosedLoopDiscoveryService.PatternOutput(key, false));
               }

               for (List<IPatternDetails> path : pathsBackToAnchor(patternsFor, fuzzyCraftablesFor, input, List.copyOf(cycleAnchors), new HashSet<>(), 0)) {
                  options.add(new ClosedLoopDiscoveryService.PathOption(path, true));
               }

               optionsByInput.add(List.copyOf(options));
            }

            LinkedHashSet<IPatternDetails> selected = new LinkedHashSet<>();
            selected.add(root);
            int[] combinations = new int[]{0};
            enumerateMemberSets(optionsByInput, 0, selected, hasReturnedInput(root), combinations, members -> {
               if (members.size() <= 27 && memberSetSignatures.add(Set.copyOf(members))) {
                  List<IPatternDetails> memberList = List.copyOf(members);
                  Result coefficientResult = ClosedLoopPatternAnalyzer.solveCoefficients(memberList, requestedOutput);
                  if (coefficientResult.solved()) {
                     if (ClosedLoopPatternAnalyzer.validateStructure(memberList) == ClosedLoopPatternAnalyzer.StructureStatus.VALID) {
                        long[] coefficients = coefficientResult.coefficients();
                        ClosedLoopDiscoveryService.OrderedCandidate ordered = analyzeBestOrder(memberList, coefficients, requestedOutput);
                        if (ordered != null) {
                           result.add(new ClosedLoopDiscoveryService.ResolvedCandidate(ordered.members(), ordered.coefficients(), ordered.analysis()));
                        }
                     }
                  }
               }
            });
         }
      }

      return List.copyOf(result);
   }

   private static ClosedLoopDiscoveryService.ResolvedCandidate combineCandidates(
      ClosedLoopDiscoveryService.ResolvedCandidate base, ClosedLoopDiscoveryService.ResolvedCandidate supplier, AEKey requestedOutput
   ) {
      LinkedHashSet<IPatternDetails> members = new LinkedHashSet<>();
      members.addAll(base.members());
      int baseSize = members.size();
      members.addAll(supplier.members());
      if (members.size() != baseSize && members.size() <= 27) {
         List<IPatternDetails> memberList = List.copyOf(members);
         Result coefficientResult = ClosedLoopPatternAnalyzer.solveCoefficients(memberList, requestedOutput);
         if (coefficientResult.solved() && ClosedLoopPatternAnalyzer.validateStructure(memberList) == ClosedLoopPatternAnalyzer.StructureStatus.VALID) {
            ClosedLoopDiscoveryService.OrderedCandidate ordered = analyzeBestOrder(memberList, coefficientResult.coefficients(), requestedOutput);
            return ordered != null ? new ClosedLoopDiscoveryService.ResolvedCandidate(ordered.members(), ordered.coefficients(), ordered.analysis()) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void enumerateMemberSets(
      List<List<ClosedLoopDiscoveryService.PathOption>> optionsByInput,
      int index,
      LinkedHashSet<IPatternDetails> selected,
      boolean closes,
      int[] combinations,
      Consumer<LinkedHashSet<IPatternDetails>> sink
   ) {
      if (combinations[0] < 128) {
         if (index == optionsByInput.size()) {
            combinations[0]++;
            if (closes) {
               sink.accept(new LinkedHashSet<>(selected));
            }
         } else {
            for (ClosedLoopDiscoveryService.PathOption option : optionsByInput.get(index)) {
               ArrayList<IPatternDetails> added = new ArrayList<>();

               for (IPatternDetails member : option.members()) {
                  if (selected.add(member)) {
                     added.add(member);
                  }
               }

               if (selected.size() <= 27) {
                  enumerateMemberSets(optionsByInput, index + 1, selected, closes || option.closes(), combinations, sink);
               }

               for (int i = added.size() - 1; i >= 0; i--) {
                  selected.remove(added.get(i));
               }

               if (combinations[0] >= 128) {
                  return;
               }
            }
         }
      }
   }

   private static List<List<IPatternDetails>> pathsBackToAnchor(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor,
      Function<AEKey, ? extends Iterable<AEKey>> fuzzyCraftablesFor,
      ClosedLoopDiscoveryService.InputRequirement needed,
      List<ClosedLoopDiscoveryService.PatternOutput> anchors,
      Set<ClosedLoopDiscoveryService.InputIdentity> visiting,
      int depth
   ) {
      ClosedLoopDiscoveryService.InputIdentity identity = needed.identity();
      if (depth < 8 && visiting.add(identity)) {
         try {
            ArrayList<List<IPatternDetails>> result = new ArrayList<>();

            for (IPatternDetails pattern : patternsForRequirement(patternsFor, fuzzyCraftablesFor, needed)) {
               if (produces(pattern, needed)) {
                  List<ClosedLoopDiscoveryService.InputRequirement> inputs = inputRequirements(pattern);
                  if (inputs != null) {
                     if (inputs.stream().anyMatch(inputx -> acceptsAny(inputx, anchors)) || hasReturnedInput(pattern)) {
                        result.add(List.of(pattern));
                     }

                     for (ClosedLoopDiscoveryService.InputRequirement input : inputs) {
                        if (!acceptsAny(input, anchors)) {
                           for (List<IPatternDetails> tail : pathsBackToAnchor(patternsFor, fuzzyCraftablesFor, input, anchors, visiting, depth + 1)) {
                              ArrayList<IPatternDetails> path = new ArrayList<>(tail.size() + 1);
                              path.add(pattern);
                              path.addAll(tail);
                              result.add(List.copyOf(path));
                              if (result.size() >= 32) {
                                 return List.copyOf(result);
                              }
                           }
                        }
                     }
                  }
               }
            }

            return List.copyOf(result);
         } finally {
            visiting.remove(identity);
         }
      } else {
         return List.of();
      }
   }

   private static Iterable<IPatternDetails> patternsForRequirement(
      Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor,
      Function<AEKey, ? extends Iterable<AEKey>> fuzzyCraftablesFor,
      ClosedLoopDiscoveryService.InputRequirement requirement
   ) {
      LinkedHashSet<AEKey> lookupKeys = new LinkedHashSet<>(requirement.keys());
      if (requirement.fuzzy()) {
         for (AEKey template : requirement.keys()) {
            Iterable<AEKey> variants = (Iterable<AEKey>)fuzzyCraftablesFor.apply(template);
            if (variants != null) {
               for (AEKey variant : variants) {
                  if (variant != null && accepts(requirement, new ClosedLoopDiscoveryService.PatternOutput(variant, false))) {
                     lookupKeys.add(variant);
                  }
               }
            }
         }
      }

      LinkedHashSet<IPatternDetails> result = new LinkedHashSet<>();

      for (AEKey key : lookupKeys) {
         for (IPatternDetails pattern : safePatterns(patternsFor, key)) {
            if (pattern != null) {
               result.add(pattern);
            }
         }
      }

      return List.copyOf(result);
   }

   private static Iterable<IPatternDetails> safePatterns(Function<AEKey, ? extends Iterable<IPatternDetails>> patternsFor, AEKey key) {
      Iterable<IPatternDetails> patterns = (Iterable<IPatternDetails>)patternsFor.apply(key);
      return (Iterable<IPatternDetails>)(patterns != null ? patterns : List.of());
   }

   private static List<IPatternDetails> copyPatterns(Iterable<IPatternDetails> patterns) {
      if (patterns == null) {
         return List.of();
      } else {
         ArrayList<IPatternDetails> result = new ArrayList<>();

         for (IPatternDetails pattern : patterns) {
            if (pattern instanceof TianshuClosedLoopPatternDetails nested) {
               result.add(new ClosedLoopDiscoveryService.ClosedLoopMacroDiscoveryView(nested));
            } else if (pattern != null) {
               result.add(pattern);
            }
         }

         return List.copyOf(result);
      }
   }

   private static List<AEKey> copyKeys(Iterable<AEKey> keys) {
      if (keys == null) {
         return List.of();
      } else {
         LinkedHashSet<AEKey> result = new LinkedHashSet<>();

         for (AEKey key : keys) {
            if (key != null) {
               result.add(key);
            }
         }

         return List.copyOf(result);
      }
   }

   private static ClosedLoopDiscoveryService.OrderedCandidate analyzeBestOrder(List<IPatternDetails> members, long[] coefficients, AEKey output) {
      ArrayList<ClosedLoopPatternAnalyzer.Member> analyzed = new ArrayList<>(members.size());

      for (int i = 0; i < members.size(); i++) {
         analyzed.add(new ClosedLoopPatternAnalyzer.Member(members.get(i), coefficients[i]));
      }

      ClosedLoopPatternAnalyzer.OrderedAnalysis ordered = ClosedLoopPatternAnalyzer.analyzeBestOrder(analyzed, output);
      if (ordered == null) {
         return null;
      } else {
         ArrayList<IPatternDetails> orderedDetails = new ArrayList<>(ordered.members().size());
         long[] orderedCoefficients = new long[ordered.members().size()];

         for (int i = 0; i < ordered.members().size(); i++) {
            orderedDetails.add(ordered.members().get(i).details());
            orderedCoefficients[i] = ordered.members().get(i).copies();
         }

         return new ClosedLoopDiscoveryService.OrderedCandidate(List.copyOf(orderedDetails), orderedCoefficients, ordered.analysis());
      }
   }

   private static List<ClosedLoopDiscoveryService.InputRequirement> inputRequirements(IPatternDetails details) {
      if (details != null && !(details instanceof TianshuClosedLoopPatternDetails)) {
         IInput[] inputs = details.getInputs();
         OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails value
            ? value
            : null;
         ArrayList<ClosedLoopDiscoveryService.InputRequirement> result = new ArrayList<>(inputs.length);

         for (int slot = 0; slot < inputs.length; slot++) {
            IInput input = inputs[slot];
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0 || possible[0].what() == null) {
               return null;
            }

            LinkedHashSet<AEKey> keys = new LinkedHashSet<>();

            for (GenericStack candidate : possible) {
               if (candidate != null && candidate.what() != null) {
                  keys.add(candidate.what());
               }
            }

            if (keys.isEmpty()) {
               return null;
            }

            result.add(new ClosedLoopDiscoveryService.InputRequirement(possible, List.copyOf(keys), overload != null && overload.isFuzzyInput(slot)));
         }

         return List.copyOf(result);
      } else {
         return null;
      }
   }

   private static boolean hasReturnedInput(IPatternDetails details) {
      IInput[] inputs = details.getInputs();
      List<ClosedLoopDiscoveryService.InputRequirement> requirements = inputRequirements(details);
      if (requirements == null) {
         return false;
      } else {
         for (int slot = 0; slot < inputs.length; slot++) {
            IInput input = inputs[slot];
            GenericStack[] possible = input.getPossibleInputs();

            for (GenericStack candidate : possible) {
               if (candidate != null && candidate.what() != null) {
                  AEKey remaining = input.getRemainingKey(candidate.what());
                  if (remaining != null && accepts(requirements.get(slot), new ClosedLoopDiscoveryService.PatternOutput(remaining, false))) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private static boolean produces(IPatternDetails details, ClosedLoopDiscoveryService.InputRequirement requirement) {
      if (details == null) {
         return false;
      } else {
         for (ClosedLoopDiscoveryService.PatternOutput output : outputs(details)) {
            if (accepts(requirement, output)) {
               return true;
            }
         }

         return false;
      }
   }

   private static List<ClosedLoopDiscoveryService.PatternOutput> exactOutputs(IPatternDetails details, AEKey key) {
      ArrayList<ClosedLoopDiscoveryService.PatternOutput> result = new ArrayList<>();

      for (ClosedLoopDiscoveryService.PatternOutput output : outputs(details)) {
         if (key.equals(output.key())) {
            result.add(output);
         }
      }

      return List.copyOf(result);
   }

   private static List<ClosedLoopDiscoveryService.PatternOutput> outputs(IPatternDetails details) {
      if (details == null) {
         return List.of();
      } else {
         OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails value
            ? value
            : null;
         GenericStack[] outputs = details.getOutputs();
         ArrayList<ClosedLoopDiscoveryService.PatternOutput> result = new ArrayList<>(outputs.length);

         for (int slot = 0; slot < outputs.length; slot++) {
            GenericStack output = outputs[slot];
            if (output != null && output.what() != null && output.amount() > 0L) {
               result.add(new ClosedLoopDiscoveryService.PatternOutput(output.what(), overload != null && overload.isFuzzyOutput(slot)));
            }
         }

         return List.copyOf(result);
      }
   }

   private static boolean acceptsAny(ClosedLoopDiscoveryService.InputRequirement requirement, List<ClosedLoopDiscoveryService.PatternOutput> outputs) {
      for (ClosedLoopDiscoveryService.PatternOutput output : outputs) {
         if (accepts(requirement, output)) {
            return true;
         }
      }

      return false;
   }

   private static boolean accepts(ClosedLoopDiscoveryService.InputRequirement requirement, ClosedLoopDiscoveryService.PatternOutput output) {
      return ClosedLoopPatternAnalyzer.acceptsLoopOutput(requirement.possibleInputs(), requirement.fuzzy(), output.key(), output.fuzzy());
   }

   private ClosedLoopDiscoveryService() {
   }

   private static final class ClosedLoopMacroDiscoveryView implements IPatternDetails {
      private final TianshuClosedLoopPatternDetails delegate;
      private final IInput[] inputs;

      private ClosedLoopMacroDiscoveryView(TianshuClosedLoopPatternDetails delegate) {
         this.delegate = Objects.requireNonNull(delegate, "delegate");
         ClosedLoopPatternPayload payload = Objects.requireNonNull(delegate.closedLoopPayload(), "closedLoopPayload");
         ArrayList<IInput> result = new ArrayList<>(payload.seeds().size() + payload.externalInputs().size());

         for (GenericStack seed : payload.seeds()) {
            result.add(new ClosedLoopDiscoveryService.MacroInput(seed.what(), seed.amount(), true));
         }

         for (GenericStack external : payload.externalInputs()) {
            result.add(new ClosedLoopDiscoveryService.MacroInput(external.what(), external.amount(), false));
         }

         this.inputs = result.toArray(IInput[]::new);
      }

      public AEItemKey getDefinition() {
         return this.delegate.getDefinition();
      }

      public IInput[] getInputs() {
         return (IInput[])this.inputs.clone();
      }

      public GenericStack[] getOutputs() {
         return this.delegate.closedLoopPayload().netOutputs().toArray(new GenericStack[0]);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof ClosedLoopDiscoveryService.ClosedLoopMacroDiscoveryView other && this.delegate.equals(other.delegate)) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode() {
         return this.delegate.hashCode();
      }
   }

   public static record DiscoveryResult(List<ClosedLoopDiscoveryCandidate> candidates, boolean rejectedUndecodablePattern) {
      public DiscoveryResult(List<ClosedLoopDiscoveryCandidate> candidates, boolean rejectedUndecodablePattern) {
         candidates = List.copyOf(candidates);
         this.candidates = candidates;
         this.rejectedUndecodablePattern = rejectedUndecodablePattern;
      }
   }

   private static record InputIdentity(List<AEKey> keys, boolean fuzzy) {
      private InputIdentity(List<AEKey> keys, boolean fuzzy) {
         keys = List.copyOf(keys);
         this.keys = keys;
         this.fuzzy = fuzzy;
      }
   }

   private static final class InputRequirement {
      private final GenericStack[] possibleInputs;
      private final List<AEKey> keys;
      private final boolean fuzzy;

      private InputRequirement(GenericStack[] possibleInputs, List<AEKey> keys, boolean fuzzy) {
         this.possibleInputs = (GenericStack[])possibleInputs.clone();
         this.keys = List.copyOf(keys);
         this.fuzzy = fuzzy;
      }

      private GenericStack[] possibleInputs() {
         return this.possibleInputs;
      }

      private List<AEKey> keys() {
         return this.keys;
      }

      private boolean fuzzy() {
         return this.fuzzy;
      }

      private ClosedLoopDiscoveryService.InputIdentity identity() {
         return new ClosedLoopDiscoveryService.InputIdentity(this.keys, this.fuzzy);
      }
   }

   private static record MacroInput(AEKey key, long amount, boolean reusable) implements IInput {
      private MacroInput(AEKey key, long amount, boolean reusable) {
         Objects.requireNonNull(key, "key");
         if (amount < 1L) {
            throw new IllegalArgumentException("macro input must be positive");
         } else {
            this.key = key;
            this.amount = amount;
            this.reusable = reusable;
         }
      }

      public GenericStack[] getPossibleInputs() {
         return new GenericStack[]{new GenericStack(this.key, 1L)};
      }

      public long getMultiplier() {
         return this.amount;
      }

      public boolean isValid(AEKey candidate, Level level) {
         return this.key.equals(candidate);
      }

      public AEKey getRemainingKey(AEKey template) {
         return this.reusable && this.key.equals(template) ? this.key : null;
      }
   }

   private static record OrderedCandidate(List<IPatternDetails> members, long[] coefficients, ClosedLoopAnalysis analysis) {
   }

   private static record PathOption(List<IPatternDetails> members, boolean closes) {
   }

   private static record PatternOutput(AEKey key, boolean fuzzy) {
   }

   static record ResolvedCandidate(List<IPatternDetails> members, long[] coefficients, ClosedLoopAnalysis analysis) {
   }
}
