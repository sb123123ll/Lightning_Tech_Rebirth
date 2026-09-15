package com.moakiee.ae2lt.machine.firmament.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FirmamentConversionLockedRecipe {
   private static final String TAG_RECIPE_ID = "RecipeId";
   private static final String TAG_RESULT = "Result";
   private static final String TAG_RESULTS = "Results";
   private static final String TAG_PROCESS_TIME = "ProcessTime";
   private static final String TAG_INPUTS = "InputConsumptions";
   private final ResourceLocation recipeId;
   private final List<ItemStack> results;
   private final int processTime;
   private final int[] inputConsumptions;

   public FirmamentConversionLockedRecipe(ResourceLocation recipeId, List<ItemStack> results, int processTime, int[] inputConsumptions) {
      this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
      Objects.requireNonNull(results, "results");
      this.processTime = processTime;
      if (results.isEmpty() || results.size() > 4) {
         throw new IllegalArgumentException("results must contain 1 to 4 entries");
      } else if (results.stream().anyMatch(ItemStack::m_41619_)) {
         throw new IllegalArgumentException("results cannot contain empty stacks");
      } else if (processTime <= 0) {
         throw new IllegalArgumentException("processTime must be positive");
      } else if (inputConsumptions.length != 3) {
         throw new IllegalArgumentException("inputConsumptions must have length 3");
      } else {
         this.results = results.stream().<ItemStack>map(ItemStack::m_41777_).toList();
         this.inputConsumptions = Arrays.copyOf(inputConsumptions, inputConsumptions.length);
      }
   }

   public FirmamentConversionLockedRecipe(ResourceLocation recipeId, ItemStack result, int processTime, int[] inputConsumptions) {
      this(recipeId, List.of(result), processTime, inputConsumptions);
   }

   public static FirmamentConversionLockedRecipe fromCandidate(FirmamentConversionRecipeCandidate candidate) {
      return new FirmamentConversionLockedRecipe(
         candidate.recipeId(), candidate.recipe().getResultStacks(), candidate.recipe().processTime(), candidate.match().inputConsumptions()
      );
   }

   public ResourceLocation recipeId() {
      return this.recipeId;
   }

   public ItemStack result() {
      return this.results.get(0).m_41777_();
   }

   public List<ItemStack> results() {
      return this.results.stream().<ItemStack>map(ItemStack::m_41777_).toList();
   }

   public int processTime() {
      return this.processTime;
   }

   public int inputConsumptionForSlot(int slot) {
      if (slot >= 0 && slot <= 2) {
         return this.inputConsumptions[slot];
      } else {
         throw new IllegalArgumentException("slot must be one of the three input slots");
      }
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("RecipeId", this.recipeId.toString());
      ListTag resultTags = new ListTag();

      for (ItemStack result : this.results) {
         resultTags.add(result.m_41739_(new CompoundTag()));
      }

      tag.m_128365_("Results", resultTags);
      tag.m_128405_("ProcessTime", this.processTime);
      tag.m_128365_("InputConsumptions", new IntArrayTag(Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length)));
      return tag;
   }

   @Nullable
   public static FirmamentConversionLockedRecipe fromTag(CompoundTag tag) {
      if (!tag.m_128441_("RecipeId")) {
         return null;
      } else {
         List<ItemStack> results = readResults(tag);
         if (results.isEmpty()) {
            return null;
         } else {
            int processTime = tag.m_128451_("ProcessTime");
            int[] inputConsumptions = tag.m_128465_("InputConsumptions");
            return processTime > 0 && inputConsumptions.length == 3
               ? new FirmamentConversionLockedRecipe(ResourceLocation.m_135820_(tag.m_128461_("RecipeId")), results, processTime, inputConsumptions)
               : null;
         }
      }
   }

   private static List<ItemStack> readResults(CompoundTag tag) {
      if (tag.m_128425_("Results", 9)) {
         ListTag resultTags = tag.m_128437_("Results", 10);
         if (!resultTags.isEmpty() && resultTags.size() <= 4) {
            List<ItemStack> results = new ArrayList<>(resultTags.size());

            for (int index = 0; index < resultTags.size(); index++) {
               ItemStack result = ItemStack.m_41712_(resultTags.m_128728_(index));
               if (result.m_41619_()) {
                  return List.of();
               }

               results.add(result);
            }

            return List.copyOf(results);
         } else {
            return List.of();
         }
      } else if (tag.m_128425_("Result", 10)) {
         ItemStack result = ItemStack.m_41712_(tag.m_128469_("Result"));
         return result.m_41619_() ? List.of() : List.of(result);
      } else {
         return List.of();
      }
   }
}
