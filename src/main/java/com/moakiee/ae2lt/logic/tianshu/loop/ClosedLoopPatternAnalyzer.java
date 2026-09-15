package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import com.moakiee.thunderbolt.core.crafting.planner.PositiveIntegerLinearSolver;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import com.moakiee.thunderbolt.core.crafting.planner.PositiveIntegerLinearSolver.Constraint;
import com.moakiee.thunderbolt.core.crafting.planner.PositiveIntegerLinearSolver.Result;
import com.moakiee.thunderbolt.core.crafting.planner.PositiveIntegerLinearSolver.Status;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class ClosedLoopPatternAnalyzer {
   public static final int MAX_MEMBERS = 27;

   public static Result solveCoefficients(List<IPatternDetails> members, AEKey requestedOutput) {
      if (members != null && !members.isEmpty() && requestedOutput != null) {
         List<ClosedLoopPatternAnalyzer.LoopOutput> possibleLoopOutputs = possibleLoopOutputs(members);
         if (possibleLoopOutputs == null) {
            return solverResult(Status.INVALID_INPUT);
         } else {
            ArrayList<ClosedLoopPatternAnalyzer.Balance> balances = new ArrayList<>(members.size());
            LinkedHashMap<AEKey, Long> consumed = new LinkedHashMap<>();
            LinkedHashMap<AEKey, Long> produced = new LinkedHashMap<>();

            for (IPatternDetails member : members) {
               ClosedLoopPatternAnalyzer.Balance balance = balance(member, possibleLoopOutputs);
               if (balance == null) {
                  return solverResult(Status.INVALID_INPUT);
               }

               balances.add(balance);
               mergeScaled(consumed, balance.consumed(), 1L);
               mergeScaled(produced, balance.produced(), 1L);
            }

            LinkedHashSet<AEKey> cycleKeys = new LinkedHashSet<>();

            for (AEKey key : consumed.keySet()) {
               if (produced.getOrDefault(key, 0L) > 0L) {
                  cycleKeys.add(key);
               }
            }

            if (cycleKeys.isEmpty()) {
               return solverResult(Status.INFEASIBLE);
            } else {
               ArrayList<Constraint> constraints = new ArrayList<>(cycleKeys.size() + 1);

               for (AEKey keyx : cycleKeys) {
                  constraints.add(new Constraint(netRow(balances, keyx), 0L));
               }

               constraints.add(new Constraint(netRow(balances, requestedOutput), 1L));
               Result solved = PositiveIntegerLinearSolver.solve(members.size(), constraints);
               if (!solved.solved()) {
                  return solved;
               } else {
                  ArrayList<ClosedLoopPatternAnalyzer.Member> analyzed = new ArrayList<>(members.size());
                  long[] coefficients = normalizeIntegerRatio(solved.coefficients());
                  if (coefficients == null) {
                     return solverResult(Status.INTERNAL_ERROR);
                  } else {
                     for (int i = 0; i < members.size(); i++) {
                        analyzed.add(new ClosedLoopPatternAnalyzer.Member(members.get(i), coefficients[i]));
                     }

                     return analyze(analyzed, requestedOutput) != null ? new Result(Status.SOLVED, coefficients) : solverResult(Status.INTERNAL_ERROR);
                  }
               }
            }
         }
      } else {
         return solverResult(Status.INVALID_INPUT);
      }
   }

   public static ClosedLoopPatternAnalyzer.StructureStatus validateStructure(List<IPatternDetails> members) {
      if (members != null && !members.isEmpty() && members.size() <= 27) {
         return prepareBalances(members) != null ? ClosedLoopPatternAnalyzer.StructureStatus.VALID : ClosedLoopPatternAnalyzer.StructureStatus.INVALID;
      } else {
         return ClosedLoopPatternAnalyzer.StructureStatus.INVALID;
      }
   }

   public static boolean isMinimalIntegerRatio(long[] copiesPerCycle) {
      if (copiesPerCycle != null && copiesPerCycle.length != 0) {
         long divisor = 0L;

         for (long copies : copiesPerCycle) {
            if (copies <= 0L) {
               return false;
            }

            divisor = gcd(divisor, copies);
         }

         return divisor == 1L;
      } else {
         return false;
      }
   }

   private static long[] normalizeIntegerRatio(long[] copiesPerCycle) {
      if (copiesPerCycle != null && copiesPerCycle.length != 0) {
         long divisor = 0L;

         for (long copies : copiesPerCycle) {
            if (copies <= 0L) {
               return null;
            }

            divisor = gcd(divisor, copies);
         }

         if (divisor <= 0L) {
            return null;
         } else {
            long[] normalized = (long[])copiesPerCycle.clone();
            if (divisor > 1L) {
               for (int i = 0; i < normalized.length; i++) {
                  normalized[i] /= divisor;
               }
            }

            return normalized;
         }
      } else {
         return null;
      }
   }

   private static long gcd(long left, long right) {
      while (right != 0L) {
         long remainder = left % right;
         left = right;
         right = remainder;
      }

      return left;
   }

   public static boolean isPrimitiveCycle(List<ClosedLoopPatternAnalyzer.Member> members) {
      if (members != null && !members.isEmpty()) {
         long[] copies = new long[members.size()];

         for (int i = 0; i < members.size(); i++) {
            ClosedLoopPatternAnalyzer.Member member = members.get(i);
            if (member == null || member.details() == null || member.copies() <= 0L) {
               return false;
            }

            copies[i] = member.copies();
         }

         return isMinimalIntegerRatio(copies);
      } else {
         return false;
      }
   }

   public static ClosedLoopAnalysis analyze(List<ClosedLoopPatternAnalyzer.Member> members, AEKey requestedOutput) {
      if (members == null || members.isEmpty() || requestedOutput == null) {
         return null;
      } else if (!isPrimitiveCycle(members)) {
         return null;
      } else {
         LinkedHashMap<AEKey, Long> consumed = new LinkedHashMap<>();
         LinkedHashMap<AEKey, Long> produced = new LinkedHashMap<>();
         ArrayList<ClosedLoopPatternAnalyzer.Balance> perMember = new ArrayList<>(members.size());
         ArrayList<IPatternDetails> details = new ArrayList<>(members.size());

         for (ClosedLoopPatternAnalyzer.Member member : members) {
            if (member == null || member.details() == null) {
               return null;
            }

            details.add(member.details());
         }

         List<ClosedLoopPatternAnalyzer.LoopOutput> possibleLoopOutputs = possibleLoopOutputs(details);
         if (possibleLoopOutputs == null) {
            return null;
         } else {
            for (ClosedLoopPatternAnalyzer.Member member : members) {
               ClosedLoopPatternAnalyzer.Balance balance = balance(member.details(), possibleLoopOutputs);
               if (balance == null) {
                  return null;
               }

               perMember.add(balance);
               mergeScaled(consumed, balance.consumed(), member.copies());
               mergeScaled(produced, balance.produced(), member.copies());
            }

            LinkedHashSet<AEKey> cycleKeys = new LinkedHashSet<>();

            for (AEKey key : consumed.keySet()) {
               if (produced.getOrDefault(key, 0L) > 0L) {
                  cycleKeys.add(key);
               }
            }

            if (cycleKeys.isEmpty()) {
               return null;
            } else {
               LinkedHashMap<AEKey, Long> net = new LinkedHashMap<>();

               for (Entry<AEKey, Long> entry : produced.entrySet()) {
                  net.put(entry.getKey(), Long.valueOf(entry.getValue() - consumed.getOrDefault(entry.getKey(), 0L)));
               }

               for (Entry<AEKey, Long> entry : consumed.entrySet()) {
                  net.putIfAbsent(entry.getKey(), Long.valueOf(-entry.getValue()));
               }

               if (net.getOrDefault(requestedOutput, 0L) <= 0L) {
                  return null;
               } else {
                  for (AEKey keyx : cycleKeys) {
                     if (net.getOrDefault(keyx, 0L) < 0L) {
                        return null;
                     }
                  }

                  HashMap<AEKey, Long> balances = new HashMap<>();
                  LinkedHashMap<AEKey, Long> seedAmounts = new LinkedHashMap<>();

                  for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                     ClosedLoopPatternAnalyzer.Member member = members.get(memberIndex);
                     ClosedLoopPatternAnalyzer.Balance balance = perMember.get(memberIndex);

                     for (AEKey keyxx : cycleKeys) {
                        long consumedByGroup = Sat.mul(balance.consumed().getOrDefault(keyxx, 0L), member.copies());
                        long producedByGroup = Sat.mul(balance.produced().getOrDefault(keyxx, 0L), member.copies());
                        long held = balances.getOrDefault(keyxx, 0L);
                        long deficit = Math.max(0L, consumedByGroup - held);
                        if (deficit > 0L) {
                           seedAmounts.merge(keyxx, Long.valueOf(deficit), Sat::add);
                        }

                        long afterConsumption = Math.max(held, consumedByGroup) - consumedByGroup;
                        balances.put(keyxx, Sat.add(afterConsumption, producedByGroup));
                     }
                  }

                  if (seedAmounts.isEmpty()) {
                     return null;
                  } else {
                     List<GenericStack> seeds = toStacks(seedAmounts);
                     LinkedHashMap<AEKey, Long> external = new LinkedHashMap<>();
                     LinkedHashMap<AEKey, Long> outputs = new LinkedHashMap<>();

                     for (Entry<AEKey, Long> entry : net.entrySet()) {
                        if (entry.getValue() < 0L && !cycleKeys.contains(entry.getKey())) {
                           external.put(entry.getKey(), Long.valueOf(-entry.getValue()));
                        } else if (entry.getValue() > 0L) {
                           outputs.put(entry.getKey(), entry.getValue());
                        }
                     }

                     return new ClosedLoopAnalysis(seeds, toStacks(external), toStacks(outputs));
                  }
               }
            }
         }
      }
   }

   public static ClosedLoopPatternAnalyzer.OrderedAnalysis analyzeBestOrder(List<ClosedLoopPatternAnalyzer.Member> members, AEKey requestedOutput) {
      if (members != null && !members.isEmpty() && members.size() <= 27) {
         List<ClosedLoopPatternAnalyzer.Member> source = List.copyOf(members);
         ClosedLoopPatternAnalyzer.OrderingModel model = ClosedLoopPatternAnalyzer.OrderingModel.create(source);
         if (model == null) {
            return null;
         } else {
            ClosedLoopPatternAnalyzer.BestOrder best = new ClosedLoopPatternAnalyzer.BestOrder();
            considerOrder(source, requestedOutput, best);

            for (int first = 0; first < source.size(); first++) {
               considerOrder(greedyOrder(model, first), requestedOutput, best);
            }

            return best.analysis;
         }
      } else {
         return null;
      }
   }

   public static List<ClosedLoopPatternAnalyzer.MemberFlow> deriveMemberFlows(List<ClosedLoopPatternAnalyzer.Member> members, List<GenericStack> seeds) {
      if (members != null && !members.isEmpty() && seeds != null && !seeds.isEmpty() && isPrimitiveCycle(members)) {
         ArrayList<IPatternDetails> details = new ArrayList<>(members.size());

         for (ClosedLoopPatternAnalyzer.Member member : members) {
            if (member == null || member.details() == null) {
               return List.of();
            }

            details.add(member.details());
         }

         List<ClosedLoopPatternAnalyzer.LoopOutput> possibleOutputs = possibleLoopOutputs(details);
         if (possibleOutputs == null) {
            return List.of();
         } else {
            ArrayList<ClosedLoopPatternAnalyzer.Balance> scaled = new ArrayList<>(members.size());
            LinkedHashMap<AEKey, Long> totalConsumed = new LinkedHashMap<>();
            LinkedHashMap<AEKey, Long> totalProduced = new LinkedHashMap<>();

            for (ClosedLoopPatternAnalyzer.Member member : members) {
               ClosedLoopPatternAnalyzer.Balance perCopy = balance(member.details(), possibleOutputs);
               if (perCopy == null) {
                  return List.of();
               }

               Map<AEKey, Long> consumed = scaledCopy(perCopy.consumed(), member.copies());
               Map<AEKey, Long> produced = scaledCopy(perCopy.produced(), member.copies());
               scaled.add(new ClosedLoopPatternAnalyzer.Balance(consumed, produced, perCopy.inputSeedBySlot()));
               mergeScaled(totalConsumed, consumed, 1L);
               mergeScaled(totalProduced, produced, 1L);
            }

            LinkedHashSet<AEKey> cycleKeys = new LinkedHashSet<>();

            for (AEKey key : totalConsumed.keySet()) {
               if (totalProduced.getOrDefault(key, 0L) > 0L) {
                  cycleKeys.add(key);
               }
            }

            LinkedHashMap<AEKey, Long> seedAmounts = new LinkedHashMap<>();

            for (GenericStack seed : seeds) {
               if (seed != null && seed.what() != null && seed.amount() > 0L) {
                  seedAmounts.merge(seed.what(), Long.valueOf(seed.amount()), Sat::add);
               }
            }

            if (!cycleKeys.containsAll(seedAmounts.keySet())) {
               return List.of();
            } else {
               ArrayList<Map<AEKey, Long>> required = new ArrayList<>(Collections.nCopies(members.size() + 1, Map.of()));
               required.set(members.size(), Map.copyOf(seedAmounts));

               for (int memberIndex = members.size() - 1; memberIndex >= 0; memberIndex--) {
                  ClosedLoopPatternAnalyzer.Balance balance = scaled.get(memberIndex);
                  Map<AEKey, Long> after = required.get(memberIndex + 1);
                  LinkedHashMap<AEKey, Long> before = new LinkedHashMap<>();

                  for (AEKey keyx : cycleKeys) {
                     long consumed = balance.consumed().getOrDefault(keyx, 0L);
                     long produced = balance.produced().getOrDefault(keyx, 0L);
                     long retainedAfter = after.getOrDefault(keyx, 0L);
                     long neededBefore = Sat.add(consumed, Math.max(0L, retainedAfter - produced));
                     if (neededBefore > 0L) {
                        before.put(keyx, Long.valueOf(neededBefore));
                     }
                  }

                  required.set(memberIndex, Map.copyOf(before));
               }

               if (!required.get(0).equals(seedAmounts)) {
                  return List.of();
               } else {
                  ArrayList<ClosedLoopPatternAnalyzer.MemberFlow> result = new ArrayList<>(members.size());

                  for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                     ClosedLoopPatternAnalyzer.Balance balance = scaled.get(memberIndex);
                     Map<AEKey, Long> before = required.get(memberIndex);
                     Map<AEKey, Long> after = required.get(memberIndex + 1);
                     LinkedHashMap<AEKey, Long> inputSeed = new LinkedHashMap<>();
                     LinkedHashMap<AEKey, Long> outputSeed = new LinkedHashMap<>();

                     for (Entry<AEKey, Long> entry : balance.consumed().entrySet()) {
                        if (cycleKeys.contains(entry.getKey()) && entry.getValue() > 0L) {
                           inputSeed.put(entry.getKey(), entry.getValue());
                        }
                     }

                     if (inputSeed.isEmpty()) {
                        return List.of();
                     }

                     for (Entry<AEKey, Long> entryx : balance.produced().entrySet()) {
                        AEKey keyxx = entryx.getKey();
                        long produced = entryx.getValue();
                        long seedOutput = 0L;
                        if (cycleKeys.contains(keyxx)) {
                           long carried = Math.max(0L, before.getOrDefault(keyxx, 0L) - balance.consumed().getOrDefault(keyxx, 0L));
                           seedOutput = Math.min(produced, Math.max(0L, after.getOrDefault(keyxx, 0L) - carried));
                           if (seedOutput > 0L) {
                              outputSeed.put(keyxx, Long.valueOf(seedOutput));
                           }
                        }
                     }

                     LinkedHashMap<Integer, AEKey> inputSeedBySlot = new LinkedHashMap<>();

                     for (Entry<Integer, AEKey> entryxx : balance.inputSeedBySlot().entrySet()) {
                        if (cycleKeys.contains(entryxx.getValue())) {
                           inputSeedBySlot.put(entryxx.getKey(), entryxx.getValue());
                        }
                     }

                     result.add(new ClosedLoopPatternAnalyzer.MemberFlow(Map.copyOf(inputSeed), Map.copyOf(outputSeed), Map.copyOf(inputSeedBySlot)));
                  }

                  List<ClosedLoopPatternAnalyzer.MemberFlow> immutable = List.copyOf(result);
                  return hasSafeDynamicSeedRouting(members, immutable) ? immutable : List.of();
               }
            }
         }
      } else {
         return List.of();
      }
   }

   static boolean hasSafeDynamicSeedRouting(List<ClosedLoopPatternAnalyzer.Member> members, List<ClosedLoopPatternAnalyzer.MemberFlow> flows) {
      if (members != null && flows != null && members.size() == flows.size()) {
         LinkedHashMap<AEKey, ClosedLoopPatternAnalyzer.DynamicSeedDomain> requiredDomains = new LinkedHashMap<>();

         try {
            for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
               ClosedLoopPatternAnalyzer.Member member = members.get(memberIndex);
               IPatternDetails details = member.details();
               ClosedLoopPatternAnalyzer.MemberFlow flow = flows.get(memberIndex);
               OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails candidate
                  ? candidate
                  : null;
               LinkedHashMap<AEKey, Long> exactCapacity = new LinkedHashMap<>();
               LinkedHashMap<AEKey, ClosedLoopPatternAnalyzer.DynamicSeedDomain> dynamicCapacity = new LinkedHashMap<>();
               GenericStack[] outputs = details.getOutputs();

               for (int slot = 0; slot < outputs.length; slot++) {
                  GenericStack output = outputs[slot];
                  long amount = Sat.mul(output.amount(), member.copies());
                  if (output.what() == null || amount <= 0L) {
                     return false;
                  }

                  if (overload != null && overload.isFuzzyOutput(slot)) {
                     dynamicCapacity.computeIfAbsent(output.what(), ignoredx -> new ClosedLoopPatternAnalyzer.DynamicSeedDomain()).markFuzzy(true);
                  } else {
                     exactCapacity.merge(output.what(), Long.valueOf(amount), Sat::add);
                  }
               }

               IInput[] executionInputs = ClosedLoopExpandedPatternDetails.pinReusableSeedInputs(details, flow.inputSeedBySlot());

               for (Entry<Integer, AEKey> mapped : flow.inputSeedBySlot().entrySet()) {
                  int slot = mapped.getKey();
                  if (slot < 0 || slot >= executionInputs.length) {
                     return false;
                  }

                  IInput input = executionInputs[slot];
                  AEKey selected = mapped.getValue();
                  AEKey plannedRemainder = input.getRemainingKey(selected);
                  if (plannedRemainder != null) {
                     long amountx = Sat.mul(input.getMultiplier(), member.copies());
                     if (amountx <= 0L) {
                        return false;
                     }

                     ClosedLoopPatternAnalyzer.DynamicSeedDomain domain = new ClosedLoopPatternAnalyzer.DynamicSeedDomain();
                     boolean dynamic = overload != null && overload.isFuzzyInput(slot) && plannedRemainder.dropSecondary().equals(selected.dropSecondary());
                     if (dynamic) {
                        domain.markFuzzy(false);
                     }

                     for (GenericStack possible : input.getPossibleInputs()) {
                        if (possible.what() == null) {
                           return false;
                        }

                        AEKey actualRemainder = input.getRemainingKey(possible.what());
                        if (actualRemainder == null) {
                           return false;
                        }

                        if (!plannedRemainder.equals(actualRemainder)) {
                           dynamic = true;
                           domain.addVariant(actualRemainder);
                        }
                     }

                     if (dynamic) {
                        dynamicCapacity.merge(plannedRemainder, domain, ClosedLoopPatternAnalyzer.DynamicSeedDomain::merge);
                     } else {
                        exactCapacity.merge(plannedRemainder, Long.valueOf(amountx), Sat::add);
                     }
                  }
               }

               for (Entry<AEKey, Long> outputx : flow.outputSeed().entrySet()) {
                  long exact = exactCapacity.getOrDefault(outputx.getKey(), 0L);
                  if (outputx.getValue() > exact) {
                     ClosedLoopPatternAnalyzer.DynamicSeedDomain domainx = dynamicCapacity.get(outputx.getKey());
                     if (domainx == null) {
                        return false;
                     }

                     requiredDomains.merge(outputx.getKey(), domainx.copy(), ClosedLoopPatternAnalyzer.DynamicSeedDomain::merge);
                  }
               }
            }

            for (Entry<AEKey, ClosedLoopPatternAnalyzer.DynamicSeedDomain> required : requiredDomains.entrySet()) {
               AEKey planned = required.getKey();
               ClosedLoopPatternAnalyzer.DynamicSeedDomain domainx = required.getValue();

               for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                  ClosedLoopPatternAnalyzer.MemberFlow flow = flows.get(memberIndex);
                  if (flow.inputSeed().getOrDefault(planned, 0L) > 0L) {
                     IPatternDetails details = members.get(memberIndex).details();
                     OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails candidate
                        ? candidate
                        : null;
                     IInput[] inputs = ClosedLoopExpandedPatternDetails.pinReusableSeedInputs(details, flow.inputSeedBySlot());
                     long bundleUnits = 0L;
                     boolean mapped = false;

                     for (Entry<Integer, AEKey> seedSlot : flow.inputSeedBySlot().entrySet()) {
                        if (planned.equals(seedSlot.getValue())) {
                           mapped = true;
                           int slotx = seedSlot.getKey();
                           boolean fuzzy = overload != null && overload.isFuzzyInput(slotx);
                           if (domainx.fuzzy && !fuzzy) {
                              return false;
                           }

                           GenericStack[] possible = inputs[slotx].getPossibleInputs();
                           long selectedAmount = 0L;

                           for (GenericStack candidatex : possible) {
                              if (candidatex.what() == null) {
                                 return false;
                              }

                              boolean selected = planned.equals(candidatex.what())
                                 || fuzzy && planned.dropSecondary().equals(candidatex.what().dropSecondary());
                              if (selected && selectedAmount == 0L) {
                                 selectedAmount = candidatex.amount();
                              }
                           }

                           if (selectedAmount <= 0L) {
                              return false;
                           }

                           for (AEKey variant : domainx.variants) {
                              boolean accepted = fuzzy
                                 ? planned.dropSecondary().equals(variant.dropSecondary())
                                 : Arrays.stream(possible).anyMatch(candidatex -> variant.equals(candidatex.what()));
                              if (!accepted) {
                                 return false;
                              }
                           }

                           bundleUnits = Sat.add(bundleUnits, Sat.mul(selectedAmount, inputs[slotx].getMultiplier()));
                        }
                     }

                     if (!mapped || bundleUnits <= 0L) {
                        return false;
                     }

                     if (bundleUnits > 1L) {
                        return false;
                     }
                  }
               }
            }

            return true;
         } catch (RuntimeException var28) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean hasInputSeedPerMember(List<ClosedLoopPatternAnalyzer.Member> members, List<GenericStack> seeds) {
      if (members != null && !members.isEmpty()) {
         List<ClosedLoopPatternAnalyzer.MemberFlow> flows = deriveMemberFlows(members, seeds);
         if (flows.size() != members.size()) {
            return false;
         } else {
            for (ClosedLoopPatternAnalyzer.MemberFlow flow : flows) {
               if (positiveInputSeedTypes(flow) < 1) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   static boolean hasSingleSeedInputPerMember(List<ClosedLoopPatternAnalyzer.MemberFlow> flows) {
      if (flows != null && !flows.isEmpty()) {
         for (ClosedLoopPatternAnalyzer.MemberFlow flow : flows) {
            if (positiveInputSeedTypes(flow) != 1) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static int positiveInputSeedTypes(ClosedLoopPatternAnalyzer.MemberFlow flow) {
      if (flow == null) {
         return 0;
      } else {
         int positiveTypes = 0;

         for (Long amount : flow.inputSeed().values()) {
            if (amount != null && amount > 0L) {
               positiveTypes++;
            }
         }

         return positiveTypes;
      }
   }

   private static void considerOrder(List<ClosedLoopPatternAnalyzer.Member> members, AEKey requestedOutput, ClosedLoopPatternAnalyzer.BestOrder best) {
      if (members != null && !members.isEmpty()) {
         ClosedLoopAnalysis analysis = analyze(members, requestedOutput);
         if (analysis != null && hasInputSeedPerMember(members, analysis.seeds())) {
            long seedTotal = 0L;

            for (GenericStack seed : analysis.seeds()) {
               seedTotal = Sat.add(seedTotal, seed.amount());
            }

            if (best.analysis == null || seedTotal < best.seedTotal) {
               best.analysis = new ClosedLoopPatternAnalyzer.OrderedAnalysis(members, analysis);
               best.seedTotal = seedTotal;
            }
         }
      }
   }

   private static List<ClosedLoopPatternAnalyzer.Member> greedyOrder(ClosedLoopPatternAnalyzer.OrderingModel model, int forcedFirst) {
      int memberCount = model.members.size();
      ArrayList<ClosedLoopPatternAnalyzer.Member> ordered = new ArrayList<>(memberCount);
      boolean[] used = new boolean[memberCount];
      LinkedHashMap<AEKey, Long> held = new LinkedHashMap<>();
      appendGreedyMember(model, forcedFirst, ordered, used, held);

      while (ordered.size() < memberCount) {
         int selected = -1;
         long selectedDeficit = Long.MAX_VALUE;

         for (int candidate = 0; candidate < memberCount; candidate++) {
            if (!used[candidate]) {
               long deficit = incrementalSeedDeficit(model.scaledBalances.get(candidate), model.cycleKeys, held);
               if (selected < 0 || deficit < selectedDeficit) {
                  selected = candidate;
                  selectedDeficit = deficit;
               }
            }
         }

         appendGreedyMember(model, selected, ordered, used, held);
      }

      return List.copyOf(ordered);
   }

   private static void appendGreedyMember(
      ClosedLoopPatternAnalyzer.OrderingModel model, int memberIndex, List<ClosedLoopPatternAnalyzer.Member> ordered, boolean[] used, Map<AEKey, Long> held
   ) {
      if (memberIndex >= 0 && memberIndex < model.members.size() && !used[memberIndex]) {
         used[memberIndex] = true;
         ordered.add(model.members.get(memberIndex));
         ClosedLoopPatternAnalyzer.Balance balance = model.scaledBalances.get(memberIndex);

         for (AEKey key : model.cycleKeys) {
            long consumed = balance.consumed().getOrDefault(key, 0L);
            long produced = balance.produced().getOrDefault(key, 0L);
            long available = held.getOrDefault(key, 0L);
            long afterConsumption = Math.max(available, consumed) - consumed;
            held.put(key, Sat.add(afterConsumption, produced));
         }
      } else {
         throw new IllegalArgumentException("invalid greedy member index");
      }
   }

   private static long incrementalSeedDeficit(ClosedLoopPatternAnalyzer.Balance balance, Set<AEKey> cycleKeys, Map<AEKey, Long> held) {
      long result = 0L;

      for (AEKey key : cycleKeys) {
         long consumed = balance.consumed().getOrDefault(key, 0L);
         long deficit = Math.max(0L, consumed - held.getOrDefault(key, 0L));
         result = Sat.add(result, deficit);
      }

      return result;
   }

   private static ClosedLoopPatternAnalyzer.Balance balance(IPatternDetails details, List<ClosedLoopPatternAnalyzer.LoopOutput> possibleLoopOutputs) {
      if (details != null && !(details instanceof TianshuClosedLoopPatternDetails)) {
         LinkedHashMap<AEKey, Long> consumed = new LinkedHashMap<>();
         LinkedHashMap<AEKey, Long> produced = new LinkedHashMap<>();
         LinkedHashMap<Integer, AEKey> inputSeedBySlot = new LinkedHashMap<>();
         IInput[] inputs = details.getInputs();
         OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(details) instanceof OverloadedProviderOnlyPatternDetails value
            ? value
            : null;

         for (int slot = 0; slot < inputs.length; slot++) {
            IInput input = inputs[slot];
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0 || possible[0].what() == null) {
               return null;
            }

            ClosedLoopPatternAnalyzer.LoopMatch loopMatch = matchingLoopOutput(possible, possibleLoopOutputs, overload != null && overload.isFuzzyInput(slot));
            if (loopMatch != null && loopMatch.forbidden()) {
               return null;
            }

            AEKey concrete = loopMatch != null ? loopMatch.key() : null;
            if (concrete == null) {
               concrete = possible[0].what();
            }

            long templateAmount = possible[0].amount();
            boolean fuzzySlot = overload != null && overload.isFuzzyInput(slot);
            if (fuzzySlot) {
               Long matchedAmount = null;

               for (GenericStack candidate : possible) {
                  if (candidate.what() != null && concrete.dropSecondary().equals(candidate.what().dropSecondary())) {
                     if (matchedAmount != null && matchedAmount != candidate.amount()) {
                        return null;
                     }

                     matchedAmount = candidate.amount();
                  }
               }

               if (matchedAmount == null) {
                  return null;
               }

               templateAmount = matchedAmount;
            } else {
               for (GenericStack candidatex : possible) {
                  if (concrete.equals(candidatex.what())) {
                     templateAmount = candidatex.amount();
                     break;
                  }
               }
            }

            long amount = Sat.mul(templateAmount, input.getMultiplier());
            if (amount <= 0L) {
               return null;
            }

            consumed.merge(concrete, Long.valueOf(amount), Sat::add);
            inputSeedBySlot.put(Integer.valueOf(slot), concrete);
            AEKey remaining = input.getRemainingKey(concrete);
            if (remaining != null) {
               produced.merge(remaining, Long.valueOf(input.getMultiplier()), Sat::add);
            }
         }

         for (GenericStack output : details.getOutputs()) {
            if (output.what() == null || output.amount() <= 0L) {
               return null;
            }

            produced.merge(output.what(), Long.valueOf(output.amount()), Sat::add);
         }

         return new ClosedLoopPatternAnalyzer.Balance(consumed, produced, Map.copyOf(inputSeedBySlot));
      } else {
         return null;
      }
   }

   private static List<ClosedLoopPatternAnalyzer.LoopOutput> possibleLoopOutputs(List<IPatternDetails> members) {
      ArrayList<ClosedLoopPatternAnalyzer.LoopOutput> result = new ArrayList<>();

      for (IPatternDetails member : members) {
         if (member == null) {
            return null;
         }

         OverloadedProviderOnlyPatternDetails overload = CraftingPatternDelegates.forProviderLookup(member) instanceof OverloadedProviderOnlyPatternDetails value
            ? value
            : null;
         GenericStack[] outputs = member.getOutputs();

         for (int slot = 0; slot < outputs.length; slot++) {
            GenericStack output = outputs[slot];
            if (output == null || output.what() == null || output.amount() <= 0L) {
               return null;
            }

            result.add(new ClosedLoopPatternAnalyzer.LoopOutput(output.what(), overload != null && overload.isFuzzyOutput(slot)));
         }
      }

      return List.copyOf(result);
   }

   private static List<ClosedLoopPatternAnalyzer.Balance> prepareBalances(List<IPatternDetails> members) {
      List<ClosedLoopPatternAnalyzer.LoopOutput> outputs = possibleLoopOutputs(members);
      if (outputs == null) {
         return null;
      } else {
         ArrayList<ClosedLoopPatternAnalyzer.Balance> result = new ArrayList<>(members.size());

         for (IPatternDetails member : members) {
            ClosedLoopPatternAnalyzer.Balance balance = balance(member, outputs);
            if (balance == null) {
               return null;
            }

            result.add(balance);
         }

         return List.copyOf(result);
      }
   }

   private static long[] netRow(List<ClosedLoopPatternAnalyzer.Balance> balances, AEKey key) {
      long[] result = new long[balances.size()];

      for (int i = 0; i < balances.size(); i++) {
         ClosedLoopPatternAnalyzer.Balance balance = balances.get(i);
         result[i] = balance.produced().getOrDefault(key, 0L) - balance.consumed().getOrDefault(key, 0L);
      }

      return result;
   }

   private static Result solverResult(Status status) {
      return new Result(status, new long[0]);
   }

   private static ClosedLoopPatternAnalyzer.LoopMatch matchingLoopOutput(
      GenericStack[] possibleInputs, List<ClosedLoopPatternAnalyzer.LoopOutput> outputs, boolean ignoreSecondary
   ) {
      ClosedLoopPatternAnalyzer.LoopMatch forbidden = null;

      for (ClosedLoopPatternAnalyzer.LoopOutput output : outputs) {
         for (GenericStack possible : possibleInputs) {
            if (possible.what() != null) {
               if (ignoreSecondary && possible.what().dropSecondary().equals(output.key().dropSecondary())) {
                  ClosedLoopPatternAnalyzer.LoopMatch match = new ClosedLoopPatternAnalyzer.LoopMatch(
                     output.key(), isDurabilityFuzzy(possible.what(), output.key())
                  );
                  if (!match.forbidden()) {
                     return match;
                  }

                  if (forbidden == null) {
                     forbidden = match;
                  }
               }

               if (!ignoreSecondary && output.fuzzy() && possible.what().equals(output.key()) && forbidden == null) {
                  forbidden = new ClosedLoopPatternAnalyzer.LoopMatch(output.key(), true);
               }

               if (!ignoreSecondary && !output.fuzzy() && possible.what().equals(output.key())) {
                  return new ClosedLoopPatternAnalyzer.LoopMatch(output.key(), false);
               }
            }
         }
      }

      return forbidden;
   }

   static boolean acceptsLoopOutput(GenericStack[] possibleInputs, boolean inputFuzzy, AEKey output, boolean outputFuzzy) {
      if (possibleInputs != null && output != null) {
         ClosedLoopPatternAnalyzer.LoopMatch match = matchingLoopOutput(
            possibleInputs, List.of(new ClosedLoopPatternAnalyzer.LoopOutput(output, outputFuzzy)), inputFuzzy
         );
         return match != null && !match.forbidden();
      } else {
         return false;
      }
   }

   private static boolean isDurabilityFuzzy(AEKey input, AEKey output) {
      if (input.equals(output)) {
         return false;
      } else {
         if (input instanceof AEItemKey itemInput && output instanceof AEItemKey itemOutput && itemInput.getItem() == itemOutput.getItem()) {
            return itemInput.toStack().m_41763_() || itemOutput.toStack().m_41763_();
         }

         return false;
      }
   }

   private static void mergeScaled(Map<AEKey, Long> target, Map<AEKey, Long> source, long copies) {
      for (Entry<AEKey, Long> entry : source.entrySet()) {
         target.merge(entry.getKey(), Sat.mul(entry.getValue(), copies), Sat::add);
      }
   }

   private static Map<AEKey, Long> scaledCopy(Map<AEKey, Long> source, long copies) {
      LinkedHashMap<AEKey, Long> result = new LinkedHashMap<>();
      mergeScaled(result, source, copies);
      return Map.copyOf(result);
   }

   private static List<GenericStack> toStacks(Map<AEKey, Long> values) {
      ArrayList<GenericStack> result = new ArrayList<>(values.size());

      for (Entry<AEKey, Long> entry : values.entrySet()) {
         if (entry.getValue() > 0L) {
            result.add(new GenericStack(entry.getKey(), entry.getValue()));
         }
      }

      return List.copyOf(result);
   }

   private ClosedLoopPatternAnalyzer() {
   }

   private static record Balance(Map<AEKey, Long> consumed, Map<AEKey, Long> produced, Map<Integer, AEKey> inputSeedBySlot) {
   }

   private static final class BestOrder {
      private ClosedLoopPatternAnalyzer.OrderedAnalysis analysis;
      private long seedTotal = Long.MAX_VALUE;
   }

   private static final class DynamicSeedDomain {
      private boolean fuzzy;
      private boolean lateBound;
      private final Set<AEKey> variants = new LinkedHashSet<>();

      private ClosedLoopPatternAnalyzer.DynamicSeedDomain markFuzzy(boolean lateBound) {
         this.fuzzy = true;
         this.lateBound |= lateBound;
         return this;
      }

      private ClosedLoopPatternAnalyzer.DynamicSeedDomain addVariant(AEKey variant) {
         if (variant != null) {
            this.variants.add(variant);
         }

         return this;
      }

      private ClosedLoopPatternAnalyzer.DynamicSeedDomain merge(ClosedLoopPatternAnalyzer.DynamicSeedDomain other) {
         if (other == null) {
            return this;
         } else {
            this.fuzzy = this.fuzzy | other.fuzzy;
            this.lateBound = this.lateBound | other.lateBound;
            this.variants.addAll(other.variants);
            return this;
         }
      }

      private ClosedLoopPatternAnalyzer.DynamicSeedDomain copy() {
         return new ClosedLoopPatternAnalyzer.DynamicSeedDomain().merge(this);
      }
   }

   private static record LoopMatch(AEKey key, boolean forbidden) {
   }

   private static record LoopOutput(AEKey key, boolean fuzzy) {
   }

   public static record Member(IPatternDetails details, long copies) {
   }

   public static record MemberFlow(Map<AEKey, Long> inputSeed, Map<AEKey, Long> outputSeed, Map<Integer, AEKey> inputSeedBySlot) {
      public MemberFlow(Map<AEKey, Long> inputSeed, Map<AEKey, Long> outputSeed) {
         this(inputSeed, outputSeed, Map.of());
      }

      public MemberFlow(Map<AEKey, Long> inputSeed, Map<AEKey, Long> outputSeed, Map<Integer, AEKey> inputSeedBySlot) {
         inputSeed = Map.copyOf(inputSeed);
         outputSeed = Map.copyOf(outputSeed);
         inputSeedBySlot = Map.copyOf(inputSeedBySlot);
         this.inputSeed = inputSeed;
         this.outputSeed = outputSeed;
         this.inputSeedBySlot = inputSeedBySlot;
      }
   }

   public static record OrderedAnalysis(List<ClosedLoopPatternAnalyzer.Member> members, ClosedLoopAnalysis analysis) {
      public OrderedAnalysis(List<ClosedLoopPatternAnalyzer.Member> members, ClosedLoopAnalysis analysis) {
         members = List.copyOf(members);
         this.members = members;
         this.analysis = analysis;
      }
   }

   private static record OrderingModel(
      List<ClosedLoopPatternAnalyzer.Member> members, List<ClosedLoopPatternAnalyzer.Balance> scaledBalances, Set<AEKey> cycleKeys
   ) {
      private static ClosedLoopPatternAnalyzer.OrderingModel create(List<ClosedLoopPatternAnalyzer.Member> members) {
         if (members != null && !members.isEmpty() && ClosedLoopPatternAnalyzer.isPrimitiveCycle(members)) {
            ArrayList<IPatternDetails> details = new ArrayList<>(members.size());

            for (ClosedLoopPatternAnalyzer.Member member : members) {
               if (member == null || member.details() == null) {
                  return null;
               }

               details.add(member.details());
            }

            List<ClosedLoopPatternAnalyzer.LoopOutput> possibleOutputs = ClosedLoopPatternAnalyzer.possibleLoopOutputs(details);
            if (possibleOutputs == null) {
               return null;
            } else {
               ArrayList<ClosedLoopPatternAnalyzer.Balance> scaledBalances = new ArrayList<>(members.size());
               LinkedHashMap<AEKey, Long> totalConsumed = new LinkedHashMap<>();
               LinkedHashMap<AEKey, Long> totalProduced = new LinkedHashMap<>();

               for (ClosedLoopPatternAnalyzer.Member member : members) {
                  ClosedLoopPatternAnalyzer.Balance perCopy = ClosedLoopPatternAnalyzer.balance(member.details(), possibleOutputs);
                  if (perCopy == null) {
                     return null;
                  }

                  Map<AEKey, Long> consumed = ClosedLoopPatternAnalyzer.scaledCopy(perCopy.consumed(), member.copies());
                  Map<AEKey, Long> produced = ClosedLoopPatternAnalyzer.scaledCopy(perCopy.produced(), member.copies());
                  scaledBalances.add(new ClosedLoopPatternAnalyzer.Balance(consumed, produced, perCopy.inputSeedBySlot()));
                  ClosedLoopPatternAnalyzer.mergeScaled(totalConsumed, consumed, 1L);
                  ClosedLoopPatternAnalyzer.mergeScaled(totalProduced, produced, 1L);
               }

               LinkedHashSet<AEKey> cycleKeys = new LinkedHashSet<>();

               for (AEKey key : totalConsumed.keySet()) {
                  if (totalProduced.getOrDefault(key, 0L) > 0L) {
                     cycleKeys.add(key);
                  }
               }

               return cycleKeys.isEmpty()
                  ? null
                  : new ClosedLoopPatternAnalyzer.OrderingModel(
                     List.copyOf(members), List.copyOf(scaledBalances), Collections.unmodifiableSet(new LinkedHashSet<>(cycleKeys))
                  );
            }
         } else {
            return null;
         }
      }
   }

   public static enum StructureStatus {
      VALID,
      INVALID;
   }
}
