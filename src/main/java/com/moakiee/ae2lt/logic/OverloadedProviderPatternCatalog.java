package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.util.inv.AppEngInternalInventory;
import com.moakiee.thunderbolt.core.crafting.support.CraftingPatternDelegates;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class OverloadedProviderPatternCatalog {
   private final Map<IPatternDetails, IPatternDetails> resolvedByIdentity = WeakIdentityMaps.weakKeysAndValues();
   private final Map<IPatternDetails, OverloadedProviderPatternCatalog.Registration> registeredByEquality = new WeakHashMap<>();
   private final Map<IPatternDetails, Integer> slotByCanonicalIdentity = new IdentityHashMap<>();
   private final Map<Integer, IPatternDetails> canonicalBySlot = new HashMap<>();

   void rebuild(AppEngInternalInventory inventory, Level level, List<IPatternDetails> visiblePatterns, Set<AEKey> patternInputs) {
      HashMap<IPatternDetails, OverloadedProviderPatternCatalog.Registration> previousRegistrations = new HashMap<>(this.registeredByEquality);
      this.clear();
      visiblePatterns.clear();
      patternInputs.clear();

      for (int slot = 0; slot < inventory.size(); slot++) {
         IPatternDetails details = PatternDetailsHelper.decodePattern(inventory.getStackInSlot(slot), level);
         if (details != null) {
            visiblePatterns.add(details);
            this.register(details, previousRegistrations, slot);

            for (IInput input : details.getInputs()) {
               for (GenericStack possibleInput : input.getPossibleInputs()) {
                  patternInputs.add(possibleInput.what().dropSecondary());
               }
            }
         }
      }
   }

   @Nullable
   IPatternDetails resolve(IPatternDetails executionDetails) {
      if (executionDetails == null) {
         return null;
      } else {
         IPatternDetails direct = this.resolvedByIdentity.get(executionDetails);
         if (direct != null) {
            return direct;
         } else {
            IPatternDetails providerDetails = CraftingPatternDelegates.forProviderLookup(executionDetails);
            if (providerDetails != executionDetails) {
               direct = this.resolvedByIdentity.get(providerDetails);
               if (direct != null) {
                  this.resolvedByIdentity.put(executionDetails, direct);
                  return direct;
               }
            }

            OverloadedProviderPatternCatalog.Registration registration = this.registeredByEquality.get(providerDetails);
            IPatternDetails resolved = registration == null ? null : registration.resolve();
            if (resolved != null) {
               this.resolvedByIdentity.put(providerDetails, resolved);
               if (providerDetails != executionDetails) {
                  this.resolvedByIdentity.put(executionDetails, resolved);
               }
            }

            return resolved;
         }
      }
   }

   void clear() {
      this.resolvedByIdentity.clear();
      this.registeredByEquality.clear();
      this.slotByCanonicalIdentity.clear();
      this.canonicalBySlot.clear();
   }

   void register(IPatternDetails details) {
      this.register(details, Map.of(), -1);
   }

   void register(IPatternDetails details, int slot) {
      this.register(details, Map.of(), slot);
   }

   private void register(IPatternDetails details, Map<IPatternDetails, OverloadedProviderPatternCatalog.Registration> previousRegistrations, int slot) {
      OverloadedProviderPatternCatalog.Registration registration = this.registeredByEquality.get(details);
      if (registration == null) {
         OverloadedProviderPatternCatalog.Registration previous = previousRegistrations.get(details);
         IPatternDetails previousDetails = previous == null ? null : previous.resolve();
         IPatternDetails candidate = previousDetails != null ? previousDetails : details;
         registration = new OverloadedProviderPatternCatalog.Registration(candidate, details);
         this.registeredByEquality.put(details, registration);
      }

      IPatternDetails canonicalDetails = registration.resolve();
      if (canonicalDetails == null) {
         canonicalDetails = details;
         registration = new OverloadedProviderPatternCatalog.Registration(details, details);
         this.registeredByEquality.put(details, registration);
      }

      this.resolvedByIdentity.put(details, canonicalDetails);
      if (slot >= 0) {
         this.canonicalBySlot.put(slot, canonicalDetails);
         this.slotByCanonicalIdentity.putIfAbsent(canonicalDetails, slot);
      }
   }

   int slotOf(IPatternDetails canonicalDetails) {
      return this.slotByCanonicalIdentity.getOrDefault(canonicalDetails, -1);
   }

   @Nullable
   IPatternDetails patternAtSlot(int slot) {
      return this.canonicalBySlot.get(slot);
   }

   private static final class Registration {
      private final WeakReference<IPatternDetails> preferred;
      private final WeakReference<IPatternDetails> current;

      private Registration(IPatternDetails preferred, IPatternDetails current) {
         this.preferred = new WeakReference<>(preferred);
         this.current = new WeakReference<>(current);
      }

      @Nullable
      private IPatternDetails resolve() {
         IPatternDetails result = this.preferred.get();
         return result != null ? result : this.current.get();
      }
   }
}
