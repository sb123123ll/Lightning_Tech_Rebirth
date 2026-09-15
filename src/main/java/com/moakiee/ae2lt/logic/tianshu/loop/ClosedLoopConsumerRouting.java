package com.moakiee.ae2lt.logic.tianshu.loop;

import appeng.api.stacks.AEKey;
import com.moakiee.thunderbolt.core.crafting.planner.Sat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;

public final class ClosedLoopConsumerRouting {
   private static final String CONSUMER_ID_PREFIX = "ae2lt:closed-loop-consumer:v1:";

   public static ClosedLoopConsumerRouting.RoutingPlan compile(UUID groupId, List<ClosedLoopPatternAnalyzer.MemberFlow> orderedFlows) {
      Objects.requireNonNull(groupId, "groupId");
      Objects.requireNonNull(orderedFlows, "orderedFlows");
      if (orderedFlows.isEmpty()) {
         throw new IllegalArgumentException("a closed-loop route requires at least one member");
      } else {
         List<ClosedLoopPatternAnalyzer.MemberFlow> flows = List.copyOf(orderedFlows);
         ArrayList<UUID> consumerIds = new ArrayList<>(flows.size());
         ArrayList<LinkedHashMap<AEKey, Long>> bootstrapByConsumer = new ArrayList<>(flows.size());
         ArrayList<LinkedHashMap<UUID, LinkedHashMap<AEKey, Long>>> routesByProducer = new ArrayList<>(flows.size());
         ArrayList<LinkedHashMap<UUID, LinkedHashMap<AEKey, Long>>> wrappedRoutesByProducer = new ArrayList<>(flows.size());
         LinkedHashSet<AEKey> keys = new LinkedHashSet<>();

         for (int memberIndex = 0; memberIndex < flows.size(); memberIndex++) {
            ClosedLoopPatternAnalyzer.MemberFlow flow = Objects.requireNonNull(flows.get(memberIndex), "orderedFlows[" + memberIndex + "]");
            validateAmounts(flow.inputSeed(), "inputSeed", memberIndex, keys);
            validateAmounts(flow.outputSeed(), "outputSeed", memberIndex, keys);
            consumerIds.add(consumerId(groupId, memberIndex));
            bootstrapByConsumer.add(new LinkedHashMap<>());
            routesByProducer.add(new LinkedHashMap<>());
            wrappedRoutesByProducer.add(new LinkedHashMap<>());
         }

         for (AEKey key : keys) {
            routeKey(flows, consumerIds, bootstrapByConsumer, routesByProducer, wrappedRoutesByProducer, key);
         }

         ArrayList<ClosedLoopConsumerRouting.ConsumerAccount> consumers = new ArrayList<>(flows.size());
         ArrayList<ClosedLoopConsumerRouting.ProducerRouting> producers = new ArrayList<>(flows.size());
         LinkedHashMap<AEKey, Long> bootstrapSeed = new LinkedHashMap<>();

         for (int memberIndex = 0; memberIndex < flows.size(); memberIndex++) {
            Map<AEKey, Long> consumerBootstrap = immutableMap(bootstrapByConsumer.get(memberIndex));
            consumers.add(new ClosedLoopConsumerRouting.ConsumerAccount(memberIndex, consumerIds.get(memberIndex), consumerBootstrap));
            merge(bootstrapSeed, consumerBootstrap);
            producers.add(
               new ClosedLoopConsumerRouting.ProducerRouting(
                  memberIndex, immutableNestedMap(routesByProducer.get(memberIndex)), immutableNestedMap(wrappedRoutesByProducer.get(memberIndex))
               )
            );
         }

         return new ClosedLoopConsumerRouting.RoutingPlan(groupId, consumers, producers, immutableMap(bootstrapSeed));
      }
   }

   private static void routeKey(
      List<ClosedLoopPatternAnalyzer.MemberFlow> flows,
      List<UUID> consumerIds,
      List<LinkedHashMap<AEKey, Long>> bootstrapByConsumer,
      List<LinkedHashMap<UUID, LinkedHashMap<AEKey, Long>>> routesByProducer,
      List<LinkedHashMap<UUID, LinkedHashMap<AEKey, Long>>> wrappedRoutesByProducer,
      AEKey key
   ) {
      long[] remainingDemand = new long[flows.size()];

      for (int consumerIndex = 0; consumerIndex < flows.size(); consumerIndex++) {
         remainingDemand[consumerIndex] = positiveAmount(flows.get(consumerIndex).inputSeed(), key);
      }

      for (int producerIndex = 0; producerIndex < flows.size(); producerIndex++) {
         long remainingOutput = positiveAmount(flows.get(producerIndex).outputSeed(), key);
         int consumerIndex = (producerIndex + 1) % flows.size();

         for (int visited = 0; remainingOutput > 0L && visited < flows.size(); visited++) {
            long amount = Math.min(remainingOutput, remainingDemand[consumerIndex]);
            if (amount > 0L) {
               addRoute(routesByProducer.get(producerIndex), consumerIds.get(consumerIndex), key, amount);
               remainingDemand[consumerIndex] -= amount;
               remainingOutput -= amount;
               if (consumerIndex <= producerIndex) {
                  add(bootstrapByConsumer.get(consumerIndex), key, amount);
                  addRoute(wrappedRoutesByProducer.get(producerIndex), consumerIds.get(consumerIndex), key, amount);
               }
            }

            consumerIndex = (consumerIndex + 1) % flows.size();
         }

         if (remainingOutput > 0L) {
            throw unbalanced(key, "unmatched output", remainingOutput);
         }
      }

      for (long remaining : remainingDemand) {
         if (remaining > 0L) {
            throw unbalanced(key, "unmatched input", remaining);
         }
      }
   }

   private static void validateAmounts(Map<AEKey, Long> amounts, String label, int memberIndex, LinkedHashSet<AEKey> keys) {
      Objects.requireNonNull(amounts, label + " for member " + memberIndex);

      for (Entry<AEKey, Long> entry : amounts.entrySet()) {
         AEKey key = Objects.requireNonNull(entry.getKey(), label + " key for member " + memberIndex);
         Long boxedAmount = Objects.requireNonNull(entry.getValue(), label + " amount for member " + memberIndex);
         if (boxedAmount < 0L) {
            throw new IllegalArgumentException(label + " amount must not be negative for member " + memberIndex);
         }

         if (boxedAmount > 0L) {
            keys.add(key);
         }
      }
   }

   private static long positiveAmount(Map<AEKey, Long> amounts, AEKey key) {
      return Math.min(2305843009213693951L, Math.max(0L, amounts.getOrDefault(key, 0L)));
   }

   private static void addRoute(LinkedHashMap<UUID, LinkedHashMap<AEKey, Long>> producerRoutes, UUID consumerId, AEKey key, long amount) {
      add(producerRoutes.computeIfAbsent(consumerId, ignored -> new LinkedHashMap<>()), key, amount);
   }

   private static void merge(LinkedHashMap<AEKey, Long> target, Map<AEKey, Long> amounts) {
      for (Entry<AEKey, Long> entry : amounts.entrySet()) {
         add(target, entry.getKey(), entry.getValue());
      }
   }

   private static void add(LinkedHashMap<AEKey, Long> target, AEKey key, long amount) {
      if (amount > 0L) {
         target.merge(key, Long.valueOf(amount), Sat::add);
      }
   }

   private static IllegalArgumentException unbalanced(AEKey key, String kind, long amount) {
      return new IllegalArgumentException("unbalanced closed-loop seed flow for " + key + ": " + kind + " " + amount);
   }

   private static UUID consumerId(UUID groupId, int memberIndex) {
      return UUID.nameUUIDFromBytes(("ae2lt:closed-loop-consumer:v1:" + groupId + ":" + memberIndex).getBytes(StandardCharsets.UTF_8));
   }

   private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
      return Collections.unmodifiableMap(new LinkedHashMap<>(source));
   }

   private static Map<UUID, Map<AEKey, Long>> immutableNestedMap(Map<UUID, ? extends Map<AEKey, Long>> source) {
      LinkedHashMap<UUID, Map<AEKey, Long>> copy = new LinkedHashMap<>();

      for (Entry<UUID, ? extends Map<AEKey, Long>> entry : source.entrySet()) {
         copy.put(entry.getKey(), immutableMap((Map<AEKey, Long>)entry.getValue()));
      }

      return Collections.unmodifiableMap(copy);
   }

   private ClosedLoopConsumerRouting() {
   }

   public static record ConsumerAccount(int memberIndex, UUID consumerId, Map<AEKey, Long> bootstrapSeed) {
      public ConsumerAccount(int memberIndex, UUID consumerId, Map<AEKey, Long> bootstrapSeed) {
         if (memberIndex < 0) {
            throw new IllegalArgumentException("memberIndex must be positive");
         } else {
            Objects.requireNonNull(consumerId, "consumerId");
            bootstrapSeed = ClosedLoopConsumerRouting.immutableMap(bootstrapSeed);
            this.memberIndex = memberIndex;
            this.consumerId = consumerId;
            this.bootstrapSeed = bootstrapSeed;
         }
      }
   }

   public static record ProducerRouting(int memberIndex, Map<UUID, Map<AEKey, Long>> targets, Map<UUID, Map<AEKey, Long>> wrappedTargets) {
      public ProducerRouting(int memberIndex, Map<UUID, Map<AEKey, Long>> targets, Map<UUID, Map<AEKey, Long>> wrappedTargets) {
         if (memberIndex < 0) {
            throw new IllegalArgumentException("memberIndex must be positive");
         } else {
            targets = ClosedLoopConsumerRouting.immutableNestedMap(targets);
            wrappedTargets = ClosedLoopConsumerRouting.immutableNestedMap(wrappedTargets);
            this.memberIndex = memberIndex;
            this.targets = targets;
            this.wrappedTargets = wrappedTargets;
         }
      }
   }

   public static record RoutingPlan(
      UUID groupId,
      List<ClosedLoopConsumerRouting.ConsumerAccount> consumers,
      List<ClosedLoopConsumerRouting.ProducerRouting> producers,
      Map<AEKey, Long> bootstrapSeed
   ) {
      public RoutingPlan(
         UUID groupId,
         List<ClosedLoopConsumerRouting.ConsumerAccount> consumers,
         List<ClosedLoopConsumerRouting.ProducerRouting> producers,
         Map<AEKey, Long> bootstrapSeed
      ) {
         Objects.requireNonNull(groupId, "groupId");
         consumers = List.copyOf(consumers);
         producers = List.copyOf(producers);
         bootstrapSeed = ClosedLoopConsumerRouting.immutableMap(bootstrapSeed);
         this.groupId = groupId;
         this.consumers = consumers;
         this.producers = producers;
         this.bootstrapSeed = bootstrapSeed;
      }
   }
}
