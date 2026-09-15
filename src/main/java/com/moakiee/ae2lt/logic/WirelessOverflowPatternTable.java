package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.moakiee.ae2lt.overload.runtime.model.MatchMode;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadPatternDetails;
import com.moakiee.ae2lt.overload.runtime.pattern.OverloadedProviderOnlyPatternDetails;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

final class WirelessOverflowPatternTable {
   private static final int MAX_PATTERN_IDS = 65535;
   private final Map<IPatternDetails, Integer> byIdentity = WeakIdentityMaps.weakKeys();
   private final Object2IntOpenHashMap<IPatternDetails> byPattern = new Object2IntOpenHashMap();
   private final Int2ObjectOpenHashMap<IPatternDetails> byId = new Int2ObjectOpenHashMap();
   private int nextId;

   WirelessOverflowPatternTable() {
      this.byPattern.defaultReturnValue(-1);
   }

   short intern(IPatternDetails pattern, Supplier<? extends Iterable<? extends WirelessOverflowPatternTable.PatternReference>> liveReferences) {
      Integer cachedId = this.byIdentity.get(pattern);
      int id = cachedId != null ? cachedId : this.byPattern.getInt(pattern);
      if (id >= 0) {
         if (cachedId == null) {
            this.byIdentity.put(pattern, id);
         }

         return (short)id;
      } else {
         if (this.byId.size() >= 65535) {
            this.compact((Iterable<? extends WirelessOverflowPatternTable.PatternReference>)liveReferences.get());
         }

         for (int attempts = 0; attempts <= 65535; attempts++) {
            id = this.allocateId();
            if (!this.byId.containsKey(id)) {
               this.put(id, pattern);
               return (short)id;
            }
         }

         this.compact((Iterable<? extends WirelessOverflowPatternTable.PatternReference>)liveReferences.get());
         id = this.allocateId();
         this.put(id, pattern);
         return (short)id;
      }
   }

   @Nullable
   IPatternDetails get(int unsignedId) {
      return (IPatternDetails)this.byId.get(unsignedId);
   }

   void restore(int id, IPatternDetails pattern) {
      this.put(id, pattern);
      this.nextId = Math.max(this.nextId, id + 1 & 65535);
   }

   void clear() {
      this.byIdentity.clear();
      this.byPattern.clear();
      this.byId.clear();
      this.nextId = 0;
   }

   private void compact(Iterable<? extends WirelessOverflowPatternTable.PatternReference> liveReferences) {
      Int2ObjectOpenHashMap<IPatternDetails> previous = new Int2ObjectOpenHashMap(this.byId);
      this.clear();

      for (WirelessOverflowPatternTable.PatternReference reference : liveReferences) {
         if (reference.usesPatternDefinition()) {
            IPatternDetails pattern = (IPatternDetails)previous.get(reference.unsignedPatternId());
            if (pattern != null) {
               int id = this.allocateId();
               reference.setPatternId((short)id);
               this.put(id, pattern);
            }
         }
      }
   }

   private int allocateId() {
      int id = this.nextId++ & 65535;
      if (this.nextId > 65535) {
         this.nextId = 0;
      }

      return id;
   }

   private void put(int id, IPatternDetails pattern) {
      this.byIdentity.put(pattern, id);
      this.byPattern.put(pattern, id);
      this.byId.put(id, pattern);
   }

   static boolean isCompactEligible(IPatternDetails pattern) {
      OverloadPatternDetails overloadDetails = pattern instanceof OverloadedProviderOnlyPatternDetails overload ? overload.overloadPatternDetailsView() : null;
      HashSet<AEKey> seen = new HashSet<>();
      IInput[] inputs = pattern.getInputs();

      for (int i = 0; i < inputs.length; i++) {
         if (overloadDetails != null && overloadDetails.inputMode(i) != MatchMode.STRICT) {
            return false;
         }

         GenericStack[] possible = inputs[i].getPossibleInputs();
         if (possible.length != 1 || !seen.add(possible[0].what())) {
            return false;
         }
      }

      return true;
   }

   static int findSlotIndex(IInput[] inputs, AEKey key) {
      for (int i = 0; i < inputs.length; i++) {
         GenericStack[] possible = inputs[i].getPossibleInputs();
         if (possible.length == 1 && possible[0].what().equals(key)) {
            return i;
         }
      }

      return -1;
   }

   static boolean verifySequentialOverflow(IInput[] inputs, int stuckIndex, List<GenericStack> overflow) {
      if (stuckIndex >= 0 && stuckIndex < inputs.length && overflow.size() == inputs.length - stuckIndex) {
         for (int i = 0; i < overflow.size(); i++) {
            GenericStack stack = overflow.get(i);
            GenericStack[] possible = inputs[stuckIndex + i].getPossibleInputs();
            if (possible.length != 1 || !possible[0].what().equals(stack.what())) {
               return false;
            }

            long fullAmount = inputAmount(inputs[stuckIndex + i]);
            if (i == 0) {
               if (stack.amount() <= 0L || stack.amount() > fullAmount) {
                  return false;
               }
            } else if (stack.amount() != fullAmount) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static long inputAmount(IInput input) {
      GenericStack[] possible = input.getPossibleInputs();
      return possible.length == 0 ? 0L : possible[0].amount() * input.getMultiplier();
   }

   interface PatternReference {
      boolean usesPatternDefinition();

      int unsignedPatternId();

      void setPatternId(short var1);
   }
}
