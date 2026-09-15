package com.moakiee.ae2lt.machine.crystalcatalyzer.recipe;

import com.moakiee.ae2lt.me.key.LightningKey;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CrystalCatalyzerLockedRecipe {
   private static final String TAG_RECIPE_ID = "RecipeId";
   private static final String TAG_OUTPUT = "Output";
   private static final String TAG_ENERGY = "Energy";
   private static final String TAG_OUTPUT_MULTIPLIER = "OutputMultiplier";
   private static final String TAG_LIGHTNING_COST = "LightningCost";
   private static final String TAG_LIGHTNING_TIER = "LightningTier";
   private final ResourceLocation recipeId;
   private final ItemStack output;
   private final int energyPerCycle;
   private final int outputMultiplier;
   private final int lightningCost;
   private final LightningKey.Tier lightningTier;

   public CrystalCatalyzerLockedRecipe(
      ResourceLocation recipeId, ItemStack output, int energyPerCycle, int outputMultiplier, int lightningCost, LightningKey.Tier lightningTier
   ) {
      this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
      this.output = Objects.requireNonNull(output, "output").m_41777_();
      this.energyPerCycle = energyPerCycle;
      this.outputMultiplier = outputMultiplier;
      this.lightningCost = lightningCost;
      this.lightningTier = Objects.requireNonNull(lightningTier, "lightningTier");
      if (output.m_41619_()) {
         throw new IllegalArgumentException("output cannot be empty");
      } else if (energyPerCycle <= 0) {
         throw new IllegalArgumentException("energyPerCycle must be positive");
      } else if (outputMultiplier <= 0) {
         throw new IllegalArgumentException("outputMultiplier must be positive");
      } else if (lightningCost < 1) {
         throw new IllegalArgumentException("lightningCost must be positive");
      }
   }

   public static CrystalCatalyzerLockedRecipe fromCandidate(CrystalCatalyzerRecipeCandidate candidate, int outputMultiplier) {
      CrystalCatalyzerRecipe recipe = candidate.recipe();
      return new CrystalCatalyzerLockedRecipe(
         recipe.m_6423_(), recipe.getOutputTemplate(), recipe.energyPerCycle(), outputMultiplier, recipe.lightningCost(), recipe.lightningTier()
      );
   }

   public ResourceLocation recipeId() {
      return this.recipeId;
   }

   public ItemStack output() {
      return this.output.m_41777_();
   }

   public int energyPerCycle() {
      return this.energyPerCycle;
   }

   public int outputMultiplier() {
      return this.outputMultiplier;
   }

   public int lightningCost() {
      return this.lightningCost;
   }

   public LightningKey.Tier lightningTier() {
      return this.lightningTier;
   }

   public long totalEnergy() {
      return (long)this.energyPerCycle;
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.m_128359_("RecipeId", this.recipeId.toString());
      tag.m_128365_("Output", this.output.m_41739_(new CompoundTag()));
      tag.m_128405_("Energy", this.energyPerCycle);
      tag.m_128405_("OutputMultiplier", this.outputMultiplier);
      tag.m_128405_("LightningCost", this.lightningCost);
      tag.m_128359_("LightningTier", this.lightningTier.m_7912_());
      return tag;
   }

   @Nullable
   public static CrystalCatalyzerLockedRecipe fromTag(CompoundTag tag) {
      return fromTag(tag, 1);
   }

   @Nullable
   public static CrystalCatalyzerLockedRecipe fromTag(CompoundTag tag, int defaultOutputMultiplier) {
      if (tag.m_128441_("RecipeId") && tag.m_128425_("Output", 10)) {
         ItemStack output = ItemStack.m_41712_(tag.m_128469_("Output"));
         if (output.m_41619_()) {
            return null;
         } else {
            int energy = tag.m_128451_("Energy");
            if (energy <= 0) {
               return null;
            } else {
               int outputMultiplier = tag.m_128425_("OutputMultiplier", 3) ? tag.m_128451_("OutputMultiplier") : defaultOutputMultiplier;
               if (outputMultiplier <= 0) {
                  return null;
               } else {
                  int lightningCost = tag.m_128425_("LightningCost", 99) ? tag.m_128451_("LightningCost") : 1;
                  if (lightningCost < 1) {
                     lightningCost = 1;
                  }

                  LightningKey.Tier lightningTier = tag.m_128425_("LightningTier", 8)
                     ? LightningKey.Tier.fromSerializedName(tag.m_128461_("LightningTier"))
                     : CrystalCatalyzerRecipe.DEFAULT_LIGHTNING_TIER;
                  return new CrystalCatalyzerLockedRecipe(
                     ResourceLocation.m_135820_(tag.m_128461_("RecipeId")), output, energy, outputMultiplier, lightningCost, lightningTier
                  );
               }
            }
         }
      } else {
         return null;
      }
   }
}
