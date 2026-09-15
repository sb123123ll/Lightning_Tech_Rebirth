package com.moakiee.ae2lt.machine.overloadfactory.recipe;

import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class OverloadProcessingLockedRecipe {
   private static final String TAG_RECIPE_ID = "RecipeId";
   private static final String TAG_TOTAL_ENERGY = "TotalEnergy";
   private static final String TAG_TOTAL_LIGHTNING_COST = "TotalLightningCost";
   private static final String TAG_LIGHTNING_TIER = "LightningTier";
   private static final String TAG_PARALLEL = "Parallel";
   private static final String TAG_INPUTS = "InputConsumptions";
   private final ResourceLocation recipeId;
   private final long totalEnergy;
   private final long totalLightningCost;
   private final LightningKey.Tier lightningTier;
   private final int parallel;
   private final int[] inputConsumptions;

   public OverloadProcessingLockedRecipe(
      ResourceLocation recipeId, long totalEnergy, long totalLightningCost, LightningKey.Tier lightningTier, int parallel, int[] inputConsumptions
   ) {
      this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
      this.totalEnergy = totalEnergy;
      this.totalLightningCost = totalLightningCost;
      this.lightningTier = Objects.requireNonNull(lightningTier, "lightningTier");
      int maxParallel = OverloadProcessingFactoryInventory.getMaxParallel();
      if (parallel <= 0 || parallel > maxParallel) {
         throw new IllegalArgumentException("parallel must be in range 1.." + maxParallel);
      } else if (totalEnergy <= 0L) {
         throw new IllegalArgumentException("totalEnergy must be positive");
      } else if (totalLightningCost <= 0L) {
         throw new IllegalArgumentException("totalLightningCost must be positive");
      } else if (inputConsumptions.length != 9) {
         throw new IllegalArgumentException("inputConsumptions must have length 9");
      } else {
         this.parallel = parallel;
         this.inputConsumptions = Arrays.copyOf(inputConsumptions, inputConsumptions.length);
      }
   }

   public static OverloadProcessingLockedRecipe fromCandidate(OverloadProcessingRecipeCandidate candidate) {
      OverloadProcessingRecipe recipe = candidate.recipe();
      return new OverloadProcessingLockedRecipe(
         recipe.m_6423_(),
         candidate.totalEnergy(),
         candidate.totalLightningCost(),
         recipe.lightningTier(),
         candidate.parallel(),
         candidate.match().inputConsumptions()
      );
   }

   public ResourceLocation recipeId() {
      return this.recipeId;
   }

   public long totalEnergy() {
      return this.totalEnergy;
   }

   public long totalLightningCost() {
      return this.totalLightningCost;
   }

   public LightningKey.Tier lightningTier() {
      return this.lightningTier;
   }

   public int parallel() {
      return this.parallel;
   }

   public int inputConsumptionForSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         return this.inputConsumptions[slot];
      } else {
         throw new IllegalArgumentException("slot must be an input slot");
      }
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("RecipeId", this.recipeId.toString());
      tag.m_128356_("TotalEnergy", this.totalEnergy);
      tag.m_128356_("TotalLightningCost", this.totalLightningCost);
      tag.m_128359_("LightningTier", this.lightningTier.m_7912_());
      tag.m_128405_("Parallel", this.parallel);
      tag.m_128365_("InputConsumptions", new IntArrayTag(Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length)));
      return tag;
   }

   @Nullable
   public static OverloadProcessingLockedRecipe fromTag(CompoundTag tag) {
      if (!tag.m_128425_("RecipeId", 8)) {
         return null;
      } else {
         long totalEnergy = tag.m_128454_("TotalEnergy");
         long totalLightningCost = tag.m_128454_("TotalLightningCost");
         int parallel = tag.m_128451_("Parallel");
         int[] inputConsumptions = tag.m_128465_("InputConsumptions");
         if (inputConsumptions.length != 9) {
            return null;
         } else if (totalEnergy > 0L && totalLightningCost > 0L && parallel > 0) {
            LightningKey.Tier lightningTier = tag.m_128425_("LightningTier", 8)
               ? LightningKey.Tier.fromSerializedName(tag.m_128461_("LightningTier"))
               : OverloadProcessingRecipe.DEFAULT_LIGHTNING_TIER;
            return new OverloadProcessingLockedRecipe(
               ResourceLocation.m_135820_(tag.m_128461_("RecipeId")), totalEnergy, totalLightningCost, lightningTier, parallel, inputConsumptions
            );
         } else {
            return null;
         }
      }
   }
}
