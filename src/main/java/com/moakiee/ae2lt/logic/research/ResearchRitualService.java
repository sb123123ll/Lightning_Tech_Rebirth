package com.moakiee.ae2lt.logic.research;

import com.moakiee.ae2lt.blockentity.AtmosphericIonizerBlockEntity;
import com.moakiee.ae2lt.entity.RitualHyperdimensionalPigmeeEntity;
import com.moakiee.ae2lt.item.ResearchNoteItem;
import com.moakiee.ae2lt.lightning.ProtectedItemEntityHelper;
import com.moakiee.ae2lt.registry.ModFumos;
import com.moakiee.ae2lt.registry.ModItems;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class ResearchRitualService {
   private static final Logger LOG = LogUtils.getLogger();
   private static final int EXPECTED_ITEM_COUNT = 9;
   private static final String TAG_RITUAL_LIGHTNING = "ae2lt.research_ritual_lightning";
   private static final String TAG_RITUAL_IONIZER_POS = "ae2lt.research_ritual_ionizer_pos";
   private static final Comparator<ItemEntity> DROP_ORDER = Comparator.<ItemEntity>comparingInt(ItemEntity::m_32059_).reversed();

   private ResearchRitualService() {
   }

   public static void markRitualLightning(LightningBolt lightningBolt, BlockPos ionizerPos) {
      CompoundTag data = lightningBolt.getPersistentData();
      data.m_128379_("ae2lt.research_ritual_lightning", true);
      data.m_128356_("ae2lt.research_ritual_ionizer_pos", ionizerPos.m_121878_());
   }

   public static void handleLightning(ServerLevel level, LightningBolt lightningBolt) {
      CompoundTag data = lightningBolt.getPersistentData();
      if (data.m_128471_("ae2lt.research_ritual_lightning") && data.m_128441_("ae2lt.research_ritual_ionizer_pos")) {
         BlockPos ionizerPos = BlockPos.m_122022_(data.m_128454_("ae2lt.research_ritual_ionizer_pos"));
         if (level.m_7702_(ionizerPos) instanceof AtmosphericIonizerBlockEntity ionizer && ionizer.isInstalledInWorld()) {
            LOG.debug("[ae2lt/ritual] handleLightning: authenticated ionizer at {}", ionizerPos);
            tryHandleIonizer(level, ionizer);
            return;
         }

         LOG.debug("[ae2lt/ritual] handleLightning: marked bolt has no valid ionizer at {}", ionizerPos);
      }
   }

   private static void tryHandleIonizer(ServerLevel level, AtmosphericIonizerBlockEntity ionizer) {
      BlockPos ionizerPos = ionizer.m_58899_();
      AABB box = ritualSearchBox(ionizerPos);
      List<ItemEntity> scanned = level.m_6443_(ItemEntity.class, box, itemEntity -> itemEntity.m_6084_() && !itemEntity.m_32055_().m_41619_());
      LOG.debug("[ae2lt/ritual] tryHandleIonizer: ionizer={} box={} scanned={}", new Object[]{ionizerPos, box, scanned.size()});
      if (scanned.isEmpty()) {
         LOG.debug("[ae2lt/ritual] tryHandleIonizer: reaction zone empty, abort.");
      } else {
         ItemEntity anchorNote = scanned.stream()
            .filter(itemEntity -> ResearchNoteItem.isUsableGeneratedNote(itemEntity.m_32055_()))
            .max(Comparator.comparingInt(ItemEntity::m_32059_))
            .orElse(null);
         if (anchorNote == null) {
            LOG.debug("[ae2lt/ritual] tryHandleIonizer: no usable generated research note in zone (items={}).", scanned.size());
         } else {
            ResearchNoteData note = ResearchNoteItem.getData(anchorNote.m_32055_());
            if (note == null) {
               LOG.debug("[ae2lt/ritual] tryHandleIonizer: anchor note has no ResearchNoteData, abort.");
            } else {
               LOG.debug(
                  "[ae2lt/ritual] tryHandleIonizer: anchor age={} goal={} recipe={}", new Object[]{anchorNote.m_32059_(), note.goal(), note.recipeItems()}
               );
               List<ItemEntity> candidates = scanned.stream()
                  .filter(itemEntity -> itemEntity != anchorNote)
                  .filter(itemEntity -> itemEntity.m_32059_() <= anchorNote.m_32059_())
                  .sorted(DROP_ORDER)
                  .toList();
               if (candidates.size() != 9) {
                  LOG.debug(
                     "[ae2lt/ritual] tryHandleIonizer: candidate count {} != expected {} (ages: {})",
                     new Object[]{candidates.size(), 9, candidates.stream().map(ItemEntity::m_32059_).toList()}
                  );
               } else {
                  List<ResourceLocation> thrownSequence = candidates.stream()
                     .map(itemEntity -> BuiltInRegistries.f_257033_.m_7981_(itemEntity.m_32055_().m_41720_()))
                     .toList();
                  LOG.debug("[ae2lt/ritual] tryHandleIonizer: thrown sequence (oldest->newest) = {}", thrownSequence);
                  if (!sameMultiset(thrownSequence, note.recipeItems())) {
                     LOG.debug("[ae2lt/ritual] tryHandleIonizer: multiset mismatch, abort (expected={}, got={})", note.recipeItems(), thrownSequence);
                  } else {
                     if (matchesDropOrder(candidates, note.recipeItems())) {
                        LOG.info("[ae2lt/ritual] SUCCESS at ionizer={} goal={} (ordered recipe matched).", ionizerPos, note.goal());
                        succeed(level, anchorNote, note, candidates);
                     } else {
                        LOG.info(
                           "[ae2lt/ritual] FAIL at ionizer={} (items correct but order wrong). expected={} got={}",
                           new Object[]{ionizerPos, note.recipeItems(), thrownSequence}
                        );
                        fail(level, anchorNote, candidates);
                     }
                  }
               }
            }
         }
      }
   }

   private static void succeed(ServerLevel level, ItemEntity anchorNote, ResearchNoteData note, List<ItemEntity> candidates) {
      long gameTime = level.m_46467_();
      consumeParticipants(candidates);
      ItemStack noteStack = anchorNote.m_32055_().m_41777_();
      ResearchNoteItem.applyGeneratedState(noteStack, note.withConsumed(true));
      anchorNote.m_32045_(noteStack);
      ItemEntity reward = new RitualHyperdimensionalPigmeeEntity(
         level, anchorNote.m_20185_(), anchorNote.m_20186_() + 0.25, anchorNote.m_20189_(), createRewardStack()
      );
      reward.m_20256_(Vec3.f_82478_);
      ProtectedItemEntityHelper.applyOutputProtection(reward, gameTime);
      level.m_7967_(reward);
      level.m_8767_(ParticleTypes.f_175830_, anchorNote.m_20185_(), anchorNote.m_20186_() + 0.15, anchorNote.m_20189_(), 30, 0.45, 0.35, 0.45, 0.02);
      level.m_5594_(null, anchorNote.m_20183_(), SoundEvents.f_12089_, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   private static void fail(ServerLevel level, ItemEntity anchorNote, List<ItemEntity> candidates) {
      long gameTime = level.m_46467_();
      List<Vec3> positions = consumeParticipants(candidates);
      int fragmentCount = 1 + level.f_46441_.m_188503_(3);

      for (int i = 0; i < fragmentCount; i++) {
         ItemEntity fragment = new ItemEntity(
            level, anchorNote.m_20185_(), anchorNote.m_20186_() + 0.1, anchorNote.m_20189_(), new ItemStack((ItemLike)ModItems.CHARRED_RITUAL_FRAGMENT.get())
         );
         fragment.m_20334_((level.f_46441_.m_188500_() - 0.5) * 0.08, 0.05, (level.f_46441_.m_188500_() - 0.5) * 0.08);
         ProtectedItemEntityHelper.applyOutputProtection(fragment, gameTime);
         level.m_7967_(fragment);
      }

      for (Vec3 position : positions) {
         level.m_8767_(ParticleTypes.f_123762_, position.f_82479_, position.f_82480_ + 0.1, position.f_82481_, 12, 0.1, 0.05, 0.1, 0.01);
      }

      level.m_5594_(null, anchorNote.m_20183_(), SoundEvents.f_11937_, SoundSource.BLOCKS, 0.9F, 0.7F);
   }

   private static List<Vec3> consumeParticipants(List<ItemEntity> candidates) {
      List<Vec3> positions = new ArrayList<>(candidates.size());

      for (ItemEntity candidate : candidates) {
         positions.add(candidate.m_20182_());
         ItemStack stack = candidate.m_32055_().m_41777_();
         stack.m_41774_(1);
         if (stack.m_41619_()) {
            candidate.m_146870_();
         } else {
            candidate.m_32045_(stack);
         }
      }

      return positions;
   }

   private static ItemStack createRewardStack() {
      return new ItemStack((ItemLike)ModFumos.HYPERDIMENSIONAL_PIGMEE_FUMO_ITEM.get());
   }

   private static boolean sameMultiset(List<ResourceLocation> left, List<ResourceLocation> right) {
      if (left.size() != right.size()) {
         return false;
      } else {
         Map<ResourceLocation, Integer> counts = new HashMap<>();

         for (ResourceLocation id : left) {
            counts.merge(id, 1, Integer::sum);
         }

         for (ResourceLocation id : right) {
            Integer current = counts.get(id);
            if (current == null) {
               return false;
            }

            if (current <= 1) {
               counts.remove(id);
            } else {
               counts.put(id, current - 1);
            }
         }

         return counts.isEmpty();
      }
   }

   private static boolean matchesDropOrder(List<ItemEntity> orderedCandidates, List<ResourceLocation> expectedOrder) {
      int start = 0;

      while (start < orderedCandidates.size()) {
         int age = orderedCandidates.get(start).m_32059_();
         int end = start + 1;

         while (end < orderedCandidates.size() && orderedCandidates.get(end).m_32059_() == age) {
            end++;
         }

         List<ResourceLocation> actualTickGroup = orderedCandidates.subList(start, end)
            .stream()
            .map(itemEntity -> BuiltInRegistries.f_257033_.m_7981_(itemEntity.m_32055_().m_41720_()))
            .toList();
         if (!sameMultiset(actualTickGroup, expectedOrder.subList(start, end))) {
            return false;
         }

         start = end;
      }

      return true;
   }

   private static AABB ritualSearchBox(BlockPos ionizerPos) {
      return new AABB(
         (double)ionizerPos.m_123341_() - 1.0,
         (double)ionizerPos.m_123342_() + 1.0,
         (double)ionizerPos.m_123343_() - 1.0,
         (double)ionizerPos.m_123341_() + 2.0,
         (double)ionizerPos.m_123342_() + 5.0,
         (double)ionizerPos.m_123343_() + 2.0
      );
   }
}
