package com.moakiee.ae2lt.logic.research;

import com.moakiee.ae2lt.config.AE2LTCommonConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;

public final class ResearchNoteGenerator {
   private static final int RANDOM_ITEM_COUNT = 6;
   private static final long SALT_RESEARCH_NOTE = 5956243336164843933L;
   public static final List<ResourceLocation> FIXED_RECIPE_ITEMS = List.of(item("pigmee_core"), item("module_undying"), item("module_phase_lock"));

   private ResearchNoteGenerator() {
   }

   public static boolean hasValidPool() {
      return configuredAvailableCandidates().size() >= 6;
   }

   public static ResearchNoteData generate(ServerLevel level) {
      UUID ritualSeed = UUID.randomUUID();
      RandomSource random = RandomSource.m_216335_(mixSeed(ritualSeed, level.m_7654_().m_129783_().m_7328_()));
      List<ResearchNoteGenerator.SelectedItem> selected = new ArrayList<>(9);

      for (ResourceLocation fixed : FIXED_RECIPE_ITEMS) {
         selected.add(new ResearchNoteGenerator.SelectedItem(fixed, itemTranslationKey(fixed)));
      }

      List<ResearchNoteGenerator.SelectedItem> randomSelected = new ArrayList<>(6);

      for (ResearchNoteGenerator.Candidate candidate : weightedPickAvailable(6, random)) {
         randomSelected.add(new ResearchNoteGenerator.SelectedItem(candidate.id(), candidate.pickDescriptionKey(random)));
      }

      if (randomSelected.size() != 6) {
         throw new IllegalStateException("Research note random pool has fewer than six available entries.");
      } else {
         shuffle(randomSelected, random);
         selected.addAll(randomSelected);
         return new ResearchNoteData(
            ritualSeed,
            RitualGoal.HYPERDIMENSIONAL_PIGMEE,
            selected.stream().map(ResearchNoteGenerator.SelectedItem::id).toList(),
            selected.stream().map(ResearchNoteGenerator.SelectedItem::descriptionKey).toList(),
            false
         );
      }
   }

   private static List<ResearchNoteGenerator.Candidate> weightedPickAvailable(int count, RandomSource random) {
      List<ResearchNoteGenerator.Candidate> pool = new ArrayList<>(configuredAvailableCandidates());
      List<ResearchNoteGenerator.Candidate> picked = new ArrayList<>(count);

      while (picked.size() < count && !pool.isEmpty()) {
         long totalWeight = pool.stream().mapToLong(ResearchNoteGenerator.Candidate::weight).sum();
         long roll = Math.floorMod(random.m_188505_(), totalWeight);
         long cursor = 0L;

         for (int i = 0; i < pool.size(); i++) {
            ResearchNoteGenerator.Candidate candidate = pool.get(i);
            cursor += (long)candidate.weight();
            if (roll < cursor) {
               picked.add(candidate);
               pool.remove(i);
               break;
            }
         }
      }

      return picked;
   }

   private static List<ResearchNoteGenerator.Candidate> configuredAvailableCandidates() {
      return AE2LTCommonConfig.easterEggWeights()
         .entrySet()
         .stream()
         .map(entry -> new ResearchNoteGenerator.Candidate(entry.getKey(), entry.getValue()))
         .filter(ResearchNoteGenerator::isAvailable)
         .toList();
   }

   private static boolean isAvailable(ResearchNoteGenerator.Candidate candidate) {
      return BuiltInRegistries.f_257033_.m_6612_(candidate.id()).filter(item -> item != Items.f_41852_).isPresent();
   }

   private static void shuffle(List<ResearchNoteGenerator.SelectedItem> selected, RandomSource random) {
      for (int i = selected.size() - 1; i > 0; i--) {
         int other = random.m_188503_(i + 1);
         ResearchNoteGenerator.SelectedItem temporary = selected.get(i);
         selected.set(i, selected.get(other));
         selected.set(other, temporary);
      }
   }

   private static String itemTranslationKey(ResourceLocation id) {
      return "item." + id.m_135827_() + "." + id.m_135815_().replace('/', '.');
   }

   private static long mixSeed(UUID ritualSeed, long worldSeed) {
      return ritualSeed.getMostSignificantBits() ^ ritualSeed.getLeastSignificantBits() ^ Long.rotateLeft(worldSeed, 17) ^ 5956243336164843933L;
   }

   private static ResourceLocation item(String path) {
      return new ResourceLocation("ae2lt", path);
   }

   private static record Candidate(ResourceLocation id, int weight) {
      private String pickDescriptionKey(RandomSource random) {
         return !AE2LTCommonConfig.isDefaultEasterEggCandidate(this.id)
            ? ResearchNoteGenerator.itemTranslationKey(this.id)
            : "ae2lt.research_note.desc." + this.id.m_135827_() + "." + this.id.m_135815_().replace('/', '.') + "." + random.m_188503_(2);
      }
   }

   private static record SelectedItem(ResourceLocation id, String descriptionKey) {
   }
}
