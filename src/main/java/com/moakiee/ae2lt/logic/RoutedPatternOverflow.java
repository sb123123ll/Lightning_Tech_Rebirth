package com.moakiee.ae2lt.logic;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

final class RoutedPatternOverflow {
   private final List<RoutedPatternOverflow.Entry> entries;

   private RoutedPatternOverflow(List<RoutedPatternOverflow.Entry> entries) {
      this.entries = new ArrayList<>(entries);
   }

   static RoutedPatternOverflow unrouted(List<GenericStack> stacks) {
      ArrayList<RoutedPatternOverflow.Entry> entries = new ArrayList<>(stacks.size());

      for (GenericStack stack : stacks) {
         if (stack != null && stack.what() != null && stack.amount() > 0L) {
            entries.add(new RoutedPatternOverflow.Entry(null, stack));
         }
      }

      return new RoutedPatternOverflow(entries);
   }

   static RoutedPatternOverflow routed(List<RoutedPatternOverflow.Entry> entries) {
      return new RoutedPatternOverflow(entries);
   }

   List<RoutedPatternOverflow.Entry> snapshot() {
      return List.copyOf(this.entries);
   }

   boolean isEmpty() {
      return this.entries.isEmpty();
   }

   boolean hasExplicitFaces() {
      for (RoutedPatternOverflow.Entry entry : this.entries) {
         if (entry.face() != null) {
            return true;
         }
      }

      return false;
   }

   boolean flushUnrouted(RoutedPatternOverflow.UnroutedFlusher flusher) {
      if (this.hasExplicitFaces()) {
         throw new IllegalStateException("A routed overflow cannot use an unrouted machine adapter");
      } else {
         long before = totalAmount(this.entries);
         ArrayList<GenericStack> stacks = new ArrayList<>(this.entries.size());

         for (RoutedPatternOverflow.Entry entry : this.entries) {
            stacks.add(entry.stack());
         }

         flusher.flush(stacks);
         ArrayList<RoutedPatternOverflow.Entry> remaining = new ArrayList<>(stacks.size());

         for (GenericStack stack : stacks) {
            if (stack != null && stack.what() != null && stack.amount() > 0L) {
               remaining.add(new RoutedPatternOverflow.Entry(null, stack));
            }
         }

         long after = totalAmount(remaining);
         if (after > before) {
            throw new IllegalStateException("Pattern overflow adapter increased the queued amount");
         } else {
            this.entries.clear();
            this.entries.addAll(remaining);
            return after < before;
         }
      }
   }

   boolean flush(Direction defaultFace, RoutedPatternOverflow.Inserter inserter) {
      boolean progressed = false;
      int i = 0;

      while (i < this.entries.size()) {
         RoutedPatternOverflow.Entry entry = this.entries.get(i);
         GenericStack stack = entry.stack();
         long inserted = inserter.insert(entry.resolvedFace(defaultFace), stack.what(), stack.amount());
         if (inserted < 0L || inserted > stack.amount()) {
            throw new IllegalStateException("Pattern overflow target returned an invalid insertion amount");
         }

         if (inserted >= stack.amount()) {
            this.entries.remove(i);
            progressed = true;
         } else {
            if (inserted > 0L) {
               this.entries.set(i, new RoutedPatternOverflow.Entry(entry.face(), new GenericStack(stack.what(), stack.amount() - inserted)));
               progressed = true;
            }

            i++;
         }
      }

      return progressed;
   }

   private static long totalAmount(List<RoutedPatternOverflow.Entry> entries) {
      long total = 0L;

      for (RoutedPatternOverflow.Entry entry : entries) {
         long amount = entry.stack().amount();
         total = Long.MAX_VALUE - total < amount ? Long.MAX_VALUE : total + amount;
      }

      return total;
   }

   static record Entry(@Nullable Direction face, GenericStack stack) {
      Entry(@Nullable Direction face, GenericStack stack) {
         if (stack != null && stack.what() != null && stack.amount() > 0L) {
            this.face = face;
            this.stack = stack;
         } else {
            throw new IllegalArgumentException("Overflow entries require a positive stack");
         }
      }

      Direction resolvedFace(Direction defaultFace) {
         return this.face != null ? this.face : defaultFace;
      }
   }

   @FunctionalInterface
   interface Inserter {
      long insert(Direction var1, AEKey var2, long var3);
   }

   @FunctionalInterface
   interface UnroutedFlusher {
      void flush(List<GenericStack> var1);
   }
}
