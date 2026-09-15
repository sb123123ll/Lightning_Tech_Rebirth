package com.moakiee.ae2lt.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class ProviderReturnSweep {
   static final int ACTIVE_INTERVAL = 20;
   static final int MAX_INTERVAL = 128;
   private final DueTaskQueue<ProviderTarget> due = new DueTaskQueue<>();
   private List<ProviderTarget> targets = List.of();
   private Set<ProviderTarget> targetSet = Set.of();
   private final Set<ProviderTarget> remaining = new HashSet<>();
   private int interval = 20;
   private boolean roundActive;

   void synchronize(List<? extends ProviderTarget> currentTargets, long gameTick) {
      LinkedHashSet<ProviderTarget> distinctSet = new LinkedHashSet<>();
      distinctSet.addAll(currentTargets);
      List<ProviderTarget> distinct = List.copyOf(distinctSet);
      if (!distinct.equals(this.targets)) {
         this.targets = distinct;
         this.targetSet = Set.copyOf(distinct);
         this.interval = 20;
         this.roundActive = false;
         this.startRound(gameTick);
      }
   }

   @Nullable
   ProviderTarget pollDue(long gameTick) {
      return this.due.pollDue(gameTick);
   }

   void recordPeriodic(ProviderTarget target, long gameTick, OutputReturnResult result) {
      if (this.targetSet.contains(target) && this.remaining.remove(target)) {
         if (result.keepsSweepActive()) {
            this.activate(gameTick);
         }

         this.finishRoundIfComplete(gameTick);
      }
   }

   void recordDispatch(ProviderTarget target, long gameTick) {
      boolean wasIdle = this.interval != 20;
      this.interval = 20;
      this.roundActive = true;
      if (this.targetSet.contains(target) && this.remaining.remove(target)) {
         this.due.remove(target);
      }

      if (this.remaining.isEmpty()) {
         this.completeRound(gameTick);
      } else if (wasIdle) {
         this.scheduleRemaining(gameTick);
      }
   }

   long nextDueTick() {
      return this.due.nextDueTick();
   }

   int interval() {
      return this.interval;
   }

   void clear() {
      this.due.clear();
      this.targets = List.of();
      this.targetSet = Set.of();
      this.remaining.clear();
      this.interval = 20;
      this.roundActive = false;
   }

   private void activate(long gameTick) {
      if (this.interval == 20) {
         this.roundActive = true;
      } else {
         this.interval = 20;
         this.roundActive = true;
         this.scheduleRemaining(gameTick);
      }
   }

   private void finishRoundIfComplete(long gameTick) {
      if (this.remaining.isEmpty()) {
         this.completeRound(gameTick);
      }
   }

   private void completeRound(long gameTick) {
      this.interval = this.roundActive ? 20 : Math.min(128, this.interval * 2);
      this.roundActive = false;
      this.startRound(gameTick + 1L);
   }

   private void startRound(long firstTick) {
      this.due.clear();
      this.remaining.clear();
      this.remaining.addAll(this.targets);
      this.schedule(this.targets, firstTick);
   }

   private void scheduleRemaining(long firstTick) {
      this.due.clear();
      ArrayList<ProviderTarget> orderedRemaining = new ArrayList<>(this.remaining.size());

      for (ProviderTarget target : this.targets) {
         if (this.remaining.contains(target)) {
            orderedRemaining.add(target);
         }
      }

      this.schedule(orderedRemaining, firstTick);
   }

   private void schedule(List<ProviderTarget> scheduled, long firstTick) {
      int size = scheduled.size();
      if (size != 0) {
         for (int i = 0; i < size; i++) {
            long offset = ((long)(i + 1) * (long)this.interval - 1L) / (long)size;
            this.due.schedule(scheduled.get(i), firstTick + offset);
         }
      }
   }
}
