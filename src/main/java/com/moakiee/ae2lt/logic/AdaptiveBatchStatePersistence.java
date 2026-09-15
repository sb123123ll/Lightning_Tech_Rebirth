package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import appeng.util.inv.AppEngInternalInventory;
import com.moakiee.ae2lt.blockentity.OverloadedPatternProviderBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class AdaptiveBatchStatePersistence {
   private static final String TAG_ROOT = "ae2lt:adaptive_batch_state";
   private static final String TAG_VERSION = "version";
   private static final int VERSION = 1;
   private static final String TAG_PATTERNS = "patterns";
   private static final String TAG_TARGETS = "targets";
   private static final String TAG_SLOT = "slot";
   private static final String TAG_STACK = "stack";
   private static final String TAG_KIND = "kind";
   private static final String TAG_DIRECTION = "direction";
   private static final String TAG_ADDRESS = "address";
   private static final String TAG_STATES = "states";
   private static final byte KIND_NORMAL = 0;
   private static final byte KIND_WIRELESS = 1;
   private static final String TAG_REMEMBERED_CHUNK = "remembered_chunk";
   private static final String TAG_STEP = "step";
   private static final String TAG_NEXT_CHUNK = "next_chunk";
   private static final String TAG_PROVEN_CHUNK = "proven_chunk";
   private static final String TAG_PROVEN_SUCCESSES = "proven_successes";
   private static final String TAG_REPEAT_CURRENT = "repeat_current";
   private static final String TAG_GROWTH_CAPPED = "growth_capped";
   private static final String TAG_BACKING_OFF = "backing_off";
   private static final String TAG_LAST_SUCCESSFUL_TICK = "last_successful_tick";
   private static final String TAG_LAST_ATTEMPT_TICK = "last_attempt_tick";
   @Nullable
   private AdaptiveBatchStatePersistence.PendingState pending;

   void write(
      CompoundTag ownerTag,
      AppEngInternalInventory patternInventory,
      OverloadedProviderPatternCatalog patternCatalog,
      ProviderNormalDispatch normalDispatch,
      List<OverloadedPatternProviderBlockEntity.WirelessConnection> wirelessConnections
   ) {
      ListTag targetTags = new ListTag();
      HashSet<Integer> usedPatternSlots = new HashSet<>();

      for (Entry<Direction, ProviderTarget> entry : normalDispatch.targets().entrySet()) {
         CompoundTag targetTag = writeTarget(entry.getValue(), patternCatalog, usedPatternSlots);
         if (targetTag != null) {
            targetTag.m_128344_("kind", (byte)0);
            targetTag.m_128344_("direction", (byte)entry.getKey().m_122411_());
            targetTags.add(targetTag);
         }
      }

      for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : wirelessConnections) {
         CompoundTag targetTag = writeTarget(connection, patternCatalog, usedPatternSlots);
         if (targetTag != null) {
            targetTag.m_128344_("kind", (byte)1);
            targetTag.m_128365_("address", connection.toTag());
            targetTags.add(targetTag);
         }
      }

      if (targetTags.isEmpty()) {
         ownerTag.m_128473_("ae2lt:adaptive_batch_state");
      } else {
         ListTag patternTags = new ListTag();
         usedPatternSlots.stream().sorted().forEach(slot -> {
            if (slot >= 0 && slot < patternInventory.size()) {
               ItemStack stack = patternInventory.getStackInSlot(slot);
               if (!stack.m_41619_()) {
                  CompoundTag patternTag = new CompoundTag();
                  patternTag.m_128405_("slot", slot);
                  patternTag.m_128365_("stack", stack.m_41739_(new CompoundTag()));
                  patternTags.add(patternTag);
               }
            }
         });
         if (patternTags.isEmpty()) {
            ownerTag.m_128473_("ae2lt:adaptive_batch_state");
         } else {
            CompoundTag root = new CompoundTag();
            root.m_128405_("version", 1);
            root.m_128365_("patterns", patternTags);
            root.m_128365_("targets", targetTags);
            ownerTag.m_128365_("ae2lt:adaptive_batch_state", root);
         }
      }
   }

   @Nullable
   private static CompoundTag writeTarget(ProviderTarget target, OverloadedProviderPatternCatalog patternCatalog, Set<Integer> usedPatternSlots) {
      ListTag stateTags = new ListTag();
      target.adaptiveBatchSnapshots().forEach((pattern, snapshot) -> {
         int slot = patternCatalog.slotOf(pattern);
         if (slot >= 0 && snapshot.isValid()) {
            CompoundTag stateTag = writeSnapshot(snapshot);
            stateTag.m_128405_("slot", slot);
            stateTags.add(stateTag);
            usedPatternSlots.add(slot);
         }
      });
      if (stateTags.isEmpty()) {
         return null;
      } else {
         CompoundTag result = new CompoundTag();
         result.m_128365_("states", stateTags);
         return result;
      }
   }

   void read(CompoundTag ownerTag, int patternCapacity, int wirelessCapacity) {
      this.pending = null;
      if (ownerTag.m_128425_("ae2lt:adaptive_batch_state", 10)) {
         CompoundTag root = ownerTag.m_128469_("ae2lt:adaptive_batch_state");
         if (root.m_128451_("version") == 1) {
            int safePatternCapacity = Math.max(0, patternCapacity);
            HashMap<Integer, ItemStack> patterns = new HashMap<>();
            ListTag patternTags = root.m_128437_("patterns", 10);
            int patternCount = Math.min(patternTags.size(), safePatternCapacity);

            for (int i = 0; i < patternCount; i++) {
               CompoundTag patternTag = patternTags.m_128728_(i);
               int slot = patternTag.m_128451_("slot");
               if (slot >= 0 && slot < safePatternCapacity && patternTag.m_128425_("stack", 10)) {
                  try {
                     ItemStack stack = ItemStack.m_41712_(patternTag.m_128469_("stack"));
                     if (!stack.m_41619_()) {
                        patterns.putIfAbsent(slot, stack);
                     }
                  } catch (RuntimeException var19) {
                  }
               }
            }

            if (!patterns.isEmpty()) {
               int safeTargetCapacity = Math.max(0, wirelessCapacity) + 6;
               ArrayList<AdaptiveBatchStatePersistence.PendingTarget> targets = new ArrayList<>();
               ListTag targetTags = root.m_128437_("targets", 10);
               int targetCount = Math.min(targetTags.size(), safeTargetCapacity);

               for (int ix = 0; ix < targetCount; ix++) {
                  CompoundTag targetTag = targetTags.m_128728_(ix);
                  Map<Integer, ProviderTarget.AdaptiveBatchSnapshot> states = readStates(targetTag.m_128437_("states", 10), safePatternCapacity, patterns);
                  if (!states.isEmpty()) {
                     byte kind = targetTag.m_128445_("kind");
                     if (kind == 0) {
                        int directionId = targetTag.m_128445_("direction");
                        if (directionId >= 0 && directionId < Direction.values().length) {
                           targets.add(AdaptiveBatchStatePersistence.PendingTarget.normal(Direction.m_122376_(directionId), states));
                        }
                     } else if (kind == 1 && targetTag.m_128425_("address", 10)) {
                        try {
                           targets.add(
                              AdaptiveBatchStatePersistence.PendingTarget.wireless(
                                 OverloadedPatternProviderBlockEntity.WirelessConnection.fromTag(targetTag.m_128469_("address")), states
                              )
                           );
                        } catch (RuntimeException var18) {
                        }
                     }
                  }
               }

               if (!targets.isEmpty()) {
                  this.pending = new AdaptiveBatchStatePersistence.PendingState(Map.copyOf(patterns), List.copyOf(targets));
               }
            }
         }
      }
   }

   private static Map<Integer, ProviderTarget.AdaptiveBatchSnapshot> readStates(ListTag stateTags, int patternCapacity, Map<Integer, ItemStack> patterns) {
      HashMap<Integer, ProviderTarget.AdaptiveBatchSnapshot> states = new HashMap<>();
      int stateCount = Math.min(stateTags.size(), patternCapacity);

      for (int i = 0; i < stateCount; i++) {
         CompoundTag stateTag = stateTags.m_128728_(i);
         int slot = stateTag.m_128451_("slot");
         if (patterns.containsKey(slot)) {
            ProviderTarget.AdaptiveBatchSnapshot snapshot = readSnapshot(stateTag);
            if (snapshot != null) {
               states.putIfAbsent(slot, snapshot);
            }
         }
      }

      return states;
   }

   boolean finishLoad(
      @Nullable ServerLevel level,
      BlockPos providerPos,
      AppEngInternalInventory patternInventory,
      OverloadedProviderPatternCatalog patternCatalog,
      ProviderNormalDispatch normalDispatch,
      List<Direction> activeNormalDirections,
      List<OverloadedPatternProviderBlockEntity.WirelessConnection> wirelessConnections
   ) {
      AdaptiveBatchStatePersistence.PendingState state = this.pending;
      if (state != null && level != null) {
         HashMap<Integer, IPatternDetails> validPatterns = new HashMap<>();
         state.patterns().forEach((slot, savedStack) -> {
            if (slot >= 0 && slot < patternInventory.size()) {
               ItemStack currentStack = patternInventory.getStackInSlot(slot);
               IPatternDetails pattern = patternCatalog.patternAtSlot(slot);
               if (pattern != null && ItemStack.m_150942_(savedStack, currentStack)) {
                  validPatterns.put(slot, pattern);
               }
            }
         });

         for (AdaptiveBatchStatePersistence.PendingTarget targetState : state.targets()) {
            ProviderTarget target;
            if (targetState.normalDirection() != null) {
               if (!activeNormalDirections.contains(targetState.normalDirection())) {
                  continue;
               }

               target = normalDispatch.target(level, providerPos, targetState.normalDirection());
            } else {
               target = findWirelessTarget(wirelessConnections, targetState.wirelessAddress());
               if (target == null) {
                  continue;
               }
            }

            targetState.states().forEach((slot, snapshot) -> {
               IPatternDetails pattern = validPatterns.get(slot);
               if (pattern != null) {
                  target.restoreAdaptiveBatchSnapshot(pattern, snapshot);
               }
            });
         }

         this.pending = null;
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   private static OverloadedPatternProviderBlockEntity.WirelessConnection findWirelessTarget(
      List<OverloadedPatternProviderBlockEntity.WirelessConnection> connections, @Nullable OverloadedPatternProviderBlockEntity.WirelessConnection address
   ) {
      if (address == null) {
         return null;
      } else {
         for (OverloadedPatternProviderBlockEntity.WirelessConnection connection : connections) {
            if (connection.equals(address)) {
               return connection;
            }
         }

         return null;
      }
   }

   void clear() {
      this.pending = null;
   }

   static CompoundTag writeSnapshot(ProviderTarget.AdaptiveBatchSnapshot snapshot) {
      CompoundTag tag = new CompoundTag();
      if (snapshot.rememberedChunk() > 0) {
         tag.m_128405_("remembered_chunk", snapshot.rememberedChunk());
      }

      ProviderTarget.BatchStepSnapshot step = snapshot.step();
      if (step != null) {
         CompoundTag stepTag = new CompoundTag();
         stepTag.m_128405_("next_chunk", step.nextChunk());
         stepTag.m_128405_("proven_chunk", step.provenChunk());
         stepTag.m_128405_("proven_successes", step.provenSuccesses());
         stepTag.m_128379_("repeat_current", step.repeatCurrent());
         stepTag.m_128379_("growth_capped", step.growthCapped());
         stepTag.m_128379_("backing_off", step.backingOff());
         stepTag.m_128356_("last_successful_tick", step.lastSuccessfulTick());
         stepTag.m_128356_("last_attempt_tick", step.lastAttemptTick());
         tag.m_128365_("step", stepTag);
      }

      return tag;
   }

   @Nullable
   static ProviderTarget.AdaptiveBatchSnapshot readSnapshot(CompoundTag tag) {
      int rememberedChunk = tag.m_128425_("remembered_chunk", 3) ? tag.m_128451_("remembered_chunk") : 0;
      ProviderTarget.BatchStepSnapshot step = null;
      if (tag.m_128425_("step", 10)) {
         CompoundTag stepTag = tag.m_128469_("step");
         step = new ProviderTarget.BatchStepSnapshot(
            stepTag.m_128451_("next_chunk"),
            stepTag.m_128451_("proven_chunk"),
            stepTag.m_128451_("proven_successes"),
            stepTag.m_128471_("repeat_current"),
            stepTag.m_128471_("growth_capped"),
            stepTag.m_128471_("backing_off"),
            stepTag.m_128454_("last_successful_tick"),
            stepTag.m_128454_("last_attempt_tick")
         );
      }

      ProviderTarget.AdaptiveBatchSnapshot snapshot = new ProviderTarget.AdaptiveBatchSnapshot(rememberedChunk, step);
      return snapshot.isValid() ? snapshot : null;
   }

   private static record PendingState(Map<Integer, ItemStack> patterns, List<AdaptiveBatchStatePersistence.PendingTarget> targets) {
   }

   private static record PendingTarget(
      @Nullable Direction normalDirection,
      @Nullable OverloadedPatternProviderBlockEntity.WirelessConnection wirelessAddress,
      Map<Integer, ProviderTarget.AdaptiveBatchSnapshot> states
   ) {
      private static AdaptiveBatchStatePersistence.PendingTarget normal(Direction direction, Map<Integer, ProviderTarget.AdaptiveBatchSnapshot> states) {
         return new AdaptiveBatchStatePersistence.PendingTarget(direction, null, Map.copyOf(states));
      }

      private static AdaptiveBatchStatePersistence.PendingTarget wireless(
         OverloadedPatternProviderBlockEntity.WirelessConnection address, Map<Integer, ProviderTarget.AdaptiveBatchSnapshot> states
      ) {
         return new AdaptiveBatchStatePersistence.PendingTarget(null, address, Map.copyOf(states));
      }
   }
}
